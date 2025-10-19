package com.eshesapesle.status;

import com.eshesapesle.EsHesapEsle;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.awt.Color;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Canlı sunucu durum paneli sistemi
 */
public class LiveStatusPanel {
    
    private final EsHesapEsle plugin;
    private final JDA jda;
    private BukkitTask updateTask;
    private Message statusMessage;
    
    // Config değerleri
    private boolean enabled;
    private String channelId;
    private String serverName;
    private String serverIp;
    private int updateInterval;
    private String guildId;
    
    public LiveStatusPanel(EsHesapEsle plugin, JDA jda) {
        this.plugin = plugin;
        this.jda = jda;
        loadConfig();
    }
    
    /**
     * Config değerlerini yükler
     */
    private void loadConfig() {
        this.enabled = plugin.getConfig().getBoolean("discord.status-panel.enabled", false);
        this.channelId = plugin.getConfig().getString("discord.status-panel.channel-id", "");
        this.serverName = plugin.getConfig().getString("discord.status-panel.server-name", "Minecraft Server");
        this.serverIp = plugin.getConfig().getString("discord.status-panel.server-ip", "");
        this.updateInterval = plugin.getConfig().getInt("discord.status-panel.update-interval", 60);
        this.guildId = plugin.getConfig().getString("discord.guild-id", "");
        
        plugin.getLogger().info("Status Panel Ayarları:");
        plugin.getLogger().info("- Aktif: " + enabled);
        plugin.getLogger().info("- Kanal ID: " + channelId);
        plugin.getLogger().info("- Sunucu Adı: " + serverName);
        plugin.getLogger().info("- Güncelleme: " + updateInterval + " saniye");
    }
    
    /**
     * Status panel sistemini başlatır
     */
    public void start() {
        if (!enabled) {
            plugin.getLogger().info("Status panel devre dışı.");
            return;
        }
        
        if (channelId.isEmpty()) {
            plugin.getLogger().warning("Status panel kanal ID'si boş! Config'i kontrol edin.");
            return;
        }
        
        try {
            Guild guild = jda.getGuildById(guildId);
            if (guild == null) {
                plugin.getLogger().warning("Discord sunucusu bulunamadı: " + guildId);
                return;
            }
            
            TextChannel channel = guild.getTextChannelById(channelId);
            if (channel == null) {
                plugin.getLogger().warning("Status panel kanalı bulunamadı: " + channelId);
                return;
            }
            
            // Mevcut status mesajını bul veya yeni oluştur
            findOrCreateStatusMessage(channel);
            
            // Güncelleme görevini başlat
            startUpdateTask();
            
            plugin.getLogger().info("✅ Status panel başarıyla başlatıldı!");
            
        } catch (Exception e) {
            plugin.getLogger().severe("Status panel başlatma hatası: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Mevcut status mesajını bulur veya yeni oluşturur
     */
    private void findOrCreateStatusMessage(TextChannel channel) {
        channel.getHistory().retrievePast(50).queue(messages -> {
            // Bot'un gönderdiği status mesajını ara
            Message existingMessage = messages.stream()
                .filter(msg -> msg.getAuthor().equals(jda.getSelfUser()))
                .filter(msg -> !msg.getEmbeds().isEmpty())
                .filter(msg -> {
                    MessageEmbed embed = msg.getEmbeds().get(0);
                    return embed.getTitle() != null && embed.getTitle().contains("Sunucu Durumu");
                })
                .findFirst()
                .orElse(null);
            
            if (existingMessage != null) {
                this.statusMessage = existingMessage;
                plugin.getLogger().info("Mevcut status mesajı bulundu, güncelleniyor...");
                updateStatusMessage();
            } else {
                // Yeni mesaj oluştur
                createNewStatusMessage(channel);
            }
        }, error -> {
            plugin.getLogger().warning("Mesaj geçmişi alınırken hata: " + error.getMessage());
            createNewStatusMessage(channel);
        });
    }
    
    /**
     * Yeni status mesajı oluşturur
     */
    private void createNewStatusMessage(TextChannel channel) {
        MessageEmbed embed = createStatusEmbed();
        channel.sendMessageEmbeds(embed).queue(message -> {
            this.statusMessage = message;
            plugin.getLogger().info("Yeni status mesajı oluşturuldu!");
        }, error -> {
            plugin.getLogger().severe("Status mesajı oluşturulamadı: " + error.getMessage());
        });
    }
    
    /**
     * Güncelleme görevini başlatır
     */
    private void startUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        
        // İlk güncellemeyi 5 saniye sonra yap
        updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, 
            this::updateStatusMessage, 
            100L, // 5 saniye gecikme
            updateInterval * 20L // Güncelleme aralığı
        );
        
        plugin.getLogger().info("Status panel güncelleme görevi başlatıldı (her " + updateInterval + " saniye)");
    }
    
    /**
     * Status mesajını günceller
     */
    private void updateStatusMessage() {
        if (statusMessage == null) {
            plugin.getLogger().warning("Status mesajı bulunamadı!");
            return;
        }
        
        try {
            MessageEmbed newEmbed = createStatusEmbed();
            statusMessage.editMessageEmbeds(newEmbed).queue(
                success -> {
                    // Başarılı güncelleme (sessiz)
                },
                error -> {
                    plugin.getLogger().warning("Status mesajı güncellenirken hata: " + error.getMessage());
                    
                    // Mesaj silinmişse yeniden oluştur
                    if (error.getMessage().contains("Unknown Message")) {
                        plugin.getLogger().info("Status mesajı silinmiş, yeniden oluşturuluyor...");
                        recreateStatusMessage();
                    }
                }
            );
        } catch (Exception e) {
            plugin.getLogger().warning("Status güncelleme hatası: " + e.getMessage());
        }
    }
    
    /**
     * Status mesajını yeniden oluşturur
     */
    private void recreateStatusMessage() {
        try {
            Guild guild = jda.getGuildById(guildId);
            if (guild == null) return;
            
            TextChannel channel = guild.getTextChannelById(channelId);
            if (channel == null) return;
            
            createNewStatusMessage(channel);
        } catch (Exception e) {
            plugin.getLogger().severe("Status mesajı yeniden oluşturma hatası: " + e.getMessage());
        }
    }
    
    /**
     * Status embed'ini oluşturur
     */
    private MessageEmbed createStatusEmbed() {
        EmbedBuilder embed = new EmbedBuilder();
        
        // Sunucu verilerini topla
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        int maxPlayers = Bukkit.getMaxPlayers();
        double tps = getTPS();
        long[] memoryInfo = getMemoryInfo();
        long uptime = getUptime();
        List<String> playerNames = getPlayerNames();
        
        // Başlık ve renk
        embed.setTitle("🌟 " + serverName + " - Sunucu Durumu");
        embed.setColor(getStatusColor(tps, memoryInfo[2]));
        
        // Sunucu IP
        if (!serverIp.isEmpty()) {
            embed.setDescription("**🔗 Sunucu IP:** `" + serverIp + "`");
        }
        
        // TPS
        String tpsEmoji = getTpsEmoji(tps);
        embed.addField(tpsEmoji + " TPS", 
            String.format("**%.1f**/20.0", tps), true);
        
        // RAM
        String memoryEmoji = getMemoryEmoji(memoryInfo[2]);
        embed.addField(memoryEmoji + " RAM", 
            String.format("**%d MB** / %d MB\n(%%%d)", 
                memoryInfo[0], memoryInfo[1], memoryInfo[2]), true);
        
        // Uptime
        embed.addField("⏰ Çalışma Süresi", 
            formatUptime(uptime), true);
        
        // Oyuncular
        String playersEmoji = onlinePlayers > 0 ? "👥" : "😴";
        embed.addField(playersEmoji + " Oyuncular", 
            String.format("**%d**/%d çevrimiçi", onlinePlayers, maxPlayers), true);
        
        // Sürüm
        embed.addField("⚙️ Sürüm", 
            getServerVersion(), true);
        
        // Son güncelleme
        embed.addField("🔄 Güncelleme", 
            "<t:" + Instant.now().getEpochSecond() + ":R>", true);
        
        // Oyuncu listesi
        if (!playerNames.isEmpty()) {
            embed.addField("🎮 Çevrimiçi Oyuncular", 
                formatPlayerList(playerNames), false);
        }
        
        // Footer
        embed.setFooter(serverName + " • Otomatik güncelleme");
        embed.setTimestamp(Instant.now());
        
        return embed.build();
    }
    
    /**
     * TPS değerini alır
     */
    private double getTPS() {
        try {
            Object server = Bukkit.getServer().getClass().getMethod("getServer").invoke(null);
            Object[] recentTps = (Object[]) server.getClass().getField("recentTps").get(server);
            return Math.min(20.0, Math.max(0.0, (Double) recentTps[0]));
        } catch (Exception e) {
            return 20.0; // Varsayılan değer
        }
    }
    
    /**
     * Memory bilgilerini alır [kullanılan, maksimum, yüzde]
     */
    private long[] getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024; // MB
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024; // MB
        long percentage = maxMemory > 0 ? (usedMemory * 100) / maxMemory : 0;
        
        return new long[]{usedMemory, maxMemory, percentage};
    }
    
    /**
     * Uptime'ı alır
     */
    private long getUptime() {
        return ManagementFactory.getRuntimeMXBean().getUptime();
    }
    
    /**
     * Oyuncu isimlerini alır
     */
    private List<String> getPlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .collect(Collectors.toList());
    }
    
    /**
     * Sunucu sürümünü alır
     */
    private String getServerVersion() {
        String version = Bukkit.getVersion();
        if (version.contains("Paper")) return "📄 Paper";
        if (version.contains("Spigot")) return "🔧 Spigot";
        if (version.contains("Bukkit")) return "🪣 Bukkit";
        return "⚙️ " + version.split(" ")[0];
    }
    
    /**
     * Status rengini belirler
     */
    private Color getStatusColor(double tps, long memoryPercent) {
        if (tps >= 19.0 && memoryPercent < 80) {
            return new Color(87, 242, 135); // Yeşil
        } else if (tps >= 15.0 && memoryPercent < 90) {
            return new Color(254, 231, 92); // Sarı
        } else {
            return new Color(237, 66, 69); // Kırmızı
        }
    }
    
    /**
     * TPS emoji'sini seçer
     */
    private String getTpsEmoji(double tps) {
        if (tps >= 19.0) return "🟢";
        if (tps >= 15.0) return "🟡";
        return "🔴";
    }
    
    /**
     * Memory emoji'sini seçer
     */
    private String getMemoryEmoji(long memoryPercent) {
        if (memoryPercent < 70) return "🟢";
        if (memoryPercent < 85) return "🟡";
        return "🔴";
    }
    
    /**
     * Uptime'ı formatlar
     */
    private String formatUptime(long uptimeMs) {
        long days = TimeUnit.MILLISECONDS.toDays(uptimeMs);
        long hours = TimeUnit.MILLISECONDS.toHours(uptimeMs) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(uptimeMs) % 60;
        
        if (days > 0) {
            return String.format("**%d** gün **%d** saat", days, hours);
        } else if (hours > 0) {
            return String.format("**%d** saat **%d** dakika", hours, minutes);
        } else {
            return String.format("**%d** dakika", minutes);
        }
    }
    
    /**
     * Oyuncu listesini formatlar
     */
    private String formatPlayerList(List<String> players) {
        if (players.isEmpty()) {
            return "*Çevrimiçi oyuncu yok*";
        }
        
        if (players.size() <= 10) {
            return "`" + String.join("`, `", players) + "`";
        } else {
            List<String> first10 = players.subList(0, 10);
            return "`" + String.join("`, `", first10) + "` *ve " + (players.size() - 10) + " kişi daha...*";
        }
    }
    
    /**
     * Sistemi durdurur
     */
    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        plugin.getLogger().info("Status panel durduruldu.");
    }
    
    /**
     * Sistemi yeniden başlatır
     */
    public void reload() {
        stop();
        loadConfig();
        if (enabled) {
            start();
        }
    }
    
    /**
     * Manuel güncelleme yapar
     */
    public void forceUpdate() {
        updateStatusMessage();
        plugin.getLogger().info("Status panel manuel olarak güncellendi!");
    }
    
    // Getters
    public boolean isEnabled() { return enabled; }
    public String getChannelId() { return channelId; }
    public int getUpdateInterval() { return updateInterval; }
    public String getServerName() { return serverName; }
}