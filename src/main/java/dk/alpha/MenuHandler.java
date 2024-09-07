package dk.alpha;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;

public class MenuHandler implements Listener {

    private final Main plugin;

    public MenuHandler(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction().toString().contains("RIGHT_CLICK") && event.getPlayer().isSneaking()) {
            Player player = event.getPlayer();
            if (event.getClickedBlock() != null) {
                Location clickedLocation = event.getClickedBlock().getLocation();
                File playerFile = new File(plugin.getDataFolder(), "playerdata/" + player.getUniqueId() + ".yml");

                if (playerFile.exists()) {
                    FileConfiguration playerData = YamlConfiguration.loadConfiguration(playerFile);
                    Location mineLocation = (Location) playerData.get("mine.location");

                    if (mineLocation != null && mineLocation.equals(clickedLocation)) {
                        Inventory menu = Bukkit.createInventory(null, 9, getMenuTitle());
                        setMenuItems(menu);
                        player.openInventory(menu);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(getMenuTitle())) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            String itemName = event.getCurrentItem().getItemMeta().getDisplayName();

            switch (itemName) {
                case "§cRemove Mine":
                    removeMine(player);
                    break;
                case "§bAdd Player":
                    addPlayerToMine(player);
                    break;
                case "§eToggle Public":
                    togglePublic(player);
                    break;
                case "§eToggle Private":
                    togglePrivate(player);
                    break;
            }
        }
    }

    private void setMenuItems(Inventory inventory) {
        FileConfiguration menuConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "menu.yml"));
        for (String key : menuConfig.getConfigurationSection("menu.items").getKeys(false)) {
            String itemName = menuConfig.getString("menu.items." + key + ".name");
            Material itemId = Material.getMaterial(menuConfig.getInt("menu.items." + key + ".id"));
            List<String> lore = menuConfig.getStringList("menu.items." + key + ".lore");

            if (itemId != null) {
                ItemStack item = new ItemStack(itemId);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(itemName);
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                inventory.addItem(item);
            }
        }
    }

    public String getMenuTitle() {
        FileConfiguration menuConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "menu.yml"));
        return menuConfig.getString("menu.title", "Mine Management");
    }

    private void removeMine(Player player) {
        // Implement mine removal logic
        player.sendMessage("Mine removed.");
    }

    private void addPlayerToMine(Player player) {
        // Implement logic to add a player to the mine
        player.sendMessage("Player added to mine.");
    }

    private void togglePublic(Player player) {
        // Implement toggle public logic
        player.sendMessage("Mine is now public.");
    }

    private void togglePrivate(Player player) {
        // Implement toggle private logic
        player.sendMessage("Mine is now private.");
    }
}
