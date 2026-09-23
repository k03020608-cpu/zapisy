package pl.zapisy;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class ZapisyPlugin extends JavaPlugin {

    private ZapisyManager manager;
    private TurniejManager turniej;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        manager = new ZapisyManager(this);
        manager.load();

        turniej = new TurniejManager(this);
        turniej.load();

        ZapisyCommands commands = new ZapisyCommands(manager, turniej);
        String[] nazwyKomend = {
                "zapisz", "wypisz", "zapisani",
                "resetlisty", "zamknijzapisy", "otworzzapisy",
                "stworzturniej", "wygral", "turniej"
        };
        for (String name : nazwyKomend) {
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
