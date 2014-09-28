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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import de.minestar.library.plugin.annotations.OnDisable;
import de.minestar.library.plugin.annotations.OnEnable;
import de.minestar.library.plugin.annotations.Plugin;
import de.minestar.library.plugin.exceptions.MissingHardDependencyException;
import de.minestar.library.plugin.units.Priority;

public class AbstractPlugin {

    private final PluginManager pluginManager;
    private boolean enabled;
    private Object instance;
    private final String name, version;
    private final String[] softDependencies, hardDependencies;
    private final Map<Priority, List<Method>> onEnableMap, onDisableMap;

    protected AbstractPlugin(PluginManager pluginManager, Class<?> clazz) throws InstantiationException, IllegalAccessException {
        this.instance = clazz.newInstance();
        this.pluginManager = pluginManager;
        this.name = this.fetchPluginName();
        this.version = this.fetchPluginVersion();
        this.softDependencies = this.fetchSoftDependencies();
        this.hardDependencies = this.fetchHardDependencies();
        this.onEnableMap = this.fetchMethods(OnEnable.class);
        this.onDisableMap = this.fetchMethods(OnDisable.class);
        this.enabled = false;
    }

    private String fetchPluginName() {
        return this.instance.getClass().getSimpleName();
    }

    private String fetchPluginVersion() {
        // fetch annotations
        Annotation[] annotations = this.instance.getClass().getDeclaredAnnotations();
        for (Annotation annotation : annotations) {
            // if the class is "Plugin", return the version
            if (annotation instanceof Plugin) {
                Plugin plugin = (Plugin) annotation;
                return plugin.version();
            }
        }
        return "UNKNOWN";
    }

    private String[] fetchSoftDependencies() {
        // fetch annotations
        Annotation[] annotations = this.instance.getClass().getDeclaredAnnotations();
        for (Annotation annotation : annotations) {
            // if the class is "Plugin", return the softdependencies
            if (annotation instanceof Plugin) {
                Plugin plugin = (Plugin) annotation;
                return plugin.softDepend();
            }
        }
        return new String[0];
    }

    private String[] fetchHardDependencies() {
        // fetch annotations
        Annotation[] annotations = this.instance.getClass().getDeclaredAnnotations();
        for (Annotation annotation : annotations) {
            // if the class is "Plugin", return the harddependencies
            if (annotation instanceof Plugin) {
                Plugin plugin = (Plugin) annotation;
                return plugin.hardDepend();
            }
        }
        return new String[0];
    }

    private Map<Priority, List<Method>> fetchMethods(Class<?> clazz) {
        // create new map
        Map<Priority, List<Method>> map = new TreeMap<Priority, List<Method>>();

        // fetch methods
        Method[] methods = this.instance.getClass().getDeclaredMethods();
        Annotation[] annotations;
        for (Method method : methods) {
            // arguments are not allowed
            if (method.getParameterTypes().length > 0) {
                continue;
            }

            // fetch annotations
            annotations = method.getDeclaredAnnotations();
            for (Annotation annotation : annotations) {
                // if the class is the same, append the method
                if (clazz.isAssignableFrom(annotation.getClass())) {

                    // fetch level
                    Priority level = Priority.THIRD_MOST;
                    if (OnEnable.class.isAssignableFrom(annotation.getClass())) {
                        OnEnable onEnable = (OnEnable) annotation;
                        level = onEnable.priority();
                    } else if (OnDisable.class.isAssignableFrom(annotation.getClass())) {
                        OnDisable onDisable = (OnDisable) annotation;
                        level = onDisable.priority();
                    }

                    // create arrayList, if there is none yet
                    if (!map.containsKey(level)) {
                        map.put(level, new ArrayList<Method>());
                    }

                    // set accessible
                    method.setAccessible(true);

                    // append the method
                    map.get(level).add(method);
                }
            }
        }
        // return an unmodifiable map
        return Collections.unmodifiableMap(map);
    }

    private void callMethods(Map<Priority, List<Method>> map) {
        for (Map.Entry<Priority, List<Method>> entry : map.entrySet()) {
            for (Method method : entry.getValue()) {
                try {
                    method.invoke(this.instance);
                } catch (Exception print) {
                    print.printStackTrace();
                }
            }
        }
    }

    protected boolean enable() throws MissingHardDependencyException {
        return this.enable(new TreeSet<String>());
    }

    protected boolean enable(Set<String> activatedPlugins) throws MissingHardDependencyException {
        // plugin is already activated
        if (activatedPlugins.contains(this.name) || this.enabled) {
            return true;
        }

        // load soft-dependencies
        for (String pluginName : this.softDependencies) {
            if (this.pluginManager.enablePlugin(pluginName)) {
                activatedPlugins.add(pluginName);
            }
        }

        // load hard-dependencies
        for (String pluginName : this.hardDependencies) {
            if (this.pluginManager.hasPlugin(pluginName) && this.pluginManager.enablePlugin(pluginName, activatedPlugins)) {
                activatedPlugins.add(pluginName);
            } else {
                throw new MissingHardDependencyException("Plugin not enabled: " + this.getName() + " [ v" + this.getVersion() + " ] -> Harddependency not found: " + pluginName + " !");
            }
        }

        this.callMethods(this.onEnableMap);
        this.enabled = true;
        return true;
    }

    protected boolean disable() {
        if (this.enabled) {
            this.callMethods(this.onDisableMap);
            this.enabled = false;
            return true;
        }
        return false;
    }

    // //////////////////////////////////////////////////////////////////////////////////////
    //
    // GET & SET
    //
    // //////////////////////////////////////////////////////////////////////////////////////

    protected String getName() {
        return this.name;
    }

    protected String getVersion() {
        return this.version;
    }

    protected boolean isEnabled() {
        return this.enabled;
    }
}
