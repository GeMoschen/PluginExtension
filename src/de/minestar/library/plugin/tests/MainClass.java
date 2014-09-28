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

import java.io.File;
import java.io.IOException;

import de.minestar.library.plugin.PluginManager;

public class MainClass {

    public static void main(String[] args) throws IOException, InterruptedException {
        PluginManager pm = new PluginManager(new File(args[0]));
        pm.listPlugins();

        // start up
        System.out.println("\n---------------------------------------");
        System.out.println("Enabling all plugins...");
        System.out.println("---------------------------------------");
        pm.enablePlugins();

        // full reload
        System.out.println("\n---------------------------------------");
        System.out.println("Reload #1 coming in 2000ms...");
        System.out.println("---------------------------------------");
        Thread.sleep(2000);
        pm.reloadAllPlugins();

        // current reload
        System.out.println("\n---------------------------------------");
        System.out.println("Reload #2 coming in 2000ms...");
        System.out.println("---------------------------------------");
        Thread.sleep(2000);
        pm.reloadLoadedPlugins();

        // disable
        System.out.println("\n---------------------------------------");
        System.out.println("Disabling all plugins in 2000ms...");
        System.out.println("---------------------------------------");
        Thread.sleep(2000);
        pm.disablePlugins();

        System.out.println("\nDONE!");
    }
}
