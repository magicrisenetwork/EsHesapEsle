package com.eshesapesle.updater;

import com.eshesapesle.EsHesapEsle;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;

/**
 * Otomatik plugin güncelleme sistemi
 */
public class AutoUpdater {
    
    private final EsHesapEsle plugin;
    private final String updateUrl;
    private final String versionCheckUrl;
    private final String currentVersion;
    private final File pluginFile;
    
    public AutoUpdater(EsHesapEsle plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
        this.pluginFile = getPluginFile();
        
        // GitHub releases veya kendi sunucunuz
        this.updateUrl = plugin.getConfig().getString("updater.download-url", 
            "https://api.github.com/repos/YOUR_USERNAME/EsHesapEsle/releases/latest");
        this.versionCheckUrl = plugin.getConfig().getString("updater.version-check-url", 
            "https://api.github.com/repos/YOUR_USERNAME/EsHesapEsle/releases/latest");
    }
    
    /**
     * Güncelleme kontrolü yapar
     */
    public CompletableFuture<UpdateResult> checkForUpdates() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                plugin.getLogger().info("Güncelleme kontrol ediliyor...");
                
                URL url = new URL(versionCheckUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "EsHesapEsle-Updater");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(10000);
                
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    return new UpdateResult(false, "Güncelleme sunucusuna bağlanılamadı: " + responseCode, null, null);
                }
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                // JSON yanıtını parse et
                JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                String latestVersion = json.get("tag_name").getAsString();
                String downloadUrl = json.getAsJsonArray("assets").get(0).getAsJsonObject().get("download_url").getAsString();
                String changelog = json.get("body").getAsString();
                
                // Versiyon karşılaştır
                if (isNewerVersion(latestVersion, currentVersion)) {
                    plugin.getLogger().info("Yeni güncelleme bulundu: " + latestVersion);
                    return new UpdateResult(true, "Yeni güncelleme mevcut!", latestVersion, downloadUrl, changelog);
                } else {
                    plugin.getLogger().info("Plugin güncel: " + currentVersion);
                    return new UpdateResult(false, "Plugin zaten güncel!", currentVersion, null);
                }
                
            } catch (Exception e) {
                plugin.getLogger().warning("Güncelleme kontrolü başarısız: " + e.getMessage());
                return new UpdateResult(false, "Güncelleme kontrolü başarısız: " + e.getMessage(), null, null);
            }
        });
    }
    
    /**
     * Güncellemeyi indirir ve uygular
     */
    public CompletableFuture<Boolean> downloadAndUpdate(String downloadUrl, CommandSender sender) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                sender.sendMessage(ChatColor.YELLOW + "🔄 Güncelleme indiriliyor...");
                plugin.getLogger().info("Güncelleme indiriliyor: " + downloadUrl);
                
                // Geçici dosya oluştur
                File tempFile = new File(plugin.getDataFolder().getParent(), "EsHesapEsle-update.jar");
                
                // Dosyayı indir
                URL url = new URL(downloadUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "EsHesapEsle-Updater");
                
                try (InputStream in = connection.getInputStream();
                     FileOutputStream out = new FileOutputStream(tempFile)) {
                    
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalBytes = 0;
                    long fileSize = connection.getContentLengthLong();
                    
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        totalBytes += bytesRead;
                        
                        // İlerleme göster
                        if (fileSize > 0) {
                            int progress = (int) ((totalBytes * 100) / fileSize);
                            if (progress % 20 == 0) {
                                sender.sendMessage(ChatColor.YELLOW + "📥 İndiriliyor: %" + progress);
                            }
                        }
                    }
                }
                
                sender.sendMessage(ChatColor.GREEN + "✅ Güncelleme indirildi!");
                plugin.getLogger().info("Güncelleme başarıyla indirildi.");
                
                // Güncellemeyi uygula
                return applyUpdate(tempFile, sender);
                
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "❌ Güncelleme indirilemedi: " + e.getMessage());
                plugin.getLogger().severe("Güncelleme indirme hatası: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Güncellemeyi uygular
     */
    private boolean applyUpdate(File updateFile, CommandSender sender) {
        try {
            sender.sendMessage(ChatColor.YELLOW + "🔄 Güncelleme uygulanıyor...");
            
            // Mevcut plugin dosyasını yedekle
            File backupFile = new File(pluginFile.getParent(), "EsHesapEsle-backup.jar");
            Files.copy(pluginFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            sender.sendMessage(ChatColor.GREEN + "💾 Yedek oluşturuldu!");
            
            // Discord'a güncelleme mesajı gönder
            if (plugin.getDiscordBot() != null) {
                plugin.getDiscordBot().sendUpdateMessage(true, "Güncelleme uygulanıyor...");
            }
            
            // Güncelleme scriptini oluştur
            createUpdateScript(updateFile, sender);
            
            sender.sendMessage(ChatColor.GREEN + "✅ Güncelleme hazırlandı!");
            sender.sendMessage(ChatColor.YELLOW + "🔄 Sunucu 5 saniye içinde yeniden başlatılacak...");
            
            // 5 saniye bekle ve sunucuyu yeniden başlat
            new BukkitRunnable() {
                int countdown = 5;
                
                @Override
                public void run() {
                    if (countdown > 0) {
                        Bukkit.broadcastMessage(ChatColor.YELLOW + "🔄 Güncelleme için sunucu " + countdown + " saniye içinde yeniden başlatılacak!");
                        countdown--;
                    } else {
                        Bukkit.broadcastMessage(ChatColor.GREEN + "🚀 Güncelleme uygulanıyor! Sunucu yeniden başlatılıyor...");
                        
                        // Güncelleme scriptini çalıştır ve sunucuyu kapat
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                executeUpdateScript();
                                Bukkit.shutdown();
                            }
                        }.runTaskLater(plugin, 20L);
                        
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0L, 20L);
            
            return true;
            
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "❌ Güncelleme uygulanamadı: " + e.getMessage());
            plugin.getLogger().severe("Güncelleme uygulama hatası: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Güncelleme scriptini oluşturur
     */
    private void createUpdateScript(File updateFile, CommandSender sender) {
        try {
            File scriptFile = new File(plugin.getDataFolder().getParent(), "update-script.bat");
            
            StringBuilder script = new StringBuilder();
            script.append("@echo off\n");
            script.append("echo EsHesapEsle Guncelleme Scripti\n");
            script.append("echo Bekleniyor...\n");
            script.append("timeout /t 3 /nobreak > nul\n");
            script.append("echo Eski plugin siliniyor...\n");
            script.append("del \"").append(pluginFile.getAbsolutePath()).append("\"\n");
            script.append("echo Yeni plugin kopyalaniyor...\n");
            script.append("copy \"").append(updateFile.getAbsolutePath()).append("\" \"").append(pluginFile.getAbsolutePath()).append("\"\n");
            script.append("echo Gecici dosyalar temizleniyor...\n");
            script.append("del \"").append(updateFile.getAbsolutePath()).append("\"\n");
            script.append("del \"").append(scriptFile.getAbsolutePath()).append("\"\n");
            script.append("echo Guncelleme tamamlandi!\n");
            script.append("echo Sunucu yeniden baslatiliyor...\n");
            
            // Sunucu başlatma komutu (özelleştirilebilir)
            String startCommand = plugin.getConfig().getString("updater.start-command", "start.bat");
            script.append("call \"").append(startCommand).append("\"\n");
            
            Files.write(scriptFile.toPath(), script.toString().getBytes());
            
            sender.sendMessage(ChatColor.GREEN + "📝 Güncelleme scripti oluşturuldu!");
            
        } catch (Exception e) {
            plugin.getLogger().severe("Güncelleme scripti oluşturulamadı: " + e.getMessage());
        }
    }
    
    /**
     * Güncelleme scriptini çalıştırır
     */
    private void executeUpdateScript() {
        try {
            File scriptFile = new File(plugin.getDataFolder().getParent(), "update-script.bat");
            if (scriptFile.exists()) {
                ProcessBuilder pb = new ProcessBuilder("cmd", "/c", scriptFile.getAbsolutePath());
                pb.start();
                plugin.getLogger().info("Güncelleme scripti çalıştırıldı!");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Güncelleme scripti çalıştırılamadı: " + e.getMessage());
        }
    }
    
    /**
     * Otomatik güncelleme kontrolü başlatır
     */
    public void startAutoUpdateCheck() {
        if (!plugin.getConfig().getBoolean("updater.auto-check", true)) {
            return;
        }
        
        int checkInterval = plugin.getConfig().getInt("updater.check-interval-hours", 6) * 72000; // Saat -> tick
        
        new BukkitRunnable() {
            @Override
            public void run() {
                checkForUpdates().thenAccept(result -> {
                    if (result.hasUpdate()) {
                        plugin.getLogger().info("Yeni güncelleme mevcut: " + result.getLatestVersion());
                        
                        // Discord'a bildir
                        if (plugin.getDiscordBot() != null) {
                            plugin.getDiscordBot().sendUpdateNotification(result);
                        }
                        
                        // Otomatik güncelleme aktifse indir
                        if (plugin.getConfig().getBoolean("updater.auto-update", false)) {
                            plugin.getLogger().info("Otomatik güncelleme başlatılıyor...");
                            downloadAndUpdate(result.getDownloadUrl(), Bukkit.getConsoleSender());
                        }
                    }
                });
            }
        }.runTaskTimerAsynchronously(plugin, 1200L, checkInterval); // 1 dakika sonra başla, her X saatte kontrol et
    }
    
    /**
     * Plugin dosyasını bulur
     */
    private File getPluginFile() {
        try {
            return new File(AutoUpdater.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Exception e) {
            // Fallback
            return new File("plugins/EsHesapEsle.jar");
        }
    }
    
    /**
     * Versiyon karşılaştırması yapar
     */
    private boolean isNewerVersion(String latest, String current) {
        try {
            // Basit versiyon karşılaştırması (1.0.0 formatı için)
            String[] latestParts = latest.replace("v", "").split("\\.");
            String[] currentParts = current.replace("v", "").split("\\.");
            
            for (int i = 0; i < Math.max(latestParts.length, currentParts.length); i++) {
                int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                
                if (latestPart > currentPart) {
                    return true;
                } else if (latestPart < currentPart) {
                    return false;
                }
            }
            
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("Versiyon karşılaştırma hatası: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Güncelleme sonucu sınıfı
     */
    public static class UpdateResult {
        private final boolean hasUpdate;
        private final String message;
        private final String latestVersion;
        private final String downloadUrl;
        private final String changelog;
        
        public UpdateResult(boolean hasUpdate, String message, String latestVersion, String downloadUrl) {
            this(hasUpdate, message, latestVersion, downloadUrl, null);
        }
        
        public UpdateResult(boolean hasUpdate, String message, String latestVersion, String downloadUrl, String changelog) {
            this.hasUpdate = hasUpdate;
            this.message = message;
            this.latestVersion = latestVersion;
            this.downloadUrl = downloadUrl;
            this.changelog = changelog;
        }
        
        public boolean hasUpdate() { return hasUpdate; }
        public String getMessage() { return message; }
        public String getLatestVersion() { return latestVersion; }
        public String getDownloadUrl() { return downloadUrl; }
        public String getChangelog() { return changelog; }
    }
}