package dev.restonic4.hide_and_seek;

import dev.restonic4.hide_and_seek.manager.GameManager;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class HideAndSeekListener implements Listener {
    private final GameManager gameManager;

    public HideAndSeekListener(GameManager gameManager) {
        this.gameManager = gameManager;
        setupTeams();
    }

    private void setupTeams() {
        Scoreboard scoreboard = HideAndSeekPlugin.instance.getServer().getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam("HAS_HIDDEN");
        if (team == null) {
            team = scoreboard.registerNewTeam("HAS_HIDDEN");
        }
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        team.setCanSeeFriendlyInvisibles(false);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.setGameMode(GameMode.ADVENTURE);

        Scoreboard scoreboard = HideAndSeekPlugin.instance.getServer().getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam("HAS_HIDDEN");
        if (team != null) {
            team.addEntry(player.getName());
        }

        if (gameManager.isRunning() && !gameManager.isSeeker(player) && !gameManager.isHider(player)) {
            // New players joining during a game become hiders? Or spectators?
            // User said "rest are hiders" but usually mid-game joining means spectator.
            // Let's make them hiders for now as requested "rest are hiders".
            gameManager.addHider(player);
        }

        gameManager.updateAllTabLists();
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        HideAndSeekPlugin.instance.getServer().getScheduler().runTaskLater(HideAndSeekPlugin.instance,
                gameManager::updateAllTabLists, 1L);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (gameManager.isFrozen()) {
            // Prevent movement but allow looking around
            if (event.getFrom().getX() != event.getTo().getX() ||
                    event.getFrom().getY() != event.getTo().getY() ||
                    event.getFrom().getZ() != event.getTo().getZ()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!event.getPlayer().isOp()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!event.getPlayer().isOp()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // "The players do not take any kind of damage"
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player seeker && event.getEntity() instanceof Player hider) {
            if (gameManager.isRunning() && gameManager.isSeeker(seeker) && gameManager.isHider(hider)) {
                gameManager.catchHider(hider);
            }
        }
    }
}