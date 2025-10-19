package com.eshesapesle.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class YAMLStorage implements Storage {
    private final Plugin plugin;
    private FileConfiguration config;
    private final File configFile;
    
    public YAMLStorage(Plugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "linked_accounts.yml");
    }
    
    @Override
    public void connect() {
        try {
            if (!configFile.exists()) {
                plugin.getDataFolder().mkdirs();
                configFile.createNewFile();
            }
            config = YamlConfiguration.loadConfiguration(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Hesap verileri yüklenirken hata: " + e.getMessage());
        }
    }
    
    @Override
    public void disconnect() {
        try {
            if (config != null) {
                config.save(configFile);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Hesap verileri kaydedilirken hata: " + e.getMessage());
        }
    }
    
    @Override
    public void linkAccounts(UUID playerUUID, String discordId) {
        config.set(playerUUID.toString() + ".discord", discordId);
        config.set(playerUUID.toString() + ".timestamp", System.currentTimeMillis());
        saveConfig();
    }
    
    @Override
    public String getDiscordId(UUID playerUUID) {
        return config.getString(playerUUID.toString() + ".discord");
    }
    
    @Override
    public UUID getMinecraftUUID(String discordId) {
        if (config.getKeys(false).isEmpty()) return null;
        
        for (String key : config.getKeys(false)) {
            if (discordId.equals(config.getString(key + ".discord"))) {
                try {
                    return UUID.fromString(key);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Geçersiz UUID formatı: " + key);
                }
            }
        }
        return null;
    }
    
    @Override
    public boolean isLinked(UUID playerUUID) {
        return getDiscordId(playerUUID) != null;
    }
    
    @Override
    public boolean isLinked(String discordId) {
        return getMinecraftUUID(discordId) != null;
    }
    
    @Override
    public void unlinkAccounts(UUID playerUUID) {
        config.set(playerUUID.toString(), null);
        saveConfig();
    }
    
    @Override
    public int getLinkedAccountsCount() {
        return config.getKeys(false).size();
    }
    
    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Hesap verileri kaydedilirken hata: " + e.getMessage());
        }
    }
} 