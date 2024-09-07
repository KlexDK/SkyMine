package dk.alpha;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.List;

public class MineCommandExecutor implements CommandExecutor {

    private final Main plugin;
    private final FileConfigurationHandler fileConfigHandler;
    private final PlayerInteractHandler interactHandler;

    public MineCommandExecutor(Main plugin, FileConfigurationHandler fileConfigHandler, PlayerInteractHandler interactHandler) {
        this.plugin = plugin;
        this.fileConfigHandler = fileConfigHandler;
        this.interactHandler = interactHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Brug: /mine [list|give|remove]");
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Kun spillere kan bruge denne kommando.");
            return false;
        }

        Player player = (Player) sender;
        String subCommand = args[0];

        switch (subCommand.toLowerCase()) {
            case "list":
                listMines(player);
                break;
            case "give":
                if (args.length != 2) {
                    player.sendMessage("Brug: /mine give [type]");
                    return false;
                }
                String type = args[1];
                giveMine(player, type);
                break;
            case "remove":
                if (args.length != 2) {
                    player.sendMessage("Brug: /mine remove [type]");
                    return false;
                }
                type = args[1];
                removeMine(player, type);
                break;
            default:
                player.sendMessage("Ukendt subkommando. Brug /mine [list|give|remove]");
                break;
        }

        return true;
    }

    private void listMines(Player player) {
        File playerFile = fileConfigHandler.getPlayerDataFile(player);
        if (playerFile.exists()) {
            FileConfiguration playerData = YamlConfiguration.loadConfiguration(playerFile);
            Location mineLocation = (Location) playerData.get("mine.location");
            String mineType = playerData.getString("mine.type");

            if (mineLocation != null && mineType != null) {
                player.sendMessage("Din placerede mine:");
                player.sendMessage("Type: " + mineType);
                player.sendMessage("Placering: " + mineLocation.getBlockX() + ", " + mineLocation.getBlockY() + ", " + mineLocation.getBlockZ());
            } else {
                player.sendMessage("Ingen placerede miner fundet.");
            }
        } else {
            player.sendMessage("Ingen data fundet for dig.");
        }
    }

    private void giveMine(Player player, String type) {
        if (!plugin.getConfig().getConfigurationSection("mines").getKeys(false).contains(type)) {
            player.sendMessage("Den valgte mine-type findes ikke.");
            return;
        }

        Material mineMaterial = Material.getMaterial(plugin.getConfig().getString("mines." + type + ".item"));
        if (mineMaterial != null) {
            // Create the item stack
            ItemStack itemStack = new ItemStack(mineMaterial, 1);
            ItemMeta itemMeta = itemStack.getItemMeta();

            // Set the name and lore if they are specified in the config
            String itemName = plugin.getConfig().getString("mines." + type + ".item-name");
            List<String> itemLore = plugin.getConfig().getStringList("mines." + type + ".item-lore");

            if (itemMeta != null) {
                if (itemName != null && !itemName.isEmpty()) {
                    itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', itemName));
                }
                if (itemLore != null && !itemLore.isEmpty()) {
                    for (int i = 0; i < itemLore.size(); i++) {
                        itemLore.set(i, ChatColor.translateAlternateColorCodes('&', itemLore.get(i)));
                    }
                    itemMeta.setLore(itemLore);
                }
                itemStack.setItemMeta(itemMeta);
            }

            // Give the item to the player
            player.getInventory().addItem(itemStack);
            player.sendMessage("Du har fået en mine af typen: " + type);
        } else {
            player.sendMessage("Ugyldigt item til minen.");
        }
    }

    private void removeMine(Player player, String type) {
        if (!plugin.getConfig().getConfigurationSection("mines").getKeys(false).contains(type)) {
            player.sendMessage("Den valgte mine-type findes ikke.");
            return;
        }

        File playerFile = fileConfigHandler.getPlayerDataFile(player);
        if (playerFile.exists()) {
            FileConfiguration playerData = YamlConfiguration.loadConfiguration(playerFile);
            Location mineLocation = (Location) playerData.get("mine.location");

            if (mineLocation != null) {
                // Remove the mine and clean up
                interactHandler.removeMine(mineLocation, type);

                // Clean up player data
                playerData.set("mine.location", null);
                playerData.set("mine.type", null);
                playerData.set("owner", null);
                fileConfigHandler.savePlayerData(player, playerData);

                player.sendMessage("Din mine af typen " + type + " er blevet fjernet.");
            } else {
                player.sendMessage("Ingen mine fundet.");
            }
        } else {
            player.sendMessage("Ingen data fundet for dig.");
        }
    }
}
