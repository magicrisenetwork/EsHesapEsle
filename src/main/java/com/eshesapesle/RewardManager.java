package com.eshesapesle;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

public class RewardManager {
    private final EsHesapEsle plugin;
    private final Set<UUID> usedRewards;
    private final Random random;
    
    public RewardManager(EsHesapEsle plugin) {
        this.plugin = plugin;
        this.usedRewards = new HashSet<>();
        this.random = new Random();
    }
    
    public void giveRandomReward(Player player) {
        FileConfiguration config = plugin.getConfig();
        
        // Ödül sistemi aktif mi kontrol et
        if (!config.getBoolean("discord.reward.enabled", true)) {
            player.sendMessage(ChatColor.RED + "Ödül sistemi şu anda devre dışı!");
            return;
        }
        
        // Daha önce ödül almış mı kontrol et
        if (usedRewards.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Zaten ödülünüzü aldınız!");
            return;
        }
        
        List<String> rewards = config.getStringList("discord.reward.rewards");
        if (rewards.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Ödül listesi boş!");
            return;
        }
        
        // Rastgele bir ödül seç
        String reward = rewards.get(random.nextInt(rewards.size()));
        
        // Ödülü kullanıldı olarak işaretle (komut çalıştırılmadan önce)
        usedRewards.add(player.getUniqueId());
        
        // Ödülü ver (Ana thread'de çalıştır)
        String command = reward.replace("%player%", player.getName());
        
        // Ana thread'de mi kontrol et
        if (Bukkit.isPrimaryThread()) {
            // Zaten ana thread'deyiz, direkt çalıştır
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            sendRewardNotifications(player, reward);
        } else {
            // Asenkron thread'deyiz, ana thread'e geç
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                sendRewardNotifications(player, reward);
            });
        }
    }
    
    private void sendRewardNotifications(Player player, String reward) {
        // Discord'a bildirim gönder
        String channelId = plugin.getConfig().getString("discord.reward.channel-id");
        if (channelId != null && !channelId.isEmpty()) {
            plugin.getDiscordBot().sendRewardMessage(player, reward);
        }
        
        // Oyuncuya bildirim gönder
        player.sendMessage(ChatColor.GREEN + "Ödülünüz başarıyla verildi!");
    }
    
    public boolean hasUsedReward(UUID playerUUID) {
        return usedRewards.contains(playerUUID);
    }
} 