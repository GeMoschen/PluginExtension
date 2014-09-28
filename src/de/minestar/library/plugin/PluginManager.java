/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2014 Minestar.de
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.minestar.library.plugin;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import de.minestar.library.plugin.annotations.ExternalPlugin;
import de.minestar.library.plugin.annotations.Plugin;
import de.minestar.library.plugin.exceptions.CircularDependencyException;
import de.minestar.library.plugin.exceptions.MissingDependencyException;
import de.minestar.library.plugin.exceptions.PluginExistsException;

public class PluginManager {

    private final File pluginFolder;
    private final Map<String, PluginDefinition> loadedPlugins, enabledPlugins;

    public PluginManager(File pluginFolder) throws IOException {
        this(pluginFolder, false);
    }

    public PluginManager(File pluginFolder, boolean enablePlugins) throws IOException {
        this.pluginFolder = pluginFolder;
        this.loadedPlugins = this.loadPlugins(pluginFolder);
        this.enabledPlugins = new HashMap<String, PluginDefinition>();
        if (enablePlugins) {
            this.enablePlugins();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, PluginDefinition> loadPlugins(File folder) throws IOException {
        // create new set
        Map<String, PluginDefinition> map = new HashMap<String, PluginDefinition>();

        // folder must exist
        if (folder.exists()) {
            File[] files = folder.listFiles();
            for (File file : files) {
                // directories and files without ".jar"-ending are ignored
                if (file.isDirectory() || !file.getName().endsWith(".jar")) {
                    continue;
                }

                // open JAR
                JarFile jarFile = new JarFile(file);
                Enumeration<JarEntry> e = jarFile.entries();

                // create URL and classloader
                URL[] urls = { new URL("jar:file:" + file.getAbsolutePath() + "!/") };
                URLClassLoader cl = URLClassLoader.newInstance(urls);

                // iterate over elements...
                Class<?> clazz;
                while (e.hasMoreElements()) {
                    try {
                        // get the entry
                        clazz = this.processJarEntry(cl, (JarEntry) e.nextElement());
                        if (clazz != null) {
                            if (!map.containsKey(clazz.getSimpleName())) {
                                // create "AbstractPlugin"
                                PluginDefinition plugin = PluginDefinition.createPlugin(this, (Class<? extends ExternalPlugin>) clazz);
                                if (plugin != null) {
                                    map.put(plugin.getName(), plugin);
                                }
                            } else {
                                throw new PluginExistsException("A plugin named '" + clazz.getSimpleName() + "' already exists!");
                            }
                        }
                    } catch (PluginExistsException print) {
                        print.printStackTrace();
                    }
                }

                // close JAR
                jarFile.close();
            }
        }
        return map;
    }

    private Class<?> processJarEntry(URLClassLoader cl, JarEntry jarEntry) {
        // directories and files without ".class"-ending are ignored
        if (jarEntry.isDirectory() || !jarEntry.getName().endsWith(".class")) {
            return null;
        }

        // -6, because of .class
        String className = jarEntry.getName().substring(0, jarEntry.getName().length() - 6);
        className = className.replace('/', '.');

        try {
            // load the class
            Class<?> clazz = cl.loadClass(className);

            // search for "Plugin"-Annotation
            if (this.hasAnnotation(clazz, Plugin.class)) {
                return clazz;
            }
        } catch (ClassNotFoundException print) {
            print.printStackTrace();
        }
        return null;
    }

    private boolean hasAnnotation(Class<?> clazz, Class<?> annotationToSearch) {
        // fetch annotations
        Annotation[] annotations = clazz.getDeclaredAnnotations();
        for (Annotation annotation : annotations) {
            // if the annotation is inherited, return true
            if (annotationToSearch.isAssignableFrom(annotation.getClass())) {
                return true;
            }
        }
        return false;
    }

    public boolean isPluginEnabled(String name) {
        return this.hasPlugin(name) && this.getPlugin(name).isEnabled();
    }

    protected boolean hasPlugin(String name) {
        return this.loadedPlugins.containsKey(name);
    }

    public <T> T getPlugin(Class<? extends ExternalPlugin> clazz) {
        PluginDefinition plugin = this.enabledPlugins.get(clazz.getSimpleName());
        if (plugin != null) {
            return plugin.getInstance(clazz);
        }
        return null;
    }

    protected PluginDefinition getPlugin(String name) {
        return this.loadedPlugins.get(name);
    }

    private void enablePlugin(PluginDefinition plugin) {
        // catch null
        if (plugin == null) {
            return;
        }

        try {
            // check for enabled plugins with the same name
            if (this.enabledPlugins.containsKey(plugin.getName())) {
                // if the name is the same, but the objects are different =>
                // throw an exception
                if (!this.enabledPlugins.get(plugin.getName()).equals(plugin)) {
                    throw new PluginExistsException("A different plugin named '" + plugin.getName() + "' already exists!");
                } else {
                    return;
                }
            }

            // enable plugin, only if it is disabled
            if (!plugin.isEnabled()) {
                if (plugin.enable()) {
                    this.enabledPlugins.put(plugin.getName(), plugin);
                    System.out.println("Plugin enabled: " + plugin.getName() + " [ v" + plugin.getVersion() + " ]!");
                } else {
                    System.out.println("Plugin not enabled: " + plugin.getName() + " [ v" + plugin.getVersion() + " ]!");
                }
            }
        } catch (PluginExistsException print) {
            print.printStackTrace();
        }
    }

    public void enablePlugins() {
        // unload first
        this.disablePlugins();

        // check missing dependencies
        checkForMissingDependencies();

        // check circular dependencies
        checkForCircularDependencies();

        // enable all plugins
        for (PluginDefinition plugin : this.loadedPlugins.values()) {
            this.enablePlugin(plugin);
        }

        // call postEnable-methods
        for (PluginDefinition plugin : this.enabledPlugins.values()) {
            plugin.onPostEnable();
        }
    }

    private void checkForMissingDependencies() {
        // check for missing dependencies
        boolean missingCheckCompleted = false;
        while (!missingCheckCompleted) {
            missingCheckCompleted = true;
            try {
                for (PluginDefinition plugin : this.loadedPlugins.values()) {
                    plugin.updateDependencies();
                }
            } catch (MissingDependencyException print) {
                // print the error
                System.err.println(print.getMessage());

                // remove the plugin
                this.loadedPlugins.remove(print.getPlugin().getName());

                // reset "missingCheckCompleted"
                missingCheckCompleted = false;
            }
        }
    }

    private void checkForCircularDependencies() {
        // check for circular dependencies
        boolean circularCheckCompleted = false;
        while (!circularCheckCompleted) {
            circularCheckCompleted = true;
            try {
                for (PluginDefinition plugin : this.loadedPlugins.values()) {
                    plugin.checkForCircularDependencies(plugin, new ArrayList<PluginDefinition>());
                }
            } catch (CircularDependencyException print) {
                // print the error
                System.err.println(print.getMessage());

                // remove the plugins
                this.loadedPlugins.remove(print.getPluginOne().getName());
                this.loadedPlugins.remove(print.getPluginTwo().getName());

                // reset "circularCheckCompleted"
                circularCheckCompleted = false;
            }
        }
    }

    protected boolean disablePlugin(String name) {
        // fetch "AbstractPlugin"
        PluginDefinition plugin = this.enabledPlugins.get(name);
        if (plugin == null) {
            return false;
        }

        // disable plugin
        if (plugin.disable()) {
            this.enabledPlugins.remove(plugin.getName());
            System.out.println("Plugin disabled: " + plugin.getName() + " [ v" + plugin.getVersion() + " ]!");
            return true;
        }
        System.out.println("Plugin not disabled: " + plugin.getName() + " [ v" + plugin.getVersion() + " ]!");
        return false;
    }

    public void disablePlugins() {
        // call preDisable-methods
        for (PluginDefinition plugin : this.enabledPlugins.values()) {
            plugin.onPreDisable();
        }

        // disable all plugins
        for (PluginDefinition plugin : this.enabledPlugins.values()) {
            if (plugin.disable()) {
                System.out.println("Plugin disabled: " + plugin.getName() + " [ v" + plugin.getVersion() + " ]!");
            } else {
                System.out.println("Plugin not disabled: " + plugin.getName() + " [ v" + plugin.getVersion() + " ]!");
            }
        }

        // clear list
        this.enabledPlugins.clear();
    }

    public void reloadLoadedPlugins() {
        // disable plugins
        this.disablePlugins();

        // enable plugins
        this.enablePlugins();
    }

    public void reloadAllPlugins() throws IOException {
        // disable plugins
        this.disablePlugins();

        // clear old maps
        this.loadedPlugins.clear();

        // search plugins
        this.loadedPlugins.putAll(this.loadPlugins(this.pluginFolder));

        // enable plugins
        this.enablePlugins();
    }

    public void listPlugins() {
        System.out.println("Plugins found: " + this.loadedPlugins.size());
        for (PluginDefinition plugin : this.loadedPlugins.values()) {
            System.out.println("-> " + plugin.getName() + " [ v" + plugin.getVersion() + " ]");
        }
    }

}
