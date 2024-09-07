package dk.alpha;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private FileConfigurationHandler fileConfigHandler;
    private HologramHandler hologramHandler;
    private PlayerInteractHandler playerInteractHandler;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.fileConfigHandler = new FileConfigurationHandler(this);
        this.hologramHandler = new HologramHandler(this);
        this.playerInteractHandler = new PlayerInteractHandler(this, this.fileConfigHandler, this.hologramHandler);

        // Register event listeners
        getServer().getPluginManager().registerEvents(this.playerInteractHandler, this);

        // Register command executors
        getCommand("mine").setExecutor(new MineCommandExecutor(this, this.fileConfigHandler, this.playerInteractHandler));

        getLogger().info("Mine Plugin Enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Mine Plugin Disabled!");
    }
}
