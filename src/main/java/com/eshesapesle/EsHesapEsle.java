package com.eshesapesle;

import com.eshesapesle.discord.DiscordBot;
import com.eshesapesle.storage.Storage;
import com.eshesapesle.storage.YAMLStorage;
import com.eshesapesle.commands.OneriCommand;
import com.eshesapesle.commands.StatusCommand;
import com.eshesapesle.commands.MaintenanceCommand;
import com.eshesapesle.commands.UpdateCommand;
import com.eshesapesle.listeners.MaintenanceListener;
import com.eshesapesle.updater.AutoUpdater;
import com.eshesapesle.mail.ServerStatusMailer;

import com.eshesapesle.RewardManager;
import net.dv8tion.jda.api.entities.Role;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EsHesapEsle extends JavaPlugin implements Listener {
    private Storage storage;
    private DiscordBot discordBot;
    private final Map<String, LinkCode> linkCodes = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> codeAttempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 3;
    private static final long ATTEMPT_RESET_TIME = 300000; // 5 dakika

    private RewardManager rewardManager;
    private ServerStatusMailer statusMailer;

    
    @Override
    public void onEnable() {
        // Kaliteli başlangıç mesajı
        getLogger().info("§6╔══════════════════════════════════════════════════════════════╗");
        getLogger().info("§6║                    EsHesapEsle v1.0                          ║");
        getLogger().info("§6║                                                              ║");
        getLogger().info("§6║  Discord Hesap Eşleştirme Sistemi                           ║");
        getLogger().info("§6║  Geliştirici: Hermes/Livany                                        ║");
        getLogger().info("§6║  Discord: https://discord.gg/magicrise                        ║");
        getLogger().info("§6║                                                              ║");
        getLogger().info("§6║  Başlatılıyor...                                            ║");
        getLogger().info("§6╚══════════════════════════════════════════════════════════════╝");
        
        // Config'i yükle
        saveDefaultConfig();
        reloadConfig();
        

        

        
        // Normal plugin başlatma işlemleri
        setupStorage();
        setupDiscordBot();
        registerCommands();
        registerEvents();
        
        // Öneri komutunu kaydet
        getCommand("öneri").setExecutor(new OneriCommand(this));
        
        // Status komutunu kaydet
        getCommand("status").setExecutor(new StatusCommand(this));
        
        // Bakım komutunu kaydet
        getCommand("bakım").setExecutor(new MaintenanceCommand(this));
        
        // Güncelleme komutunu kaydet
        getCommand("guncelle").setExecutor(new UpdateCommand(this));
        

        
        this.rewardManager = new RewardManager(this);
        
        // Mail sistemi başlat
        this.statusMailer = new ServerStatusMailer(this);
        this.statusMailer.start();
        
        // Otomatik güncelleme sistemini başlat
        AutoUpdater autoUpdater = new AutoUpdater(this);
        autoUpdater.startAutoUpdateCheck();
        
        // Başarılı başlangıç mesajı
        getLogger().info("§a╔══════════════════════════════════════════════════════════════╗");
        getLogger().info("§a║                    PLUGIN BAŞARILI                           ║");
        getLogger().info("§a║                                                              ║");
        getLogger().info("§a║  ✓ Discord Bot: Bağlandı                                    ║");
        getLogger().info("§a║  ✓ Depolama Sistemi: Aktif                                  ║");
        getLogger().info("§a║  ✓ Komutlar: Kaydedildi                                     ║");
        getLogger().info("§a║  ✓ Eventler: Kaydedildi                                     ║");
        getLogger().info("§a║  ✓ Öneri Sistemi: Aktif                                     ║");
        getLogger().info("§a║  ✓ Ödül Sistemi: Aktif                                      ║");
        getLogger().info("§a║  ✓ Status Panel: Aktif                                      ║");
        getLogger().info("§a║  ✓ Otomatik Güncelleme: " + (getConfig().getBoolean("updater.auto-check", true) ? "Aktif" : "Pasif") + "                            ║");
        getLogger().info("§a║  ✓ Mail Rapor Sistemi: " + (statusMailer.isEnabled() ? "Aktif" : "Pasif") + "                                ║");
        getLogger().info("§a║  ✓ Müzik Sistemi: Aktif (Spotify Playlist)                 ║");

        getLogger().info("§a║                                                              ║");
        getLogger().info("§a║  Plugin başarıyla aktif edildi!                             ║");
        getLogger().info("§a║  Discord: https://discord.gg/magicrise                         ║");
        getLogger().info("§a╚══════════════════════════════════════════════════════════════╝");
    }
    
    private void setupStorage() {
        storage = new YAMLStorage(this);
        try {
            storage.connect();
            getLogger().info("Depolama sistemi başarıyla başlatıldı!");
        } catch (Exception e) {
            getLogger().severe("Depolama sistemi başlatılırken hata: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    private void setupDiscordBot() {
        discordBot = new DiscordBot(this);
        discordBot.start();
    }
    
    private void startCleanupTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            // Süresi dolmuş kodları temizle
            int before = linkCodes.size();
            linkCodes.entrySet().removeIf(entry -> entry.getValue().isExpired());
            int after = linkCodes.size();
            
            if (before != after) {
                getLogger().info(String.format("Süresi dolmuş %d kod temizlendi.", before - after));
            }
            
            // Rate limiting temizleme (5 dakika sonra)
            long currentTime = System.currentTimeMillis();
            codeAttempts.entrySet().removeIf(entry -> 
                currentTime - entry.getValue() * 60000L > ATTEMPT_RESET_TIME
            );
        }, 1200L, 1200L); // Her dakika çalıştır
    }
    
    @Override
    public void onDisable() {
        // Mail sistemini kapat
        if (statusMailer != null) {
            statusMailer.stop();
        }
        
        // Discord botunu kapat
        if (discordBot != null) {
            discordBot.shutdown();
        }
        
        // Storage'ı kapat
        if (storage != null) {
            try {
                storage.disconnect();
            } catch (Exception e) {
                getLogger().warning("Depolama sistemi kapatılırken hata: " + e.getMessage());
            }
        }
        
        // Kaliteli kapanış mesajı
        getLogger().info("§c╔══════════════════════════════════════════════════════════════╗");
        getLogger().info("§c║                    PLUGIN KAPATILDI                          ║");
        getLogger().info("§c║                                                              ║");
        getLogger().info("§c║  ✓ Discord Bot: Kapatıldı                                   ║");
        getLogger().info("§c║  ✓ Depolama Sistemi: Kapatıldı                              ║");
        getLogger().info("§c║  ✓ Tüm bağlantılar: Temizlendi                              ║");
        getLogger().info("§c║                                                              ║");
        getLogger().info("§c║  Plugin güvenli bir şekilde kapatıldı.                      ║");
        getLogger().info("§c║  Discord: https://discord.gg/magicrise                         ║");
        getLogger().info("§c╚══════════════════════════════════════════════════════════════╝");
    }
    
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (discordBot != null) {
            discordBot.sendChatMessage(event.getPlayer(), event.getMessage());
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Discord'a giriş mesajı gönder
        if (discordBot != null) {
            discordBot.sendPlayerJoinMessage(player);
        }
        
        // Rol güncelleme işlemi
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                if (storage.isLinked(player.getUniqueId())) {
                    discordBot.updatePlayerRoles(player.getUniqueId());
                }
            } catch (Exception e) {
                getLogger().warning(player.getName() + " için roller güncellenirken hata: " + e.getMessage());
            }
        });
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (discordBot != null) {
            discordBot.sendPlayerQuitMessage(event.getPlayer());
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("esh")) {
            return handleAdminCommand(sender, args);
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cBu komut sadece oyuncular tarafından kullanılabilir!");
            return true;
        }
        
        Player player = (Player) sender;
        
        switch (command.getName().toLowerCase()) {
            case "discord":
                if (args.length == 0) {
                    player.sendMessage("§cKullanım: /discord <eşle|sil>");
                    return true;
                }
                
                switch (args[0].toLowerCase()) {
                    case "eşle":
                        handleLinkCommand(player);
                        break;
                    case "sil":
                        handleUnlinkCommand(player);
                        break;
                    default:
                        player.sendMessage("§cGeçersiz alt komut! Kullanım: /discord <eşle|sil>");
                }
                break;
                
            case "profilim":
                handleProfileCommand(player);
                break;
        }
        
        return true;
    }
    
    private boolean handleAdminCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("esh.admin")) {
            sender.sendMessage("§cBu komutu kullanmak için yetkiniz yok!");
            return true;
        }
        
        if (args.length == 0) {
            sender.sendMessage("§cKullanım:");
            sender.sendMessage("§e/esh reload §7- Eklentiyi yeniden yapılandırır");
            sender.sendMessage("§e/esh info <oyuncu> §7- Oyuncunun bilgilerini gösterir");
            sender.sendMessage("§e/esh unlink <oyuncu> §7- Oyuncunun eşlemesini kaldırır");
            sender.sendMessage("§e/esh mail <send|reload|status> §7- Mail sistemi yönetimi");
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload":
                return handleReloadCommand(sender);
            case "info":
                if (args.length < 2) {
                    sender.sendMessage("§cKullanım: /esh info <oyuncu>");
                    return true;
                }
                return handleInfoCommand(sender, args[1]);
            case "unlink":
                if (args.length < 2) {
                    sender.sendMessage("§cKullanım: /esh unlink <oyuncu>");
                    return true;
                }
                return handleAdminUnlinkCommand(sender, args[1]);
            case "mail":
                if (args.length < 2) {
                    sender.sendMessage("§cKullanım: /esh mail <send|reload|status>");
                    return true;
                }
                return handleMailCommand(sender, args[1]);
            default:
                sender.sendMessage("§cGeçersiz alt komut!");
                return true;
        }
    }
    
    private boolean handleReloadCommand(CommandSender sender) {
        try {
            // Config'i yeniden yükle
            reloadConfig();
            
            // Storage'ı yeniden başlat
            if (storage != null) {
                storage.disconnect();
            }
            setupStorage();
            
            // Discord botunu yeniden başlat
            if (discordBot != null) {
                discordBot.shutdown();
            }
            setupDiscordBot();
            
            // Mail sistemini yeniden başlat
            if (statusMailer != null) {
                statusMailer.reload();
            }
            
            sender.sendMessage("§aEklenti başarıyla yeniden yapılandırıldı!");
            return true;
            
        } catch (Exception e) {
            sender.sendMessage("§cYeniden yapılandırma sırasında bir hata oluştu!");
            getLogger().severe("Reload hatası: " + e.getMessage());
            e.printStackTrace();
            return true;
        }
    }
    
    private boolean handleInfoCommand(CommandSender sender, String playerName) {
        // Oyuncuyu bul
        Player target = Bukkit.getPlayer(playerName);
        UUID targetUUID = target != null ? target.getUniqueId() : null;
        
        if (targetUUID == null) {
            // Offline oyuncu için UUID bulmayı dene
            try {
                targetUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
            } catch (Exception e) {
                sender.sendMessage("§cBelirtilen oyuncu bulunamadı!");
                return true;
            }
        }
        
        try {
            if (!storage.isLinked(targetUUID)) {
                sender.sendMessage("§c" + playerName + " adlı oyuncunun Discord hesabı eşleştirilmemiş!");
                return true;
            }
            
            String discordId = storage.getDiscordId(targetUUID);
            sender.sendMessage("§6=== Oyuncu Bilgileri ===");
            sender.sendMessage("§7Minecraft: §f" + playerName);
            sender.sendMessage("§7UUID: §f" + targetUUID);
            sender.sendMessage("§7Discord ID: §f" + discordId);
            
            // Discord bilgilerini al
            discordBot.getGuild().retrieveMemberById(discordId).queue(member -> {
                if (member != null) {
                    sender.sendMessage("§7Discord Tag: §f" + member.getUser().getAsTag());
                    sender.sendMessage("§7Roller:");
                    member.getRoles().forEach(role -> 
                        sender.sendMessage("§8- §f" + role.getName())
                    );
                }
            }, error -> {
                sender.sendMessage("§cDiscord bilgileri alınamadı: Kullanıcı sunucuda bulunamadı.");
            });
            
            return true;
            
        } catch (Exception e) {
            sender.sendMessage("§cBilgiler alınırken bir hata oluştu!");
            getLogger().warning("Info komutu hatası: " + e.getMessage());
            return true;
        }
    }
    
    private boolean handleAdminUnlinkCommand(CommandSender sender, String playerName) {
        // Oyuncuyu bul
        Player target = Bukkit.getPlayer(playerName);
        UUID targetUUID = target != null ? target.getUniqueId() : null;
        
        if (targetUUID == null) {
            // Offline oyuncu için UUID bulmayı dene
            try {
                targetUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
            } catch (Exception e) {
                sender.sendMessage("§cBelirtilen oyuncu bulunamadı!");
                return true;
            }
        }
        
        try {
            if (!storage.isLinked(targetUUID)) {
                sender.sendMessage("§c" + playerName + " adlı oyuncunun Discord hesabı zaten eşleştirilmemiş!");
                return true;
            }
            
            // Discord ID'yi al ve rolleri temizle
            String discordId = storage.getDiscordId(targetUUID);
            if (discordId != null) {
                discordBot.getGuild().retrieveMemberById(discordId).queue(member -> {
                    if (member != null) {
                        // Eşleştirme rolünü kaldır
                        if (discordBot.getEslendiRolId() != null && !discordBot.getEslendiRolId().isEmpty()) {
                            Role eslendiRole = discordBot.getGuild().getRoleById(discordBot.getEslendiRolId());
                            if (eslendiRole != null) {
                                discordBot.getGuild().removeRoleFromMember(member, eslendiRole).queue();
                            }
                        }
                    }
                });
            }
            
            // Eşleştirmeyi kaldır
            storage.unlinkAccounts(targetUUID);
            
            sender.sendMessage("§a" + playerName + " adlı oyuncunun Discord hesap eşleştirmesi kaldırıldı!");
            if (target != null && target.isOnline()) {
                target.sendMessage("§cDiscord hesap eşleştirmeniz bir yetkili tarafından kaldırıldı!");
            }
            
            return true;
            
        } catch (Exception e) {
            sender.sendMessage("§cEşleştirme kaldırılırken bir hata oluştu!");
            getLogger().warning("Admin unlink komutu hatası: " + e.getMessage());
            return true;
        }
    }
    
    private void handleLinkCommand(Player player) {
        try {
            UUID playerUUID = player.getUniqueId();
            
            // Limit kontrolü
            int attempts = codeAttempts.getOrDefault(playerUUID, 0);
            if (attempts >= MAX_ATTEMPTS) {
                player.sendMessage("§cÇok fazla kod isteğinde bulundunuz! Lütfen 5 dakika bekleyin.");
                return;
            }
            
            if (storage.isLinked(playerUUID)) {
                player.sendMessage(getConfig().getString("messages.already-linked", "§cHesabınız zaten Discord ile eşleştirilmiş!"));
                return;
            }
            
            // Yeni kod oluştur
            String code = generateLinkCode();
            linkCodes.put(code, new LinkCode(playerUUID));
            
            // Deneme sayısını artır
            codeAttempts.put(playerUUID, attempts + 1);
            
            // 5 dakika sonra deneme sayısını sıfırla
            Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> 
                codeAttempts.remove(playerUUID), 6000L); // 5 dakika = 6000 tick
            
            player.sendMessage("§aDiscord hesabınızı eşleştirmek için kodunuz: §e" + code);
            player.sendMessage("§7Bu kodu Discord'da §f/hesapesle §7komutu ile kullanın.");
            player.sendMessage("§7Kod §c2 dakika §7sonra geçerliliğini yitirecektir.");
            
        } catch (Exception e) {
            player.sendMessage("§cBir hata oluştu! Lütfen daha sonra tekrar deneyin.");
            getLogger().warning("Hesap eşleştirme hatası: " + e.getMessage());
        }
    }

    
    private void handleUnlinkCommand(Player player) {
        try {
            if (!storage.isLinked(player.getUniqueId())) {
                player.sendMessage(getConfig().getString("messages.not-linked", "§cHesabınız henüz Discord ile eşleştirilmemiş!"));
                return;
            }
            
            storage.unlinkAccounts(player.getUniqueId());
            player.sendMessage(getConfig().getString("messages.unlink-success", "§aHesabınızın Discord eşleştirmesi başarıyla kaldırıldı!"));
            
        } catch (Exception e) {
            player.sendMessage("§cBir hata oluştu! Lütfen daha sonra tekrar deneyin.");
            getLogger().warning("Hesap eşleştirme silme hatası: " + e.getMessage());
        }
    }
    
    private void handleProfileCommand(Player player) {
        try {
            if (!storage.isLinked(player.getUniqueId())) {
                player.sendMessage(getConfig().getString("messages.not-linked", "§cHesabınız henüz Discord ile eşleştirilmemiş!"));
                return;
            }
            
            String discordId = storage.getDiscordId(player.getUniqueId());
            player.sendMessage("§6=== Discord Profil Bilgileri ===");
            player.sendMessage("§7Minecraft: §f" + player.getName());
            player.sendMessage("§7Discord ID: §f" + discordId);
            
            // Discord bot üzerinden kullanıcı bilgilerini al
            discordBot.getGuild().retrieveMemberById(discordId).queue(member -> {
                if (member != null) {
                    player.sendMessage("§7Discord Tag: §f" + member.getUser().getAsTag());
                    player.sendMessage("§7Roller:");
                    member.getRoles().forEach(role -> 
                        player.sendMessage("§8- §f" + role.getName())
                    );
                }
            });
            
        } catch (Exception e) {
            player.sendMessage("§cBir hata oluştu! Lütfen daha sonra tekrar deneyin.");
            getLogger().warning("Profil görüntüleme hatası: " + e.getMessage());
        }
    }
    
    private String generateLinkCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random rnd = new Random();
        
        do {
            code.setLength(0);
            for (int i = 0; i < 6; i++) {
                code.append(chars.charAt(rnd.nextInt(chars.length())));
            }
        } while (linkCodes.containsKey(code.toString()));
        
        return code.toString();
    }
    
    /**
     * Discord'dan gelen kod ile hesap eşleme
     */
    public boolean linkAccount(String code, String discordId) {
        try {
            LinkCode linkCode = linkCodes.get(code);
            if (linkCode == null || linkCode.isExpired()) {
                return false;
            }
            
            UUID playerUUID = linkCode.getPlayerUUID();
            Player player = Bukkit.getPlayer(playerUUID);
            
            if (player == null) {
                return false;
            }
            
            // Hesapları eşle
            storage.linkAccounts(playerUUID, discordId);
            
            // Kodu temizle
            linkCodes.remove(code);
            
            // Oyuncuya bilgi ver
            player.sendMessage("§a✅ Discord hesabınız başarıyla eşleştirildi!");
            
            // Discord rollerini güncelle
            if (discordBot != null) {
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    discordBot.updatePlayerRoles(playerUUID);
                });
            }
            
            return true;
            
        } catch (Exception e) {
            getLogger().severe("Hesap eşleme hatası: " + e.getMessage());
            return false;
        }
    }
    
    // API Metodları
    public Storage getStorage() {
        return storage;
    }
    
    public boolean isValidCode(String code) {
        LinkCode linkCode = linkCodes.get(code);
        return linkCode != null && !linkCode.isExpired();
    }
    
    public UUID getPlayerUUIDByCode(String code) {
        LinkCode linkCode = linkCodes.get(code);
        return linkCode != null && !linkCode.isExpired() ? linkCode.getPlayerUUID() : null;
    }
    
    public void removeCode(String code) {
        linkCodes.remove(code);
    }
    
    /**
     * Link kodunu doğrular ve geçerliyse UUID döndürür
     */
    public UUID validateLinkCode(String code) {
        LinkCode linkCode = linkCodes.get(code);
        if (linkCode != null && !linkCode.isExpired()) {
            // Kodu kullan ve sil
            UUID playerUUID = linkCode.getPlayerUUID();
            linkCodes.remove(code);
            return playerUUID;
        }
        return null;
    }
    
    public DiscordBot getDiscordBot() {
        return discordBot;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }
    
    public ServerStatusMailer getStatusMailer() {
        return statusMailer;
    }
    
    private boolean handleMailCommand(CommandSender sender, String action) {
        if (statusMailer == null) {
            sender.sendMessage("§cMail sistemi başlatılmamış!");
            return true;
        }
        
        switch (action.toLowerCase()) {
            case "send":
                sender.sendMessage("§7Manuel rapor gönderiliyor...");
                statusMailer.sendManualReport();
                sender.sendMessage("§aManuel rapor gönderildi!");
                break;
                
            case "reload":
                statusMailer.reload();
                sender.sendMessage("§aMail sistemi yeniden yüklendi!");
                break;
                
            case "status":
                sender.sendMessage("§6=== Mail Sistemi Durumu ===");
                sender.sendMessage("§7Aktif: " + (statusMailer.isEnabled() ? "§aEvet" : "§cHayır"));
                sender.sendMessage("§7Alıcı: §f" + statusMailer.getRecipientEmail());
                sender.sendMessage("§7Aralık: §f" + statusMailer.getIntervalHours() + " saat");
                break;
                
            default:
                sender.sendMessage("§cGeçersiz eylem! Kullanım: /esh mail <send|reload|status>");
                break;
        }
        
        return true;
    }

    private void registerCommands() {
        // Discord komutu
        getCommand("discord").setExecutor(this);
        // Profilim komutu
        getCommand("profilim").setExecutor(this);
        // Admin komutu
        getCommand("esh").setExecutor(this);
    }

    private void registerEvents() {
        // Event listener'ı kaydet
        getServer().getPluginManager().registerEvents(this, this);
        
        // Bakım listener'ını kaydet
        getServer().getPluginManager().registerEvents(new MaintenanceListener(this), this);
        // Temizleme görevini başlat
        startCleanupTask();
    }
    

    
    /**
     * Link kodu sınıfı
     */
    public static class LinkCode {
        private final UUID playerUUID;
        private final long createdTime;
        private static final long EXPIRY_TIME = 300000; // 5 dakika
        
        public LinkCode(UUID playerUUID) {
            this.playerUUID = playerUUID;
            this.createdTime = System.currentTimeMillis();
        }
        
        public UUID getPlayerUUID() {
            return playerUUID;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - createdTime > EXPIRY_TIME;
        }
        
        public long getRemainingTime() {
            long remaining = EXPIRY_TIME - (System.currentTimeMillis() - createdTime);
            return Math.max(0, remaining);
        }
    }

} 