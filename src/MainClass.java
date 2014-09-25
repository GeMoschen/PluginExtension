import java.io.File;
import java.io.IOException;

import de.minestar.library.plugin.PluginManager;

public class MainClass {

    public static void main(String[] args) throws IOException {
        PluginManager pm = new PluginManager(new File(args[0]));
        System.out.println("Plugins found: " + pm.getRegisteredClasses().size());
        for (Class<?> plugin : pm.getRegisteredClasses()) {
            System.out.println("-> " + plugin.getName());
        }

        System.out.println("\nEnabling all plugins...");
        pm.loadAllPlugins();

        System.out.println("\nDisabling all plugins...");
        pm.unloadAllPlugins();
    }
}
