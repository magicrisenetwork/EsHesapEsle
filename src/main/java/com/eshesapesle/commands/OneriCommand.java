package com.eshesapesle.commands;

import com.eshesapesle.EsHesapEsle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.TabCompleter;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class OneriCommand implements CommandExecutor, TabCompleter {
    private final EsHesapEsle plugin;

    public OneriCommand(EsHesapEsle plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Bu komut sadece oyuncular tarafından kullanılabilir!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "gönder":
            case "gonder":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Kullanım: /öneri gönder <metin>");
                    return true;
                }
                handleSendSuggestion(player, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                break;

            case "liste":
            case "list":
                player.sendMessage(ChatColor.YELLOW + "Son önerilerinizi Discord sunucumuzdan görüntüleyebilirsiniz!");
                break;

            default:
                sendHelpMessage(player);
                break;
        }

        return true;
    }

    private void handleSendSuggestion(Player player, String suggestion) {
        // Öneri uzunluğunu kontrol et
        if (suggestion.length() < 10) {
            player.sendMessage(ChatColor.RED + "Öneriniz çok kısa! En az 10 karakter uzunluğunda olmalı.");
            return;
        }
        
        if (suggestion.length() > 1000) {
            player.sendMessage(ChatColor.RED + "Öneriniz çok uzun! En fazla 1000 karakter olabilir.");
            return;
        }

        // Discord ID'sini kontrol et
        UUID playerUUID = player.getUniqueId();
        String discordId = plugin.getStorage().getDiscordId(playerUUID);

        if (discordId == null) {
            player.sendMessage(ChatColor.RED + "Öneri gönderebilmek için önce Discord hesabınızı eşleştirmelisiniz!");
            player.sendMessage(ChatColor.YELLOW + "Discord sunucumuza katılın ve /discord eşle komutunu kullanın.");
            return;
        }

        // Öneriyi Discord'a gönder
        plugin.getDiscordBot().sendSuggestion(player, suggestion);
        
        player.sendMessage(ChatColor.GREEN + "✔ Öneriniz başarıyla gönderildi!");
        player.sendMessage(ChatColor.GRAY + "Discord'dan önerinizin durumunu takip edebilirsiniz.");
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.YELLOW + "=== Öneri Komutları ===");
        player.sendMessage(ChatColor.GOLD + "/öneri gönder <metin> " + ChatColor.GRAY + "- Yeni bir öneri gönderir");
        player.sendMessage(ChatColor.GOLD + "/öneri liste " + ChatColor.GRAY + "- Son önerileri görüntüler");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("gönder");
            completions.add("liste");
            return completions;
        }
        
        return new ArrayList<>();
    }
} 