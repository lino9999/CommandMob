package com.Lino.SimpleCommandMob;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SimpleCommandMob extends JavaPlugin implements Listener, CommandExecutor {

    private FileConfiguration config;
    private final Random random = new Random();
    private final Map<String, String> messages = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("mobcommands").setExecutor(this);
        getLogger().info(ChatColor.GREEN + "Plugin activated successfully!");
    }

    private void loadMessages() {
        messages.clear();
        ConfigurationSection msgSection = config.getConfigurationSection("messages");
        if (msgSection != null) {
            for (String key : msgSection.getKeys(false)) {
                messages.put(key, msgSection.getString(key));
            }
        }
        messages.putIfAbsent("admin-reload", "&aConfigurazione ricaricata!");
        messages.putIfAbsent("no-permission", "&cPermesso negato!");
        messages.putIfAbsent("reward-message", "&aPremio ricevuto!");
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;

        Player player = getPlayerFromDamager(event.getDamager());
        if (player == null) return;

        LivingEntity mob = (LivingEntity) event.getEntity();
        String mobName = mob.getType().name();

        ConfigurationSection mobSection = config.getConfigurationSection("mobs." + mobName);
        if (mobSection == null) return;

        String permission = mobSection.getString("permission");
        if (permission != null && !player.hasPermission(permission)) {
            event.setCancelled(true);
            sendMessage(player, messages.getOrDefault("no-permission", "&cNo access!")
                    .replace("%mob%", mobName.toLowerCase()));
        }
    }

    private Player getPlayerFromDamager(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        } else if (damager instanceof Projectile) {
            Object shooter = ((Projectile) damager).getShooter();
            if (shooter instanceof Player) {
                return (Player) shooter;
            }
        }
        return null;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity mob = event.getEntity();
        Player player = mob.getKiller();
        if (player == null) return;

        String mobName = mob.getType().name();
        ConfigurationSection mobSection = config.getConfigurationSection("mobs." + mobName);
        if (mobSection == null) return;

        ConfigurationSection commandsSection = mobSection.getConfigurationSection("commands");
        String command = getWeightedCommand(commandsSection);
        if (command != null && !command.isEmpty()) {
            executeCommand(command, player);
            sendMessage(player, messages.getOrDefault("reward-message", "&aPremio ricevuto!")
                    .replace("%mob%", mobName.toLowerCase()));
        }
    }

    private String getWeightedCommand(ConfigurationSection commandsSection) {
        if (commandsSection == null) return null;

        // Primo ciclo: calcola il peso totale
        int totalWeight = 0;
        for (String key : commandsSection.getKeys(false)) {
            totalWeight += commandsSection.getInt(key, 1);
        }
        if (totalWeight == 0) return null;

        int randomNumber = random.nextInt(totalWeight);
        int cumulativeWeight = 0;
        // Secondo ciclo: seleziona il comando in base al peso
        for (String cmd : commandsSection.getKeys(false)) {
            cumulativeWeight += commandsSection.getInt(cmd, 1);
            if (randomNumber < cumulativeWeight) {
                return cmd;
            }
        }
        return null;
    }

    private void executeCommand(String command, Player player) {
        String parsedCommand = command
                .replace("%player%", player.getName())
                .replace("%uuid%", player.getUniqueId().toString())
                .replace("%world%", player.getWorld().getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand);
    }

    private void sendMessage(Player player, String message) {
        if (message == null || message.isEmpty()) return;
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("mobcommands") && args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("mobcommands.reload")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        messages.getOrDefault("no-permission", "&cNo access!")));
                return true;
            }
            try {
                reloadConfig();
                String reloadMsg = messages.getOrDefault("admin-reload",
                        "&aConfigurazione ricaricata con successo!");
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', reloadMsg));
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "Error while reloading: " + e.getMessage());
                getLogger().severe("Error in reloading: " + e);
            }
            return true;
        }
        return false;
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        config = getConfig();
        loadMessages();
    }

    @Override
    public void onDisable() {
        getLogger().info(ChatColor.RED + "Plugin deactivated!");
    }
}
