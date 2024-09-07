package dk.alpha;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class FileConfigurationHandler {

    private final Main plugin;
    private final File playerDataFolder;

    public FileConfigurationHandler(Main plugin) {
        this.plugin = plugin;
        this.playerDataFolder = new File(plugin.getDataFolder(), "PlayerData");
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }
    }

    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    public File getPlayerDataFile(Player player) {
        return new File(playerDataFolder, player.getUniqueId().toString() + ".yml");
    }

    public void savePlayerData(Player player, FileConfiguration playerData) {
        try {
            playerData.save(getPlayerDataFile(player));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
