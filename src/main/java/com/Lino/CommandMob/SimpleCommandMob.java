package com.Lino.CommandMob;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class SimpleCommandMob extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private FileConfiguration config;
    private final Map<String, String> messages = new HashMap<>(8);
    private final Map<EntityType, MobConfig> mobConfigs = new EnumMap<>(EntityType.class);
    private static final List<String> RELOAD_COMPLETION = Collections.singletonList("reload");

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfiguration();

        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("mobcommands").setExecutor(this);
        getCommand("mobcommands").setTabCompleter(this);

        getLogger().info("Plugin activated successfully!");
    }

    @Override
    public void onDisable() {
        // Clear maps
        messages.clear();
        mobConfigs.clear();
    }

    private void loadConfiguration() {
        config = getConfig();
        loadMessages();
        loadMobConfigs();
    }

    private void loadMessages() {
        messages.clear();

        // Load with defaults
        messages.put("reward-message", config.getString("messages.reward-message",
                "&aYou received a reward for killing %mob%!"));
        messages.put("admin-reload", config.getString("messages.admin-reload",
                "&aConfiguration reloaded successfully!"));
        messages.put("unknown-command", config.getString("messages.unknown-command",
                "&cUnknown command. Use /mobcommands reload"));
        messages.put("reload-error", config.getString("messages.reload-error",
                "&cError while reloading configuration: %error%"));
    }

    private void loadMobConfigs() {
        mobConfigs.clear();

        ConfigurationSection mobsSection = config.getConfigurationSection("mobs");
        if (mobsSection == null) return;

        for (String mobName : mobsSection.getKeys(false)) {
            try {
                EntityType entityType = EntityType.valueOf(mobName.toUpperCase());
                if (!entityType.isAlive()) continue;

                ConfigurationSection commandsSection = mobsSection.getConfigurationSection(
                        mobName + ".commands");
                if (commandsSection == null) continue;

                List<WeightedCommand> commands = new ArrayList<>();
                int totalWeight = 0;

                for (String command : commandsSection.getKeys(false)) {
                    int weight = commandsSection.getInt(command, 1);
                    if (weight > 0) {
                        commands.add(new WeightedCommand(command, weight));
                        totalWeight += weight;
                    }
                }

                if (!commands.isEmpty()) {
                    mobConfigs.put(entityType, new MobConfig(commands, totalWeight));
                }

            } catch (IllegalArgumentException ignored) {
                // Invalid entity type, skip
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        MobConfig config = mobConfigs.get(event.getEntityType());
        if (config == null) return;

        String command = config.getRandomCommand();
        if (command != null && !command.isEmpty()) {
            // Parse command with placeholders
            String parsed = command
                    .replace("%player%", killer.getName())
                    .replace("%uuid%", killer.getUniqueId().toString())
                    .replace("%world%", killer.getWorld().getName())
                    .replace("%x%", String.valueOf(killer.getLocation().getBlockX()))
                    .replace("%y%", String.valueOf(killer.getLocation().getBlockY()))
                    .replace("%z%", String.valueOf(killer.getLocation().getBlockZ()));

            // Remove minecraft: prefix if present (it's not needed for console commands)
            if (parsed.startsWith("minecraft:")) {
                parsed = parsed.substring(10); // length of "minecraft:"
            }
            // Remove leading slash if present
            if (parsed.length() > 0 && parsed.charAt(0) == '/') {
                parsed = parsed.substring(1);
            }

            executeCommand(parsed, killer, event.getEntityType().name());
        }
    }

    private void executeCommand(String command, Player player, String mobName) {
        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

            // Send reward message
            String msg = messages.get("reward-message");
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        msg.replace("%mob%", mobName.toLowerCase().replace('_', ' '))));
            }
        } catch (Exception e) {
            getLogger().warning("Failed to execute command: " + command + " - " + e.getMessage());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            try {
                reloadConfig();
                loadConfiguration();
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        messages.get("admin-reload")));
            } catch (Exception e) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        messages.get("reload-error").replace("%error%",
                                e.getMessage() != null ? e.getMessage() : "Unknown error")));
                getLogger().log(Level.SEVERE, "Error reloading configuration", e);
            }
            return true;
        }

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                messages.get("unknown-command")));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1 && "reload".startsWith(args[0].toLowerCase())) {
            return RELOAD_COMPLETION;
        }
        return Collections.emptyList();
    }

    // Optimized weighted random selection
    private static class MobConfig {
        private final List<WeightedCommand> commands;
        private final int totalWeight;

        MobConfig(List<WeightedCommand> commands, int totalWeight) {
            this.commands = commands;
            this.totalWeight = totalWeight;
        }

        String getRandomCommand() {
            if (commands.isEmpty() || totalWeight <= 0) return null;

            int random = ThreadLocalRandom.current().nextInt(totalWeight);
            int cumulative = 0;

            for (WeightedCommand wc : commands) {
                cumulative += wc.weight;
                if (random < cumulative) {
                    return wc.command;
                }
            }
            return null;
        }
    }

    private static class WeightedCommand {
        final String command;
        final int weight;

        WeightedCommand(String command, int weight) {
            this.command = command;
            this.weight = weight;
        }
    }
}