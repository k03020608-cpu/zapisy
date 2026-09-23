package pl.zapisy;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ZapisyManager {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, String> signedUp = new LinkedHashMap<>();

    public ZapisyManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        signedUp.clear();
        if (!file.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("zapisani");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            try {
                signedUp.put(UUID.fromString(key), section.getString(key, "?"));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Pominięto nieprawidłowy wpis w data.yml: " + key);
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        signedUp.forEach((uuid, name) -> yaml.set("zapisani." + uuid, name));

        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Nie udało się zapisać data.yml: " + e.getMessage());
        }
    }

    public boolean add(Player player) {
        if (signedUp.containsKey(player.getUniqueId())) {
            return false;
        }
        signedUp.put(player.getUniqueId(), player.getName());
        save();
        return true;
    }

    public boolean remove(Player player) {
        if (signedUp.remove(player.getUniqueId()) == null) {
            return false;
        }
        save();
        return true;
    }

    public List<String> names() {
        return new ArrayList<>(signedUp.values());
    }

    public int size() {
        return signedUp.size();
    }

    public void clear() {
        signedUp.clear();
        save();
    }

    public boolean isOpen() {
        return plugin.getConfig().getBoolean("signups-open", true);
    }

    public void setOpen(boolean open) {
        plugin.getConfig().set("signups-open", open);
        plugin.saveConfig();
    }
}
