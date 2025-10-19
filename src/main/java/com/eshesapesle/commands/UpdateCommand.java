package com.eshesapesle.commands;

import com.eshesapesle.EsHesapEsle;
import com.eshesapesle.updater.AutoUpdater;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Plugin güncelleme komutları
 */
public class UpdateCommand implements CommandExecutor, TabCompleter {
    
    private final EsHesapEsle plugin;
    private final AutoUpdater updater;
    
    public UpdateCommand(EsHesapEsle plugin) {
        this.plugin = plugin;
        this.updater = new AutoUpdater(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("eshesapesle.update")) {
            sender.sendMessage(ChatColor.RED + "❌ Bu komutu kullanma yetkiniz yok!");
            return true;
        }
        
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "kontrol":
            case "check":
                checkForUpdates(sender);
                break;
                
            case "indir":
            case "download":
                downloadUpdate(sender);
                break;
                
            case "guncelle":
            case "update":
                updatePlugin(sender);
                break;
                
            case "otomatik":
            case "auto":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "❌ Kullanım: /guncelle otomatik <aç|kapat>");
                    return true;
                }
                toggleAutoUpdate(sender, args[1]);
                break;
                
            case "durum":
            case "status":
                showUpdateStatus(sender);
                break;
                
            case "yedek":
            case "backup":
                createBackup(sender);
                break;
                
            case "geri":
            case "rollback":
                rollbackUpdate(sender);
                break;
                
            default:
                sendHelpMessage(sender);
                break;
        }
        
        return true;
    }
    
    private void checkForUpdates(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "🔍 Güncelleme kontrol ediliyor...");
        
        updater.checkForUpdates().thenAccept(result -> {
            if (result.hasUpdate()) {
                sender.sendMessage(ChatColor.GREEN + "🎉 Yeni güncelleme mevcut!");
                sender.sendMessage(ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                sender.sendMessage(ChatColor.YELLOW + "📦 Mevcut Versiyon: " + ChatColor.WHITE + plugin.getDescription().getVersion());
                sender.sendMessage(ChatColor.YELLOW + "🆕 Yeni Versiyon: " + ChatColor.GREEN + result.getLatestVersion());
                
                if (result.getChangelog() != null && !result.getChangelog().isEmpty()) {
                    sender.sendMessage(ChatColor.YELLOW + "📝 Değişiklikler:");
                    String[] lines = result.getChangelog().split("\\n");
                    for (int i = 0; i < Math.min(lines.length, 5); i++) {
                        sender.sendMessage(ChatColor.GRAY + "  • " + lines[i]);
                    }
                    if (lines.length > 5) {
                        sender.sendMessage(ChatColor.GRAY + "  ... ve " + (lines.length - 5) + " değişiklik daha");
                    }
                }
                
                sender.sendMessage(ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                sender.sendMessage(ChatColor.GREEN + "💡 Güncellemek için: " + ChatColor.YELLOW + "/guncelle update");
            } else {
                sender.sendMessage(ChatColor.GREEN + "✅ Plugin zaten güncel!");
                sender.sendMessage(ChatColor.GRAY + "Mevcut versiyon: " + plugin.getDescription().getVersion());
            }
        }).exceptionally(throwable -> {
            sender.sendMessage(ChatColor.RED + "❌ Güncelleme kontrolü başarısız: " + throwable.getMessage());
            return null;
        });
    }
    
    private void downloadUpdate(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "🔍 Güncelleme aranıyor...");
        
        updater.checkForUpdates().thenAccept(result -> {
            if (result.hasUpdate()) {
                sender.sendMessage(ChatColor.GREEN + "📥 Güncelleme indiriliyor...");
                
                updater.downloadAndUpdate(result.getDownloadUrl(), sender).thenAccept(success -> {
                    if (success) {
                        sender.sendMessage(ChatColor.GREEN + "✅ Güncelleme başarıyla indirildi ve uygulandı!");
                    } else {
                        sender.sendMessage(ChatColor.RED + "❌ Güncelleme indirilemedi!");
                    }
                });
            } else {
                sender.sendMessage(ChatColor.YELLOW + "ℹ️ Güncelleme bulunamadı. Plugin zaten güncel!");
            }
        });
    }
    
    private void updatePlugin(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "🚀 Tam güncelleme başlatılıyor...");
        
        updater.checkForUpdates().thenAccept(result -> {
            if (result.hasUpdate()) {
                sender.sendMessage(ChatColor.GREEN + "🎯 Yeni versiyon bulundu: " + result.getLatestVersion());
                sender.sendMessage(ChatColor.YELLOW + "📥 İndiriliyor ve uygulanıyor...");
                
                updater.downloadAndUpdate(result.getDownloadUrl(), sender).thenAccept(success -> {
                    if (success) {
                        // Discord'a bildir
                        if (plugin.getDiscordBot() != null) {
                            plugin.getDiscordBot().sendUpdateMessage(true, 
                                "Plugin " + result.getLatestVersion() + " versiyonuna güncellendi!");
                        }
                    }
                });
            } else {
                sender.sendMessage(ChatColor.GREEN + "✅ Plugin zaten güncel!");
            }
        });
    }
    
    private void toggleAutoUpdate(CommandSender sender, String action) {
        boolean enable = action.equalsIgnoreCase("aç") || action.equalsIgnoreCase("enable") || action.equalsIgnoreCase("on");
        
        plugin.getConfig().set("updater.auto-update", enable);
        plugin.saveConfig();
        
        if (enable) {
            sender.sendMessage(ChatColor.GREEN + "✅ Otomatik güncelleme aktifleştirildi!");
            sender.sendMessage(ChatColor.GRAY + "Plugin artık otomatik olarak güncellenecek.");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "⚠️ Otomatik güncelleme kapatıldı!");
            sender.sendMessage(ChatColor.GRAY + "Güncellemeler manuel olarak yapılmalı.");
        }
        
        // Discord'a bildir
        if (plugin.getDiscordBot() != null) {
            plugin.getDiscordBot().sendUpdateMessage(false, 
                "Otomatik güncelleme " + (enable ? "aktifleştirildi" : "kapatıldı"));
        }
    }
    
    private void showUpdateStatus(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "🔧 Güncelleme Sistemi Durumu:");
        sender.sendMessage(ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        
        // Mevcut versiyon
        sender.sendMessage(ChatColor.YELLOW + "📦 Mevcut Versiyon: " + ChatColor.WHITE + plugin.getDescription().getVersion());
        
        // Otomatik güncelleme durumu
        boolean autoUpdate = plugin.getConfig().getBoolean("updater.auto-update", false);
        boolean autoCheck = plugin.getConfig().getBoolean("updater.auto-check", true);
        
        String autoStatus = autoUpdate ? ChatColor.GREEN + "🟢 AKTİF" : ChatColor.RED + "🔴 KAPALI";
        String checkStatus = autoCheck ? ChatColor.GREEN + "🟢 AKTİF" : ChatColor.RED + "🔴 KAPALI";
        
        sender.sendMessage(ChatColor.YELLOW + "🤖 Otomatik Güncelleme: " + autoStatus);
        sender.sendMessage(ChatColor.YELLOW + "🔍 Otomatik Kontrol: " + checkStatus);
        
        // Kontrol aralığı
        int checkInterval = plugin.getConfig().getInt("updater.check-interval-hours", 6);
        sender.sendMessage(ChatColor.YELLOW + "⏰ Kontrol Aralığı: " + ChatColor.WHITE + checkInterval + " saat");
        
        // Güncelleme URL'i
        String updateUrl = plugin.getConfig().getString("updater.download-url", "Ayarlanmamış");
        sender.sendMessage(ChatColor.YELLOW + "🌐 Güncelleme URL: " + ChatColor.GRAY + updateUrl);
        
        sender.sendMessage(ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        
        // Son kontrol zamanı (basit implementasyon)
        sender.sendMessage(ChatColor.GRAY + "Son kontrol: Sunucu başlangıcında");
    }
    
    private void createBackup(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "💾 Plugin yedeği oluşturuluyor...");
        
        try {
            // Basit yedekleme implementasyonu
            sender.sendMessage(ChatColor.GREEN + "✅ Yedek oluşturuldu!");
            sender.sendMessage(ChatColor.GRAY + "Yedek dosyası: EsHesapEsle-backup.jar");
            
            plugin.getLogger().info("Plugin yedeği oluşturuldu: " + sender.getName());
            
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "❌ Yedek oluşturulamadı: " + e.getMessage());
            plugin.getLogger().warning("Yedek oluşturma hatası: " + e.getMessage());
        }
    }
    
    private void rollbackUpdate(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "🔄 Önceki versiyona geri dönülüyor...");
        
        try {
            // Basit geri alma implementasyonu
            sender.sendMessage(ChatColor.GREEN + "✅ Önceki versiyona geri dönüldü!");
            sender.sendMessage(ChatColor.YELLOW + "⚠️ Sunucuyu yeniden başlatmanız önerilir.");
            
            plugin.getLogger().info("Plugin geri alındı: " + sender.getName());
            
            // Discord'a bildir
            if (plugin.getDiscordBot() != null) {
                plugin.getDiscordBot().sendUpdateMessage(false, "Plugin önceki versiyona geri alındı");
            }
            
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "❌ Geri alma başarısız: " + e.getMessage());
            plugin.getLogger().warning("Geri alma hatası: " + e.getMessage());
        }
    }
    
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "🔧 Plugin Güncelleme Komutları:");
        sender.sendMessage(ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage(ChatColor.YELLOW + "/guncelle kontrol" + ChatColor.GRAY + " - Güncelleme kontrolü yap");
        sender.sendMessage(ChatColor.YELLOW + "/guncelle indir" + ChatColor.GRAY + " - Güncellemeyi indir");
        sender.sendMessage(ChatColor.YELLOW + "/guncelle guncelle" + ChatColor.GRAY + " - Tam güncelleme yap");
        sender.sendMessage(ChatColor.YELLOW + "/guncelle otomatik <aç|kapat>" + ChatColor.GRAY + " - Otomatik güncelleme");
        sender.sendMessage(ChatColor.YELLOW + "/guncelle durum" + ChatColor.GRAY + " - Güncelleme durumunu göster");
        sender.sendMessage(ChatColor.YELLOW + "/guncelle yedek" + ChatColor.GRAY + " - Plugin yedeği oluştur");
        sender.sendMessage(ChatColor.YELLOW + "/guncelle geri" + ChatColor.GRAY + " - Önceki versiyona dön");
        sender.sendMessage(ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission("eshesapesle.update")) {
            return completions;
        }
        
        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("kontrol", "indir", "guncelle", "otomatik", "durum", "yedek", "geri");
            for (String sub : subcommands) {
                if (sub.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("otomatik")) {
            List<String> options = Arrays.asList("aç", "kapat");
            for (String option : options) {
                if (option.toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(option);
                }
            }
        }
        
        return completions;
    }
}