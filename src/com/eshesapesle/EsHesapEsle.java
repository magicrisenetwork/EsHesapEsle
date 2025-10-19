package com.eshesapesle;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.PermissionAttachmentInfo;
import com.eshesapesle.storage.Storage;
import com.eshesapesle.storage.MySQLStorage;
import com.eshesapesle.storage.YAMLStorage;
import com.eshesapesle.discord.DiscordBot;

import java.util.HashMap;
import java.util.UUID;
import java.util.Random;
import java.util.Set;

public class EsHesapEsle extends JavaPlugin implements Listener {
    private HashMap<UUID, LinkCode> activeCodes;
    private Random random;
    private Storage storage;
    private DiscordBot discordBot;
    
    @Override
    public void onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig();
        
        // Initialize storage based on configuration
        initializeStorage();
        
        this.activeCodes = new HashMap<>();
        this.random = new Random();
        
        // Discord bot'u başlat
        this.discordBot = new DiscordBot(this);
        this.discordBot.start();
        
        // Event listener'ı kaydet
        getServer().getPluginManager().registerEvents(this, this);
        
        getLogger().info("EsHesapEsle plugin aktif!");
    }
    
    @Override
    public void onDisable() {
        try {
            if (storage != null) {
                storage.disconnect();
            }
            if (discordBot != null) {
                discordBot.shutdown();
            }
        } catch (Exception e) {
            getLogger().severe("Depolama kapatılırken hata oluştu: " + e.getMessage());
        }
        getLogger().info("EsHesapEsle plugin devre dışı!");
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Oyuncu giriş yaptığında rollerini güncelle
        if (storage != null && discordBot != null) {
            try {
                if (storage.isLinked(player.getUniqueId())) {
                    discordBot.updatePlayerRoles(player.getUniqueId());
                }
            } catch (Exception e) {
                getLogger().warning(player.getName() + " için roller güncellenirken hata: " + e.getMessage());
            }
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cBu komut sadece oyuncular tarafından kullanılabilir!");
            return true;
        }
        
        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();
        
        try {
            switch (command.getName().toLowerCase()) {
                case "discord":
                    if (args.length == 0) {
                        player.sendMessage("§cKullanım: /discord <eşle|sil>");
                        return true;
                    }
                    
                    switch (args[0].toLowerCase()) {
                        case "eşle":
                            return handleLinkCommand(player);
                        case "sil":
                            return handleUnlinkCommand(player);
                        default:
                            player.sendMessage("§cGeçersiz alt komut! Kullanım: /discord <eşle|sil>");
                            return true;
                    }
                    
                case "profilim":
                    return handleProfileCommand(player);
            }
        } catch (Exception e) {
            player.sendMessage("§cBir hata oluştu! Lütfen daha sonra tekrar deneyin.");
            getLogger().severe("Komut işlenirken hata: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    private boolean handleLinkCommand(Player player) throws Exception {
        UUID playerUUID = player.getUniqueId();
        
        // Check if already linked
        if (storage.isLinked(playerUUID)) {
            player.sendMessage(getConfig().getString("messages.already-linked", "§cHesabınız zaten Discord ile eşleştirilmiş!"));
            return true;
        }
        
        // Check for existing active code
        if (activeCodes.containsKey(playerUUID)) {
            LinkCode existingCode = activeCodes.get(playerUUID);
            if (!existingCode.isExpired()) {
                player.sendMessage("§cZaten aktif bir kodunuz var: §e" + existingCode.getCode());
                player.sendMessage("§cBu kod §e" + existingCode.getRemainingTime() + " §cdakika sonra geçerliliğini yitirecek.");
                return true;
            }
        }
        
        // Generate new code
        String code = generateCode();
        LinkCode linkCode = new LinkCode(code);
        activeCodes.put(playerUUID, linkCode);
        
        // Send message to player
        player.sendMessage("§a[EŞLEME] Discord sunucusuna gir ve #hesap-eşle kanalına aşağıdaki komutu yaz:");
        player.sendMessage("§e/hesapesle " + code);
        player.sendMessage("§7Bu kod 5 dakika boyunca geçerlidir.");
        
        return true;
    }
    
    private boolean handleUnlinkCommand(Player player) throws Exception {
        UUID playerUUID = player.getUniqueId();
        
        if (!storage.isLinked(playerUUID)) {
            player.sendMessage("§cHesabınız zaten Discord ile eşleştirilmemiş!");
            return true;
        }
        
        String discordId = storage.getDiscordId(playerUUID);
        storage.linkAccounts(playerUUID, null); // Null to unlink
        
        player.sendMessage("§aHesabınızın Discord eşleştirmesi kaldırıldı!");
        return true;
    }
    
    private boolean handleProfileCommand(Player player) throws Exception {
        UUID playerUUID = player.getUniqueId();
        
        if (!storage.isLinked(playerUUID)) {
            player.sendMessage("§cProfil bilgilerinizi görüntülemek için önce Discord hesabınızı eşleştirmelisiniz!");
            player.sendMessage("§7Eşleştirmek için: §e/discord eşle");
            return true;
        }
        
        String discordId = storage.getDiscordId(playerUUID);
        player.sendMessage("§6=== Profil Bilgileri ===");
        player.sendMessage("§7Minecraft: §f" + player.getName());
        player.sendMessage("§7Discord ID: §f" + discordId);
        return true;
    }
    
    private void initializeStorage() {
        FileConfiguration config = getConfig();
        String storageType = config.getString("storage.type", "yaml").toLowerCase();
        
        try {
            switch (storageType) {
                case "mysql":
                    String url = config.getString("storage.mysql.url");
                    String username = config.getString("storage.mysql.username");
                    String password = config.getString("storage.mysql.password");
                    
                    if (url == null || username == null || password == null) {
                        getLogger().severe("MySQL ayarları eksik! YAML depolama kullanılıyor.");
                        storage = new YAMLStorage(this);
                    } else {
                        storage = new MySQLStorage(url, username, password);
                    }
                    break;
                    
                case "yaml":
                default:
                    storage = new YAMLStorage(this);
                    break;
            }
            
            storage.connect();
            getLogger().info(storageType.toUpperCase() + " depolama başarıyla başlatıldı!");
            
        } catch (Exception e) {
            getLogger().severe("Depolama başlatılamadı: " + e.getMessage());
            e.printStackTrace();
            // Fallback to YAML if there's an error
            storage = new YAMLStorage(this);
            try {
                storage.connect();
            } catch (Exception ex) {
                getLogger().severe("YAML depolama da başlatılamadı!");
                ex.printStackTrace();
            }
        }
    }
    
    private String generateCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }
    
    public boolean isValidCode(String code) {
        for (LinkCode linkCode : activeCodes.values()) {
            if (linkCode.getCode().equals(code) && !linkCode.isExpired()) {
                return true;
            }
        }
        return false;
    }
    
    public UUID getPlayerUUIDByCode(String code) {
        for (HashMap.Entry<UUID, LinkCode> entry : activeCodes.entrySet()) {
            if (entry.getValue().getCode().equals(code) && !entry.getValue().isExpired()) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    public void removeCode(String code) {
        activeCodes.entrySet().removeIf(entry -> entry.getValue().getCode().equals(code));
    }
    
    public Storage getStorage() {
        return storage;
    }
} 