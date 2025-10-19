package com.giuliocappelli.autoPayday;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class AutoPayday extends JavaPlugin {

    private static Economy economy = null;
    private long intervalTicks;
    private double onlineAmount;
    private double offlineAmount;
    private String message;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Read values from config.yml
        long minutes = getConfig().getLong("interval-minutes", 30);
        onlineAmount = getConfig().getDouble("online-amount", 250.0);
        offlineAmount = getConfig().getDouble("offline-amount", 20.0);
        message = getConfig().getString("message", "<green>💸 You have recieved <gold>{amount}$!</gold>");

        // Calculate ticks (20 tick = 1 secondo)
        intervalTicks = minutes * 60L * 20L;

        // Setup Vault
        if (!setupEconomy()) {
            getLogger().severe("Vault not found! Plugin disabled");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Periodic Task
        new BukkitRunnable() {
            @Override
            public void run() {
                payPlayers();
            }
        }.runTaskTimer(this, 0L, intervalTicks);

        getLogger().info("AutoPayday enabled with an interval of " + minutes + " minutes.");
    }

    @Override
    public void onDisable() {
        getLogger().info("AutoPayday disabled.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }


    private void payPlayers() {
        int iOnline = 0;
        int iOffline = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            iOnline++;
            economy.depositPlayer(player, onlineAmount);
            player.sendMessage(
                    MiniMessage.miniMessage().deserialize(
                            message.replace("{amount}", String.valueOf(onlineAmount))
                    )
            );
        }
        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            if (!offline.isOnline()) {
                iOffline++;
                economy.depositPlayer(offline, offlineAmount);
            }
        }

        getLogger().info("AutoPayday: payday given, online=" + iOnline + ", offline=" + iOffline);
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("autopayday")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("autopayday.reload")) {
                    sender.sendMessage("§cYou don't have the permission to reload the plugin!");
                    return true;
                }
                reloadConfig();
                sender.sendMessage("§aAutoPayday: config reloaded!");
                getLogger().info(sender.getName() + " has reloaded the config!.");
                return true;
            }
            sender.sendMessage("§eUse: /autopayday reload");
            return true;
        }
        return false;
    }

}
