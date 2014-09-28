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

import de.minestar.library.plugin.annotations.PostEnable;
import de.minestar.library.plugin.annotations.Plugin;
import de.minestar.library.plugin.units.Priority;

@Plugin(version = "1.0", softDepend = { "BSoftDependPlugin" }, hardDepend = { "ZHardDependPlugin" })
public class ATestPlugin {

    @PostEnable(priority = Priority.FIFTH_MOST)
    private void enable() {
        System.out.println("enabling #1");
    }

    @PostEnable(priority = Priority.FIRST_MOST)
    public void enable2() {
        System.out.println("enabling #2");
    }

    @PostEnable(priority = Priority.THIRD_MOST)
    private void enable3() {
        System.out.println("enabling #3");
    }
}
