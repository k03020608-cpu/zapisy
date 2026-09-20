package pl.zapisy;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class ZapisyPlugin extends JavaPlugin {

    private ZapisyManager manager;

    @Override
    public void onEnable() {
        manager = new ZapisyManager(this);
        manager.load();

        ZapisyCommands commands = new ZapisyCommands(manager);
        for (String name : new String[]{"zapisz", "wypisz", "zapisani"}) {
            PluginCommand command = getCommand(name);
            if (command != null) {
                command.setExecutor(commands);
            }
        }

        getLogger().info("Załadowano " + manager.size() + " zapisanych graczy.");
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.save();
        }
    }
}
