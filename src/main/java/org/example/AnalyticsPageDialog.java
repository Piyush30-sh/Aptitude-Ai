package org.example;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Analytics page with live metrics, actionable insights, and interactive mini
 * charts.
 */
public class AnalyticsPageDialog extends JPanel {
    private static final Color BG = new Color(18, 22, 29);
    private static final Color SURFACE = new Color(30, 36, 47);
    private static final Color BORDER = new Color(70, 80, 99);
    private static final Color TEXT = new Color(226, 232, 240);
    private static final Color TEXT_DIM = new Color(148, 163, 184);

    private final AptitudeGui parentGui;
    private JTabbedPane tabbedPane;
    private JLabel totalChip;
    private JLabel accuracyChip;
    private JLabel streakChip;

    public AnalyticsPageDialog(AptitudeGui parent) {
        this.parentGui = parent;
        setupPanel();
        refreshData();
    }

    public void refreshData() {
        StatisticsTracker tracker = getTracker();
        if (tracker == null) {
            return;
        }

        totalChip.setText(tracker.getTotalAttempted() + " total attempts");
        accuracyChip.setText(String.format("%.1f%% overall", tracker.getOverallAccuracy()));
        streakChip.setText("best streak " + tracker.getBestStreak());

        rebuildTabs();
        revalidate();
        repaint();
    }

    private void setupPanel() {
        setLayout(new BorderLayout());
        setBackground(BG);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(18, 22, 10, 22));

        JLabel title = new JLabel("Analytics Hub");
        title.setFont(new Font("Inter", Font.BOLD, 24));
        title.setForeground(TEXT);
        header.add(title, BorderLayout.WEST);

        JLabel subtitle = new JLabel("Live performance insights and prioritized improvement plan");
        subtitle.setFont(new Font("Inter", Font.PLAIN, 12));
        subtitle.setForeground(TEXT_DIM);
        header.add(subtitle, BorderLayout.SOUTH);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        totalChip = createInfoChip("0 total", new Color(54, 75, 97));
        accuracyChip = createInfoChip("0% overall", new Color(62, 86, 73));
        streakChip = createInfoChip("best streak 0", new Color(96, 76, 59));

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setFocusPainted(false);
        refreshBtn.setBorderPainted(false);
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.setFont(new Font("Inter", Font.BOLD, 11));
        refreshBtn.setForeground(TEXT);
        refreshBtn.setBackground(new Color(63, 83, 110));
        refreshBtn.addActionListener(e -> {
            refreshData();
            if (parentGui != null) {
                parentGui.showNotification("Analytics refreshed");
            }
        });

        right.add(totalChip);
        right.add(accuracyChip);
        right.add(streakChip);
        right.add(refreshBtn);
        header.add(right, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(BG);
        tabbedPane.setForeground(TEXT);
        tabbedPane.setFont(new Font("Inter", Font.BOLD, 13));
        tabbedPane.setBorder(new EmptyBorder(0, 14, 14, 14));
        add(tabbedPane, BorderLayout.CENTER);
    }

    private void rebuildTabs() {
        tabbedPane.removeAll();

        tabbedPane.addTab("Overview", createScrollableTab(createOverviewPanel()));
        tabbedPane.addTab("Weak Areas", createScrollableTab(createWeakAreasPanel()));
        tabbedPane.addTab("Topic Breakdown", createScrollableTab(createTopicBreakdownPanel()));
        tabbedPane.addTab("Recommendations", createScrollableTab(createRecommendationsPanel(), 700));
    }

    private JPanel createOverviewPanel() {
        StatisticsTracker tracker = getTracker();

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.weightx = 1.0;

        double overallAccuracy = tracker.getOverallAccuracy();
        int totalAttempted = tracker.getTotalAttempted();
        int totalCorrect = tracker.getTotalCorrect();
        int sessionAttempted = parentGui != null ? parentGui.getSessionTotalAttempted() : 0;
        int currentStreak = parentGui != null ? parentGui.getCurrentStreakCount() : 0;
        int bestStreak = tracker.getBestStreak();
        double avgSeconds = tracker.getAverageTimeSeconds();
        String totalTime = formatDuration(tracker.getTotalStudyTimeMillis());

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(createMetricCard(
                "Overall Accuracy",
                String.format("%.1f%%", overallAccuracy),
                totalCorrect + " correct / " + totalAttempted + " attempts",
                new Color(34, 197, 94)), gbc);

        gbc.gridx = 1;
        panel.add(createMetricCard(
                "Session Throughput",
                String.valueOf(sessionAttempted),
                "attempts in current session",
                new Color(59, 130, 246)), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(createMetricCard(
                "Current Streak",
                String.valueOf(currentStreak),
                "best streak " + bestStreak,
                new Color(245, 158, 11)), gbc);

        gbc.gridx = 1;
        panel.add(createMetricCard(
                "Average Response",
                String.format("%.1fs", avgSeconds),
                "total study time " + totalTime,
                new Color(168, 85, 247)), gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        panel.add(createMiniChartCard(
                "Accuracy Trend",
                "Each point is an attempt group. Group 1 is oldest, last group is latest.",
                toTrendPoints(tracker.getRecentAccuracyTrend(10)),
                true,
                false,
                new Color(96, 165, 250)), gbc);

        gbc.gridy = 3;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
    }

    private JPanel createWeakAreasPanel() {
        StatisticsTracker tracker = getTracker();
        Map<String, Double> weakMap = tracker.getWeakAreas();

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.weightx = 1.0;

        panel.add(createActionCard(createSectionIntro(
                "Targeted Improvement",
                "Focus first on lower-accuracy topics to maximize score growth.")), gbc);

        gbc.gridy++;
        panel.add(createMiniChartCard(
                "Weak-Area Heat",
                "Lower bars indicate stronger topics. Hover each bar for exact accuracy.",
                toWeakAreaPoints(weakMap),
                false,
                true,
                new Color(245, 158, 11)), gbc);

        gbc.gridy++;
        if (weakMap.isEmpty()) {
            panel.add(createActionCard(createSectionIntro(
                    "No Critical Weak Areas",
                    "Current data does not show any topics under the weak-area threshold.")), gbc);
        } else {
            List<Map.Entry<String, Double>> items = new ArrayList<>(weakMap.entrySet());
            items.sort(Comparator.comparingDouble(Map.Entry::getValue));

            for (Map.Entry<String, Double> entry : items) {
                int percent = (int) Math.round(entry.getValue() * 100);
                panel.add(createWeakAreaCard(entry.getKey(), percent), gbc);
                gbc.gridy++;
            }
        }

        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(Box.createVerticalGlue(), gbc);
        return panel;
    }

    private JPanel createTopicBreakdownPanel() {
        StatisticsTracker tracker = getTracker();
        Map<String, StatisticsTracker.TopicStats> statsMap = tracker.getAllTopicStats();

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.weightx = 1.0;

        panel.add(createActionCard(createLegendPanel()), gbc);
        gbc.gridy++;

        panel.add(createMiniChartCard(
                "Topic Accuracy Snapshot",
                "Top attempted topics. Hover bars to inspect topic-level scores.",
                toTopicPoints(statsMap),
                false,
                true,
                new Color(34, 197, 94)), gbc);

        gbc.gridy++;
        if (statsMap.isEmpty()) {
            panel.add(createActionCard(createSectionIntro(
                    "No Topic Data Yet",
                    "Answer more questions to unlock topic-level performance analytics.")), gbc);
        } else {
            List<StatisticsTracker.TopicStats> ordered = new ArrayList<>(statsMap.values());
            ordered.sort((a, b) -> Integer.compare(b.getTotalAttempts(), a.getTotalAttempts()));

            for (StatisticsTracker.TopicStats topicStats : ordered) {
                int accuracy = (int) Math.round(topicStats.getAccuracy() * 100);
                panel.add(createTopicBarCard(topicStats.topic, accuracy, topicStats), gbc);
                gbc.gridy++;
            }
        }

        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(Box.createVerticalGlue(), gbc);
        return panel;
    }

    private JPanel createRecommendationsPanel() {
        StatisticsTracker tracker = getTracker();
        StatisticsTracker.RecommendationResult rec = tracker.getLearningRecommendations();

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.weightx = 1.0;

        int urgencyScore = calculateUrgencyScore(rec, tracker);

        panel.add(createActionCard(createRecommendationsHero(rec, tracker, urgencyScore)), gbc);

        gbc.gridy++;
        panel.add(createMiniChartCard(
                "Focus Pressure Matrix",
                "Hover each bar to inspect where attention is most needed right now.",
                toRecommendationPoints(rec, tracker),
                false,
                true,
                new Color(129, 140, 248)), gbc);

        List<RecommendationTask> tasks = buildRecommendationTasks(rec, tracker);
        gbc.gridy++;
        if (tasks.isEmpty()) {
            panel.add(createActionCard(createSectionIntro(
                    "No Recommendations Yet",
                    "Attempt more questions to unlock a personalized priority plan.")), gbc);
        } else {
            panel.add(createPriorityBoard(tasks), gbc);
        }

        gbc.gridy++;
        panel.add(createActionCard(createRecommendationActionBar()), gbc);

        if (rec != null && rec.mistakePatterns != null && !rec.mistakePatterns.isEmpty()) {
            List<Map.Entry<String, Integer>> patterns = new ArrayList<>(rec.mistakePatterns.entrySet());
            patterns.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

            StringBuilder patternText = new StringBuilder();
            int take = Math.min(5, patterns.size());
            for (int i = 0; i < take; i++) {
                Map.Entry<String, Integer> p = patterns.get(i);
                if (i > 0) {
                    patternText.append(" | ");
                }
                patternText.append(p.getKey()).append(" (x").append(p.getValue()).append(")");
            }

            gbc.gridy++;
            panel.add(createRecommendationCard(
                    "Pattern Watchlist",
                    patternText.toString(),
                    "Fix the highest-frequency pattern first for faster score improvement."), gbc);
        }

        gbc.gridy++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(Box.createVerticalGlue(), gbc);
        return panel;
    }

    private JPanel createRecommendationsHero(StatisticsTracker.RecommendationResult rec,
            StatisticsTracker tracker,
            int urgencyScore) {
        JPanel hero = new JPanel(new BorderLayout(12, 10));
        hero.setOpaque(false);

        JLabel heading = new JLabel("Recommendation Command Center");
        heading.setForeground(TEXT);
        heading.setFont(new Font("Inter", Font.BOLD, 16));

        JTextArea subtitle = createWrappedTextArea(
                "Prioritized actions generated from weak areas, review timing, and error patterns.",
                TEXT_DIM,
                Font.PLAIN,
                11,
                62);

        JPanel titleWrap = new JPanel(new GridLayout(2, 1, 0, 4));
        titleWrap.setOpaque(false);
        titleWrap.add(heading);
        titleWrap.add(subtitle);

        Color urgencyColor = urgencyScore >= 70
                ? new Color(239, 68, 68)
                : urgencyScore >= 40
                        ? new Color(245, 158, 11)
                        : new Color(34, 197, 94);

        JLabel scoreBadge = new JLabel("Priority Score  " + urgencyScore + "/100", SwingConstants.CENTER);
        scoreBadge.setOpaque(true);
        scoreBadge
                .setBackground(new Color(urgencyColor.getRed(), urgencyColor.getGreen(), urgencyColor.getBlue(), 180));
        scoreBadge.setForeground(new Color(248, 250, 252));
        scoreBadge.setFont(new Font("Inter", Font.BOLD, 12));
        scoreBadge.setBorder(new EmptyBorder(8, 14, 8, 14));

        int weakCount = rec != null && rec.weakAreasToReview != null ? rec.weakAreasToReview.size() : 0;
        int dueCount = rec != null && rec.reviewDueTopics != null ? rec.reviewDueTopics.size() : 0;
        int patternCount = rec != null && rec.mistakePatterns != null ? rec.mistakePatterns.size() : 0;
        String nextTopic = rec != null && rec.suggestedNextTopic != null && !rec.suggestedNextTopic.isBlank()
                ? rec.suggestedNextTopic
                : "Not enough signal yet";

        JPanel chipRow = new JPanel(new GridLayout(2, 2, 8, 6));
        chipRow.setOpaque(false);
        chipRow.add(createRecommendationPill("Weak: " + weakCount, new Color(153, 27, 27)));
        chipRow.add(createRecommendationPill("Review Due: " + dueCount, new Color(30, 64, 175)));
        chipRow.add(createRecommendationPill("Patterns: " + patternCount, new Color(91, 33, 182)));
        chipRow.add(createRecommendationPill(
                "Accuracy: " + String.format("%.1f%%", tracker == null ? 0.0 : tracker.getOverallAccuracy()),
                new Color(22, 101, 52)));

        JTextArea nextTopicLabel = createWrappedTextArea(
                "Suggested next topic: " + nextTopic,
                new Color(219, 231, 255),
                Font.BOLD,
                12,
                62);

        JPanel lower = new JPanel(new BorderLayout(0, 7));
        lower.setOpaque(false);
        lower.add(chipRow, BorderLayout.NORTH);
        lower.add(nextTopicLabel, BorderLayout.SOUTH);

        hero.add(titleWrap, BorderLayout.CENTER);
        hero.add(scoreBadge, BorderLayout.EAST);
        hero.add(lower, BorderLayout.SOUTH);
        return hero;
    }

    private JLabel createRecommendationPill(String text, Color base) {
        JLabel chip = new JLabel(text);
        chip.setOpaque(true);
        chip.setBackground(new Color(base.getRed(), base.getGreen(), base.getBlue(), 170));
        chip.setForeground(new Color(241, 245, 249));
        chip.setFont(new Font("Inter", Font.BOLD, 10));
        chip.setBorder(new EmptyBorder(5, 9, 5, 9));
        return chip;
    }

    private JPanel createPriorityBoard(List<RecommendationTask> tasks) {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setOpaque(false);

        JLabel title = new JLabel("Priority Execution Board");
        title.setForeground(TEXT);
        title.setFont(new Font("Inter", Font.BOLD, 14));

        JLabel subtitle = new JLabel("Follow these actions in sequence for the highest improvement impact.");
        subtitle.setForeground(TEXT_DIM);
        subtitle.setFont(new Font("Inter", Font.PLAIN, 11));

        JPanel head = new JPanel(new GridLayout(2, 1, 0, 3));
        head.setOpaque(false);
        head.add(title);
        head.add(subtitle);

        JPanel list = new JPanel(new GridLayout(tasks.size(), 1, 0, 8));
        list.setOpaque(false);
        for (RecommendationTask task : tasks) {
            list.add(createRecommendationTaskCard(task));
        }

        root.add(head, BorderLayout.NORTH);
        root.add(list, BorderLayout.CENTER);
        return root;
    }

    private JPanel createRecommendationTaskCard(RecommendationTask task) {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBackground(new Color(34, 41, 54));
        card.setBorder(new CompoundBorder(
                new LineBorder(new Color(86, 97, 120), 1, true),
                new EmptyBorder(10, 12, 10, 12)));

        JLabel priority = new JLabel(task.priority, SwingConstants.CENTER);
        priority.setOpaque(true);
        priority.setPreferredSize(new Dimension(38, 26));
        priority.setBackground(new Color(task.accent.getRed(), task.accent.getGreen(), task.accent.getBlue(), 190));
        priority.setForeground(new Color(248, 250, 252));
        priority.setFont(new Font("Inter", Font.BOLD, 11));

        JLabel title = new JLabel(task.title);
        title.setForeground(TEXT);
        title.setFont(new Font("Inter", Font.BOLD, 13));

        JTextArea desc = createWrappedTextArea(
                task.description,
                new Color(203, 213, 225),
                Font.PLAIN,
                11,
                42);

        JTextArea impact = createWrappedTextArea(
                task.impact,
                new Color(148, 163, 184),
                Font.PLAIN,
                10,
                42);

        JPanel text = new JPanel(new GridLayout(3, 1, 0, 4));
        text.setOpaque(false);
        text.add(title);
        text.add(desc);
        text.add(impact);

        JButton openBtn = new JButton("Open");
        openBtn.setFocusPainted(false);
        openBtn.setBorderPainted(false);
        openBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        openBtn.setForeground(new Color(241, 245, 249));
        openBtn.setBackground(new Color(63, 83, 110));
        openBtn.setFont(new Font("Inter", Font.BOLD, 11));
        openBtn.addActionListener(e -> {
            int tabIndex = findTabIndexByTitle(task.targetTab);
            if (tabIndex >= 0) {
                tabbedPane.setSelectedIndex(tabIndex);
            }
        });

        card.add(priority, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        card.add(openBtn, BorderLayout.EAST);
        return card;
    }

    private JPanel createRecommendationActionBar() {
        JPanel bar = new JPanel(new GridLayout(0, 2, 8, 8));
        bar.setOpaque(false);

        bar.add(createRecommendationActionButton("Open Weak Areas", new Color(217, 119, 6), () -> {
            int index = findTabIndexByTitle("Weak Areas");
            if (index >= 0) {
                tabbedPane.setSelectedIndex(index);
            }
        }));

        bar.add(createRecommendationActionButton("Open Topic Breakdown", new Color(22, 163, 74), () -> {
            int index = findTabIndexByTitle("Topic Breakdown");
            if (index >= 0) {
                tabbedPane.setSelectedIndex(index);
            }
        }));

        bar.add(createRecommendationActionButton("Refresh Plan", new Color(37, 99, 235), this::refreshData));
        return bar;
    }

    private JButton createRecommendationActionButton(String label, Color color, Runnable action) {
        JButton btn = new JButton(label);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setForeground(new Color(248, 250, 252));
        btn.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 185));
        btn.setFont(new Font("Inter", Font.BOLD, 11));
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private List<RecommendationTask> buildRecommendationTasks(StatisticsTracker.RecommendationResult rec,
            StatisticsTracker tracker) {
        List<RecommendationTask> tasks = new ArrayList<>();
        if (rec == null) {
            return tasks;
        }

        if (rec.weakAreasToReview != null && !rec.weakAreasToReview.isEmpty()) {
            tasks.add(new RecommendationTask(
                    "P1",
                    "Stabilize weak topics",
                    "Focus first on " + summarizeTopics(rec.weakAreasToReview, 3) + ".",
                    "Expected impact: raise baseline accuracy and reduce repeat errors.",
                    "Weak Areas",
                    new Color(239, 68, 68)));
        }

        if (rec.reviewDueTopics != null && !rec.reviewDueTopics.isEmpty()) {
            tasks.add(new RecommendationTask(
                    "P1",
                    "Clear review backlog",
                    "Re-attempt due topics: " + summarizeTopics(rec.reviewDueTopics, 3) + ".",
                    "Expected impact: retention boost through spaced repetition timing.",
                    "Weak Areas",
                    new Color(245, 158, 11)));
        }

        if (rec.suggestedNextTopic != null && !rec.suggestedNextTopic.isBlank()) {
            tasks.add(new RecommendationTask(
                    "P2",
                    "Advance to next topic",
                    "Move into " + rec.suggestedNextTopic + " after completing P1 items.",
                    "Expected impact: controlled progression without overload.",
                    "Topic Breakdown",
                    new Color(59, 130, 246)));
        }

        if (rec.mistakePatterns != null && !rec.mistakePatterns.isEmpty()) {
            String topPattern = rec.mistakePatterns.entrySet().stream()
                    .max(Comparator.comparingInt(e -> e.getValue() == null ? 0 : e.getValue()))
                    .map(Map.Entry::getKey)
                    .orElse("pattern drift");

            tasks.add(new RecommendationTask(
                    "P3",
                    "Correct dominant error pattern",
                    "Prioritize fixing: " + topPattern + ".",
                    "Expected impact: higher consistency during timed attempts.",
                    "Overview",
                    new Color(168, 85, 247)));
        }

        if (tasks.isEmpty()) {
            double overall = tracker != null ? tracker.getOverallAccuracy() : 0.0;
            tasks.add(new RecommendationTask(
                    "P1",
                    "Build stronger signal",
                    "Attempt 10-15 mixed questions to generate a richer recommendation profile.",
                    "Current overall accuracy: " + String.format("%.1f%%", overall),
                    "Overview",
                    new Color(16, 185, 129)));
        }

        return tasks;
    }

    private int calculateUrgencyScore(StatisticsTracker.RecommendationResult rec, StatisticsTracker tracker) {
        int weakCount = rec != null && rec.weakAreasToReview != null ? rec.weakAreasToReview.size() : 0;
        int dueCount = rec != null && rec.reviewDueTopics != null ? rec.reviewDueTopics.size() : 0;
        int patternCount = rec != null && rec.mistakePatterns != null ? rec.mistakePatterns.size() : 0;
        double overall = tracker != null ? tracker.getOverallAccuracy() : 0.0;
        int gap = (int) Math.round(Math.max(0.0, 100.0 - overall));

        int score = (weakCount * 18) + (dueCount * 12) + (patternCount * 9) + (gap / 2);
        return Math.max(0, Math.min(100, score));
    }

    private int findTabIndexByTitle(String title) {
        if (tabbedPane == null || title == null) {
            return -1;
        }
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            String tabTitle = tabbedPane.getTitleAt(i);
            if (title.equalsIgnoreCase(tabTitle)) {
                return i;
            }
        }
        return -1;
    }

    private String summarizeTopics(List<String> topics, int max) {
        if (topics == null || topics.isEmpty()) {
            return "N/A";
        }

        int limit = Math.min(Math.max(1, max), topics.size());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(topics.get(i));
        }
        if (topics.size() > limit) {
            sb.append(" +").append(topics.size() - limit).append(" more");
        }
        return sb.toString();
    }

    private JComponent createScrollableTab(JPanel panel) {
        return createScrollableTab(panel, 1080);
    }

    private JComponent createScrollableTab(JPanel panel, int maxWidth) {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setOpaque(false);

        GridBagConstraints wrapGbc = new GridBagConstraints();
        wrapGbc.gridx = 0;
        wrapGbc.gridy = 0;
        wrapGbc.weightx = 1.0;
        wrapGbc.fill = GridBagConstraints.HORIZONTAL;
        wrapGbc.anchor = GridBagConstraints.NORTH;

        int safeMaxWidth = Math.max(560, maxWidth);
        panel.setMaximumSize(new Dimension(safeMaxWidth, Integer.MAX_VALUE));
        wrap.add(panel, wrapGbc);

        wrapGbc.gridy = 1;
        wrapGbc.weighty = 1.0;
        wrapGbc.fill = GridBagConstraints.BOTH;
        wrap.add(Box.createVerticalGlue(), wrapGbc);

        JScrollPane scrollPane = new JScrollPane(wrap);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(BG);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        scrollPane.getVerticalScrollBar().setBlockIncrement(140);
        return scrollPane;
    }

    private JPanel createMetricCard(String label, String value, String caption, Color accent) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(SURFACE);
        card.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(14, 14, 14, 14)));

        JLabel labelComp = new JLabel(label);
        labelComp.setForeground(TEXT_DIM);
        labelComp.setFont(new Font("Inter", Font.PLAIN, 12));

        JLabel valueComp = new JLabel(value);
        valueComp.setForeground(accent);
        valueComp.setFont(new Font("Inter", Font.BOLD, 24));

        JLabel captionComp = new JLabel(caption);
        captionComp.setForeground(TEXT_DIM);
        captionComp.setFont(new Font("Inter", Font.PLAIN, 11));

        JPanel accentStrip = new JPanel();
        accentStrip.setBackground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 165));
        accentStrip.setPreferredSize(new Dimension(6, 1));

        JPanel center = new JPanel(new GridLayout(3, 1, 0, 4));
        center.setOpaque(false);
        center.add(labelComp);
        center.add(valueComp);
        center.add(captionComp);

        card.add(accentStrip, BorderLayout.WEST);
        card.add(center, BorderLayout.CENTER);
        return card;
    }

    private JPanel createMiniChartCard(String title,
            String subtitle,
            List<ChartPoint> points,
            boolean lineMode,
            boolean enableHorizontalScroll,
            Color accent) {
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(TEXT);
        titleLabel.setFont(new Font("Inter", Font.BOLD, 14));

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setForeground(TEXT_DIM);
        subtitleLabel.setFont(new Font("Inter", Font.PLAIN, 11));

        JPanel top = new JPanel(new GridLayout(2, 1, 0, 3));
        top.setOpaque(false);
        top.add(titleLabel);
        top.add(subtitleLabel);

        MiniInteractiveChart chart = new MiniInteractiveChart(points, lineMode, accent);

        content.add(top, BorderLayout.NORTH);
        if (enableHorizontalScroll) {
            chart.setPreferredSize(new Dimension(computeChartPreferredWidth(points), 150));

            JScrollPane chartScrollPane = new JScrollPane(
                    chart,
                    JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                    JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            chartScrollPane.setBorder(null);
            chartScrollPane.setOpaque(false);
            chartScrollPane.getViewport().setOpaque(false);
            chartScrollPane.getHorizontalScrollBar().setUnitIncrement(20);
            chartScrollPane.getHorizontalScrollBar().setBlockIncrement(140);
            // Keep the chart card width anchored to the page; only the chart content
            // scrolls.
            chartScrollPane.setMinimumSize(new Dimension(0, 168));
            chartScrollPane.setPreferredSize(new Dimension(100, 168));
            chartScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 168));
            content.add(chartScrollPane, BorderLayout.CENTER);
        } else {
            content.add(chart, BorderLayout.CENTER);
        }
        return createActionCard(content);
    }

    private JPanel createWeakAreaCard(String topic, int percent) {
        JPanel panel = new JPanel(new BorderLayout(10, 8));
        panel.setOpaque(false);

        Color level = percent < 70 ? new Color(239, 68, 68)
                : percent < 80 ? new Color(245, 158, 11)
                        : new Color(34, 197, 94);

        JLabel title = new JLabel(topic + "  (" + percent + "%)");
        title.setForeground(TEXT);
        title.setFont(new Font("Inter", Font.BOLD, 14));

        JLabel detail = new JLabel("Recommended: short focused drills and spaced review.");
        detail.setForeground(TEXT_DIM);
        detail.setFont(new Font("Inter", Font.PLAIN, 11));

        JButton tipBtn = new JButton("Study Tip");
        tipBtn.setFocusPainted(false);
        tipBtn.setBorderPainted(false);
        tipBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        tipBtn.setForeground(TEXT);
        tipBtn.setBackground(new Color(63, 83, 110));
        tipBtn.setFont(new Font("Inter", Font.BOLD, 11));
        tipBtn.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "For " + topic + ":\n\n"
                        + "1. Revisit core concept formulas/rules.\n"
                        + "2. Solve 8-12 focused questions.\n"
                        + "3. Re-attempt in timed mode after 24 hours.",
                "Study Tip", JOptionPane.INFORMATION_MESSAGE));

        JPanel text = new JPanel(new GridLayout(2, 1, 0, 4));
        text.setOpaque(false);
        text.add(title);
        text.add(detail);

        JPanel badge = new JPanel();
        badge.setBackground(new Color(level.getRed(), level.getGreen(), level.getBlue(), 175));
        badge.setPreferredSize(new Dimension(8, 1));

        panel.add(badge, BorderLayout.WEST);
        panel.add(text, BorderLayout.CENTER);
        panel.add(tipBtn, BorderLayout.EAST);

        return createActionCard(panel);
    }

    private JPanel createTopicBarCard(String topic, int accuracy, StatisticsTracker.TopicStats stats) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);

        JLabel topicLabel = new JLabel(topic);
        topicLabel.setForeground(TEXT);
        topicLabel.setFont(new Font("Inter", Font.BOLD, 12));
        topicLabel.setPreferredSize(new Dimension(160, 30));

        JPanel bar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int barHeight = 20;
                int y = (getHeight() - barHeight) / 2;

                g2.setColor(new Color(42, 49, 63));
                g2.fillRoundRect(0, y, getWidth(), barHeight, 6, 6);

                int width = (int) ((accuracy / 100.0) * getWidth());
                Color fill = accuracy >= 85 ? new Color(34, 197, 94)
                        : accuracy >= 70 ? new Color(245, 158, 11)
                                : new Color(239, 68, 68);
                g2.setColor(fill);
                g2.fillRoundRect(0, y, Math.max(2, width), barHeight, 6, 6);

                String txt = accuracy + "%  |  " + stats.getTotalAttempts() + " attempts";
                g2.setColor(new Color(226, 232, 240));
                g2.setFont(new Font("Inter", Font.BOLD, 11));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(txt)) / 2;
                int ty = y + ((barHeight - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(txt, tx, ty);
                g2.dispose();
            }
        };
        bar.setOpaque(false);

        row.add(topicLabel, BorderLayout.WEST);
        row.add(bar, BorderLayout.CENTER);
        return createActionCard(row);
    }

    private JPanel createRecommendationCard(String title, String value, String note) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(TEXT);
        titleLabel.setFont(new Font("Inter", Font.BOLD, 13));

        JTextArea valueLabel = createWrappedTextArea(
                value,
                new Color(219, 231, 255),
                Font.PLAIN,
                12,
                56);

        JTextArea noteLabel = createWrappedTextArea(
                note,
                TEXT_DIM,
                Font.PLAIN,
                11,
                56);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(noteLabel, BorderLayout.SOUTH);

        return createActionCard(card);
    }

    private JTextArea createWrappedTextArea(String text, Color color, int style, int size, int columns) {
        JTextArea area = new JTextArea(text == null ? "" : text);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setBorder(null);
        area.setForeground(color);
        area.setFont(new Font("Inter", style, size));
        area.setColumns(Math.max(20, columns));
        return area;
    }

    private JPanel createLegendPanel() {
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        legend.setOpaque(false);
        legend.add(createLegendItem(new Color(34, 197, 94), "Strong (85%+)"));
        legend.add(createLegendItem(new Color(245, 158, 11), "Needs Attention (70-84%)"));
        legend.add(createLegendItem(new Color(239, 68, 68), "Critical (<70%)"));
        return legend;
    }

    private JPanel createLegendItem(Color color, String label) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        item.setOpaque(false);

        JLabel dot = new JLabel("●");
        dot.setForeground(color);
        dot.setFont(new Font("Inter", Font.BOLD, 12));

        JLabel text = new JLabel(label);
        text.setForeground(TEXT_DIM);
        text.setFont(new Font("Inter", Font.PLAIN, 11));

        item.add(dot);
        item.add(text);
        return item;
    }

    private JPanel createSectionIntro(String title, String detail) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(TEXT);
        titleLabel.setFont(new Font("Inter", Font.BOLD, 13));

        JLabel detailLabel = new JLabel(detail);
        detailLabel.setForeground(TEXT_DIM);
        detailLabel.setFont(new Font("Inter", Font.PLAIN, 11));

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(detailLabel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createActionCard(JComponent content) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(SURFACE);
        card.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(12, 14, 12, 14)));

        if (content instanceof JPanel) {
            ((JPanel) content).setOpaque(false);
        }

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JLabel createInfoChip(String text, Color bg) {
        JLabel chip = new JLabel(text);
        chip.setOpaque(true);
        chip.setBackground(bg);
        chip.setForeground(TEXT);
        chip.setFont(new Font("Inter", Font.BOLD, 11));
        chip.setBorder(new EmptyBorder(5, 10, 5, 10));
        return chip;
    }

    private StatisticsTracker getTracker() {
        if (parentGui == null) {
            return null;
        }
        return parentGui.getStatisticsTracker();
    }

    private String formatDuration(long millis) {
        long safe = Math.max(0L, millis);
        long totalSeconds = safe / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }

    private List<ChartPoint> toTrendPoints(List<Integer> trend) {
        List<ChartPoint> points = new ArrayList<>();
        if (trend == null || trend.isEmpty()) {
            points.add(new ChartPoint("No attempts yet", 0));
            return points;
        }

        for (int i = 0; i < trend.size(); i++) {
            points.add(new ChartPoint(formatTrendBucketLabel(i, trend.size()), trend.get(i)));
        }
        return points;
    }

    private String formatTrendBucketLabel(int index, int totalBuckets) {
        if (totalBuckets <= 1) {
            return "Group 1 (Latest)";
        }
        int groupNumber = index + 1;
        if (index == 0) {
            return "Group " + groupNumber + " (Oldest)";
        }
        if (index == totalBuckets - 1) {
            return "Group " + groupNumber + " (Latest)";
        }
        return "Group " + groupNumber;
    }

    private List<ChartPoint> toWeakAreaPoints(Map<String, Double> weakMap) {
        List<ChartPoint> points = new ArrayList<>();
        if (weakMap == null || weakMap.isEmpty()) {
            points.add(new ChartPoint("No Data", 0));
            return points;
        }

        List<Map.Entry<String, Double>> sorted = new ArrayList<>(weakMap.entrySet());
        sorted.sort(Comparator.comparingDouble(Map.Entry::getValue));

        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, Double> entry = sorted.get(i);
            points.add(new ChartPoint(trimLabel(entry.getKey(), 40), entry.getValue() * 100.0));
        }
        return points;
    }

    private List<ChartPoint> toTopicPoints(Map<String, StatisticsTracker.TopicStats> statsMap) {
        List<ChartPoint> points = new ArrayList<>();
        if (statsMap == null || statsMap.isEmpty()) {
            points.add(new ChartPoint("No Data", 0));
            return points;
        }

        List<StatisticsTracker.TopicStats> sorted = new ArrayList<>(statsMap.values());
        sorted.sort((a, b) -> Integer.compare(b.getTotalAttempts(), a.getTotalAttempts()));

        for (int i = 0; i < sorted.size(); i++) {
            StatisticsTracker.TopicStats topicStats = sorted.get(i);
            points.add(new ChartPoint(trimLabel(topicStats.topic, 40), topicStats.getAccuracy() * 100.0));
        }
        return points;
    }

    private int computeChartPreferredWidth(List<ChartPoint> points) {
        if (points == null || points.isEmpty()) {
            return 560;
        }

        int maxLabelLength = 0;
        for (ChartPoint point : points) {
            if (point != null && point.label != null) {
                maxLabelLength = Math.max(maxLabelLength, point.label.length());
            }
        }

        int pointCount = Math.max(1, points.size());
        int perPointWidth = Math.max(120, Math.min(300, 28 + maxLabelLength * 8));
        int calculated = pointCount * perPointWidth;
        return Math.max(560, Math.min(9600, calculated));
    }

    private List<ChartPoint> toRecommendationPoints(StatisticsTracker.RecommendationResult rec,
            StatisticsTracker tracker) {
        List<ChartPoint> points = new ArrayList<>();

        int weakCount = rec != null && rec.weakAreasToReview != null ? rec.weakAreasToReview.size() : 0;
        int dueCount = rec != null && rec.reviewDueTopics != null ? rec.reviewDueTopics.size() : 0;
        int patternMax = 0;
        if (rec != null && rec.mistakePatterns != null && !rec.mistakePatterns.isEmpty()) {
            for (Integer value : rec.mistakePatterns.values()) {
                patternMax = Math.max(patternMax, value == null ? 0 : value);
            }
        }

        double overallAccuracy = tracker != null ? tracker.getOverallAccuracy() : 0;
        double weakPressure = Math.min(100.0, weakCount * 20.0);
        double reviewPressure = Math.min(100.0, dueCount * 15.0);
        double patternNoise = Math.min(100.0, patternMax * 12.0);
        double accuracyGap = Math.max(0.0, 100.0 - overallAccuracy);

        points.add(new ChartPoint("Weak", weakPressure));
        points.add(new ChartPoint("Review", reviewPressure));
        points.add(new ChartPoint("Pattern", patternNoise));
        points.add(new ChartPoint("Gap", accuracyGap));

        return points;
    }

    private String trimLabel(String label, int maxLen) {
        if (label == null) {
            return "N/A";
        }
        String cleaned = label.trim();
        if (cleaned.length() <= maxLen) {
            return cleaned;
        }
        return cleaned.substring(0, Math.max(1, maxLen - 1)) + "…";
    }

    private static class RecommendationTask {
        final String priority;
        final String title;
        final String description;
        final String impact;
        final String targetTab;
        final Color accent;

        RecommendationTask(String priority,
                String title,
                String description,
                String impact,
                String targetTab,
                Color accent) {
            this.priority = priority;
            this.title = title;
            this.description = description;
            this.impact = impact;
            this.targetTab = targetTab;
            this.accent = accent;
        }
    }

    private static class ChartPoint {
        final String label;
        final double value;

        ChartPoint(String label, double value) {
            this.label = label;
            this.value = value;
        }
    }

    private class MiniInteractiveChart extends JPanel {
        private final List<ChartPoint> points;
        private final boolean lineMode;
        private final Color accent;
        private final javax.swing.Timer animationTimer;
        private float animationProgress = 0f;
        private int hoverIndex = -1;
        private Rectangle[] barHitBoxes = new Rectangle[0];

        MiniInteractiveChart(List<ChartPoint> source, boolean lineMode, Color accent) {
            this.points = source == null ? new ArrayList<>() : new ArrayList<>(source);
            this.lineMode = lineMode;
            this.accent = accent;

            setOpaque(false);
            setPreferredSize(new Dimension(560, 150));
            setMinimumSize(new Dimension(140, 120));
            setToolTipText("chart");

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int idx = resolveHoverIndex(e.getPoint());
                    if (idx != hoverIndex) {
                        hoverIndex = idx;
                        repaint();
                    }
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseExited(MouseEvent e) {
                    hoverIndex = -1;
                    repaint();
                }
            });

            animationTimer = new javax.swing.Timer(16, e -> {
                animationProgress = Math.min(1f, animationProgress + 0.06f);
                repaint();
                if (animationProgress >= 1f) {
                    ((javax.swing.Timer) e.getSource()).stop();
                }
            });
            animationTimer.start();
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            int idx = resolveHoverIndex(event.getPoint());
            if (idx < 0 || idx >= points.size()) {
                return null;
            }
            ChartPoint point = points.get(idx);
            return point.label + " accuracy: " + String.format("%.1f", point.value) + "%";
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            int w = getWidth();
            int h = getHeight();
            int edgePadding = computeChartEdgePadding();
            int left = edgePadding;
            int right = w - edgePadding;
            int top = 14;
            int bottom = h - 30;

            if (right <= left || bottom <= top) {
                g2.dispose();
                return;
            }

            g2.setColor(new Color(42, 49, 63));
            g2.fillRoundRect(0, 0, w, h, 12, 12);
            g2.setColor(new Color(78, 89, 111));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);

            if (points.isEmpty()) {
                g2.setColor(TEXT_DIM);
                g2.setFont(new Font("Inter", Font.PLAIN, 12));
                g2.drawString("No chart data", left, top + 22);
                g2.dispose();
                return;
            }

            double maxValue = computeMaxValue();

            g2.setColor(new Color(84, 96, 118));
            for (int i = 0; i < 4; i++) {
                int y = top + i * (bottom - top) / 3;
                g2.drawLine(left, y, right, y);
            }

            g2.setColor(TEXT_DIM);
            g2.setFont(new Font("Inter", Font.PLAIN, 10));
            g2.drawString("0", 16, bottom + 4);
            g2.drawString(String.valueOf((int) Math.round(maxValue)), 8, top + 4);

            if (lineMode) {
                paintLineChart(g2, left, right, top, bottom, maxValue);
            } else {
                paintBarChart(g2, left, right, top, bottom, maxValue);
            }

            paintXAxisLabels(g2, left, right, bottom);

            g2.dispose();
        }

        private void paintLineChart(Graphics2D g2, int left, int right, int top, int bottom, double maxValue) {
            if (points.size() == 1) {
                int px = left + (right - left) / 2;
                int py = valueToY(points.get(0).value, top, bottom, maxValue);
                g2.setColor(accent);
                g2.fillOval(px - 4, py - 4, 8, 8);
                return;
            }

            Path2D linePath = new Path2D.Double();
            Path2D areaPath = new Path2D.Double();

            int lastX = left;
            for (int i = 0; i < points.size(); i++) {
                int x = left + i * (right - left) / (points.size() - 1);
                int y = valueToY(points.get(i).value, top, bottom, maxValue);
                lastX = x;

                if (i == 0) {
                    linePath.moveTo(x, y);
                    areaPath.moveTo(x, bottom);
                    areaPath.lineTo(x, y);
                } else {
                    linePath.lineTo(x, y);
                    areaPath.lineTo(x, y);
                }
            }

            areaPath.lineTo(lastX, bottom);
            areaPath.closePath();

            GradientPaint areaGradient = new GradientPaint(
                    0,
                    top,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 95),
                    0,
                    bottom,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 8));
            g2.setPaint(areaGradient);
            g2.fill(areaPath);

            g2.setStroke(new BasicStroke(2.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(accent);
            g2.draw(linePath);

            for (int i = 0; i < points.size(); i++) {
                int x = left + i * (right - left) / (points.size() - 1);
                int y = valueToY(points.get(i).value, top, bottom, maxValue);
                if (i == hoverIndex) {
                    g2.setColor(new Color(255, 255, 255));
                    g2.fillOval(x - 6, y - 6, 12, 12);
                    g2.setColor(accent.darker());
                    g2.fillOval(x - 4, y - 4, 8, 8);
                } else {
                    g2.setColor(new Color(226, 232, 240));
                    g2.fillOval(x - 3, y - 3, 6, 6);
                }
            }
        }

        private void paintBarChart(Graphics2D g2, int left, int right, int top, int bottom, double maxValue) {
            int n = points.size();
            if (n <= 0) {
                return;
            }

            int plotWidth = right - left;
            int gap = Math.max(6, plotWidth / Math.max(14, n * 8));
            int barWidth = Math.max(10, (plotWidth - (n - 1) * gap) / n);
            int barsTotalWidth = n * barWidth + (n - 1) * gap;
            int startX = left + Math.max(0, (plotWidth - barsTotalWidth) / 2);

            barHitBoxes = new Rectangle[n];

            for (int i = 0; i < n; i++) {
                int x = startX + i * (barWidth + gap);
                int y = valueToY(points.get(i).value, top, bottom, maxValue);
                int height = Math.max(2, bottom - y);

                barHitBoxes[i] = new Rectangle(x, top, barWidth, bottom - top);

                boolean hovered = i == hoverIndex;
                Color base = hovered ? brighten(accent, 28) : accent;
                g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), hovered ? 232 : 195));
                g2.fillRoundRect(x, y, barWidth, height, 8, 8);

                if (hovered) {
                    g2.setColor(new Color(255, 255, 255, 170));
                    g2.drawRoundRect(x, y, barWidth, height, 8, 8);
                }
            }
        }

        private void paintXAxisLabels(Graphics2D g2, int left, int right, int bottom) {
            if (points.isEmpty()) {
                return;
            }

            g2.setColor(TEXT_DIM);
            g2.setFont(new Font("Inter", Font.PLAIN, 10));

            int n = points.size();
            for (int i = 0; i < n; i++) {
                if (n > 8 && (i % 2) == 1) {
                    continue;
                }

                int x = n == 1
                        ? left + (right - left) / 2
                        : left + i * (right - left) / (n - 1);
                String label = points.get(i).label;

                FontMetrics fm = g2.getFontMetrics();
                int labelWidth = fm.stringWidth(label);
                int lx = x - labelWidth / 2;
                lx = Math.max(6, Math.min(lx, getWidth() - labelWidth - 6));
                int ly = bottom + 14;
                g2.drawString(label, lx, ly);
            }
        }

        private int resolveHoverIndex(Point point) {
            if (points.isEmpty()) {
                return -1;
            }

            int edgePadding = computeChartEdgePadding();
            int left = edgePadding;
            int right = getWidth() - edgePadding;
            int top = 14;
            int bottom = getHeight() - 30;
            if (right <= left || bottom <= top) {
                return -1;
            }

            if (!lineMode && barHitBoxes != null && barHitBoxes.length == points.size()) {
                for (int i = 0; i < barHitBoxes.length; i++) {
                    Rectangle hit = barHitBoxes[i];
                    if (hit != null && hit.contains(point)) {
                        return i;
                    }
                }
                return -1;
            }

            double maxValue = computeMaxValue();
            double bestDistance = Double.MAX_VALUE;
            int bestIndex = -1;

            for (int i = 0; i < points.size(); i++) {
                int x = points.size() == 1
                        ? left + (right - left) / 2
                        : left + i * (right - left) / (points.size() - 1);
                int y = valueToY(points.get(i).value, top, bottom, maxValue);
                double distance = Point.distance(point.x, point.y, x, y);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestIndex = i;
                }
            }

            return bestDistance <= 16.0 ? bestIndex : -1;
        }

        private int computeChartEdgePadding() {
            int maxLabelLength = 0;
            for (ChartPoint point : points) {
                if (point != null && point.label != null) {
                    maxLabelLength = Math.max(maxLabelLength, point.label.length());
                }
            }
            return Math.max(36, Math.min(130, 18 + maxLabelLength * 4));
        }

        private int valueToY(double value, int top, int bottom, double maxValue) {
            double ratio = maxValue <= 0.0 ? 0.0 : (value / maxValue);
            ratio = Math.max(0.0, Math.min(1.0, ratio));

            int available = bottom - top;
            double animatedRatio = ratio * animationProgress;
            return bottom - (int) Math.round(available * animatedRatio);
        }

        private double computeMaxValue() {
            double max = 0.0;
            for (ChartPoint point : points) {
                max = Math.max(max, point.value);
            }
            if (max <= 0.0) {
                return 100.0;
            }
            return Math.max(10.0, max * 1.15);
        }

        private Color brighten(Color color, int delta) {
            return new Color(
                    Math.min(255, color.getRed() + delta),
                    Math.min(255, color.getGreen() + delta),
                    Math.min(255, color.getBlue() + delta));
        }
    }
}
