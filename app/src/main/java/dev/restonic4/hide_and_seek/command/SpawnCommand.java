package dev.restonic4.hide_and_seek.command;

import dev.restonic4.hide_and_seek.manager.GameAreas;
import dev.restonic4.hide_and_seek.manager.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public class SpawnCommand implements CommandExecutor {
    private final GameManager gameManager;

    public SpawnCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        if (gameManager.isSeeker(player)) {
            player.teleport(GameAreas.getSpawn());
            player.sendMessage(Component.text("Teleported to spawn.", NamedTextColor.GREEN));
            return true;
        }

        if (gameManager.isHider(player)) {
            int usageCount = gameManager.getSpawnUsage(player);

            double slownessDuration = 15.0 * Math.pow(1.5, usageCount);
            double glowingDuration = 30.0 * Math.pow(1.5, usageCount);

            // Apply slowness II (amplifier 1)
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (slownessDuration * 20), 1));
            // Apply glowing
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, (int) (glowingDuration * 20), 0));

            player.teleport(GameAreas.getSpawn());
            gameManager.incrementSpawnUsage(player);

            player.sendMessage(
                    Component.text("Teleported to spawn. You have been penalized for using /spawn as a hider!",
                            NamedTextColor.YELLOW));
            player.sendMessage(
                    Component.text("Slowness duration: " + (int) slownessDuration + "s", NamedTextColor.RED));
            player.sendMessage(Component.text("Glowing duration: " + (int) glowingDuration + "s", NamedTextColor.RED));

            return true;
        }

        // If not in game, just teleport?
        player.teleport(GameAreas.getSpawn());
        player.sendMessage(Component.text("Teleported to spawn.", NamedTextColor.GREEN));

        return true;
    }
}
