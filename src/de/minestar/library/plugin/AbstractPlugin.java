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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.minestar.library.plugin.annotations.Plugin;
import de.minestar.library.plugin.annotations.onDisable;
import de.minestar.library.plugin.annotations.onEnable;
import de.minestar.library.plugin.exceptions.AlreadyDisabledException;
import de.minestar.library.plugin.exceptions.AlreadyEnabledException;

public class AbstractPlugin {

    private final PluginManager pluginManager;
    private boolean enabled;
    private Object instance;
    private final String name, version;
    private final List<Method> onEnableList, onDisableList;

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
            this.onEnableList = this.fetchMethods(onEnable.class);
            this.onDisableList = this.fetchMethods(onDisable.class);
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

    private List<Method> fetchMethods(Class<?> clazz) {
        // create new list
        List<Method> list = new ArrayList<Method>();

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
                    list.add(method);
                }
            }
        }
        // return an unmodifiable list
        return Collections.unmodifiableList(list);
    }

    private void callMethods(List<Method> list) {
        for (Method method : list) {
            try {
                method.invoke(this.instance);
            } catch (IllegalAccessException print) {
                print.printStackTrace();
            } catch (IllegalArgumentException print) {
                print.printStackTrace();
            } catch (InvocationTargetException print) {
                print.printStackTrace();
            }
        }
    }

    public void enable() throws AlreadyEnabledException {
        if (!this.enabled) {
            this.callMethods(this.onEnableList);
            this.enabled = true;
        } else {
            throw new AlreadyEnabledException("Plugin [ " + this.name + " ] is already enabled!");
        }
    }

    public void disable() throws AlreadyDisabledException {
        if (this.enabled) {
            this.callMethods(this.onDisableList);
            this.enabled = false;
        } else {
            throw new AlreadyDisabledException("Plugin [ " + this.name + " ] is already disabled!");
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
