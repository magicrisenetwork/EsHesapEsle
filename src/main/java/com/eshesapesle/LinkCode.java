package com.eshesapesle;

import java.util.Map;
import java.util.UUID;

public class LinkCode {
    private final UUID playerUUID;
    private final long createdAt;
    private static final long EXPIRY_TIME = 120000; // 2 dakika (milisaniye)
    
    public LinkCode(UUID playerUUID) {
        this.playerUUID = playerUUID;
        this.createdAt = System.currentTimeMillis();
    }
    
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt > EXPIRY_TIME;
    }
    
    public long getRemainingTime() {
        long remaining = EXPIRY_TIME - (System.currentTimeMillis() - createdAt);
        return Math.max(0, remaining / 1000); // Saniye cinsinden kalan süre
    }
    
    // Süresi dolmuş kodları temizle
    public static void cleanExpiredCodes(Map<String, LinkCode> linkCodes) {
        linkCodes.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
} 