package com.eshesapesle.storage;

import java.util.UUID;

public interface Storage {
    void connect();
    void disconnect();
    void linkAccounts(UUID playerUUID, String discordId);
    String getDiscordId(UUID playerUUID);
    UUID getMinecraftUUID(String discordId);
    boolean isLinked(UUID playerUUID);
    boolean isLinked(String discordId);
    void unlinkAccounts(UUID playerUUID);
    int getLinkedAccountsCount();
} 