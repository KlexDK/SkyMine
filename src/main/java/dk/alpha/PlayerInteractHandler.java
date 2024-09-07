package dk.alpha;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerInteractHandler implements Listener {
    private final JavaPlugin plugin;
    private final FileConfigurationHandler fileConfigHandler;
    private final HologramHandler hologramHandler;

    public PlayerInteractHandler(JavaPlugin plugin, FileConfigurationHandler fileConfigHandler, HologramHandler hologramHandler) {
        this.plugin = plugin;
        this.fileConfigHandler = fileConfigHandler;
        this.hologramHandler = hologramHandler;
    }

    public void resetMineContent(Location location, String mineType) {
        // Check if the mine is placed and managed by the player
        if (isMinePlaced(location, mineType)) {
            int radius = this.plugin.getConfig().getInt("mines." + mineType + ".radius");
            int height = this.plugin.getConfig().getInt("mines." + mineType + ".height");
            Location minLocation = location.clone().add(-radius, 0.0D, -radius);
            Location maxLocation = location.clone().add(radius, height, radius);

            for (int x = minLocation.getBlockX(); x <= maxLocation.getBlockX(); x++) {
                for (int y = minLocation.getBlockY(); y <= maxLocation.getBlockY(); y++) {
                    for (int z = minLocation.getBlockZ(); z <= maxLocation.getBlockZ(); z++) {
                        Location currentLocation = new Location(location.getWorld(), x, y, z);
                        Block block = currentLocation.getBlock();

                        boolean isBorder = (x == minLocation.getBlockX() || x == maxLocation.getBlockX() || z == minLocation.getBlockZ() || z == maxLocation.getBlockZ());
                        boolean isBottomLayer = (y == minLocation.getBlockY());
                        if (y != location.getBlockY() && (isBottomLayer || isBorder)) {
                            // Keep bedrock and only reset non-bedrock blocks
                            if (block.getType() != Material.BEDROCK) {
                                block.setType(Material.AIR);
                            }
                        } else if (block.getType() != Material.BEDROCK) {
                            // Reset content within the mine
                            List<String> contentList = this.plugin.getConfig().getStringList("mines." + mineType + ".content");
                            if (!contentList.isEmpty()) {
                                Random random = new Random();
                                Material content = Material.getMaterial(contentList.get(random.nextInt(contentList.size())));
                                if (content != null)
                                    block.setType(content);
                            }
                        }
                    }
                }
            }

            // Reset the timer
            long resetTimeMillis = plugin.getConfig().getLong("mines." + mineType + ".reset-time") * 1000; // Convert seconds to milliseconds
            hologramHandler.updateHologram(location, mineType, resetTimeMillis);

            // Schedule the new timer
            scheduleMineReset(location, mineType, resetTimeMillis);
        }
    }

    private boolean isMinePlaced(Location location, String mineType) {
        // Check player data to verify if the mine is placed
        File playerDataFile = fileConfigHandler.getPlayerDataFile(location.getWorld().getPlayers().get(0)); // Adjust as needed to get correct player
        if (playerDataFile.exists()) {
            FileConfiguration playerData = YamlConfiguration.loadConfiguration(playerDataFile);
            Location mineLocation = (Location) playerData.get("mine.location");
            return mineLocation != null && mineLocation.equals(location) && mineType.equals(playerData.getString("mine.type"));
        }
        return false;
    }


    // Method to remove the entire mine including bedrock and hologram
    public void removeMine(Location location, String mineType) {
        int radius = this.plugin.getConfig().getInt("mines." + mineType + ".radius");
        int height = this.plugin.getConfig().getInt("mines." + mineType + ".height");
        Location minLocation = location.clone().add(-radius, 0.0D, -radius);
        Location maxLocation = location.clone().add(radius, height, radius);

        for (int x = minLocation.getBlockX(); x <= maxLocation.getBlockX(); x++) {
            for (int y = minLocation.getBlockY(); y <= maxLocation.getBlockY(); y++) {
                for (int z = minLocation.getBlockZ(); z <= maxLocation.getBlockZ(); z++) {
                    Location currentLocation = new Location(location.getWorld(), x, y, z);
                    Block block = currentLocation.getBlock();
                    block.setType(Material.AIR); // Remove all blocks
                }
            }
        }

        // Remove bedrock
        for (int x = minLocation.getBlockX(); x <= maxLocation.getBlockX(); x++) {
            for (int z = minLocation.getBlockZ(); z <= maxLocation.getBlockZ(); z++) {
                Location currentLocation = new Location(location.getWorld(), x, minLocation.getBlockY(), z);
                Block block = currentLocation.getBlock();
                if (block.getType() == Material.BEDROCK) {
                    block.setType(Material.AIR); // Remove bedrock
                }
            }
        }

        // Remove armor stands (if any)
        for (Entity entity : location.getWorld().getNearbyEntities(location, 10.0D, 10.0D, 10.0D)) {
            if (entity.getType() == EntityType.ARMOR_STAND)
                entity.remove();
        }

        // Remove player data file
        File playerFile = fileConfigHandler.getPlayerDataFile(location.getWorld().getPlayers().get(0)); // Example player retrieval; adjust as needed
        if (playerFile.exists()) {
            playerFile.delete(); // Deletes the file associated with the player
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack itemInHand = event.getItemInHand();
        Player player = event.getPlayer();
        Location placedLocation = event.getBlock().getLocation();
        Material placedMaterial = event.getBlock().getType();
        String mineType = getMineType(placedMaterial);

        if (itemInHand != null && itemInHand.hasItemMeta()) {
            ItemMeta meta = itemInHand.getItemMeta();

            if (meta.hasDisplayName()) {
                String itemName = ChatColor.stripColor(meta.getDisplayName());

                if (mineType != null) {
                    String expectedName = plugin.getConfig().getString("mines." + mineType + ".item-name");

                    if (itemName.equals(expectedName)) {
                        clearAreaAbove(placedLocation, mineType);
                        setBedrockAround(placedLocation, mineType);

                        FileConfiguration playerData = YamlConfiguration.loadConfiguration(fileConfigHandler.getPlayerDataFile(player));
                        playerData.set("mine.location", placedLocation);
                        playerData.set("mine.type", mineType);
                        playerData.set("owner", player.getName());
                        fileConfigHandler.savePlayerData(player, playerData);

                        hologramHandler.addMineHologram(placedLocation, mineType, player.getName(), 0L);

                        // Schedule mine reset
                        scheduleMineReset(placedLocation, mineType, plugin.getConfig().getLong("mines." + mineType + ".reset-time") * 1000);

                        itemInHand.setAmount(itemInHand.getAmount() - 1);
                        if (itemInHand.getAmount() <= 0) {
                            player.getInventory().setItemInHand(null);
                        } else {
                            player.getInventory().setItemInHand(itemInHand);
                        }

                        player.sendMessage(ChatColor.GREEN + "Mine placed successfully!");
                        return;
                    }
                }
            }
        }
    }

    public void scheduleMineReset(final Location location, final String mineType, final long resetTimeMillis) {
        final long startTimeMillis = System.currentTimeMillis();
        System.out.println("Schedule Mine Reset kaldt: " + startTimeMillis);

        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTimeMillis = System.currentTimeMillis();
                long elapsedMillis = currentTimeMillis - startTimeMillis;
                long timeLeft = resetTimeMillis - elapsedMillis;

                // Debug udskrift
                System.out.println("BukkitRunnable kaldt. StartTimeMillis: " + startTimeMillis);
                System.out.println("CurrentTimeMillis: " + currentTimeMillis);
                System.out.println("ElapsedMillis: " + elapsedMillis);
                System.out.println("TimeLeft: " + timeLeft);

                if (timeLeft <= 0) {
                    timeLeft = 0;
                    // Stop opdateringen hvis tiden er udløbet
                    this.cancel();
                }

                hologramHandler.updateHologram(location, mineType, timeLeft);
            }
        }.runTaskTimer(plugin, 0, 20); // Opdater hver sekund (20 ticks = 1 sekund)
    }


    private void clearAreaAbove(Location location, String mineType) {
        int radius = this.plugin.getConfig().getInt("mines." + mineType + ".radius");
        int height = this.plugin.getConfig().getInt("mines." + mineType + ".height");
        Location minLocation = location.clone().add(-radius, 0.0D, -radius);
        Location maxLocation = location.clone().add(radius, (height - 1), radius);
        for (int x = minLocation.getBlockX(); x <= maxLocation.getBlockX(); x++) {
            for (int y = minLocation.getBlockY(); y <= maxLocation.getBlockY(); y++) {
                for (int z = minLocation.getBlockZ(); z <= maxLocation.getBlockZ(); z++) {
                    Location currentLocation = new Location(location.getWorld(), x, y, z);
                    Block block = currentLocation.getBlock();
                    if (block.getType() != Material.BEDROCK)
                        block.setType(Material.AIR);
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location blockLocation = block.getLocation();
        Player player = event.getPlayer();
        Material blockType = block.getType();
        String mineType = getMineType(blockType);
        if (mineType != null) {
            String itemMaterialName = this.plugin.getConfig().getString("mines." + mineType + ".item");
            Material itemMaterial = Material.getMaterial(itemMaterialName);
            if (itemMaterial != null && blockType == itemMaterial)
                return;
            File playerFile = this.fileConfigHandler.getPlayerDataFile(player);
            if (playerFile.exists()) {
                YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(playerFile);
                Location mineLocation = (Location)yamlConfiguration.get("mine.location");
                if (mineLocation != null && mineLocation.equals(blockLocation) && mineType.equals(yamlConfiguration.getString("mine.type"))) {
                    if (block.getType() == Material.BEDROCK) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "You cannot break bedrock!");
                    } else {
                        return;
                    }
                } else {
                    event.setCancelled(true);
                    player.sendMessage(this.plugin.getConfig().getString("mines." + mineType + ".message.break"));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction().toString().contains("RIGHT_CLICK") && event.getPlayer().isSneaking()) {
            Player player = event.getPlayer();
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock != null) {
                Location clickedLocation = clickedBlock.getLocation();
                File playerFile = this.fileConfigHandler.getPlayerDataFile(player);
                if (playerFile.exists()) {
                    YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(playerFile);
                    Location mineLocation = (Location)yamlConfiguration.get("mine.location");
                    if (mineLocation != null && mineLocation.equals(clickedLocation))
                        player.sendMessage("You have interacted with your mine at " + clickedLocation);
                }
            }
        }
    }

    private void setBedrockAround(Location location, String mineType) {
        int radius = this.plugin.getConfig().getInt("mines." + mineType + ".radius");
        int height = this.plugin.getConfig().getInt("mines." + mineType + ".height");
        Location minLocation = location.clone().add(-radius, -1.0D, -radius);
        Location maxLocation = location.clone().add(radius, height, radius);
        Location topLayerLocation = location.clone().add(-radius, 0.0D, -radius);
        int x;
        for (x = topLayerLocation.getBlockX(); x <= topLayerLocation.getBlockX() + 2 * radius; x++) {
            for (int z = topLayerLocation.getBlockZ(); z <= topLayerLocation.getBlockZ() + 2 * radius; z++) {
                Location currentLocation = new Location(location.getWorld(), x, topLayerLocation.getBlockY(), z);
                Block block = currentLocation.getBlock();
                if (block.getType() == Material.AIR)
                    block.setType(Material.BEDROCK);
            }
        }
        for (x = minLocation.getBlockX(); x <= maxLocation.getBlockX(); x++) {
            for (int y = minLocation.getBlockY(); y <= maxLocation.getBlockY(); y++) {
                for (int z = minLocation.getBlockZ(); z <= maxLocation.getBlockZ(); z++) {
                    Location currentLocation = new Location(location.getWorld(), x, y, z);
                    Block block = currentLocation.getBlock();
                    boolean isBorder = (x == minLocation.getBlockX() || x == maxLocation.getBlockX() || z == minLocation.getBlockZ() || z == maxLocation.getBlockZ());
                    boolean isBottomLayer = (y == minLocation.getBlockY());
                    if (y != topLayerLocation.getBlockY())
                        if (isBottomLayer || isBorder) {
                            if (block.getType() == Material.AIR)
                                block.setType(Material.BEDROCK);
                        } else if (block.getType() == Material.AIR) {
                            List<String> contentList = this.plugin.getConfig().getStringList("mines." + mineType + ".content");
                            if (!contentList.isEmpty()) {
                                Random random = new Random();
                                Material content = Material.getMaterial(contentList.get(random.nextInt(contentList.size())));
                                if (content != null)
                                    block.setType(content);
                            }
                        }
                }
            }
        }
    }

    private String getMineType(Material material) {
        for (String type : this.plugin.getConfig().getConfigurationSection("mines").getKeys(false)) {
            if (material == Material.getMaterial(this.plugin.getConfig().getString("mines." + type + ".item")))
                return type;
        }
        return null;
    }
}
