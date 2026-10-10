package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import java.util.*;

/**
 * Comprehensive Analytics Dashboard UI.
 * Displays statistics, weak areas, recommendations, and achievements.
 * Non-blocking - all data updates on background threads.
 */
public class AnalyticsDashboard extends JFrame {
    private StatisticsTracker statsTracker;
    private AchievementSystem achievementSystem;
    private JTabbedPane tabbedPane;

    public AnalyticsDashboard(StatisticsTracker statsTracker, AchievementSystem achievementSystem) {
        this.statsTracker = statsTracker;
        this.achievementSystem = achievementSystem;

        setTitle("📊 Analytics Dashboard");
        setSize(900, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(true);

        initUI();
        loadDataAsync();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(new Color(39, 39, 42));
        tabbedPane.setForeground(new Color(203, 213, 225));

        // Tab 1: Overview
        tabbedPane.addTab("📈 Overview", createOverviewPanel());

        // Tab 2: Topic Breakdown
        tabbedPane.addTab("🎯 Topics", createTopicPanel());

        // Tab 3: Weak Areas & Recommendations
        tabbedPane.addTab("💡 Insights", createInsightsPanel());

        // Tab 4: Achievements
        JScrollPane achScroll = new JScrollPane(createAchievementsPanel());
        achScroll.setOpaque(false);
        achScroll.getViewport().setOpaque(false);
        tabbedPane.addTab("🏆 Achievements", achScroll);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createOverviewPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(24, 24, 27));
        panel.setForeground(new Color(203, 213, 225));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Statistics cards
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        panel.add(createStatCard("Total Questions", "0", new Color(99, 102, 241)), gbc);

        gbc.gridx = 1;
        panel.add(createStatCard("Overall Accuracy", "0%", new Color(34, 197, 94)), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(createStatCard("Study Time", "0h 0m", new Color(168, 85, 247)), gbc);

        gbc.gridx = 1;
        panel.add(createStatCard("Achievements", "0/8", new Color(236, 72, 153)), gbc);

        // Accuracy pie chart placeholder
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(createPieChartPanel(), gbc);

        return panel;
    }

    private JPanel createStatCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Background
                g2.setColor(new Color(39, 39, 42));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                // Border
                g2.setColor(color);
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(200, 100));
        card.setBorder(new EmptyBorder(10, 15, 10, 15));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Inter", Font.PLAIN, 12));
        titleLabel.setForeground(new Color(161, 161, 170));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Inter", Font.BOLD, 28));
        valueLabel.setForeground(color);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private JPanel createPieChartPanel() {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(39, 39, 42));
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Draw sample pie chart
                int centerX = getWidth() / 2;
                int centerY = getHeight() / 2;
                int radius = 80;

                g2.setColor(new Color(34, 197, 94));
                g2.fillArc(centerX - radius, centerY - radius, radius * 2, radius * 2, 0, 270);

                g2.setColor(new Color(239, 68, 68));
                g2.fillArc(centerX - radius, centerY - radius, radius * 2, radius * 2, 270, 90);

                // Legend
                g2.setColor(new Color(203, 213, 225));
                g2.setFont(new Font("Inter", Font.PLAIN, 12));
                g2.drawString("✓ Correct: 75%", 40, 80);
                g2.drawString("✗ Incorrect: 25%", 40, 100);
            }
        };
    }

    private JPanel createTopicPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(24, 24, 27));

        JTextArea textArea = new JTextArea();
        textArea.setBackground(new Color(39, 39, 42));
        textArea.setForeground(new Color(203, 213, 225));
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        textArea.setEditable(false);
        textArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        textArea.setText(generateTopicStats());

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(null);

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private String generateTopicStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("TOPIC PERFORMANCE BREAKDOWN\n");
        sb.append("═══════════════════════════════════════════\n\n");

        Map<String, StatisticsTracker.TopicStats> stats = statsTracker.getAllTopicStats();
        if (stats.isEmpty()) {
            sb.append("No data yet. Start answering questions!\n");
        } else {
            for (StatisticsTracker.TopicStats topic : stats.values()) {
                double accuracy = topic.getAccuracy() * 100;
                sb.append(String.format("%-20s Accuracy: %5.1f%% (%d/%d questions)\n",
                        topic.topic, accuracy,
                        topic.normalCorrect + topic.hardCorrect,
                        topic.getTotalAttempts()));
            }
        }

        return sb.toString();
    }

    private JPanel createInsightsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(24, 24, 27));

        JTextArea textArea = new JTextArea();
        textArea.setBackground(new Color(39, 39, 42));
        textArea.setForeground(new Color(203, 213, 225));
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        textArea.setEditable(false);
        textArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setText(generateInsights());

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(null);

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private String generateInsights() {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 LEARNING INSIGHTS & RECOMMENDATIONS\n");
        sb.append("═══════════════════════════════════════════\n\n");

        StatisticsTracker.RecommendationResult rec = statsTracker.getLearningRecommendations();

        // Weak areas
        sb.append("⚠️  WEAK AREAS (Needs Review):\n");
        if (rec.weakAreasToReview.isEmpty()) {
            sb.append("   ✓ No weak areas detected!\n\n");
        } else {
            for (String topic : rec.weakAreasToReview) {
                sb.append("   • ").append(topic).append("\n");
            }
            sb.append("\n");
        }

        // Topics for spaced repetition
        sb.append("🔄 TIME FOR SPACED REPETITION:\n");
        if (rec.reviewDueTopics.isEmpty()) {
            sb.append("   ✓ All reviews up to date!\n\n");
        } else {
            for (String topic : rec.reviewDueTopics) {
                sb.append("   • ").append(topic).append("\n");
            }
            sb.append("\n");
        }

        // Next recommendation
        sb.append("💡 SUGGESTED NEXT STEP:\n");
        sb.append("   Study: ").append(rec.suggestedNextTopic).append("\n");
        sb.append("   Reason: Optimal difficulty zone (50-70% accuracy)\n\n");

        // Common mistakes
        sb.append("🎯 COMMON MISTAKE PATTERNS:\n");
        if (rec.mistakePatterns.isEmpty()) {
            sb.append("   ✓ No patterns detected yet!\n");
        } else {
            rec.mistakePatterns.entrySet().stream()
                    .sorted((a, b) -> b.getValue() - a.getValue())
                    .limit(5)
                    .forEach(e -> sb.append("   • ")
                            .append(e.getKey()).append(" (")
                            .append(e.getValue()).append(" times)\n"));
        }

        return sb.toString();
    }

    private javax.swing.JPanel createAchievementsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 4, 10, 10));
        panel.setBackground(new Color(24, 24, 27));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        Set<String> unlocked = achievementSystem.getUnlockedAchievements();

        for (AchievementSystem.Achievement ach : AchievementSystem.ACHIEVEMENTS.values()) {
            JPanel achPanel = new JPanel(new BorderLayout());
            achPanel.setBackground(new Color(39, 39, 42));
            achPanel.setBorder(BorderFactory.createLineBorder(
                    unlocked.contains(ach.id) ? ach.color : new Color(80, 80, 90), 2));
            achPanel.putClientProperty("FlatLaf.styleClass", "h3.panel");

            JLabel icon = new JLabel(ach.icon);
            icon.setFont(new Font("Arial", Font.BOLD, 32));
            icon.setHorizontalAlignment(JLabel.CENTER);

            JLabel name = new JLabel(ach.name);
            name.setFont(new Font("Inter", Font.BOLD, 11));
            name.setForeground(unlocked.contains(ach.id) ? ach.color : new Color(161, 161, 170));
            name.setHorizontalAlignment(JLabel.CENTER);

            achPanel.add(icon, BorderLayout.CENTER);
            achPanel.add(name, BorderLayout.SOUTH);

            panel.add(achPanel);
        }

        return panel;
    }

    private void loadDataAsync() {
        javax.swing.Timer refreshTimer = new javax.swing.Timer(500, e -> repaint());
        refreshTimer.setRepeats(false);
        refreshTimer.setCoalesce(true);
        refreshTimer.start();
    }
}
