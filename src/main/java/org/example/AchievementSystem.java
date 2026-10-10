package org.example;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Achievement & Badge system with visual notifications.
 * Thread-safe, non-blocking notifications on EDT.
 */
public class AchievementSystem {
    public static final Map<String, Achievement> ACHIEVEMENTS = new HashMap<>();

    private final Set<String> unlockedAchievements = Collections.synchronizedSet(new HashSet<>());
    private final List<AchievementListener> listeners = Collections.synchronizedList(new ArrayList<>());

    static {
        // Initialize achievement definitions
        ACHIEVEMENTS.put("SPEED_DEMON", new Achievement(
                "SPEED_DEMON", "⚡ Speed Demon", "Answer 5 questions in under 2 minutes",
                new Color(255, 193, 7), "⚡"));

        ACHIEVEMENTS.put("PERFECT_STREAK", new Achievement(
                "PERFECT_STREAK", "🔥 Perfect Streak", "Get 5 correct answers in a row",
                new Color(239, 68, 68), "🔥"));

        ACHIEVEMENTS.put("FLAWLESS", new Achievement(
                "FLAWLESS", "💎 Flawless", "Achieve 100% accuracy on 10 questions",
                new Color(168, 85, 247), "💎"));

        ACHIEVEMENTS.put("MASTER_LEARNER", new Achievement(
                "MASTER_LEARNER", "🧠 Master Learner", "Achieve 80%+ accuracy on 30 questions",
                new Color(34, 197, 94), "🧠"));

        ACHIEVEMENTS.put("COMEBACK_KID", new Achievement(
                "COMEBACK_KID", "🏆 Comeback Kid", "Miss 5, then get 5 correct",
                new Color(59, 130, 246), "🏆"));

        ACHIEVEMENTS.put("MARATHON", new Achievement(
                "MARATHON", "🏃 Marathon", "Answer 50 questions in a session",
                new Color(99, 102, 241), "🏃"));

        ACHIEVEMENTS.put("VERSATILE", new Achievement(
                "VERSATILE", "🌍 Versatile", "Master 5 different topics",
                new Color(236, 72, 153), "🌍"));

        ACHIEVEMENTS.put("NIGHT_OWL", new Achievement(
                "NIGHT_OWL", "🌙 Night Owl", "Study after 10 PM",
                new Color(79, 70, 229), "🌙"));
    }

    public void addListener(AchievementListener listener) {
        listeners.add(listener);
    }

    public void removeListener(AchievementListener listener) {
        listeners.remove(listener);
    }

    /**
     * Unlock achievement with automatic notification.
     * Thread-safe, posts notification on EDT.
     */
    public void unlockAchievement(String achievementId) {
        if (unlockedAchievements.contains(achievementId)) {
            return; // Already unlocked
        }

        Achievement achievement = ACHIEVEMENTS.get(achievementId);
        if (achievement == null) {
            System.err.println("Unknown achievement: " + achievementId);
            return;
        }

        unlockedAchievements.add(achievementId);

        // Notify listeners on EDT to avoid blocking
        SwingUtilities.invokeLater(() -> {
            for (AchievementListener listener : listeners) {
                try {
                    listener.onAchievementUnlocked(achievement);
                } catch (Exception e) {
                    System.err.println("Achievement listener error: " + e.getMessage());
                }
            }
        });
    }

    public Set<String> getUnlockedAchievements() {
        return new HashSet<>(unlockedAchievements);
    }

    public int getTotalUnlocked() {
        return unlockedAchievements.size();
    }

    public int getTotalAchievements() {
        return ACHIEVEMENTS.size();
    }

    public double getCompletionPercentage() {
        int total = getTotalAchievements();
        return total == 0 ? 0 : (double) getTotalUnlocked() / total * 100;
    }

    // ==================== DATA CLASSES ====================

    public static class Achievement implements Serializable {
        public String id;
        public String name;
        public String description;
        public Color color;
        public String icon;

        public Achievement(String id, String name, String description, Color color, String icon) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.color = color;
            this.icon = icon;
        }
    }

    @FunctionalInterface
    public interface AchievementListener {
        void onAchievementUnlocked(Achievement achievement);
    }
}
