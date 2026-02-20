package dev.restonic4.hide_and_seek.command;

import dev.restonic4.hide_and_seek.HideAndSeekPlugin;
import dev.restonic4.hide_and_seek.manager.CuboidArea;
import dev.restonic4.hide_and_seek.manager.GameAreas;
import dev.restonic4.hide_and_seek.manager.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class HideAndSeekCommand implements CommandExecutor, TabCompleter {
    private final GameManager gameManager;

    public HideAndSeekCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(Component.text("You must be an OP to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(
                    Component.text("Usage: /hs <seeker|start|end|freeze|unfreeze|countdown|win>", NamedTextColor.RED));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "seeker":
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /hs seeker <player>", NamedTextColor.RED));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                    return true;
                }
                gameManager.setSeeker(target);
                sender.sendMessage(Component.text("Set " + target.getName() + " as the seeker.", NamedTextColor.GREEN));
                break;

            case "start":
                gameManager.startGame();
                sender.sendMessage(Component.text("Game started!", NamedTextColor.GREEN));
                break;

            case "end":
                gameManager.stopGame();
                sender.sendMessage(Component.text("Game ended!", NamedTextColor.YELLOW));
                break;

            case "freeze":
                gameManager.setFrozen(true);
                sender.sendMessage(Component.text("Everyone frozen.", NamedTextColor.AQUA));
                break;

            case "unfreeze":
                gameManager.setFrozen(false);
                sender.sendMessage(Component.text("Everyone unfrozen.", NamedTextColor.GREEN));
                break;

            case "countdown":
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /hs countdown <seconds>", NamedTextColor.RED));
                    return true;
                }
                try {
                    int seconds = Integer.parseInt(args[1]);
                    gameManager.startCountdown(seconds);
                    sender.sendMessage(
                            Component.text("Countdown started: " + seconds + " seconds.", NamedTextColor.GREEN));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid number of seconds.", NamedTextColor.RED));
                }
                break;

            case "win":
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /hs win <winner_name>", NamedTextColor.RED));
                    return true;
                }
                String winner = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                gameManager.broadcastWin(winner);
                break;

            case "area":
                if (args.length < 3) {
                    sender.sendMessage(
                            Component.text("Usage: /hs area <name> <enable|disable|warn>", NamedTextColor.RED));
                    return true;
                }
                String areaName = args[1];
                String action = args[2].toLowerCase();

                CuboidArea foundArea = null;
                for (CuboidArea area : GameAreas.getSubAreas()) {
                    if (area.getName().equalsIgnoreCase(areaName)) {
                        foundArea = area;
                        break;
                    }
                }

                if (foundArea == null) {
                    sender.sendMessage(Component.text("Area not found. Available: " +
                            GameAreas.getSubAreas().stream().map(CuboidArea::getName).collect(Collectors.joining(", ")),
                            NamedTextColor.RED));
                    return true;
                }

                if (action.equalsIgnoreCase("warn")) {
                    HideAndSeekPlugin.instance.getAreaManager().warnSubArea(foundArea);
                    sender.sendMessage(Component.text("Sent warning to hiders in " + foundArea.getName() + "!",
                            NamedTextColor.GREEN));
                } else {
                    boolean enable = action.equalsIgnoreCase("enable") || action.equalsIgnoreCase("on");
                    HideAndSeekPlugin.instance.getAreaManager().setSubAreaEnabled(foundArea, enable);
                }
                break;

            default:
                sender.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED));
                break;
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("seeker", "start", "end", "freeze", "unfreeze", "countdown", "win", "area").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("area")) {
            return GameAreas.getSubAreas().stream()
                    .map(CuboidArea::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("area")) {
            return Arrays.asList("enable", "disable", "warn").stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("seeker")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
