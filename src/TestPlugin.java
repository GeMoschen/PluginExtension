

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
