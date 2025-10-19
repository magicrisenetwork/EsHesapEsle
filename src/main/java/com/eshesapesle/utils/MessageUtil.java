package com.eshesapesle.utils;

import com.eshesapesle.EsHesapEsle;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class MessageUtil {
    private static EsHesapEsle plugin;
    private static FileConfiguration langConfig;

    public static void init(EsHesapEsle instance) {
        plugin = instance;
        langConfig = plugin.getDiscordBot().getLangConfig();
    }

    public static void reloadConfig() {
        langConfig = plugin.getDiscordBot().getLangConfig();
    }

    public static String getPrefix() {
        return color(langConfig.getString("minecraft.prefix", "&8[&bEsHesapEsle&8]"));
    }

    public static String getMessage(String path) {
        String message = langConfig.getString("minecraft." + path);
        if (message == null) {
            plugin.getLogger().warning("Message not found: minecraft." + path);
            return "Message not found: " + path;
        }
        return color(getPrefix() + " " + message);
    }

    public static String getMessageNoPrefix(String path) {
        String message = langConfig.getString("minecraft." + path);
        if (message == null) {
            plugin.getLogger().warning("Message not found: minecraft." + path);
            return "Message not found: " + path;
        }
        return color(message);
    }

    public static String formatMessage(String path, Object... args) {
        String message = getMessage(path);
        for (int i = 0; i < args.length; i += 2) {
            message = message.replace("{" + args[i] + "}", String.valueOf(args[i + 1]));
        }
        return message;
    }

    public static void sendMessage(Player player, String path) {
        player.sendMessage(getMessage(path));
    }

    public static void sendMessage(Player player, String path, Object... args) {
        player.sendMessage(formatMessage(path, args));
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
} 