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

import de.minestar.library.plugin.annotations.Plugin;
import de.minestar.library.plugin.annotations.onDisable;
import de.minestar.library.plugin.annotations.onEnable;

@Plugin(name = "TestPlugin", version = "1.0")
public class TestPlugin {

    @onEnable
    public void onEnableMethod1() {
        System.out.println("onEnableMethod1");
    }

    @onEnable
    public void onEnableMethod2() {
        System.out.println("onEnableMethod2");
    }

    @onDisable
    public void onDisableMethod1(int arg) {
        System.out.println("onDisableMethod1");
    }

    @onDisable
    public void onDisableMethod2() {
        System.out.println("onDisableMethod2");
    }

}
