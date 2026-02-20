package dev.restonic4.hide_and_seek;

import dev.restonic4.hide_and_seek.manager.CuboidArea;
import dev.restonic4.hide_and_seek.manager.GameAreas;
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
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.SpongeAbsorbEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.entity.Projectile;
import org.bukkit.block.Sign;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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
        Player player = event.getPlayer();

        // Main Play Area enforcement
        if (!GameAreas.getPlayArea().isInside(event.getTo())) {
            player.teleport(GameAreas.getSpawn());
            player.sendMessage(Component.text("You went outside the play area!", NamedTextColor.RED));
            return;
        }

        // Subarea enforcement (Hiders only)
        if (gameManager.isHider(player)) {
            for (CuboidArea area : GameAreas.getSubAreas()) {
                if (!area.isEnabled() && area.isInside(event.getTo())) {
                    event.setCancelled(true);
                    player.sendMessage(Component.text("This area is currently disabled!", NamedTextColor.RED));
                    return;
                }
            }
        }

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
        Player player = null;
        if (event.getDamager() instanceof Player p) {
            player = p;
        } else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player p) {
            player = p;
        }

        if (player == null) {
            return;
        }

        if (event.getEntity() instanceof Player hider) {
            if (gameManager.isRunning() && gameManager.isSeeker(player) && gameManager.isHider(hider)) {
                gameManager.catchHider(hider);
            }
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (event.getRemover() instanceof Player player) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getClickedBlock() != null && event.getClickedBlock().getState() instanceof Sign) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockGrow(BlockGrowEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockForm(BlockFormEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().clear();
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onSpongeAbsorb(SpongeAbsorbEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (event.getBlock().getType().hasGravity()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        event.setCancelled(true);
    }
}