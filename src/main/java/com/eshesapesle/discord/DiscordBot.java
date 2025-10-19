package com.eshesapesle.discord;

import com.eshesapesle.EsHesapEsle;
import com.eshesapesle.updater.AutoUpdater;
import com.eshesapesle.suggestion.SuggestionAnalyzer;
import com.eshesapesle.status.LiveStatusPanel;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.OnlineStatus;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import me.clip.placeholderapi.PlaceholderAPI;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.entities.UserSnowflake;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;

public class DiscordBot extends ListenerAdapter {
    private final EsHesapEsle plugin;
    private JDA jda;
    private String guildId;
    private String eslendiRolId;
    private final String komutKanalId;
    private final List<String> yetkiliRoller;
    private final List<String> izinliKomutlar;
    private final Map<String, String> permissionRoles;
    private final int activityUpdateInterval;
    private final String activityStatus;
    private final String activityType;
    private final String activityMessage;
    private String linkChannelId;
    private Message infoMessage;
    private final SuggestionAnalyzer suggestionAnalyzer;
    
    // Log ayarları
    private final boolean chatLogEnabled;
    private final boolean joinQuitLogEnabled;
    private final String logChannelId;
    
    private final Map<String, Integer> userTicketCount = new ConcurrentHashMap<>();
    private final String ticketCategoryId;
    private final String ticketLogChannelId;
    private final String supportRoleId;
    private final String ticketChannelId;
    private final int maxTickets;
    private final boolean requireLinkedAccount;
    private final Map<String, ButtonConfig> ticketButtons;
    
    // Öneri sistemi ayarları
    private final String oneriKanalId;
    private final String oneriLogKanalId;
    private final String enIyiOneriRolId;
    private final int minOyEnIyiOneriRolu;
    private final boolean oneriSistemiAktif;
    
    private FileConfiguration langConfig;
    private String currentLanguage;
    private File langFile;
    
    // Live Status Panel
    private LiveStatusPanel liveStatusPanel;
    

    
    public DiscordBot(EsHesapEsle plugin) {
        this.plugin = plugin;
        loadLanguage();
        FileConfiguration config = plugin.getConfig();
        this.guildId = config.getString("discord.guild-id");
        this.linkChannelId = config.getString("discord.link-channel-id");
        this.eslendiRolId = config.getString("discord.roller.EslendiRolID");
        this.komutKanalId = config.getString("discord.komut-kontrol.kanal-id");
        this.yetkiliRoller = config.getStringList("discord.komut-kontrol.yetkili-roller");
        this.izinliKomutlar = config.getStringList("discord.komut-kontrol.izinli-komutlar");
        
        // Öneri sistemi ayarlarını yükle
        this.oneriKanalId = config.getString("oneri.kanal-id", "");
        this.oneriLogKanalId = config.getString("oneri.log-kanal-id", "");
        this.enIyiOneriRolId = config.getString("oneri.en-iyi-oneri-rol-id", "");
        this.minOyEnIyiOneriRolu = config.getInt("oneri.min-oy-en-iyi-oneri", 10);
        this.oneriSistemiAktif = config.getBoolean("oneri.aktif", true);
        
        // Öneri analiz sistemini başlat
        this.suggestionAnalyzer = new SuggestionAnalyzer(plugin);
        
        plugin.getLogger().info("Öneri sistemi ayarları yüklendi:");
        plugin.getLogger().info("- Sistem aktif: " + oneriSistemiAktif);
        plugin.getLogger().info("- Kanal ID: " + oneriKanalId);
        plugin.getLogger().info("- Log kanal ID: " + oneriLogKanalId);
        
        // Log ayarlarını yükle
        this.chatLogEnabled = config.getBoolean("discord.server_chat_logs.status", false);
        this.joinQuitLogEnabled = config.getBoolean("discord.server_chat_logs.joinQuitLogStatus", false);
        this.logChannelId = config.getString("discord.server_chat_logs.logChannel", "");
        
        // Aktivite ayarlarını yükle
        this.activityUpdateInterval = config.getInt("discord.activity.update-interval", 60);
        this.activityStatus = config.getString("discord.activity.status", "online");
        this.activityType = config.getString("discord.activity.type", "playing");
        this.activityMessage = config.getString("discord.activity.message", "%server_online% oyuncu oynuyor");
        
        // Yetki-rol eşleştirmelerini yükle
        this.permissionRoles = new HashMap<>();
        if (config.isConfigurationSection("discord.roller.Yetkiler")) {
            for (String permission : config.getConfigurationSection("discord.roller.Yetkiler").getKeys(false)) {
                String roleId = config.getString("discord.roller.Yetkiler." + permission);
                if (roleId != null && !roleId.isEmpty()) {
                    permissionRoles.put(permission, roleId);
                }
            }
        }
        
        // Ticket ayarlarını yükle
        this.ticketCategoryId = config.getString("discord.ticket.category-id", "");
        this.ticketLogChannelId = config.getString("discord.ticket.log-channel", "");
        this.supportRoleId = config.getString("discord.ticket.support-role", "");
        this.ticketChannelId = config.getString("discord.ticket.ticket-channel", "");
        this.maxTickets = config.getInt("discord.ticket.max-tickets", 1);
        this.requireLinkedAccount = config.getBoolean("discord.ticket.require-linked-account", true);
        
        // Buton konfigürasyonlarını yükle
        this.ticketButtons = new HashMap<>();
        ConfigurationSection buttonSection = config.getConfigurationSection("discord.ticket.buttons");
        if (buttonSection != null) {
            for (String key : buttonSection.getKeys(false)) {
                String emoji = buttonSection.getString(key + ".emoji");
                String label = buttonSection.getString(key + ".label");
                ButtonStyle color = ButtonStyle.valueOf(buttonSection.getString(key + ".color", "GRAY"));
                ticketButtons.put(key, new ButtonConfig(emoji, label, color));
            }
        }
    }
    
    public void reloadLanguage() {
        String newLanguage = plugin.getConfig().getString("language", "tr");
        plugin.getLogger().info("Dil değiştiriliyor: " + currentLanguage + " -> " + newLanguage);
        loadLanguage();
        
        // Dil değişikliğini logla
        plugin.getLogger().info("Dil başarıyla değiştirildi: " + newLanguage);
        plugin.getLogger().info("Dil dosyası: " + langFile.getName());
    }

    private void loadLanguage() {
        String language = plugin.getConfig().getString("language", "tr");
        String langFileName = language.equals("tr") ? "tr_TR.yml" : "en_EN.yml";
        
        plugin.getLogger().info("Dil dosyası yükleniyor: " + langFileName);
        
        // Dil dosyasını kontrol et
        langFile = new File(plugin.getDataFolder() + "/lang", langFileName);
        if (!langFile.exists()) {
            plugin.getLogger().info("Dil dosyası bulunamadı, oluşturuluyor...");
            langFile.getParentFile().mkdirs();
            plugin.saveResource("lang/" + langFileName, false);
        }

        // Diğer dil dosyasını da kopyala
        String otherLangFile = language.equals("tr") ? "en_EN.yml" : "tr_TR.yml";
        File otherFile = new File(plugin.getDataFolder() + "/lang", otherLangFile);
        if (!otherFile.exists()) {
            plugin.saveResource("lang/" + otherLangFile, false);
        }

        // Dil dosyasını yükle
        langConfig = YamlConfiguration.loadConfiguration(langFile);
        currentLanguage = language;
        
        plugin.getLogger().info("Dil dosyası başarıyla yüklendi: " + langFileName);
    }

    private String getMessage(String path) {
        String message = langConfig.getString("suggestions." + path);
        if (message == null) {
            plugin.getLogger().warning("Mesaj bulunamadı: " + path);
            return path;
        }
        return message;
    }

    private String formatMessage(String path, Object... args) {
        String message = getMessage(path);
        for (int i = 0; i < args.length; i += 2) {
            message = message.replace("{" + args[i] + "}", String.valueOf(args[i + 1]));
        }
        return message;
    }
    
    public void start() {
        try {
            String token = plugin.getConfig().getString("discord.bot-token");
            if (token == null || token.isEmpty() || token.equals("YOUR_BOT_TOKEN")) {
                plugin.getLogger().severe("╔══════════════════════════════════════════════════════════════╗");
                plugin.getLogger().severe("║                    DISCORD BOT HATASI                       ║");
                plugin.getLogger().severe("║                                                              ║");
                plugin.getLogger().severe("║  ❌ Discord bot token ayarlanmamış!                         ║");
                plugin.getLogger().severe("║                                                              ║");
                plugin.getLogger().severe("║  Çözüm:                                                      ║");
                plugin.getLogger().severe("║  1. config.yml dosyasını açın                               ║");
                plugin.getLogger().severe("║  2. 'bot-token: \"\"' kısmına bot token'ınızı yazın          ║");
                plugin.getLogger().severe("║  3. 'guild-id: \"\"' kısmına sunucu ID'nizi yazın           ║");
                plugin.getLogger().severe("║  4. Plugin'i yeniden başlatın                               ║");
                plugin.getLogger().severe("║                                                              ║");
                plugin.getLogger().severe("║  Bot token almak için:                                      ║");
                plugin.getLogger().severe("║  https://discord.com/developers/applications                ║");
                plugin.getLogger().severe("╚══════════════════════════════════════════════════════════════╝");
                return;
            }
            
            // JDA'yı başlat
            jda = JDABuilder.createDefault(token)
                    .enableIntents(
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.GUILD_MESSAGE_REACTIONS
                    )
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.ROLE_TAGS)
                    .addEventListeners(this)
                    .build();

            try {
                jda.awaitReady(); // JDA'nın hazır olmasını bekle
                
                // Slash komutlarını kaydet
                Guild guild = jda.getGuildById(guildId);
                if (guild != null) {
                    guild.updateCommands()
                            .addCommands(
                                Commands.slash("hesapesle", "Minecraft hesabınızı Discord ile eşleştirin")
                                    .addOption(OptionType.STRING, "kod", "Minecraft'tan aldığınız 6 haneli kod", true)
                            ).queue();
                    
                    // Aktivite güncelleme görevini başlat
                    startActivityUpdateTask();
                    
                    // Live Status Panel'i başlat
                    liveStatusPanel = new LiveStatusPanel(plugin, jda);
                    liveStatusPanel.start();
                    

                    
                    // Link paneli güncelleme görevini başlat
                    startLinkPanelUpdateTask();
                    
                    plugin.getLogger().info("Discord bot başarıyla başlatıldı!");
                } else {
                    plugin.getLogger().severe("Discord sunucusu bulunamadı! Guild ID: " + guildId);
                }
            } catch (InterruptedException e) {
                plugin.getLogger().severe("Discord bot başlatılırken timeout oluştu!");
                return;
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Discord bot başlatılırken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void startActivityUpdateTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (jda != null) {
                updateActivity();
            }
        }, 0L, activityUpdateInterval * 20L);
    }
    
    private void updateActivity() {
        try {
            // Eşleşen hesap sayısını al
            int linkedAccounts = plugin.getStorage().getLinkedAccountsCount();
            
            // PlaceholderAPI ile mesajı işle
            String processedMessage = activityMessage
                .replace("%linked_accounts%", String.valueOf(linkedAccounts));
                
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                processedMessage = PlaceholderAPI.setPlaceholders(null, processedMessage);
            }
            
            // Aktivite tipini belirle
            Activity activity = null;
            switch (activityType.toLowerCase()) {
                case "playing":
                    activity = Activity.playing(processedMessage);
                    break;
                case "watching":
                    activity = Activity.watching(processedMessage);
                    break;
                case "streaming":
                    activity = Activity.streaming(processedMessage, "https://twitch.tv/");
                    break;
                case "listening":
                    activity = Activity.listening(processedMessage);
                    break;
                default:
                    activity = Activity.playing(processedMessage);
            }
            
            // Durumu belirle
            OnlineStatus status = OnlineStatus.ONLINE;
            switch (activityStatus.toLowerCase()) {
                case "idle":
                    status = OnlineStatus.IDLE;
                    break;
                case "dnd":
                    status = OnlineStatus.DO_NOT_DISTURB;
                    break;
                case "invisible":
                    status = OnlineStatus.INVISIBLE;
                    break;
            }
            
            // Aktiviteyi güncelle
            jda.getPresence().setPresence(status, activity);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Bot aktivitesi güncellenirken hata: " + e.getMessage());
        }
    }
    
    public void shutdown() {
        try {
            plugin.getLogger().info("Discord bot kapatılıyor...");
            
            // Sunucu kapatma mesajını gönder
            sendServerShutdownMessage();
            
            // Live Status Panel'i kapat
            if (liveStatusPanel != null) {
                liveStatusPanel.stop();
                liveStatusPanel = null;
            }
            

            
            // JDA'yı güvenli şekilde kapat
            if (jda != null) {
                plugin.getLogger().info("JDA bağlantısı kapatılıyor...");
                
                // Önce shutdown başlat
                jda.shutdown();
                
                // 10 saniye bekle, sonra zorla kapat
                try {
                    if (!jda.awaitShutdown(10, TimeUnit.SECONDS)) {
                        plugin.getLogger().warning("JDA 10 saniyede kapanmadı, zorla kapatılıyor...");
                        jda.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    plugin.getLogger().warning("JDA kapatılırken kesintiye uğradı, zorla kapatılıyor...");
                    jda.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                
                jda = null;
                plugin.getLogger().info("Discord bot başarıyla kapatıldı!");
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Discord bot kapatılırken hata: " + e.getMessage());
            e.printStackTrace();
            
            // Hata durumunda zorla kapat
            if (jda != null) {
                try {
                    jda.shutdownNow();
                } catch (Exception ex) {
                    plugin.getLogger().severe("JDA zorla kapatılırken hata: " + ex.getMessage());
                }
            }
        }
    }
    
    /**
     * JDA instance'ını döndürür
     */
    public JDA getJDA() {
        return jda;
    }
    
    /**
     * LiveStatusPanel instance'ını döndürür
     */
    public LiveStatusPanel getLiveStatusPanel() {
        return liveStatusPanel;
    }
    

    
    /**
     * Guild instance'ını döndürür
     */
    public Guild getGuild() {
        if (jda == null) return null;
        return jda.getGuildById(guildId);
    }
    
    /**
     * Eşlendi rol ID'sini döndürür
     */
    public String getEslendiRolId() {
        return eslendiRolId;
    }
    
    /**
     * Dil config'ini döndürür
     */
    public FileConfiguration getLangConfig() {
        return langConfig;
    }
    
    /**
     * Chat mesajı gönderir
     */
    public void sendChatMessage(Player player, String message) {
        if (!chatLogEnabled || logChannelId.isEmpty()) return;
        
        try {
            Guild guild = getGuild();
            if (guild == null) return;
            
            TextChannel logChannel = guild.getTextChannelById(logChannelId);
            if (logChannel == null) return;
            
            EmbedBuilder embed = new EmbedBuilder()
                .setColor(0x5865F2)
                .setAuthor(player.getName(), null, "https://mc-heads.net/avatar/" + player.getName())
                .setDescription("💬 " + message)
                .setTimestamp(Instant.now());
            
            logChannel.sendMessageEmbeds(embed.build()).queue();
        } catch (Exception e) {
            plugin.getLogger().warning("Chat mesajı gönderilirken hata: " + e.getMessage());
        }
    }
    
    /**
     * Oyuncu giriş mesajı gönderir
     */
    public void sendPlayerJoinMessage(Player player) {
        if (!joinQuitLogEnabled || logChannelId.isEmpty()) return;
        
        try {
            Guild guild = getGuild();
            if (guild == null) return;
            
            TextChannel logChannel = guild.getTextChannelById(logChannelId);
            if (logChannel == null) return;
            
            EmbedBuilder embed = new EmbedBuilder()
                .setColor(0x57F287) // Yeşil
                .setAuthor(player.getName(), null, "https://mc-heads.net/avatar/" + player.getName())
                .setDescription("📥 **" + player.getName() + "** sunucuya katıldı!")
                .setTimestamp(Instant.now());
            
            logChannel.sendMessageEmbeds(embed.build()).queue();
        } catch (Exception e) {
            plugin.getLogger().warning("Giriş mesajı gönderilirken hata: " + e.getMessage());
        }
    }
    
    /**
     * Oyuncu çıkış mesajı gönderir
     */
    public void sendPlayerQuitMessage(Player player) {
        if (!joinQuitLogEnabled || logChannelId.isEmpty()) return;
        
        try {
            Guild guild = getGuild();
            if (guild == null) return;
            
            TextChannel logChannel = guild.getTextChannelById(logChannelId);
            if (logChannel == null) return;
            
            EmbedBuilder embed = new EmbedBuilder()
                .setColor(0xED4245) // Kırmızı
                .setAuthor(player.getName(), null, "https://mc-heads.net/avatar/" + player.getName())
                .setDescription("📤 **" + player.getName() + "** sunucudan ayrıldı!")
                .setTimestamp(Instant.now());
            
            logChannel.sendMessageEmbeds(embed.build()).queue();
        } catch (Exception e) {
            plugin.getLogger().warning("Çıkış mesajı gönderilirken hata: " + e.getMessage());
        }
    }
    
    /**
     * Oyuncu rollerini günceller
     */
    public void updatePlayerRoles(UUID playerUUID) {
        try {
            String discordId = plugin.getStorage().getDiscordId(playerUUID);
            if (discordId == null) return;
            
            Guild guild = getGuild();
            if (guild == null) return;
            
            Member member = guild.getMemberById(discordId);
            if (member == null) return;
            
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null || !player.isOnline()) return;
            
            updatePlayerRoles(playerUUID, member, guild);
        } catch (Exception e) {
            plugin.getLogger().warning("Oyuncu rolleri güncellenirken hata: " + e.getMessage());
        }
    }
    
    /**
     * Oyuncu rollerini günceller (detaylı)
     */
    public void updatePlayerRoles(UUID playerUUID, Member member, Guild guild) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null || !player.isOnline()) return;
        
        // Mevcut rolleri tut
        Set<Role> rolesToAdd = new HashSet<>();
        Set<Role> rolesToRemove = new HashSet<>();
        
        // Her yetki-rol eşleştirmesini kontrol et
        for (Map.Entry<String, String> entry : permissionRoles.entrySet()) {
            String permission = entry.getKey();
            String roleId = entry.getValue();
            
            // Boş rol ID'sini atla
            if (roleId == null || roleId.isEmpty()) {
                continue;
            }
            
            try {
                Role role = guild.getRoleById(roleId);
                if (role != null) {
                    if (player.hasPermission(permission)) {
                        rolesToAdd.add(role);
                    } else {
                        rolesToRemove.add(role);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Geçersiz rol ID'si: " + roleId + " - " + e.getMessage());
            }
        }
        
        // Rolleri toplu güncelle
        if (!rolesToAdd.isEmpty() || !rolesToRemove.isEmpty()) {
            guild.modifyMemberRoles(member, rolesToAdd, rolesToRemove).queue(
                success -> plugin.getLogger().info(player.getName() + " için Discord rolleri güncellendi."),
                error -> plugin.getLogger().warning(player.getName() + " için Discord rolleri güncellenirken hata: " + error.getMessage())
            );
        }
    }
    
    /**
     * Ödül mesajı gönderir
     */
    public void sendRewardMessage(Player player, String rewards) {
        // Bu metod RewardManager tarafından kullanılır
        // Şimdilik boş bırakıyoruz, gerekirse daha sonra implement edilir
    }
    
    /**
     * Öneri gönderir
     */
    public void sendSuggestion(Player player, String suggestion) {
        if (!oneriSistemiAktif || oneriKanalId.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Öneri sistemi şu anda aktif değil!");
            return;
        }
        
        try {
            Guild guild = getGuild();
            if (guild == null) {
                player.sendMessage(ChatColor.RED + "Discord sunucusuna bağlanılamadı!");
                return;
            }
            
            TextChannel oneriChannel = guild.getTextChannelById(oneriKanalId);
            if (oneriChannel == null) {
                player.sendMessage(ChatColor.RED + "Öneri kanalı bulunamadı!");
                return;
            }
            
            // Öneri embed'i oluştur
            EmbedBuilder embed = new EmbedBuilder()
                .setColor(0x5865F2)
                .setTitle("💡 Yeni Öneri")
                .setDescription(suggestion)
                .setAuthor(player.getName(), null, "https://mc-heads.net/avatar/" + player.getName())
                .setTimestamp(Instant.now())
                .setFooter("Öneri ID: " + System.currentTimeMillis());
            
            oneriChannel.sendMessageEmbeds(embed.build()).queue(message -> {
                // Tepki emojileri ekle
                message.addReaction(Emoji.fromUnicode("✅")).queue();
                message.addReaction(Emoji.fromUnicode("❌")).queue();
            });
            
        } catch (Exception e) {
            plugin.getLogger().warning("Öneri gönderilirken hata: " + e.getMessage());
            player.sendMessage(ChatColor.RED + "Öneri gönderilirken bir hata oluştu!");
        }
    }
    
    /**
     * Hesap eşleme paneli mesajını günceller
     */
    public void updateLinkPanel() {
        if (linkChannelId.isEmpty()) return;
        
        try {
            Guild guild = getGuild();
            if (guild == null) return;
            
            TextChannel linkChannel = guild.getTextChannelById(linkChannelId);
            if (linkChannel == null) return;
            
            // Eşleşen hesap sayısını al
            int linkedAccounts = plugin.getStorage().getLinkedAccountsCount();
            
            // Son eşlenen hesapları al (son 5) - basit versiyon
            List<String> recentLinks = new ArrayList<>();
            try {
                // Çevrimiçi eşleşen oyuncuları göster
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (plugin.getStorage().getDiscordId(player.getUniqueId()) != null) {
                        recentLinks.add(player.getName());
                        if (recentLinks.size() >= 5) break;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Son eşlenen hesaplar alınırken hata: " + e.getMessage());
            }
            
            // Embed oluştur
            EmbedBuilder embed = new EmbedBuilder()
                .setColor(0x5865F2)
                .setTitle("🔗 Hesap Eşleme Paneli")
                .setDescription("Minecraft hesabınızı Discord ile eşleştirin!")
                .addField("📊 İstatistikler", 
                    "**Toplam Eşleşen Hesap:** " + linkedAccounts + "\n" +
                    "**Aktif Oyuncular:** " + Bukkit.getOnlinePlayers().size(), 
                    false)
                .addField("📝 Nasıl Eşleştirilir?", 
                    "1️⃣ Minecraft'ta `/discord eşle` komutunu kullan\n" +
                    "2️⃣ Aldığın kodu buraya `/hesapesle <kod>` şeklinde yaz\n" +
                    "3️⃣ Hesabın eşleştirilecek ve özel rol alacaksın!", 
                    false);
            
            // Son eşlenen hesapları ekle
            if (!recentLinks.isEmpty()) {
                StringBuilder recentText = new StringBuilder();
                for (int i = 0; i < recentLinks.size(); i++) {
                    recentText.append((i + 1)).append(". ").append(recentLinks.get(i)).append("\n");
                }
                embed.addField("🆕 Son Eşlenen Hesaplar", recentText.toString(), false);
            }
            
            embed.setFooter("Son güncelleme")
                .setTimestamp(Instant.now());
            
            // Mevcut paneli bul ve güncelle, yoksa yeni oluştur
            linkChannel.getHistory().retrievePast(10).queue(messages -> {
                Message panelMessage = null;
                for (Message msg : messages) {
                    if (msg.getAuthor().equals(jda.getSelfUser()) && 
                        !msg.getEmbeds().isEmpty() && 
                        msg.getEmbeds().get(0).getTitle() != null &&
                        msg.getEmbeds().get(0).getTitle().contains("Hesap Eşleme Paneli")) {
                        panelMessage = msg;
                        break;
                    }
                }
                
                if (panelMessage != null) {
                    // Mevcut mesajı güncelle
                    panelMessage.editMessageEmbeds(embed.build()).queue();
                } else {
                    // Yeni mesaj gönder
                    linkChannel.sendMessageEmbeds(embed.build()).queue();
                }
            });
            
        } catch (Exception e) {
            plugin.getLogger().warning("Link paneli güncellenirken hata: " + e.getMessage());
        }
    }
    
    /**
     * Slash command etkileşimlerini işler
     */
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("hesapesle")) {
            handleLinkCommand(event);
        } else if (event.getName().equals("discord")) {
            handleDiscordCommand(event);
        }
    }
    
    /**
     * Buton etkileşimlerini işler
     */
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        
        if (buttonId.startsWith("ticket_")) {
            handleTicketButton(event);
        }
    }
    
    /**
     * Ticket buton etkileşimini işler
     */
    private void handleTicketButton(ButtonInteractionEvent event) {
        // Ticket sistemi için buton işleme
        // Şimdilik basit bir yanıt
        event.reply("Ticket sistemi yakında aktif olacak!").setEphemeral(true).queue();
    }
    
    /**
     * Hesap eşleme komutunu işler
     */
    private void handleLinkCommand(SlashCommandInteractionEvent event) {
        try {
            String code = event.getOption("kod").getAsString().toUpperCase();
            String discordId = event.getUser().getId();
            
            // Kodu doğrula ve eşleme yap
            boolean success = plugin.linkAccount(code, discordId);
            
            if (success) {
                // Başarılı eşleme
                EmbedBuilder embed = new EmbedBuilder()
                    .setColor(0x57F287) // Yeşil
                    .setTitle("✅ Hesap Eşleme Başarılı!")
                    .setDescription("Minecraft hesabınız Discord ile başarıyla eşleştirildi!")
                    .addField("Discord", event.getUser().getAsMention(), true)
                    .addField("Durum", "Eşleştirildi", true)
                    .setTimestamp(Instant.now());
                
                event.replyEmbeds(embed.build()).setEphemeral(true).queue();
                
                // Eşlendi rolünü ver
                Guild guild = getGuild();
                if (guild != null && !eslendiRolId.isEmpty()) {
                    Member member = guild.getMemberById(discordId);
                    Role eslendiRole = guild.getRoleById(eslendiRolId);
                    
                    if (member != null && eslendiRole != null) {
                        guild.addRoleToMember(member, eslendiRole).queue(
                            success2 -> plugin.getLogger().info("Eşlendi rolü verildi: " + event.getUser().getName()),
                            error -> plugin.getLogger().warning("Eşlendi rolü verilemedi: " + error.getMessage())
                        );
                    }
                }
                
                // Link panelini güncelle
                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> updateLinkPanel(), 20L);
                
            } else {
                // Başarısız eşleme
                EmbedBuilder embed = new EmbedBuilder()
                    .setColor(0xED4245) // Kırmızı
                    .setTitle("❌ Hesap Eşleme Başarısız!")
                    .setDescription("Geçersiz veya süresi dolmuş kod!")
                    .addField("Çözüm", "Minecraft'ta `/discord eşle` komutunu kullanarak yeni bir kod alın.", false)
                    .setTimestamp(Instant.now());
                
                event.replyEmbeds(embed.build()).setEphemeral(true).queue();
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Hesap eşleme hatası: " + e.getMessage());
            e.printStackTrace();
            
            EmbedBuilder embed = new EmbedBuilder()
                .setColor(0xED4245)
                .setTitle("❌ Sistem Hatası!")
                .setDescription("Hesap eşleme sırasında bir hata oluştu. Lütfen daha sonra tekrar deneyin.")
                .setTimestamp(Instant.now());
            
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
        }
    }
    
    /**
     * Discord yönetim komutlarını işler
     */
    private void handleDiscordCommand(SlashCommandInteractionEvent event) {
        // Discord yönetim komutları için
        // Şimdilik basit bir yanıt
        EmbedBuilder embed = new EmbedBuilder()
            .setColor(0x5865F2)
            .setTitle("🔧 Discord Yönetim")
            .setDescription("Discord yönetim komutları yakında aktif olacak!")
            .setTimestamp(Instant.now());
        
        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }
    
    /**
     * Link paneli güncelleme görevini başlatır
     */
    public void startLinkPanelUpdateTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (jda != null) {
                updateLinkPanel();
            }
        }, 100L, 1200L); // 1 dakikada bir güncelle (1200 tick = 60 saniye)
    }

    /**
     * Discord Event Handler - Slash Commands ve Hesap Eşleme
     */
    public class DiscordEventHandler extends ListenerAdapter {
        
        @Override
        public void onReady(ReadyEvent event) {
            plugin.getLogger().info("Discord bot başarıyla bağlandı: " + event.getJDA().getSelfUser().getName());
            
            // Slash komutlarını kaydet
            Guild guild = event.getJDA().getGuildById(guildId);
            if (guild != null) {
                guild.updateCommands().addCommands(
                    Commands.slash("hesapesle", "Minecraft hesabınızı Discord ile eşleştirin")
                        .addOption(OptionType.STRING, "kod", "Minecraft'tan aldığınız eşleştirme kodu", true),
                    Commands.slash("discord", "Discord bot yönetim komutları")
                        .addSubcommands(
                            new SubcommandData("reload", "Bot konfigürasyonunu yeniden yükle")
                        )
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                ).queue();
                
                plugin.getLogger().info("Slash komutları başarıyla kaydedildi!");
            }
        }
        
        @Override
        public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
            String commandName = event.getName();
            
            if (commandName.equals("hesapesle")) {
                handleAccountLinkCommand(event);
            } else if (commandName.equals("discord")) {
                handleDiscordCommand(event);
            }
        }
        
        /**
         * Hesap eşleme komutunu işler
         */
        private void handleAccountLinkCommand(SlashCommandInteractionEvent event) {
            String kod = event.getOption("kod").getAsString();
            String discordId = event.getUser().getId();
            
            try {
                // Kodu doğrula ve hesabı eşle
                UUID playerUUID = plugin.validateLinkCode(kod);
                
                if (playerUUID != null) {
                    // Hesabı eşle
                    plugin.getStorage().linkAccounts(playerUUID, discordId);
                }
                
                if (playerUUID != null) {
                    // Başarılı eşleme
                    Player player = Bukkit.getPlayer(playerUUID);
                    String playerName = player != null ? player.getName() : "Bilinmeyen";
                    
                    // Eşlendi rolünü ver
                    if (!eslendiRolId.isEmpty()) {
                        Guild guild = event.getGuild();
                        if (guild != null) {
                            Role eslendiRole = guild.getRoleById(eslendiRolId);
                            if (eslendiRole != null) {
                                guild.addRoleToMember(event.getUser(), eslendiRole).queue();
                            }
                        }
                    }
                    
                    // Başarı mesajı
                    EmbedBuilder embed = new EmbedBuilder()
                        .setColor(0x57F287) // Yeşil
                        .setTitle("✅ Hesap Eşleme Başarılı!")
                        .setDescription("Minecraft hesabınız başarıyla Discord ile eşleştirildi!")
                        .addField("Oyuncu", playerName, true)
                        .addField("Discord", event.getUser().getAsMention(), true)
                        .setTimestamp(Instant.now());
                    
                    event.replyEmbeds(embed.build()).setEphemeral(true).queue();
                    
                    // Oyuncuya bildir
                    if (player != null && player.isOnline()) {
                        player.sendMessage(ChatColor.GREEN + "✅ Hesabınız Discord ile başarıyla eşleştirildi!");
                    }
                    
                    // Link kanalına bildir
                    if (!linkChannelId.isEmpty()) {
                        TextChannel linkChannel = event.getGuild().getTextChannelById(linkChannelId);
                        if (linkChannel != null) {
                            EmbedBuilder linkEmbed = new EmbedBuilder()
                                .setColor(0x57F287)
                                .setTitle("🔗 Yeni Hesap Eşleme")
                                .setDescription("Yeni bir hesap eşleme gerçekleşti!")
                                .addField("Oyuncu", playerName, true)
                                .addField("Discord", event.getUser().getAsMention(), true)
                                .setTimestamp(Instant.now());
                            
                            linkChannel.sendMessageEmbeds(linkEmbed.build()).queue();
                        }
                    }
                    
                    plugin.getLogger().info("Hesap eşleme başarılı: " + playerName + " <-> " + event.getUser().getName());
                    
                    // Link panelini güncelle
                    Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> updateLinkPanel(), 20L);
                    
                } else {
                    // Başarısız eşleme
                    EmbedBuilder embed = new EmbedBuilder()
                        .setColor(0xED4245) // Kırmızı
                        .setTitle("❌ Hesap Eşleme Başarısız!")
                        .setDescription("Geçersiz veya süresi dolmuş kod!")
                        .addField("Çözüm", "Minecraft'ta `/discord eşle` komutunu kullanarak yeni bir kod alın.", false)
                        .setTimestamp(Instant.now());
                    
                    event.replyEmbeds(embed.build()).setEphemeral(true).queue();
                }
                
            } catch (Exception e) {
                plugin.getLogger().severe("Hesap eşleme hatası: " + e.getMessage());
                e.printStackTrace();
                
                EmbedBuilder embed = new EmbedBuilder()
                    .setColor(0xED4245)
                    .setTitle("❌ Sistem Hatası!")
                    .setDescription("Hesap eşleme sırasında bir hata oluştu. Lütfen daha sonra tekrar deneyin.")
                    .setTimestamp(Instant.now());
                
                event.replyEmbeds(embed.build()).setEphemeral(true).queue();
            }
        }
        
        /**
         * Discord yönetim komutlarını işler
         */
        private void handleDiscordCommand(SlashCommandInteractionEvent event) {
            String subcommand = event.getSubcommandName();
            
            if (subcommand.equals("reload")) {
                try {
                    // Config'i yeniden yükle
                    plugin.reloadConfig();
                    
                    // Bot ayarlarını yeniden yükle
                    FileConfiguration config = plugin.getConfig();
                    guildId = config.getString("discord.guild-id");
                    linkChannelId = config.getString("discord.link-channel-id");
                    eslendiRolId = config.getString("discord.roller.EslendiRolID");
                    
                    EmbedBuilder embed = new EmbedBuilder()
                        .setColor(0x57F287)
                        .setTitle("✅ Yeniden Yükleme Başarılı!")
                        .setDescription("Discord bot konfigürasyonu başarıyla yeniden yüklendi!")
                        .setTimestamp(Instant.now());
                    
                    event.replyEmbeds(embed.build()).setEphemeral(true).queue();
                    
                } catch (Exception e) {
                    plugin.getLogger().severe("Discord reload hatası: " + e.getMessage());
                    
                    EmbedBuilder embed = new EmbedBuilder()
                        .setColor(0xED4245)
                        .setTitle("❌ Yeniden Yükleme Hatası!")
                        .setDescription("Konfigürasyon yeniden yüklenirken bir hata oluştu!")
                        .setTimestamp(Instant.now());
                    
                    event.replyEmbeds(embed.build()).setEphemeral(true).queue();
                }
            }
        }
    }
    
    // ButtonConfig sınıfı
    public static class ButtonConfig {
        private final String emoji;
        private final String label;
        private final ButtonStyle color;
        
        public ButtonConfig(String emoji, String label, ButtonStyle color) {
            this.emoji = emoji;
            this.label = label;
            this.color = color;
        }
        
        public String getEmoji() { return emoji; }
        public String getLabel() { return label; }
        public ButtonStyle getColor() { return color; }
    }
    
    /**
     * Bakım durumu mesajı gönderir
     */
    public void sendMaintenanceStatusMessage(boolean enabled) {
        String statusChannelId = plugin.getConfig().getString("discord.status-channel-id", "");
        if (statusChannelId.isEmpty()) {
            // Status kanalı yoksa link kanalını kullan
            statusChannelId = plugin.getConfig().getString("discord.link-channel-id", "");
        }
        
        if (statusChannelId.isEmpty()) return;
        
        TextChannel channel = getTextChannel(statusChannelId);
        if (channel != null) {
            String serverName = plugin.getConfig().getString("discord.status-panel.server-name", "Minecraft Server");
            String serverIp = plugin.getConfig().getString("discord.status-panel.server-ip", "play.example.com");
            String discordLink = plugin.getConfig().getString("maintenance.discord-link", "https://discord.gg/example");
            String websiteLink = plugin.getConfig().getString("maintenance.website-link", "https://example.com");
            
            EmbedBuilder embed = new EmbedBuilder();
            
            if (enabled) {
                // Bakım modu açıldı
                List<String> whitelist = plugin.getConfig().getStringList("maintenance.whitelist");
                String estimatedTime = plugin.getConfig().getString("maintenance.estimated-time", "Bilinmiyor");
                
                embed.setColor(Color.ORANGE)
                    .setTitle("🔧 Bakım Modu Aktifleştirildi!")
                    .setDescription(
                        "**" + serverName + "** sunucusu bakım moduna alındı.\\n\\n" +
                        "🌐 **Sunucu IP:** `" + serverIp + "`\\n" +
                        "⚡ **Durum:** Bakım Modu\\n" +
                        "👥 **Whitelist Oyuncu:** `" + whitelist.size() + "` kişi\\n" +
                        "⏰ **Tahmini Süre:** " + estimatedTime + "\\n\\n" +
                        "🔧 **Bakım Sebebi:**\\n" +
                        "• Sunucu güncellemeleri\\n" +
                        "• Performans iyileştirmeleri\\n" +
                        "• Yeni özellik eklemeleri\\n\\n" +
                        "📢 **Güncellemeler için takipte kalın!**"
                    )
                    .setThumbnail("https://cdn.discordapp.com/emojis/853245077816377344.png")
                    .addField("📊 Bakım Bilgileri", 
                        "```" +
                        "Whitelist Oyuncu: " + whitelist.size() + "\\n" +
                        "Tahmini Süre: " + estimatedTime + "\\n" +
                        "Bakım Türü: Planlı Bakım" +
                        "```", false)
                    .addField("🔗 Bağlantılar", 
                        "💬 [Discord](" + discordLink + ")\\n" +
                        "🌐 [Website](" + websiteLink + ")", true)
                    .addField("💡 Bilgi", 
                        "Bakım sırasında sadece whitelist'teki oyuncular sunucuya girebilir.", false);
                        
            } else {
                // Bakım modu kapatıldı
                int totalLinkedAccounts = plugin.getLinkedAccountsCount();
                
                embed.setColor(Color.GREEN)
                    .setTitle("✅ Bakım Modu Tamamlandı!")
                    .setDescription(
                        "**" + serverName + "** sunucusu tekrar herkese açıldı!\\n\\n" +
                        "🌐 **Sunucu IP:** `" + serverIp + "`\\n" +
                        "⚡ **Durum:** Çevrimiçi ve Hazır\\n" +
                        "🔗 **Discord Entegrasyonu:** Aktif\\n" +
                        "👥 **Eşleştirilmiş Hesaplar:** `" + totalLinkedAccounts + "` hesap\\n\\n" +
                        "🎉 **Yenilikler:**\\n" +
                        "• Performans iyileştirmeleri\\n" +
                        "• Hata düzeltmeleri\\n" +
                        "• Yeni özellikler\\n\\n" +
                        "🎮 **Oyuna katılın ve eğlencenin tadını çıkarın!**"
                    )
                    .setThumbnail("https://cdn.discordapp.com/emojis/852869487845515264.png")
                    .addField("🎯 Sunucu Durumu", 
                        "```" +
                        "Durum: Çevrimiçi\\n" +
                        "Oyuncu Limiti: Yok\\n" +
                        "Hesap Sistemi: Aktif" +
                        "```", false)
                    .addField("🔗 Bağlantılar", 
                        "💬 [Discord](" + discordLink + ")\\n" +
                        "🌐 [Website](" + websiteLink + ")", true)
                    .addField("💡 Hesap Eşleştirme", 
                        "Minecraft'ta `/discord eşle` komutunu kullanarak hesabınızı eşleştirin!", false);
            }
            
            embed.setTimestamp(Instant.now())
                .setFooter(
                    "Bakım Yönetim Sistemi • " + serverName,
                    enabled ? "https://cdn.discordapp.com/emojis/853245077816377344.png" : 
                             "https://cdn.discordapp.com/emojis/852869487845515264.png"
                );
            
            channel.sendMessageEmbeds(embed.build()).queue(
                success -> plugin.getLogger().info("✅ Bakım durumu mesajı gönderildi: " + (enabled ? "Açıldı" : "Kapatıldı")),
                error -> plugin.getLogger().warning("❌ Bakım durumu mesajı gönderilemedi: " + error.getMessage())
            );
        } else {
            plugin.getLogger().warning("❌ Durum kanalı bulunamadı: " + statusChannelId);
        }
    }
    
    /**
     * Güncelleme bildirimi gönderir
     */
    public void sendUpdateNotification(AutoUpdater.UpdateResult result) {
        String statusChannelId = plugin.getConfig().getString("discord.status-channel-id", "");
        if (statusChannelId.isEmpty()) {
            statusChannelId = plugin.getConfig().getString("discord.link-channel-id", "");
        }
        
        if (statusChannelId.isEmpty()) return;
        
        TextChannel channel = getTextChannel(statusChannelId);
        if (channel != null) {
            String serverName = plugin.getConfig().getString("discord.status-panel.server-name", "Minecraft Server");
            
            EmbedBuilder embed = new EmbedBuilder()
                .setColor(Color.BLUE)
                .setTitle("🆕 Yeni Plugin Güncellemesi Mevcut!")
                .setDescription(
                    "**" + serverName + "** için yeni bir plugin güncellemesi bulundu!\\n\\n" +
                    "📦 **Mevcut Versiyon:** `" + plugin.getDescription().getVersion() + "`\\n" +
                    "🆕 **Yeni Versiyon:** `" + result.getLatestVersion() + "`\\n\\n" +
                    "🔧 **Güncelleme Durumu:** Bekleniyor\\n" +
                    "⚡ **Otomatik Güncelleme:** " + (plugin.getConfig().getBoolean("updater.auto-update", false) ? "Aktif" : "Pasif")
                )
                .setThumbnail("https://cdn.discordapp.com/emojis/852869487845515264.png");
            
            if (result.getChangelog() != null && !result.getChangelog().isEmpty()) {
                String changelog = result.getChangelog();
                if (changelog.length() > 1000) {
                    changelog = changelog.substring(0, 1000) + "...";
                }
                embed.addField("📝 Değişiklikler", "```" + changelog + "```", false);
            }
            
            embed.addField("💡 Bilgi", 
                "Güncelleme otomatik olarak " + (plugin.getConfig().getBoolean("updater.auto-update", false) ? 
                "uygulanacak" : "manuel olarak uygulanmalı") + ".", false)
                .setTimestamp(Instant.now())
                .setFooter("Plugin Güncelleme Sistemi • " + serverName);
            
            channel.sendMessageEmbeds(embed.build()).queue(
                success -> plugin.getLogger().info("✅ Güncelleme bildirimi gönderildi!"),
                error -> plugin.getLogger().warning("❌ Güncelleme bildirimi gönderilemedi: " + error.getMessage())
            );
        }
    }
    
    /**
     * Güncelleme durumu mesajı gönderir
     */
    public void sendUpdateMessage(boolean isUpdating, String message) {
        String statusChannelId = plugin.getConfig().getString("discord.status-channel-id", "");
        if (statusChannelId.isEmpty()) {
            statusChannelId = plugin.getConfig().getString("discord.link-channel-id", "");
        }
        
        if (statusChannelId.isEmpty()) return;
        
        TextChannel channel = getTextChannel(statusChannelId);
        if (channel != null) {
            String serverName = plugin.getConfig().getString("discord.status-panel.server-name", "Minecraft Server");
            
            EmbedBuilder embed = new EmbedBuilder();
            
            if (isUpdating) {
                embed.setColor(Color.ORANGE)
                    .setTitle("🔄 Plugin Güncelleniyor...")
                    .setDescription(
                        "**" + serverName + "** plugin güncellemesi uygulanıyor.\\n\\n" +
                        "⚡ **Durum:** " + message + "\\n" +
                        "📦 **Mevcut Versiyon:** `" + plugin.getDescription().getVersion() + "`\\n\\n" +
                        "⏰ **Sunucu yakında yeniden başlatılacak!**\\n" +
                        "🔄 **Güncelleme tamamlandığında tekrar çevrimiçi olacağız.**"
                    )
                    .setThumbnail("https://cdn.discordapp.com/emojis/853245077816377344.png")
                    .addField("💡 Bilgi", 
                        "Güncelleme sırasında sunucuya bağlanılamayabilir. Lütfen sabırlı olun.", false);
            } else {
                embed.setColor(Color.GREEN)
                    .setTitle("✅ Plugin Güncelleme Tamamlandı!")
                    .setDescription(
                        "**" + serverName + "** plugin güncellemesi başarıyla tamamlandı!\\n\\n" +
                        "✅ **Durum:** " + message + "\\n" +
                        "🆕 **Yeni Versiyon:** `" + plugin.getDescription().getVersion() + "`\\n\\n" +
                        "🎉 **Sunucu tekrar çevrimiçi!**\\n" +
                        "🎮 **Oyuna katılabilir ve yeni özellikleri deneyebilirsiniz!**"
                    )
                    .setThumbnail("https://cdn.discordapp.com/emojis/852869487845515264.png")
                    .addField("🎯 Yenilikler", 
                        "Yeni özellikler ve iyileştirmeler aktif edildi!", false);
            }
            
            embed.setTimestamp(Instant.now())
                .setFooter("Plugin Güncelleme Sistemi • " + serverName);
            
            channel.sendMessageEmbeds(embed.build()).queue(
                success -> plugin.getLogger().info("✅ Güncelleme durumu mesajı gönderildi!"),
                error -> plugin.getLogger().warning("❌ Güncelleme durumu mesajı gönderilemedi: " + error.getMessage())
            );
        }
    }
}