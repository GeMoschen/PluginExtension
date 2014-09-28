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
package de.minestar.library.plugin.units;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.minestar.library.plugin.annotations.CallMethod;
import de.minestar.library.plugin.annotations.ExternalPlugin;

public class MethodCall {

    private final ExternalPlugin instance;
    private final Method method;
    private final CallMethod callMethod;

    public MethodCall(ExternalPlugin instance, Method method, CallMethod callMethod) {
        this.instance = instance;
        this.method = method;
        this.callMethod = callMethod;
    }

    public void invoke() {
        try {
            Object[] args = new Object[callMethod.fieldNames().length];
            int index = 0;
            boolean invokeMethod = true;
            for (String value : this.callMethod.fieldNames()) {
                // get field
                Field field = instance.getClass().getDeclaredField(value);

                // the argumenttype and the fieldtype must be equal
                if (!field.getType().equals(method.getParameterTypes()[index])) {
                    invokeMethod = false;
                    break;
                }
                // set field accessible
                field.setAccessible(true);
                // fetch value
                args[index] = field.get(instance);
                // set field unaccessible
                field.setAccessible(false);
                // increment index
                index++;
            }

            // should the method be invoked?
            if (invokeMethod) {
                // set method accessible
                method.setAccessible(true);
                // invoke method
                method.invoke(this.instance, args);
                // set method unaccessible
                method.setAccessible(false);
            }
        } catch (Exception print) {
            print.printStackTrace();
        }
    }

}
