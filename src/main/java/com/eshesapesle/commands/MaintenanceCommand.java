package com.eshesapesle.commands;

import com.eshesapesle.EsHesapEsle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Bakım modu yönetim komutları
 */
public class MaintenanceCommand implements CommandExecutor, TabCompleter {
    
    private final EsHesapEsle plugin;
    
    public MaintenanceCommand(EsHesapEsle plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("eshesapesle.maintenance")) {
            sender.sendMessage(ChatColor.RED + "❌ Bu komutu kullanma yetkiniz yok!");
            return true;
        }
        
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "aç":
            case "ac":
            case "enable":
                enableMaintenance(sender);
                break;
                
            case "kapat":
            case "disable":
                disableMaintenance(sender);
                break;
                
            case "ekle":
            case "add":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "❌ Kullanım: /bakım ekle <oyuncu>");
                    return true;
                }
                addToWhitelist(sender, args[1]);
                break;
                
            case "sil":
            case "remove":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "❌ Kullanım: /bakım sil <oyuncu>");
                    return true;
                }
                removeFromWhitelist(sender, args[1]);
                break;
                
            case "liste":
            case "list":
                showWhitelist(sender);
                break;
                
            case "durum":
            case "status":
                showStatus(sender);
                break;
                
            default:
                sendHelpMessage(sender);
                break;
        }
        
        return true;
    }
    
    private void enableMaintenance(CommandSender sender) {
        plugin.getConfig().set("maintenance.enabled", true);
        plugin.saveConfig();
        
        // Tüm oyuncuları at (whitelist'te olmayanları)
        List<String> whitelist = plugin.getConfig().getStringList("maintenance.whitelist");
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!whitelist.contains(player.getName().toLowerCase()) && 
                !player.hasPermission("eshesapesle.maintenance.bypass")) {
                
                player.kickPlayer(getMaintenanceKickMessage());
            }
        }
        
        // Discord'a bildir
        if (plugin.getDiscordBot() != null) {
            plugin.getDiscordBot().sendMaintenanceStatusMessage(true);
        }
        
        sender.sendMessage(ChatColor.YELLOW + "🔧 Bakım modu aktifleştirildi!");
        sender.sendMessage(ChatColor.GRAY + "Whitelist'te olmayan oyuncular sunucudan atıldı.");
        
        // Konsola log
        plugin.getLogger().info("Bakım modu aktifleştirildi - " + sender.getName());
    }
    
    private void disableMaintenance(CommandSender sender) {
        plugin.getConfig().set("maintenance.enabled", false);
        plugin.saveConfig();
        
        // Discord'a bildir
        if (plugin.getDiscordBot() != null) {
            plugin.getDiscordBot().sendMaintenanceStatusMessage(false);
        }
        
        sender.sendMessage(ChatColor.GREEN + "✅ Bakım modu kapatıldı!");
        sender.sendMessage(ChatColor.GRAY + "Sunucu artık herkese açık.");
        
        // Konsola log
        plugin.getLogger().info("Bakım modu kapatıldı - " + sender.getName());
    }
    
    private void addToWhitelist(CommandSender sender, String playerName) {
        List<String> whitelist = plugin.getConfig().getStringList("maintenance.whitelist");
        String lowerName = playerName.toLowerCase();
        
        if (whitelist.contains(lowerName)) {
            sender.sendMessage(ChatColor.YELLOW + "⚠️ " + playerName + " zaten whitelist'te!");
            return;
        }
        
        whitelist.add(lowerName);
        plugin.getConfig().set("maintenance.whitelist", whitelist);
        plugin.saveConfig();
        
        sender.sendMessage(ChatColor.GREEN + "✅ " + playerName + " bakım whitelist'ine eklendi!");
        
        // Konsola log
        plugin.getLogger().info("Bakım whitelist'ine eklendi: " + playerName + " (Ekleyen: " + sender.getName() + ")");
    }
    
    private void removeFromWhitelist(CommandSender sender, String playerName) {
        List<String> whitelist = plugin.getConfig().getStringList("maintenance.whitelist");
        String lowerName = playerName.toLowerCase();
        
        if (!whitelist.contains(lowerName)) {
            sender.sendMessage(ChatColor.YELLOW + "⚠️ " + playerName + " whitelist'te değil!");
            return;
        }
        
        whitelist.remove(lowerName);
        plugin.getConfig().set("maintenance.whitelist", whitelist);
        plugin.saveConfig();
        
        // Eğer oyuncu çevrimiçiyse ve bakım aktifse at
        if (plugin.getConfig().getBoolean("maintenance.enabled", false)) {
            Player player = Bukkit.getPlayer(playerName);
            if (player != null && !player.hasPermission("eshesapesle.maintenance.bypass")) {
                player.kickPlayer(getMaintenanceKickMessage());
            }
        }
        
        sender.sendMessage(ChatColor.GREEN + "✅ " + playerName + " bakım whitelist'inden çıkarıldı!");
        
        // Konsola log
        plugin.getLogger().info("Bakım whitelist'inden çıkarıldı: " + playerName + " (Çıkaran: " + sender.getName() + ")");
    }
    
    private void showWhitelist(CommandSender sender) {
        List<String> whitelist = plugin.getConfig().getStringList("maintenance.whitelist");
        
        sender.sendMessage(ChatColor.GOLD + "📋 Bakım Whitelist'i:");
        sender.sendMessage(ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        
        if (whitelist.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Liste boş!");
        } else {
            for (int i = 0; i < whitelist.size(); i++) {
                String playerName = whitelist.get(i);
                boolean isOnline = Bukkit.getPlayer(playerName) != null;
                String status = isOnline ? ChatColor.GREEN + "●" : ChatColor.RED + "●";
                
                sender.sendMessage(ChatColor.GRAY + (i + 1) + ". " + status + " " + ChatColor.WHITE + playerName);
            }
        }
        
        sender.sendMessage(ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage(ChatColor.GRAY + "Toplam: " + ChatColor.YELLOW + whitelist.size() + ChatColor.GRAY + " oyuncu");
    }
    
    private void showStatus(CommandSender sender) {
        boolean maintenanceEnabled = plugin.getConfig().getBoolean("maintenance.enabled", false);
        List<String> whitelist = plugin.getConfig().getStringList("maintenance.whitelist");
        
        sender.sendMessage(ChatColor.GOLD + "🔧 Bakım Modu Durumu:");
        sender.sendMessage(ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        
        String status = maintenanceEnabled ? 
            ChatColor.RED + "🔴 AKTİF" : 
            ChatColor.GREEN + "🟢 KAPALI";
        sender.sendMessage(ChatColor.GRAY + "Durum: " + status);
        
        sender.sendMessage(ChatColor.GRAY + "Whitelist: " + ChatColor.YELLOW + whitelist.size() + ChatColor.GRAY + " oyuncu");
        
        if (maintenanceEnabled) {
            int onlineWhitelisted = 0;
            for (String name : whitelist) {
                if (Bukkit.getPlayer(name) != null) {
                    onlineWhitelisted++;
                }
            }
            sender.sendMessage(ChatColor.GRAY + "Çevrimiçi Whitelist: " + ChatColor.GREEN + onlineWhitelisted + ChatColor.GRAY + " oyuncu");
        }
        
        sender.sendMessage(ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }
    
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "🔧 Bakım Modu Komutları:");
        sender.sendMessage(ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage(ChatColor.YELLOW + "/bakım aç" + ChatColor.GRAY + " - Bakım modunu aktifleştir");
        sender.sendMessage(ChatColor.YELLOW + "/bakım kapat" + ChatColor.GRAY + " - Bakım modunu kapat");
        sender.sendMessage(ChatColor.YELLOW + "/bakım ekle <oyuncu>" + ChatColor.GRAY + " - Whitelist'e ekle");
        sender.sendMessage(ChatColor.YELLOW + "/bakım sil <oyuncu>" + ChatColor.GRAY + " - Whitelist'ten çıkar");
        sender.sendMessage(ChatColor.YELLOW + "/bakım liste" + ChatColor.GRAY + " - Whitelist'i göster");
        sender.sendMessage(ChatColor.YELLOW + "/bakım durum" + ChatColor.GRAY + " - Bakım durumunu göster");
        sender.sendMessage(ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }
    
    private String getMaintenanceKickMessage() {
        String serverName = plugin.getConfig().getString("discord.status-panel.server-name", "Minecraft Server");
        String discordLink = plugin.getConfig().getString("maintenance.discord-link", "https://discord.gg/example");
        String websiteLink = plugin.getConfig().getString("maintenance.website-link", "https://example.com");
        
        return ChatColor.RED + "🔧 " + ChatColor.BOLD + "BAKIM MODU AKTİF" + ChatColor.RESET + "\n\n" +
               ChatColor.YELLOW + serverName + ChatColor.GRAY + " şu anda bakım modunda!\n\n" +
               ChatColor.WHITE + "🔧 " + ChatColor.GRAY + "Sunucumuz güncelleme ve iyileştirmeler için\n" +
               ChatColor.GRAY + "geçici olarak kapatılmıştır.\n\n" +
               ChatColor.GREEN + "📢 " + ChatColor.WHITE + "Güncellemeler için:\n" +
               ChatColor.BLUE + "💬 Discord: " + ChatColor.AQUA + discordLink + "\n" +
               ChatColor.BLUE + "🌐 Website: " + ChatColor.AQUA + websiteLink + "\n\n" +
               ChatColor.YELLOW + "⏰ Yakında tekrar görüşmek üzere!";
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission("eshesapesle.maintenance")) {
            return completions;
        }
        
        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("aç", "kapat", "ekle", "sil", "liste", "durum");
            for (String sub : subcommands) {
                if (sub.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("ekle")) {
                // Çevrimiçi oyuncuları öner (whitelist'te olmayanları)
                List<String> whitelist = plugin.getConfig().getStringList("maintenance.whitelist");
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!whitelist.contains(player.getName().toLowerCase()) &&
                        player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(player.getName());
                    }
                }
            } else if (args[0].equalsIgnoreCase("sil")) {
                // Whitelist'teki oyuncuları öner
                List<String> whitelist = plugin.getConfig().getStringList("maintenance.whitelist");
                for (String name : whitelist) {
                    if (name.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(name);
                    }
                }
            }
        }
        
        return completions;
    }
}