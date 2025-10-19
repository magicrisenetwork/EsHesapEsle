package com.eshesapesle.suggestion;

import com.eshesapesle.EsHesapEsle;
import net.dv8tion.jda.api.EmbedBuilder;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class SuggestionAnalyzer {
    private final EsHesapEsle plugin;
    private final Map<String, Pattern> categoryPatterns;

    public SuggestionAnalyzer(EsHesapEsle plugin) {
        this.plugin = plugin;
        this.categoryPatterns = initializeCategoryPatterns();
    }

    private Map<String, Pattern> initializeCategoryPatterns() {
        Map<String, Pattern> patterns = new HashMap<>();
        
        // Ekonomi kategorisi
        patterns.put("ekonomi", Pattern.compile("(?i)(para|coin|market|shop|fiyat|ücret|satın|satış|ekonomi|ticaret|dükkân|mağaza|kredi)"));
        
        // Oyun kategorisi
        patterns.put("oyun", Pattern.compile("(?i)(pvp|savaş|düello|minigame|mini oyun|yarışma|etkinlik|event|turnuva|arena)"));
        
        // Sistem kategorisi
        patterns.put("sistem", Pattern.compile("(?i)(komut|permission|yetki|rank|rütbe|seviye|level|sistem|otomasyon|bot|plugin)"));
        
        // Topluluk kategorisi
        patterns.put("topluluk", Pattern.compile("(?i)(chat|sohbet|kurallar|yetkili|moderatör|admin|kullanıcı|oyuncu|üye|topluluk|discord)"));
        
        return patterns;
    }

    public SuggestionAnalysis analyzeSuggestion(String suggestion) {
        SuggestionAnalysis analysis = new SuggestionAnalysis();
        
        // Kategori belirleme
        analysis.category = determineCategory(suggestion);
        
        // Öneri puanlama
        analysis.score = calculateScore(suggestion);
        
        // Özet oluşturma
        analysis.summary = generateSummary(suggestion);
        
        return analysis;
    }

    private String determineCategory(String suggestion) {
        String bestCategory = "genel";
        int maxMatches = 0;

        for (Map.Entry<String, Pattern> entry : categoryPatterns.entrySet()) {
            java.util.regex.Matcher matcher = entry.getValue().matcher(suggestion);
            int matches = 0;
            while (matcher.find()) matches++;
            
            if (matches > maxMatches) {
                maxMatches = matches;
                bestCategory = entry.getKey();
            }
        }

        return bestCategory;
    }

    private int calculateScore(String suggestion) {
        int score = 50; // Başlangıç puanı
        
        // Uzunluk analizi (10-20 puan)
        int length = suggestion.length();
        if (length >= 100 && length <= 500) score += 10;
        else if (length > 500) score += 5;
        else if (length < 50) score -= 5;
        
        // Detay analizi (0-15 puan)
        if (suggestion.contains(" çünkü ") || suggestion.contains(" nedeniyle ")) score += 10;
        if (suggestion.contains(" örneğin ") || suggestion.contains(" mesela ")) score += 5;
        
        // Yapılandırma analizi (0-15 puan)
        if (containsStructuredContent(suggestion)) score += 15;
        
        // Özgünlük analizi (0-10 puan)
        if (!containsCommonPhrases(suggestion)) score += 10;
        
        return Math.min(100, Math.max(0, score));
    }

    private boolean containsStructuredContent(String text) {
        return text.contains("\n") || // Madde işaretleri veya numaralandırma
               text.matches(".*\\d+\\..*") || // Numaralandırma
               text.matches(".*[\\-\\*]\\s.*"); // Madde işaretleri
    }

    private boolean containsCommonPhrases(String text) {
        String[] commonPhrases = {"lütfen ekleyin", "eklerseniz güzel olur", "eklenmesini istiyorum"};
        for (String phrase : commonPhrases) {
            if (text.toLowerCase().contains(phrase)) return true;
        }
        return false;
    }

    private String generateSummary(String suggestion) {
        // Uzun önerileri özetle
        if (suggestion.length() <= 100) return suggestion;
        
        // Önemli cümleleri bul
        String[] sentences = suggestion.split("[.!?]+");
        if (sentences.length <= 2) return suggestion;
        
        // İlk ve son cümleyi al
        StringBuilder summary = new StringBuilder();
        summary.append(sentences[0].trim()).append(". ");
        if (sentences.length > 2) {
            summary.append("... ");
        }
        summary.append(sentences[sentences.length - 1].trim());
        
        return summary.toString();
    }

    public static class SuggestionAnalysis {
        public String category = "genel";
        public int score = 50;
        public String summary = "";
        
        public Color getScoreColor() {
            if (score >= 80) return Color.GREEN;
            if (score >= 60) return Color.YELLOW;
            return Color.ORANGE;
        }
        
        public String getScoreEmoji() {
            if (score >= 80) return "🌟";
            if (score >= 60) return "⭐";
            return "💡";
        }
        
        public String getCategoryEmoji() {
            switch (category.toLowerCase()) {
                case "ekonomi": return "💰";
                case "oyun": return "🎮";
                case "sistem": return "⚙️";
                case "topluluk": return "👥";
                default: return "📝";
            }
        }
    }
} 