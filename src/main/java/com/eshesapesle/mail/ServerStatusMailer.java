package com.eshesapesle.mail;

import com.eshesapesle.EsHesapEsle;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import javax.mail.*;
import javax.mail.internet.*;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Sunucu durumu mail gonderici sistemi
 */
public class ServerStatusMailer {
    
    private final EsHesapEsle plugin;
    private BukkitTask mailTask;
    
    // Config degerleri
    private boolean enabled;
    private String recipientEmail;
    private String smtpHost;
    private int smtpPort;
    private String smtpUsername;
    private String smtpPassword;
    private boolean smtpAuth;
    private boolean smtpTls;
    private int intervalHours;
    private String serverName;
    private String serverIp;
    
    public ServerStatusMailer(EsHesapEsle plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    /**
     * Config degerlerini yukler
     */
    private void loadConfig() {
        this.enabled = plugin.getConfig().getBoolean("mail-reports.enabled", false);
        this.recipientEmail = plugin.getConfig().getString("mail-reports.recipient-email", "altugberkan@gmail.com");
        this.smtpHost = plugin.getConfig().getString("mail-reports.smtp.host", "smtp.gmail.com");
        this.smtpPort = plugin.getConfig().getInt("mail-reports.smtp.port", 587);
        this.smtpUsername = plugin.getConfig().getString("mail-reports.smtp.username", "");
        this.smtpPassword = plugin.getConfig().getString("mail-reports.smtp.password", "");
        this.smtpAuth = plugin.getConfig().getBoolean("mail-reports.smtp.auth", true);
        this.smtpTls = plugin.getConfig().getBoolean("mail-reports.smtp.tls", true);
        boolean smtpSsl = plugin.getConfig().getBoolean("mail-reports.smtp.ssl", false);
        this.intervalHours = plugin.getConfig().getInt("mail-reports.interval-hours", 1);
        this.serverName = plugin.getConfig().getString("discord.status-panel.server-name", "Minecraft Server");
        this.serverIp = plugin.getConfig().getString("discord.status-panel.server-ip", "");
        
        plugin.getLogger().info("Mail Rapor Sistemi Ayarlari:");
        plugin.getLogger().info("- Aktif: " + enabled);
        plugin.getLogger().info("- Alici: " + recipientEmail);
        plugin.getLogger().info("- SMTP Host: " + smtpHost);
        plugin.getLogger().info("- Aralik: " + intervalHours + " saat");
    }
    
    /**
     * Mail sistemini baslatir
     */
    public void start() {
        if (!enabled) {
            plugin.getLogger().info("Mail rapor sistemi devre disi.");
            return;
        }
        
        if (smtpUsername.isEmpty() || smtpPassword.isEmpty()) {
            plugin.getLogger().warning("SMTP kullanici adi veya sifre bos! Mail sistemi baslatilamadi.");
            return;
        }
        
        // Ilk maili 5 dakika sonra gonder, sonrasinda belirlenen araliklarla
        long intervalTicks = intervalHours * 60 * 60 * 20L; // Saat -> Tick
        long initialDelay = 5 * 60 * 20L; // 5 dakika gecikme
        
        mailTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, 
            this::sendStatusReport, 
            initialDelay, 
            intervalTicks
        );
        
        plugin.getLogger().info("Mail rapor sistemi baslatildi! (Her " + intervalHours + " saatte bir)");
    }
    
    /**
     * Mail sistemini durdurur
     */
    public void stop() {
        if (mailTask != null) {
            mailTask.cancel();
            mailTask = null;
        }
        plugin.getLogger().info("Mail rapor sistemi durduruldu.");
    }
    
    /**
     * Sistemi yeniden baslatir
     */
    public void reload() {
        stop();
        loadConfig();
        if (enabled) {
            start();
        }
    }
    
    /**
     * Manuel rapor gonderir
     */
    public void sendManualReport() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::sendStatusReport);
    }
    
    /**
     * Sunucu durumu raporunu gonderir
     */
    private void sendStatusReport() {
        try {
            // Sunucu verilerini topla
            ServerStatus status = collectServerStatus();
            
            // Mail icerigini olustur
            String subject = serverName + " - Sunucu Durum Raporu";
            String htmlContent = generateHtmlReport(status);
            
            // Mail gonder
            sendEmail(subject, htmlContent);
            
            plugin.getLogger().info("Sunucu durum raporu basariyla gonderildi: " + recipientEmail);
            
        } catch (Exception e) {
            plugin.getLogger().severe("Mail gonderilirken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Sunucu durumu verilerini toplar
     */
    private ServerStatus collectServerStatus() {
        // TPS hesaplama
        double tps = getTPS();
        
        // Memory bilgileri
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024; // MB
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024; // MB
        double memoryPercent = maxMemory > 0 ? (double) usedMemory / maxMemory * 100 : 0;
        
        // Uptime
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        
        // Oyuncu bilgileri
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        int maxPlayers = Bukkit.getMaxPlayers();
        List<String> playerNames = Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .collect(Collectors.toList());
        
        // Eslesen hesap sayisi
        int linkedAccounts = 0;
        try {
            linkedAccounts = plugin.getStorage().getLinkedAccountsCount();
        } catch (Exception e) {
            plugin.getLogger().warning("Eslesen hesap sayisi alinirken hata: " + e.getMessage());
        }
        
        return new ServerStatus(tps, usedMemory, maxMemory, memoryPercent, uptime, 
                               onlinePlayers, maxPlayers, playerNames, linkedAccounts);
    }
    
    /**
     * TPS degerini alir
     */
    private double getTPS() {
        try {
            Object server = Bukkit.getServer().getClass().getMethod("getServer").invoke(null);
            Object[] recentTps = (Object[]) server.getClass().getField("recentTps").get(server);
            return Math.min(20.0, Math.max(0.0, (Double) recentTps[0]));
        } catch (Exception e) {
            return 20.0; // Varsayilan deger
        }
    }
    
    /**
     * HTML mail icerigini olusturur
     */
    private String generateHtmlReport(ServerStatus status) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        String currentTime = dateFormat.format(new Date());
        
        // Durum rengini belirle
        String statusColor = getStatusColor(status.getTps(), status.getMemoryPercent());
        String statusText = getStatusText(status.getTps(), status.getMemoryPercent());
        String statusEmoji = getStatusEmoji(status.getTps(), status.getMemoryPercent());
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html lang='tr'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>Sunucu Durum Raporu</title>");
        html.append("<style>");
        html.append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }");
        html.append(".container { max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); overflow: hidden; }");
        html.append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; }");
        html.append(".header h1 { margin: 0; font-size: 28px; font-weight: 300; }");
        html.append(".header p { margin: 10px 0 0 0; opacity: 0.9; font-size: 16px; }");
        html.append(".status-badge { display: inline-block; padding: 8px 16px; border-radius: 20px; font-weight: bold; margin: 15px 0; }");
        html.append(".content { padding: 30px; }");
        html.append(".metric-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; margin: 20px 0; }");
        html.append(".metric-card { background-color: #f8f9fa; border-radius: 8px; padding: 20px; border-left: 4px solid #667eea; }");
        html.append(".metric-title { font-size: 14px; color: #6c757d; margin-bottom: 5px; font-weight: 600; text-transform: uppercase; }");
        html.append(".metric-value { font-size: 24px; font-weight: bold; color: #2c3e50; margin-bottom: 5px; }");
        html.append(".metric-subtitle { font-size: 12px; color: #6c757d; }");
        html.append(".players-section { margin-top: 30px; padding: 20px; background-color: #f8f9fa; border-radius: 8px; }");
        html.append(".players-list { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px; }");
        html.append(".player-tag { background-color: #667eea; color: white; padding: 4px 12px; border-radius: 15px; font-size: 12px; }");
        html.append(".footer { background-color: #2c3e50; color: white; padding: 20px; text-align: center; font-size: 12px; }");
        html.append(".progress-bar { width: 100%; height: 8px; background-color: #e9ecef; border-radius: 4px; overflow: hidden; margin-top: 8px; }");
        html.append(".progress-fill { height: 100%; transition: width 0.3s ease; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        
        html.append("<div class='container'>");
        
        // Header
        html.append("<div class='header'>");
        html.append("<h1>").append(statusEmoji).append(" ").append(serverName).append("</h1>");
        html.append("<p>Sunucu Durum Raporu</p>");
        if (!serverIp.isEmpty()) {
            html.append("<p style='font-family: monospace; background: rgba(255,255,255,0.2); padding: 8px 16px; border-radius: 20px; display: inline-block; margin-top: 10px;'>");
            html.append("Sunucu IP: ").append(serverIp);
            html.append("</p>");
        }
        html.append("<div class='status-badge' style='background-color: ").append(statusColor).append(";'>");
        html.append(statusText);
        html.append("</div>");
        html.append("</div>");
        
        // Content
        html.append("<div class='content'>");
        
        // Metrics Grid
        html.append("<div class='metric-grid'>");
        
        // TPS
        html.append("<div class='metric-card'>");
        html.append("<div class='metric-title'>TPS (Tick Per Second)</div>");
        html.append("<div class='metric-value'>").append(String.format("%.1f", status.getTps())).append("/20.0</div>");
        html.append("<div class='progress-bar'>");
        html.append("<div class='progress-fill' style='width: ").append((status.getTps() / 20.0) * 100).append("%; background-color: ").append(getTpsColor(status.getTps())).append(";'></div>");
        html.append("</div>");
        html.append("</div>");
        
        // RAM
        html.append("<div class='metric-card'>");
        html.append("<div class='metric-title'>RAM Kullanimi</div>");
        html.append("<div class='metric-value'>").append(status.getUsedMemory()).append(" MB</div>");
        html.append("<div class='metric-subtitle'>").append(status.getMaxMemory()).append(" MB toplam (").append(String.format("%.1f", status.getMemoryPercent())).append("%)</div>");
        html.append("<div class='progress-bar'>");
        html.append("<div class='progress-fill' style='width: ").append(status.getMemoryPercent()).append("%; background-color: ").append(getMemoryColor(status.getMemoryPercent())).append(";'></div>");
        html.append("</div>");
        html.append("</div>");
        
        // Uptime
        html.append("<div class='metric-card'>");
        html.append("<div class='metric-title'>Calisma Suresi</div>");
        html.append("<div class='metric-value'>").append(formatUptime(status.getUptime())).append("</div>");
        html.append("</div>");
        
        // Players
        html.append("<div class='metric-card'>");
        html.append("<div class='metric-title'>Oyuncular</div>");
        html.append("<div class='metric-value'>").append(status.getOnlinePlayers()).append("/").append(status.getMaxPlayers()).append("</div>");
        html.append("<div class='metric-subtitle'>cevrimici</div>");
        html.append("</div>");
        
        // Linked Accounts
        html.append("<div class='metric-card'>");
        html.append("<div class='metric-title'>Eslesen Hesaplar</div>");
        html.append("<div class='metric-value'>").append(status.getLinkedAccounts()).append("</div>");
        html.append("<div class='metric-subtitle'>Discord ile eslestirilmis</div>");
        html.append("</div>");
        
        html.append("</div>"); // metric-grid
        
        // Players List
        if (!status.getPlayerNames().isEmpty()) {
            html.append("<div class='players-section'>");
            html.append("<h3 style='margin: 0 0 10px 0; color: #2c3e50;'>Cevrimici Oyuncular</h3>");
            html.append("<div class='players-list'>");
            
            List<String> players = status.getPlayerNames();
            int displayCount = Math.min(players.size(), 20); // Maksimum 20 oyuncu goster
            
            for (int i = 0; i < displayCount; i++) {
                html.append("<span class='player-tag'>").append(players.get(i)).append("</span>");
            }
            
            if (players.size() > 20) {
                html.append("<span class='player-tag' style='background-color: #6c757d;'>+").append(players.size() - 20).append(" kisi daha</span>");
            }
            
            html.append("</div>");
            html.append("</div>");
        }
        
        html.append("</div>"); // content
        
        // Footer
        html.append("<div class='footer'>");
        html.append("<p>📅 Rapor Tarihi: ").append(currentTime).append("</p>");
        html.append("<p>Bu rapor otomatik olarak ").append(serverName).append(" tarafindan gonderilmistir.</p>");
        html.append("<p style='margin-top: 15px; opacity: 0.7;'>EsHesapEsle Plugin v1.0 | Gelistirici: Hermes/Livany</p>");
        html.append("</div>");
        
        html.append("</div>"); // container
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }
    

    
    // Yardimci metodlar
    private String getStatusColor(double tps, double memoryPercent) {
        if (tps >= 19.0 && memoryPercent < 80) return "#28a745"; // Yesil
        if (tps >= 15.0 && memoryPercent < 90) return "#ffc107"; // Sari
        return "#dc3545"; // Kirmizi
    }
    
    private String getStatusText(double tps, double memoryPercent) {
        if (tps >= 19.0 && memoryPercent < 80) return "Mukemmel Performans";
        if (tps >= 15.0 && memoryPercent < 90) return "Iyi Performans";
        return "Performans Sorunu";
    }
    
    private String getStatusEmoji(double tps, double memoryPercent) {
        if (tps >= 19.0 && memoryPercent < 80) return "🟢";
        if (tps >= 15.0 && memoryPercent < 90) return "🟡";
        return "🔴";
    }
    
    private String getTpsColor(double tps) {
        if (tps >= 19.0) return "#28a745";
        if (tps >= 15.0) return "#ffc107";
        return "#dc3545";
    }
    
    private String getMemoryColor(double memoryPercent) {
        if (memoryPercent < 70) return "#28a745";
        if (memoryPercent < 85) return "#ffc107";
        return "#dc3545";
    }
    
    private String formatUptime(long uptimeMs) {
        long days = TimeUnit.MILLISECONDS.toDays(uptimeMs);
        long hours = TimeUnit.MILLISECONDS.toHours(uptimeMs) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(uptimeMs) % 60;
        
        if (days > 0) {
            return days + " gun " + hours + " saat";
        } else if (hours > 0) {
            return hours + " saat " + minutes + " dakika";
        } else {
            return minutes + " dakika";
        }
    }
    
    /**
     * E-posta gonderir (SSL destegi ile)
     */
    private void sendEmail(String subject, String htmlContent) throws Exception {
        Properties props = new Properties();
        
        // SMTP ayarlari
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", String.valueOf(smtpPort));
        props.put("mail.smtp.auth", String.valueOf(smtpAuth));
        
        // SSL/TLS ayarlari
        boolean smtpSsl = plugin.getConfig().getBoolean("mail-reports.smtp.ssl", false);
        if (smtpSsl) {
            // SSL (port 465)
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            props.put("mail.smtp.ssl.trust", smtpHost);
        } else if (smtpTls) {
            // STARTTLS (port 587)
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        }
        
        // Debug modu
        props.put("mail.debug", "false");
        
        // Session olustur
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUsername, smtpPassword);
            }
        });
        
        // Mail mesajini olustur
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(smtpUsername, serverName + " Sunucu"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
        message.setSubject(subject, "UTF-8");
        message.setContent(htmlContent, "text/html; charset=UTF-8");
        message.setSentDate(new Date());
        
        // Mail gonder
        Transport.send(message);
    }
    
    // Getters
    public boolean isEnabled() { return enabled; }
    public String getRecipientEmail() { return recipientEmail; }
    public int getIntervalHours() { return intervalHours; }
    
    /**
     * Sunucu durumu veri sinifi
     */
    public static class ServerStatus {
        private final double tps;
        private final long usedMemory;
        private final long maxMemory;
        private final double memoryPercent;
        private final long uptime;
        private final int onlinePlayers;
        private final int maxPlayers;
        private final List<String> playerNames;
        private final int linkedAccounts;
        
        public ServerStatus(double tps, long usedMemory, long maxMemory, double memoryPercent,
                           long uptime, int onlinePlayers, int maxPlayers, List<String> playerNames,
                           int linkedAccounts) {
            this.tps = tps;
            this.usedMemory = usedMemory;
            this.maxMemory = maxMemory;
            this.memoryPercent = memoryPercent;
            this.uptime = uptime;
            this.onlinePlayers = onlinePlayers;
            this.maxPlayers = maxPlayers;
            this.playerNames = playerNames;
            this.linkedAccounts = linkedAccounts;
        }
        
        // Getters
        public double getTps() { return tps; }
        public long getUsedMemory() { return usedMemory; }
        public long getMaxMemory() { return maxMemory; }
        public double getMemoryPercent() { return memoryPercent; }
        public long getUptime() { return uptime; }
        public int getOnlinePlayers() { return onlinePlayers; }
        public int getMaxPlayers() { return maxPlayers; }
        public List<String> getPlayerNames() { return playerNames; }
        public int getLinkedAccounts() { return linkedAccounts; }
    }
}