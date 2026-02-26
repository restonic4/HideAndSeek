package dev.restonic4.hide_and_seek.manager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.restonic4.hide_and_seek.HideAndSeekPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class WhitelistManager {
    private static final String API_URL_TEMPLATE = "https://api.chaotic-loom.com/events/%s/whitelist";
    private static final Gson gson = new Gson();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    private final String eventId;
    private List<WhitelistEntry> whitelist = new ArrayList<>();

    public WhitelistManager(String eventId) {
        this.eventId = eventId;
    }

    public void startTasks() {
        // Run immediately
        updateWhitelistAsync();

        // Every 5 minutes (5 * 60 * 20 ticks = 6000 ticks)
        new BukkitRunnable() {
            @Override
            public void run() {
                updateWhitelistAsync();
            }
        }.runTaskTimerAsynchronously(HideAndSeekPlugin.instance, 6000L, 6000L);
    }

    private void updateWhitelistAsync() {
        HideAndSeekPlugin.log.info("Fetching whitelist from API...");
        fetchWhitelist().thenAccept(newWhitelist -> {
            if (newWhitelist != null) {
                this.whitelist = newWhitelist;
                HideAndSeekPlugin.log.info("Whitelist updated successfully. " + whitelist.size() + " entries.");
            } else {
                HideAndSeekPlugin.log.warning("Failed to update whitelist. Retrying in 30 seconds.");
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        updateWhitelistAsync();
                    }
                }.runTaskLaterAsynchronously(HideAndSeekPlugin.instance, 600L); // 30 seconds = 600 ticks
            }
        });
    }

    private CompletableFuture<List<WhitelistEntry>> fetchWhitelist() {
        String url = String.format(API_URL_TEMPLATE, eventId);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return (List<WhitelistEntry>) gson.fromJson(response.body(),
                                new TypeToken<List<WhitelistEntry>>() {
                                }.getType());
                    } else {
                        HideAndSeekPlugin.log
                                .warning("API returned status code: " + response.statusCode() + " for URL: " + url);
                        return (List<WhitelistEntry>) null;
                    }
                })
                .exceptionally(ex -> {
                    HideAndSeekPlugin.log.severe("Error fetching whitelist: " + ex.getMessage());
                    return (List<WhitelistEntry>) null;
                });
    }

    public boolean isWhitelisted(UUID minecraftUuid) {
        // Allow OPs
        Player player = Bukkit.getPlayer(minecraftUuid);
        if (player != null && player.isOp()) {
            return true;
        }

        // Check local whitelist
        for (WhitelistEntry entry : whitelist) {
            if (entry.minecraft_uuid != null && entry.minecraft_uuid.equalsIgnoreCase(minecraftUuid.toString())) {
                return true;
            }
        }

        // Check Minecraft built-in whitelist
        return Bukkit.getWhitelistedPlayers().stream()
                .anyMatch(p -> p.getUniqueId().equals(minecraftUuid));
    }

    public boolean isWhitelistedOffline(UUID minecraftUuid) {
        if (Bukkit.getOfflinePlayer(minecraftUuid).isOp()) {
            return true;
        }

        for (WhitelistEntry entry : whitelist) {
            if (entry.minecraft_uuid != null && entry.minecraft_uuid.equalsIgnoreCase(minecraftUuid.toString())) {
                return true;
            }
        }

        return Bukkit.getWhitelistedPlayers().stream()
                .anyMatch(p -> p.getUniqueId().equals(minecraftUuid));
    }

    public WhitelistEntry getEntry(UUID minecraftUuid) {
        for (WhitelistEntry entry : whitelist) {
            if (entry.minecraft_uuid != null && entry.minecraft_uuid.equalsIgnoreCase(minecraftUuid.toString())) {
                return entry;
            }
        }
        return null;
    }

    public static class WhitelistEntry {
        public String uuid;
        public String minecraft_uuid;
        public String role;
        public boolean is_premium;
    }
}
