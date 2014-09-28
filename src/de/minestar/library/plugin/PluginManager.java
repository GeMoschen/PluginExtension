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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import de.minestar.library.plugin.annotations.Plugin;
import de.minestar.library.plugin.exceptions.PluginExistsException;

public class PluginManager {

    public final Map<String, Class<?>> registeredClasses;
    public Map<String, AbstractPlugin> loadedPlugins;

    public PluginManager(File folder) throws IOException {
        this(folder, false);
    }

    public PluginManager(File folder, boolean enablePlugins) throws IOException {
        this.registeredClasses = this.searchPlugins(folder);
        this.loadedPlugins = new HashMap<String, AbstractPlugin>();
        if (enablePlugins) {
            this.enablePlugins();
        }
    }

    public Map<String, Class<?>> getRegisteredClasses() {
        return Collections.unmodifiableMap(this.registeredClasses);
    }

    public Map<String, AbstractPlugin> getLoadedPlugins() {
        return Collections.unmodifiableMap(this.loadedPlugins);
    }

    private Map<String, Class<?>> searchPlugins(File folder) throws IOException {
        // create new set
        Map<String, Class<?>> map = new TreeMap<String, Class<?>>();

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
                                map.put(clazz.getSimpleName(), clazz);
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
        return Collections.unmodifiableMap(map);
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

    public void enablePlugins() {
        // unload first
        this.disablePlugins();

        // enable all plugins
        for (Class<?> clazz : this.registeredClasses.values()) {
            this.enablePlugin(clazz);
        }
    }

    public boolean enablePlugin(String name) {
        return this.enablePlugin(this.registeredClasses.get(name));
    }

    public boolean enablePlugin(Class<?> clazz) {
        // catch null
        if (clazz == null) {
            return false;
        }
        try {
            // create "AbstractPlugin"
            AbstractPlugin plugin = new AbstractPlugin(this, clazz);

            // ignore plugins with the same name
            if (this.loadedPlugins.containsKey(plugin.getName())) {
                throw new PluginExistsException("A plugin named '" + plugin.getName() + "' already exists!");
            }
            if (plugin.enable()) {
                this.loadedPlugins.put(plugin.getName(), plugin);
                System.out.println("Plugin enabled: " + plugin.getName() + " [ v" + plugin.getVersion() + " ]!");
                return true;
            }
            System.out.println("Plugin not enabled: " + plugin.getName() + " [ v" + plugin.getVersion() + " ]!");
            return false;
        } catch (PluginExistsException print) {
            print.printStackTrace();
        } catch (InstantiationException print) {
            print.printStackTrace();
        } catch (IllegalAccessException print) {
            print.printStackTrace();
        }
        System.out.println("Plugin not enabled: " + clazz.getSimpleName() + "!");
        return false;
    }

    public boolean disablePlugin(String name) {
        // fetch "AbstractPlugin"
        AbstractPlugin plugin = this.loadedPlugins.get(name);
        if (plugin == null) {
            return false;
        }

        // disable plugin
        if (plugin.disable()) {
            this.loadedPlugins.remove(plugin.getName());
            System.out.println("Plugin disabled: " + plugin.getName() + " [ v" + plugin.getVersion() + " ]!");
            return true;
        }
        System.out.println("Plugin not disabled: " + plugin.getName() + " [ v" + plugin.getVersion() + " ]!");
        return false;
    }

    public void disablePlugins() {
        // disable all plugins
        for (AbstractPlugin plugin : this.loadedPlugins.values()) {
            if (plugin.disable()) {
                System.out.println("Plugin disabled: " + plugin.getName() + " [ v" + plugin.getVersion() + " ]!");
            } else {
                System.out.println("Plugin not disabled: " + plugin.getName() + " [ v" + plugin.getVersion() + " ]!");
            }
        }

        // clear list
        this.loadedPlugins.clear();
    }
}
