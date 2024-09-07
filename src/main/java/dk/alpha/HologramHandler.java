package dk.alpha;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HologramHandler {

    private final JavaPlugin plugin;
    private final Map<Location, ArmorStand> hologramMap = new HashMap<>();

    public HologramHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void addMineHologram(Location location, String mineType, String playerName, long resetTimeMillis) {
        FileConfiguration config = plugin.getConfig();
        List<String> hologramLines = config.getStringList("holograms." + mineType + ".text");
        double offsetY = config.getDouble("holograms." + mineType + ".offsetY", 1.0);

        // Remove any existing hologram
        removeExistingHologram(location);

        // Create a new hologram
        createHologramLines(location, hologramLines, offsetY, mineType, playerName, resetTimeMillis);
    }

    private void createHologramLines(Location location, List<String> lines, double offsetY, String mineType, String playerName, long resetTimeMillis) {
        Location hologramLocation = location.clone().add(0.5, offsetY, 0.5);

        // Hvis der allerede er hologrammer på denne placering, opdater dem i stedet
        for (Map.Entry<Location, ArmorStand> entry : hologramMap.entrySet()) {
            if (entry.getKey().getWorld().equals(location.getWorld()) && entry.getKey().distance(location) < 1.0) {
                ArmorStand hologram = entry.getValue();
                if (hologram != null && hologram.isValid()) {
                    hologram.remove(); // Fjern eksisterende hologrammer
                }
            }
        }

        // Opret nye hologrammer
        for (String line : lines) {
            String processedLine = ChatColor.translateAlternateColorCodes('&', processPlaceholders(line, mineType, playerName, location, resetTimeMillis));
            ArmorStand hologram = (ArmorStand) location.getWorld().spawnEntity(hologramLocation, EntityType.ARMOR_STAND);
            hologram.setCustomName(processedLine);
            hologram.setCustomNameVisible(true);
            hologram.setGravity(false);
            hologram.setVisible(false);
            hologram.setCanPickupItems(false);
            hologram.setBasePlate(false);

            // Opdater placering og gem
            hologramMap.put(hologramLocation.clone().add(0, -0.5 * lines.indexOf(line), 0), hologram);

            // Flyt ned for næste linje
            hologramLocation.add(0, -0.5, 0);
        }
    }


    private void removeExistingHologram(Location location) {
        // Remove existing holograms at the location
        for (Map.Entry<Location, ArmorStand> entry : hologramMap.entrySet()) {
            if (entry.getKey().getWorld().equals(location.getWorld()) && entry.getKey().distance(location) < 1.0) {
                ArmorStand hologram = entry.getValue();
                if (hologram != null && hologram.isValid()) {
                    hologram.remove();
                }
            }
        }
        hologramMap.clear(); // Clear map after removal
    }

    private String processPlaceholders(String text, String mineType, String playerName, Location location, long resetTimeMillis) {
        text = text.replace("%minename%", mineType);
        text = text.replace("%player%", playerName);
        text = text.replace("%x%", String.valueOf(location.getBlockX()));
        text = text.replace("%y%", String.valueOf(location.getBlockY()));
        text = text.replace("%z%", String.valueOf(location.getBlockZ()));
        text = text.replace("%reset_time%", formatTime(resetTimeMillis)); // Format time correctly
        return text;
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        return String.format("%02d:%02d", seconds / 60, seconds % 60); // Format time as MM:SS
    }

    // Update existing hologram with the new reset time
    public void updateHologram(Location location, String mineType, long resetTimeMillis) {
        // Debug udskrift
        System.out.println("updateHologram kaldt med location: " + location);
        System.out.println("MineType: " + mineType);
        System.out.println("ResetTimeMillis: " + resetTimeMillis);

        // Find eksisterende hologrammer
        List<ArmorStand> holograms = findExistingHolograms(location);

        // Opret en ny liste med opdaterede hologram linjer
        FileConfiguration config = plugin.getConfig();
        List<String> hologramLines = config.getStringList("holograms." + mineType + ".text");

        // Opdater hver eksisterende hologram med ny tekst
        int index = 0;
        for (ArmorStand hologram : holograms) {
            if (index >= hologramLines.size()) break; // Stop hvis der er flere hologrammer end linjer

            String line = hologramLines.get(index);
            String processedLine = ChatColor.translateAlternateColorCodes('&', processPlaceholders(line, mineType, "Player", location, resetTimeMillis));
            hologram.setCustomName(processedLine);
            hologram.setCustomNameVisible(true);

            index++;
        }

        // Fjern ekstra hologrammer, hvis der er flere hologrammer end linjer
        while (index < holograms.size()) {
            holograms.get(index).remove();
            index++;
        }
    }

    private List<ArmorStand> findExistingHolograms(Location location) {
        List<ArmorStand> holograms = new ArrayList<>();
        World world = location.getWorld();
        if (world == null) return holograms; // Hvis verdenen ikke findes, returner en tom liste

        for (Entity entity : world.getEntities()) {
            if (entity instanceof ArmorStand) {
                ArmorStand armorStand = (ArmorStand) entity;
                // Tjek om ArmorStand er på den ønskede position
                if (armorStand.getLocation().distance(location) < 1.0) { // 1.0 blok afvigelse
                    holograms.add(armorStand);
                }
            }
        }
        return holograms;
    }
}
