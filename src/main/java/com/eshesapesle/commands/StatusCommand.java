package com.eshesapesle.commands;

import com.eshesapesle.EsHesapEsle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.ChatColor;

/**
 * Status panel test komutu
 */
public class StatusCommand implements CommandExecutor {
    private final EsHesapEsle plugin;

    public StatusCommand(EsHesapEsle plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("esh.admin")) {
            sender.sendMessage(ChatColor.RED + "Bu komutu kullanma yetkiniz yok!");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "=== Status Panel Komutları ===");
            sender.sendMessage(ChatColor.GOLD + "/status reload " + ChatColor.GRAY + "- Status paneli yeniden başlatır");
            sender.sendMessage(ChatColor.GOLD + "/status update " + ChatColor.GRAY + "- Status paneli manuel günceller");
            sender.sendMessage(ChatColor.GOLD + "/status info " + ChatColor.GRAY + "- Status panel bilgilerini gösterir");
            sender.sendMessage(ChatColor.GOLD + "/status mail <send|reload|status> " + ChatColor.GRAY + "- Mail sistemi yönetimi");

            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                if (plugin.getDiscordBot() != null && plugin.getDiscordBot().getLiveStatusPanel() != null) {
                    plugin.getDiscordBot().getLiveStatusPanel().reload();
                    sender.sendMessage(ChatColor.GREEN + "✅ Status panel yeniden başlatıldı!");
                } else {
                    sender.sendMessage(ChatColor.RED + "❌ Discord bot veya status panel aktif değil!");
                }
                break;

            case "update":
                if (plugin.getDiscordBot() != null && plugin.getDiscordBot().getLiveStatusPanel() != null) {
                    plugin.getDiscordBot().getLiveStatusPanel().forceUpdate();
                    sender.sendMessage(ChatColor.GREEN + "✅ Status panel manuel olarak güncellendi!");
                } else {
                    sender.sendMessage(ChatColor.RED + "❌ Discord bot veya status panel aktif değil!");
                }
                break;

            case "info":
                if (plugin.getDiscordBot() != null && plugin.getDiscordBot().getLiveStatusPanel() != null) {
                    var panel = plugin.getDiscordBot().getLiveStatusPanel();
                    sender.sendMessage(ChatColor.YELLOW + "=== Status Panel Bilgileri ===");
                    sender.sendMessage(ChatColor.GOLD + "Aktif: " + ChatColor.WHITE + (panel.isEnabled() ? "✅ Evet" : "❌ Hayır"));
                    sender.sendMessage(ChatColor.GOLD + "Kanal ID: " + ChatColor.WHITE + panel.getChannelId());
                    sender.sendMessage(ChatColor.GOLD + "Sunucu Adı: " + ChatColor.WHITE + panel.getServerName());
                    sender.sendMessage(ChatColor.GOLD + "Güncelleme Aralığı: " + ChatColor.WHITE + panel.getUpdateInterval() + " saniye");
                } else {
                    sender.sendMessage(ChatColor.RED + "❌ Discord bot veya status panel aktif değil!");
                }
                break;

            case "mail":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Kullanım: /status mail <send|reload|status>");
                    return true;
                }
                return handleMailCommand(sender, args[1]);



            default:
                sender.sendMessage(ChatColor.RED + "Geçersiz komut! /status help yazın.");
                break;
        }

        return true;
    }
    
    private boolean handleMailCommand(CommandSender sender, String action) {
        if (plugin.getStatusMailer() == null) {
            sender.sendMessage(ChatColor.RED + "❌ Mail sistemi başlatılmamış!");
            return true;
        }
        
        var mailer = plugin.getStatusMailer();
        
        switch (action.toLowerCase()) {
            case "send":
                sender.sendMessage(ChatColor.YELLOW + "📧 Manuel rapor gönderiliyor...");
                mailer.sendManualReport();
                sender.sendMessage(ChatColor.GREEN + "✅ Manuel rapor gönderildi!");
                break;
                
            case "reload":
                mailer.reload();
                sender.sendMessage(ChatColor.GREEN + "✅ Mail sistemi yeniden yüklendi!");
                break;
                
            case "status":
                sender.sendMessage(ChatColor.YELLOW + "=== Mail Sistemi Durumu ===");
                sender.sendMessage(ChatColor.GOLD + "Aktif: " + ChatColor.WHITE + (mailer.isEnabled() ? "✅ Evet" : "❌ Hayır"));
                sender.sendMessage(ChatColor.GOLD + "Alıcı: " + ChatColor.WHITE + mailer.getRecipientEmail());
                sender.sendMessage(ChatColor.GOLD + "Aralık: " + ChatColor.WHITE + mailer.getIntervalHours() + " saat");
                break;
                
            default:
                sender.sendMessage(ChatColor.RED + "❌ Geçersiz eylem! Kullanım: /status mail <send|reload|status>");
                break;
        }
        
        return true;
    }

}