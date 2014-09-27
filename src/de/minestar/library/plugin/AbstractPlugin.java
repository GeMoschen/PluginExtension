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
import java.util.TreeMap;

import de.minestar.library.plugin.annotations.OnDisable;
import de.minestar.library.plugin.annotations.OnEnable;
import de.minestar.library.plugin.annotations.Plugin;
import de.minestar.library.plugin.units.Priority;

public class AbstractPlugin {

    private final PluginManager pluginManager;
    private boolean enabled;
    private Object instance;
    private final String name, version;
    private final Map<Priority, List<Method>> onEnableList, onDisableList;

    public AbstractPlugin(PluginManager pluginManager, Class<?> clazz) {
        try {
            this.instance = clazz.newInstance();
        } catch (InstantiationException print) {
            print.printStackTrace();
        } catch (IllegalAccessException print) {
            print.printStackTrace();
        } finally {
            this.pluginManager = pluginManager;
            this.name = this.fetchPluginName();
            this.version = this.fetchPluginVersion();
            this.enabled = false;
            this.onEnableList = this.fetchMethods(OnEnable.class);
            this.onDisableList = this.fetchMethods(OnDisable.class);
        }
    }

    private String fetchPluginName() {
        // fetch annotations
        Annotation[] annotations = this.instance.getClass().getDeclaredAnnotations();
        for (Annotation annotation : annotations) {
            // if the class is "Plugin", return the name
            if (annotation instanceof Plugin) {
                Plugin plugin = (Plugin) annotation;
                return plugin.name();
            }
        }
        return "UNKNOWN";
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

    private Map<Priority, List<Method>> fetchMethods(Class<?> clazz) {
        // create new list
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
                        level = onEnable.level();
                    } else if (OnDisable.class.isAssignableFrom(annotation.getClass())) {
                        OnDisable onDisable = (OnDisable) annotation;
                        level = onDisable.level();
                    }

                    // create arrayList, if there is none yet
                    if (!map.containsKey(level)) {
                        map.put(level, new ArrayList<Method>());
                    }

                    // append the method
                    map.get(level).add(method);
                }
            }
        }
        // return an unmodifiable list
        return Collections.unmodifiableMap(map);
    }

    private void callMethods(Map<Priority, List<Method>> list) {
        for (Map.Entry<Priority, List<Method>> entry : list.entrySet()) {
            for (Method method : entry.getValue()) {
                try {
                    method.invoke(this.instance);
                } catch (Exception print) {
                    print.printStackTrace();
                }
            }
        }
    }

    public void enable() {
        if (!this.enabled) {
            this.callMethods(this.onEnableList);
            this.enabled = true;
        }
    }

    public void disable() {
        if (this.enabled) {
            this.callMethods(this.onDisableList);
            this.enabled = false;
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////
    //
    // GET & SET
    //
    // //////////////////////////////////////////////////////////////////////////////////////

    public String getName() {
        return this.name;
    }

    public String getVersion() {
        return this.version;
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

}
