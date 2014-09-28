package de.minestar.library.plugin.tests;

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

import de.minestar.library.plugin.annotations.CallMethod;
import de.minestar.library.plugin.annotations.ExternalPlugin;
import de.minestar.library.plugin.annotations.Plugin;
import de.minestar.library.plugin.annotations.PostEnable;
import de.minestar.library.plugin.units.Priority;

@Plugin(version = "1.0", dependencies = { "DependingPlugin" })
public class TestPlugin extends ExternalPlugin {

    private DependingPlugin plugin;
    private int myA, myB;

    @PostEnable(priority = Priority.FIFTH_MOST)
    private void enable() {
        System.out.println("enabling #1");
        System.out.println("---> " + plugin.test);
    }

    @PostEnable(priority = Priority.FIRST_MOST)
    public void enable2() {
        System.out.println("enabling #2");
        myA = 2;
        myB = 4;
        plugin = this.getPluginManager().getPlugin(DependingPlugin.class);
    }

    @PostEnable(priority = Priority.THIRD_MOST)
    private void enable3() {
        System.out.println("enabling #3");
    }

    @CallMethod(priority = 2, fieldNames = { "myA", "myB" })
    private void simpleMethod(int a, int b) {
        int result = a + b;
        System.out.println("Result + : " + result);
    }

    @CallMethod(priority = 3, fieldNames = { "myA", "myB" })
    private void simpleMethod2(int a, int b) {
        int result = a * b;
        System.out.println("Result * : " + result);
    }
}
