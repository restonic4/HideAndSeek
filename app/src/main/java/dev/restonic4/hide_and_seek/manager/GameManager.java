package dev.restonic4.hide_and_seek.manager;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GameManager {
    private UUID seeker;
    private final Set<UUID> hiders = new HashSet<>();
    private final Set<UUID> spectators = new HashSet<>();
    private boolean frozen = false;
    private boolean running = false;

    public void setSeeker(Player player) {
        this.seeker = player.getUniqueId();
        this.hiders.remove(this.seeker);
        this.spectators.remove(this.seeker);
        updateAllTabLists();
    }

    public UUID getSeeker() {
        return seeker;
    }

    public void addHider(Player player) {
        if (seeker != null && seeker.equals(player.getUniqueId())) {
            seeker = null;
        }
        hiders.add(player.getUniqueId());
        spectators.remove(player.getUniqueId());
        updateAllTabLists();
    }

    public void catchHider(Player hider) {
        if (hiders.remove(hider.getUniqueId())) {
            spectators.add(hider.getUniqueId());
            hider.setGameMode(GameMode.SPECTATOR);
            Bukkit.broadcast(Component.text(hider.getName() + " got caught!", NamedTextColor.RED));
            updateAllTabLists();
        }
    }

    public void startGame() {
        running = true;
        hiders.clear();
        spectators.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (seeker != null && player.getUniqueId().equals(seeker)) {
                player.setGameMode(GameMode.ADVENTURE);
            } else {
                hiders.add(player.getUniqueId());
                player.setGameMode(GameMode.ADVENTURE);
            }
        }
        updateAllTabLists();
        Bukkit.broadcast(Component.text("Game started!", NamedTextColor.GREEN));
    }

    public void stopGame() {
        running = false;
        seeker = null;
        hiders.clear();
        spectators.clear();
        frozen = false;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGameMode(GameMode.ADVENTURE);
        }
        updateAllTabLists();
        Bukkit.broadcast(Component.text("Game ended!", NamedTextColor.YELLOW));
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
        if (frozen) {
            Bukkit.broadcast(Component.text("Everyone is frozen!", NamedTextColor.AQUA));
        } else {
            Bukkit.broadcast(Component.text("You can move now!", NamedTextColor.GREEN));
        }
    }

    public void startCountdown(int seconds) {
        setFrozen(true);
        new org.bukkit.scheduler.BukkitRunnable() {
            int count = seconds;

            @Override
            public void run() {
                if (count <= 0) {
                    setFrozen(false);
                    this.cancel();
                    return;
                }
                Bukkit.broadcast(Component.text("Countdown: " + count, NamedTextColor.GOLD));
                count--;
            }
        }.runTaskTimer(dev.restonic4.hide_and_seek.HideAndSeekPlugin.instance, 0L, 20L);
    }

    public void broadcastWin(String winner) {
        Title title = Title.title(
                Component.text(winner + " Wins!", NamedTextColor.GOLD),
                Component.text("The game has concluded.", NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500)));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(title);
        }
        Bukkit.broadcast(Component.text(winner + " has won the game!", NamedTextColor.GOLD));
    }

    public void updateAllTabLists() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateTabList(player);
        }
    }

    public void updateTabList(Player player) {
        // Requirements: "We only render the hiders, and if they got caught, they get in
        // spectator and do not count on the player list."
        // We will customize the player list names and use visibility to "remove" caught
        // players.

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (isHider(online)) {
                // Hiders get a special name in tab
                online.playerListName(Component.text("[Hider] ", NamedTextColor.GRAY)
                        .append(Component.text(online.getName(), NamedTextColor.GREEN)));
                player.showPlayer(dev.restonic4.hide_and_seek.HideAndSeekPlugin.instance, online);
            } else if (isSeeker(online)) {
                // Seeker might be hidden from tab if possible, or just labeled.
                // Requirement says "only render hiders", so we'll try to keep Seeker name empty
                // in tab if they must be visible in world.
                online.playerListName(Component.empty());
                player.showPlayer(dev.restonic4.hide_and_seek.HideAndSeekPlugin.instance, online);
            } else if (spectators.contains(online.getUniqueId())) {
                // Spectators (caught hiders) do not count and shouldn't be in the list.
                // Hiding them entirely from non-spectators is standard.
                if (!player.getGameMode().equals(GameMode.SPECTATOR)) {
                    player.hidePlayer(dev.restonic4.hide_and_seek.HideAndSeekPlugin.instance, online);
                } else {
                    player.showPlayer(dev.restonic4.hide_and_seek.HideAndSeekPlugin.instance, online);
                    online.playerListName(Component.text("[Caught] " + online.getName(), NamedTextColor.RED));
                }
            } else {
                // Others (ops not in game, etc)
                player.showPlayer(dev.restonic4.hide_and_seek.HideAndSeekPlugin.instance, online);
            }
        }

        // Custom Header/Footer to fulfill "rendered with custom data"
        player.sendPlayerListHeaderAndFooter(
                Component.text("--- Hide and Seek ---", NamedTextColor.GOLD),
                Component.text("Hiders remaining: " + hiders.size(), NamedTextColor.YELLOW));
    }

    public boolean isHider(Player player) {
        return hiders.contains(player.getUniqueId());
    }

    public boolean isSeeker(Player player) {
        return seeker != null && seeker.equals(player.getUniqueId());
    }

    public boolean isRunning() {
        return running;
    }
}
