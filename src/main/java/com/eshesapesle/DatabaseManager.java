package com.eshesapesle;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class DatabaseManager {
    private Connection connection;
    private final String url;
    private final String username;
    private final String password;
    
    public DatabaseManager(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }
    
    public void connect() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(url, username, password);
            createTables();
        }
    }
    
    private void createTables() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS linked_accounts (" +
                    "minecraft_uuid VARCHAR(36) PRIMARY KEY," +
                    "discord_id VARCHAR(20) NOT NULL," +
                    "linked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.execute();
        }
    }
    
    public void linkAccounts(UUID minecraftUUID, String discordId) throws SQLException {
        String sql = "INSERT INTO linked_accounts (minecraft_uuid, discord_id) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE discord_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, minecraftUUID.toString());
            stmt.setString(2, discordId);
            stmt.setString(3, discordId);
            stmt.executeUpdate();
        }
    }
    
    public String getDiscordId(UUID minecraftUUID) throws SQLException {
        String sql = "SELECT discord_id FROM linked_accounts WHERE minecraft_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, minecraftUUID.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("discord_id");
                }
            }
        }
        return null;
    }
    
    public UUID getMinecraftUUID(String discordId) throws SQLException {
        String sql = "SELECT minecraft_uuid FROM linked_accounts WHERE discord_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, discordId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("minecraft_uuid"));
                }
            }
        }
        return null;
    }
    
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
} 