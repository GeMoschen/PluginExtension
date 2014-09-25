package de.minestar.library.plugin;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import de.minestar.library.plugin.annotations.Plugin;
import de.minestar.library.plugin.exceptions.AlreadyDisabledException;
import de.minestar.library.plugin.exceptions.AlreadyEnabledException;

public class PluginManager {

    public final Set<Class<?>> registeredClasses;
    public Set<AbstractPlugin> loadedPlugins;

    public PluginManager(File folder) throws IOException {
        this.registeredClasses = this.searchPlugins(folder);
        this.loadedPlugins = new HashSet<AbstractPlugin>();
    }

    public Set<Class<?>> getRegisteredClasses() {
        return registeredClasses;
    }

    public Set<AbstractPlugin> getLoadedPlugins() {
        return loadedPlugins;
    }

    private Set<Class<?>> searchPlugins(File folder) throws IOException {
        Set<Class<?>> set = new HashSet<Class<?>>();

        // folder must exist
        if (folder.exists()) {
            File[] files = folder.listFiles();
            for (File file : files) {

                System.out.println(file);

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
                while (e.hasMoreElements()) {
                    // get the entry
                    JarEntry jarEntry = (JarEntry) e.nextElement();

                    // directories and files without ".class"-ending are ignored
                    if (jarEntry.isDirectory() || !jarEntry.getName().endsWith(".class")) {
                        continue;
                    }
                    // -6, because of .class
                    String className = jarEntry.getName().substring(0, jarEntry.getName().length() - 6);
                    className = className.replace('/', '.');

                    try {
                        // load the class
                        Class<?> clazz = cl.loadClass(className);

                        // search for "Plugin"-Annotation
                        if (this.hasAnnotation(clazz, Plugin.class)) {
                            set.add(clazz);
                        }
                    } catch (ClassNotFoundException print) {
                        print.printStackTrace();
                    }

                }

                // close JAR
                jarFile.close();

            }
        }

        return Collections.unmodifiableSet(set);
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
            try {
                this.loadedPlugins.add(plugin);
                System.out.println("Enabling: " + plugin.getName() + " ( v" + plugin.getVersion() + " )");
                plugin.enable();
            } catch (AlreadyEnabledException print) {
                print.printStackTrace();
            }
        }
    }

    public void unloadAllPlugins() {
        // disable all plugins
        for (AbstractPlugin plugin : this.loadedPlugins) {
            try {
                System.out.println("Disabling: " + plugin.getName() + " ( v" + plugin.getVersion() + " )");
                plugin.disable();
            } catch (AlreadyDisabledException print) {
                print.printStackTrace();
            }
        }

        // clear list
        this.loadedPlugins.clear();
    }
}
