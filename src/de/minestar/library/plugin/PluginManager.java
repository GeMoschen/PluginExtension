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
                                PluginDefinition pluginDefinition = PluginDefinition.createPlugin(this, (Class<? extends ExternalPlugin>) clazz);
                                if (pluginDefinition != null) {
                                    map.put(pluginDefinition.getName(), pluginDefinition);
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

            // check class and "Plugin"-Annotation
            if (ExternalPlugin.class.isAssignableFrom(clazz) && this.hasAnnotation(clazz, Plugin.class)) {
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
        return this.hasPlugin(name) && this.getLoadedPlugin(name).isEnabled();
    }

    public boolean hasPlugin(String name) {
        return this.loadedPlugins.containsKey(name);
    }

    public <T> T getPlugin(Class<? extends ExternalPlugin> clazz) {
        PluginDefinition pluginDefinition = this.enabledPlugins.get(clazz.getSimpleName());
        if (pluginDefinition != null) {
            return pluginDefinition.getInstance(clazz);
        }
        return null;
    }

    protected PluginDefinition getLoadedPlugin(String name) {
        return this.loadedPlugins.get(name);
    }

    private void enablePlugin(PluginDefinition pluginDefinition) {
        // catch null
        if (pluginDefinition == null) {
            return;
        }

        try {
            // check for enabled plugins with the same name
            if (this.enabledPlugins.containsKey(pluginDefinition.getName())) {
                // if the name is the same, but the objects are different =>
                // throw an exception
                if (!this.enabledPlugins.get(pluginDefinition.getName()).equals(pluginDefinition)) {
                    throw new PluginExistsException("A different plugin named '" + pluginDefinition.getName() + "' already exists!");
                } else {
                    return;
                }
            }

            // enable plugin, only if it is disabled
            if (!pluginDefinition.isEnabled()) {
                if (pluginDefinition.enable()) {
                    this.enabledPlugins.put(pluginDefinition.getName(), pluginDefinition);
                    System.out.println("Plugin enabled: " + pluginDefinition.getName() + " [ v" + pluginDefinition.getVersion() + " ]!");
                } else {
                    System.err.println("Plugin not enabled: " + pluginDefinition.getName() + " [ v" + pluginDefinition.getVersion() + " ]!");
                }
            }
        } catch (PluginExistsException print) {
            print.printStackTrace();
        }
    }

    public void enablePlugins() {
        // unload first
        this.disablePlugins();

        // check circular dependencies
        this.checkForCircularDependencies();

        // check missing dependencies
        this.checkForMissingDependencies();

        // enable all plugins
        for (PluginDefinition pluginDefinition : this.loadedPlugins.values()) {
            this.enablePlugin(pluginDefinition);
        }

        // call postEnable-methods
        for (PluginDefinition pluginDefinition : this.enabledPlugins.values()) {
            pluginDefinition.callPostEnableMethods();
        }

        // call afterInitialization-Methods
        for (PluginDefinition pluginDefinition : this.enabledPlugins.values()) {
            pluginDefinition.afterInitializationCalls();
        }

    }

    private void checkForMissingDependencies() {
        // check for missing dependencies
        boolean missingCheckCompleted = false;
        while (!missingCheckCompleted) {
            missingCheckCompleted = true;
            try {
                for (PluginDefinition pluginDefinition : this.loadedPlugins.values()) {
                    pluginDefinition.updateDependencies();
                }
            } catch (MissingDependencyException print) {
                // print the error
                System.err.println(print.getMessage());

                // remove the plugin
                this.loadedPlugins.remove(print.getPluginDefinition().getName());

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
                for (PluginDefinition pluginDefinition : this.loadedPlugins.values()) {
                    pluginDefinition.checkForCircularDependencies(pluginDefinition, new ArrayList<PluginDefinition>());
                }
            } catch (CircularDependencyException print) {
                // print the error
                System.err.println(print.getMessage());

                // remove the plugins
                this.loadedPlugins.remove(print.getFirstPluginDefinition().getName());
                this.loadedPlugins.remove(print.getSecondPluginDefinition().getName());

                // reset "circularCheckCompleted"
                circularCheckCompleted = false;
            }
        }
    }

    public void disablePlugins() {
        // call preDisable-methods
        for (PluginDefinition pluginDefinition : this.enabledPlugins.values()) {
            pluginDefinition.callPreDisableMethods();
        }

        // disable all plugins
        for (PluginDefinition pluginDefinition : this.enabledPlugins.values()) {
            if (pluginDefinition.disable()) {
                System.out.println("Plugin disabled: " + pluginDefinition.getName() + " [ v" + pluginDefinition.getVersion() + " ]!");
            } else {
                System.err.println("Plugin not disabled: " + pluginDefinition.getName() + " [ v" + pluginDefinition.getVersion() + " ]!");
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
        for (PluginDefinition pluginDefinition : this.loadedPlugins.values()) {
            System.out.println("-> " + pluginDefinition.getName() + " [ v" + pluginDefinition.getVersion() + " ]");
        }
    }

}
