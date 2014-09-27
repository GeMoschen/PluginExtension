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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import de.minestar.library.plugin.annotations.Plugin;
import de.minestar.library.plugin.exceptions.PluginExistsException;

public class PluginManager {

    public final Set<Class<?>> registeredClasses;
    public Map<String, AbstractPlugin> loadedPlugins;

    public PluginManager(File folder) throws IOException {
        this.registeredClasses = this.searchPlugins(folder);
        this.loadedPlugins = new HashMap<String, AbstractPlugin>();
    }

    public Set<Class<?>> getRegisteredClasses() {
        return Collections.unmodifiableSet(this.registeredClasses);
    }

    public Map<String, AbstractPlugin> getLoadedPlugins() {
        return Collections.unmodifiableMap(this.loadedPlugins);
    }

    private Set<Class<?>> searchPlugins(File folder) throws IOException {
        // create new set
        Set<Class<?>> set = new HashSet<Class<?>>();

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
                    // get the entry
                    clazz = this.processJarEntry(cl, (JarEntry) e.nextElement());
                    if (clazz != null) {
                        set.add(clazz);
                    }
                }

                // close JAR
                jarFile.close();
            }
        }
        return Collections.unmodifiableSet(set);
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

    public void loadAllPlugins() {
        // unload first
        this.unloadAllPlugins();

        // enable all plugins
        for (Class<?> clazz : this.registeredClasses) {
            AbstractPlugin plugin = new AbstractPlugin(this, clazz);
            // ignore plugins with the same name

            try {
                if (this.loadedPlugins.containsKey(plugin.getName())) {
                    throw new PluginExistsException("A plugin named '" + plugin.getName() + "' already exists!");
                }
                plugin.enable();
                this.loadedPlugins.put(plugin.getName(), plugin);
                System.out.println("Enabled: " + plugin.getName() + " ( v" + plugin.getVersion() + " )");
            } catch (PluginExistsException print) {
                print.printStackTrace();
            }
        }
    }

    public void unloadAllPlugins() {
        // disable all plugins
        for (AbstractPlugin plugin : this.loadedPlugins.values()) {
            plugin.disable();
            System.out.println("Disabled: " + plugin.getName() + " ( v" + plugin.getVersion() + " )");
        }

        // clear list
        this.loadedPlugins.clear();
    }
}
