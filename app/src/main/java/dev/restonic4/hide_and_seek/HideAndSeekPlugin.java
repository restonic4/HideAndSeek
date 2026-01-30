package dev.restonic4.hide_and_seek;

import dev.restonic4.hide_and_seek.command.HideAndSeekCommand;
import dev.restonic4.hide_and_seek.manager.GameManager;
import java.io.File;
import java.util.logging.Logger;

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

        this.gameManager = new GameManager();

        setup();
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

        new Metrics(this, BSTATS_PLUGIN_ID);
    }

    @Override
    public void onDisable() {
        log.info("Thanks for using " + NAME + "!");
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}