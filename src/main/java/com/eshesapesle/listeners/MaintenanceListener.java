package com.eshesapesle.listeners;

import com.eshesapesle.EsHesapEsle;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.List;

/**
 * Bakım modu giriş kontrolü
 */
public class MaintenanceListener implements Listener {
    
    private final EsHesapEsle plugin;
    
    public MaintenanceListener(EsHesapEsle plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        // Bakım modu aktif değilse izin ver
        if (!plugin.getConfig().getBoolean("maintenance.enabled", false)) {
            return;
        }
        
        // Bypass yetkisi varsa izin ver
        if (event.getPlayer().hasPermission("eshesapesle.maintenance.bypass")) {
            return;
        }
        
        // Whitelist'te varsa izin ver
        List<String> whitelist = plugin.getConfig().getStringList("maintenance.whitelist");
        if (whitelist.contains(event.getPlayer().getName().toLowerCase())) {
            return;
        }
        
        // Bakım mesajı ile reddet
        String kickMessage = getMaintenanceMessage();
        event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, kickMessage);
        
        // Log
        plugin.getLogger().info("Bakım modu nedeniyle giriş reddedildi: " + event.getPlayer().getName());
    }
    
    private String getMaintenanceMessage() {
        String serverName = plugin.getConfig().getString("discord.status-panel.server-name", "Minecraft Server");
        String discordLink = plugin.getConfig().getString("maintenance.discord-link", "https://discord.gg/example");
        String websiteLink = plugin.getConfig().getString("maintenance.website-link", "https://example.com");
        String estimatedTime = plugin.getConfig().getString("maintenance.estimated-time", "Bilinmiyor");
        
        return ChatColor.RED + "🔧 " + ChatColor.BOLD + "BAKIM MODU AKTİF" + ChatColor.RESET + "\n\n" +
               ChatColor.YELLOW + serverName + ChatColor.GRAY + " şu anda bakım modunda!\n\n" +
               ChatColor.WHITE + "🔧 " + ChatColor.GRAY + "Sunucumuz güncelleme ve iyileştirmeler için\n" +
               ChatColor.GRAY + "geçici olarak kapatılmıştır.\n\n" +
               ChatColor.GOLD + "⏰ Tahmini Süre: " + ChatColor.YELLOW + estimatedTime + "\n\n" +
               ChatColor.GREEN + "📢 " + ChatColor.WHITE + "Güncellemeler için:\n" +
               ChatColor.BLUE + "💬 Discord: " + ChatColor.AQUA + discordLink + "\n" +
               ChatColor.BLUE + "🌐 Website: " + ChatColor.AQUA + websiteLink + "\n\n" +
               ChatColor.YELLOW + "⏰ Yakında tekrar görüşmek üzere!\n" +
               ChatColor.GRAY + "Anlayışınız için teşekkürler. ❤️";
    }
}