package dev.restonic4.hide_and_seek;

import dev.restonic4.hide_and_seek.command.HideAndSeekCommand;
import dev.restonic4.hide_and_seek.command.SpawnCommand;
import dev.restonic4.hide_and_seek.manager.AreaManager;
import dev.restonic4.hide_and_seek.manager.GameManager;
import java.io.File;
import java.util.logging.Logger;

import dev.restonic4.hide_and_seek.manager.WhitelistManager;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

import dev.restonic4.hide_and_seek.bstats.Metrics;

public class HideAndSeekPlugin extends JavaPlugin {

    public static HideAndSeekPlugin instance;
    public static Logger log;
    public final static String NAME = "HideAndSeek";
    public final static int BSTATS_PLUGIN_ID = 20765;

    private GameManager gameManager;
    private AreaManager areaManager;
    private WhitelistManager whitelistManager;
    private String eventId;

    public HideAndSeekPlugin() {
        super();
    }

    protected HideAndSeekPlugin(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder,
            File file) {
        super(loader, description, dataFolder, file);
    }

    @Override
    public void onEnable() {
        instance = this;
        log = getLogger();
        logTemplateAttribution();

        saveDefaultConfig();
        this.eventId = getConfig().getString("event-id", "default-event-id");
        log.info("Using event-id: " + this.eventId + " for whitelist fetching.");

        this.gameManager = new GameManager();
        this.areaManager = new AreaManager(this.gameManager);
        this.whitelistManager = new WhitelistManager(this.eventId);

        setup();
        this.gameManager.startReminderTask();
        this.whitelistManager.startTasks();
        log.info("Ready!");
    }

    private void logTemplateAttribution() {
        log.info("[ATTRIBUTION] Powered by the PaperMC Plugin Template created by Joseph Hale (https://jhale.dev)");
    }

    private void setup() {
        getServer().getPluginManager().registerEvents(new HideAndSeekListener(gameManager), this);

        HideAndSeekCommand cmd = new HideAndSeekCommand(gameManager);
        getCommand("hs").setExecutor(cmd);
        getCommand("hs").setTabCompleter(cmd);

        SpawnCommand spawnCmd = new SpawnCommand(gameManager);
        getCommand("spawn").setExecutor(spawnCmd);

        new Metrics(this, BSTATS_PLUGIN_ID);
    }

    @Override
    public void onDisable() {
        log.info("Thanks for using " + NAME + "!");
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public AreaManager getAreaManager() {
        return areaManager;
    }

    public WhitelistManager getWhitelistManager() {
        return whitelistManager;
    }
}