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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import de.minestar.library.plugin.annotations.ExternalPlugin;
import de.minestar.library.plugin.annotations.Plugin;
import de.minestar.library.plugin.annotations.PostEnable;
import de.minestar.library.plugin.annotations.PreDisable;
import de.minestar.library.plugin.exceptions.CircularDependencyException;
import de.minestar.library.plugin.exceptions.MissingDependencyException;
import de.minestar.library.plugin.exceptions.PluginCreationFailedException;
import de.minestar.library.plugin.units.Priority;

public class PluginDefinition {

    private final PluginManager pluginManager;
    private boolean enabled;
    private ExternalPlugin instance;
    private final String name, version;
    private final String[] dependencies;
    private final List<PluginDefinition> dependingPlugins;
    private final Map<Priority, List<Method>> postEnableMap, preDisableMap;

    protected static PluginDefinition createPlugin(PluginManager pluginManager, Class<? extends ExternalPlugin> clazz) {
        try {
            return new PluginDefinition(pluginManager, clazz);
        } catch (PluginCreationFailedException print) {
            print.getOriginalException().printStackTrace();
        }
        return null;
    }

    private PluginDefinition(PluginManager pluginManager, Class<? extends ExternalPlugin> clazz) throws PluginCreationFailedException {

        try {
            this.instance = (ExternalPlugin) clazz.newInstance();

            this.pluginManager = pluginManager;
            this.name = this.fetchPluginName();
            this.version = this.fetchPluginVersion();
            this.dependencies = this.fetchDependencies();
            this.postEnableMap = this.fetchMethods(PostEnable.class);
            this.preDisableMap = this.fetchMethods(PreDisable.class);
            this.dependingPlugins = new ArrayList<PluginDefinition>();
            this.enabled = false;
            Field field = ExternalPlugin.class.getDeclaredField("pluginManager");
            field.setAccessible(true);
            field.set(this.instance, this.pluginManager);
            field.setAccessible(false);
        } catch (Exception originalException) {
            throw new PluginCreationFailedException("Could not create plugin '" + clazz.getSimpleName() + "'!", originalException);
        }
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

    private String[] fetchDependencies() {
        // fetch annotations
        Annotation[] annotations = this.instance.getClass().getDeclaredAnnotations();
        for (Annotation annotation : annotations) {
            // if the class is "Plugin", return the harddependencies
            if (annotation instanceof Plugin) {
                Plugin plugin = (Plugin) annotation;
                return plugin.dependencies();
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
                    if (PostEnable.class.isAssignableFrom(annotation.getClass())) {
                        PostEnable onEnable = (PostEnable) annotation;
                        level = onEnable.priority();
                    } else if (PreDisable.class.isAssignableFrom(annotation.getClass())) {
                        PreDisable onDisable = (PreDisable) annotation;
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

    protected void updateDependencies() throws MissingDependencyException {
        // clear list
        this.dependingPlugins.clear();

        // iterate and search plugins
        for (String dependency : this.dependencies) {
            PluginDefinition plugin = this.pluginManager.getPlugin(dependency);
            if (plugin == null) {
                throw new MissingDependencyException("Plugin '" + this.name + "' is missing depending plugin '" + dependency + "'. Plugin will be ignored!", this, dependency);
            } else {
                this.dependingPlugins.add(plugin);
            }
        }
    }

    public void checkForCircularDependencies(PluginDefinition father, List<PluginDefinition> list) throws CircularDependencyException {
        // check, if we are already in the set
        if (list.contains(this)) {
            throw new CircularDependencyException("Circular dependency between '" + this.getName() + "' and '" + father.getName() + "' found. Both plugins will be ignored!", this, father);
        }

        // add this "AbstractPlugin"
        list.add(this);

        // recursive call
        for (PluginDefinition plugin : this.dependingPlugins) {
            plugin.checkForCircularDependencies(this, list);
        }
    }

    protected boolean enable() {
        this.enabled = true;
        return true;
    }

    protected boolean disable() {
        if (this.enabled) {
            this.enabled = false;
            return true;
        }
        return false;
    }

    protected boolean onPostEnable() {
        if (this.enabled) {
            this.callMethods(this.postEnableMap);
            return true;
        }
        return false;
    }

    protected boolean onPreDisable() {
        if (this.enabled) {
            this.callMethods(this.preDisableMap);
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public <T> T getInstance(Class<?> clazz) {
        return (T) instance;
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