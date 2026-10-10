package org.example;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;

/**
 * Tools page with quick utility actions and operational shortcuts.
 * Uses live data from AptitudeGui and StatisticsTracker.
 */
public class ToolsPageDialog extends JPanel {
    private static final Color BG = new Color(18, 22, 29);
    private static final Color SURFACE = new Color(30, 36, 47);
    private static final Color SURFACE_HOVER = new Color(39, 47, 61);
    private static final Color BORDER = new Color(70, 80, 99);
    private static final Color TEXT = new Color(226, 232, 240);
    private static final Color TEXT_DIM = new Color(148, 163, 184);

    private final AptitudeGui parentGui;
    private JLabel healthChip;
    private JLabel sessionChip;
    private JLabel accuracyChip;
    private JToggleButton performanceToggle;

    public ToolsPageDialog(AptitudeGui parent) {
        this.parentGui = parent;
        setupPanel();
        refreshData();
    }

    public void refreshData() {
        if (parentGui == null) {
            return;
        }

        healthChip.setText("Essential tools");
        sessionChip.setText(parentGui.getSessionTotalAttempted() + " attempts");
        accuracyChip.setText(parentGui.getCurrentAccuracyPercent() + "% accuracy");

        if (performanceToggle != null) {
            boolean enabled = parentGui.isPerformanceMonitorVisible();
            performanceToggle.setSelected(enabled);
            performanceToggle.setText(enabled ? "ON" : "OFF");
            performanceToggle.setBackground(enabled ? new Color(34, 104, 80) : new Color(98, 56, 61));
        }

        revalidate();
        repaint();
    }

    private void setupPanel() {
        setLayout(new BorderLayout());
        setBackground(BG);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(18, 22, 10, 22));

        JLabel titleLabel = new JLabel("Tools Workspace");
        titleLabel.setFont(new Font("Inter", Font.BOLD, 24));
        titleLabel.setForeground(TEXT);
        header.add(titleLabel, BorderLayout.WEST);

        JLabel subtitleLabel = new JLabel("Essential actions only");
        subtitleLabel.setFont(new Font("Inter", Font.PLAIN, 12));
        subtitleLabel.setForeground(TEXT_DIM);
        header.add(subtitleLabel, BorderLayout.SOUTH);

        JPanel chipRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        chipRow.setOpaque(false);
        healthChip = createInfoChip("0 weak", new Color(54, 75, 97));
        sessionChip = createInfoChip("0 attempts", new Color(62, 86, 73));
        accuracyChip = createInfoChip("0% accuracy", new Color(96, 76, 59));
        chipRow.add(healthChip);
        chipRow.add(sessionChip);
        chipRow.add(accuracyChip);
        header.add(chipRow, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        JPanel gridPanel = new JPanel(new GridBagLayout());
        gridPanel.setOpaque(false);
        gridPanel.setBorder(new EmptyBorder(8, 14, 20, 14));

        GridBagConstraints row = new GridBagConstraints();
        row.gridx = 0;
        row.gridy = 0;
        row.gridwidth = 2;
        row.weightx = 1.0;
        row.fill = GridBagConstraints.HORIZONTAL;
        row.insets = new Insets(8, 8, 10, 8);
        gridPanel.add(createPerformanceToggleCard(), row);

        addToolCard(gridPanel, 0, 1,
                createToolCard("NEW", "Start New Session", "Reset active question state and begin fresh.",
                        new Color(59, 130, 246), this::startNewSession));

        addToolCard(gridPanel, 1, 1,
                createToolCard("STATS", "Session Snapshot", "View current attempts, accuracy, and streak.",
                        new Color(14, 165, 233), this::showSessionSnapshot));

        addToolCard(gridPanel, 0, 2,
                createToolCard("EXPORT", "Export Snapshot Report", "Save current performance summary to logs.",
                        new Color(168, 85, 247), this::exportSnapshotReport));

        addToolCard(gridPanel, 1, 2,
                createToolCard("RESET", "Reset Statistics", "Clear recorded stats and start from zero.",
                        new Color(239, 68, 68), this::resetStatistics));

        GridBagConstraints filler = new GridBagConstraints();
        filler.gridx = 0;
        filler.gridy = 3;
        filler.gridwidth = 2;
        filler.weighty = 1.0;
        filler.fill = GridBagConstraints.BOTH;
        gridPanel.add(Box.createVerticalGlue(), filler);

        JPanel contentWrap = new JPanel(new GridBagLayout());
        contentWrap.setOpaque(false);

        GridBagConstraints wrapGbc = new GridBagConstraints();
        wrapGbc.gridx = 0;
        wrapGbc.gridy = 0;
        wrapGbc.weightx = 1.0;
        wrapGbc.fill = GridBagConstraints.HORIZONTAL;
        wrapGbc.anchor = GridBagConstraints.NORTH;

        gridPanel.setMaximumSize(new Dimension(700, Integer.MAX_VALUE));
        contentWrap.add(gridPanel, wrapGbc);

        wrapGbc.gridy = 1;
        wrapGbc.weighty = 1.0;
        wrapGbc.fill = GridBagConstraints.BOTH;
        contentWrap.add(Box.createVerticalGlue(), wrapGbc);

        JScrollPane scrollPane = new JScrollPane(contentWrap);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(BG);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        scrollPane.getVerticalScrollBar().setBlockIncrement(140);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void addToolCard(JPanel parent, int col, int row, JPanel card) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = col;
        gbc.gridy = row;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(8, 8, 8, 8);
        parent.add(card, gbc);
    }

    private JPanel createToolCard(String badgeText, String title, String description, Color accent, Runnable action) {
        JPanel card = new JPanel(new BorderLayout(12, 12));
        card.setBackground(SURFACE);
        card.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(12, 12, 12, 12)));

        JLabel badge = new JLabel(badgeText, SwingConstants.CENTER);
        badge.setOpaque(true);
        badge.setForeground(new Color(241, 245, 249));
        badge.setBackground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 185));
        badge.setFont(new Font("Inter", Font.BOLD, 11));
        badge.setPreferredSize(new Dimension(56, 26));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(TEXT);
        titleLabel.setFont(new Font("Inter", Font.BOLD, 15));

        JTextArea descLabel = createWrappedTextArea(description, TEXT_DIM, Font.PLAIN, 11, 26);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        textPanel.setOpaque(false);
        textPanel.add(titleLabel);
        textPanel.add(descLabel);

        JPanel left = new JPanel(new BorderLayout(10, 0));
        left.setOpaque(false);
        left.add(badge, BorderLayout.WEST);
        left.add(textPanel, BorderLayout.CENTER);

        JButton runBtn = new JButton("Run");
        runBtn.setFont(new Font("Inter", Font.BOLD, 12));
        runBtn.setForeground(new Color(241, 245, 249));
        runBtn.setBackground(new Color(66, 78, 101));
        runBtn.setBorderPainted(false);
        runBtn.setFocusPainted(false);
        runBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        runBtn.setPreferredSize(new Dimension(74, 32));
        runBtn.addActionListener(e -> action.run());

        Color runBase = runBtn.getBackground();
        Color runHover = brighten(runBase, 18);

        MouseAdapter hover = new MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                card.setBackground(SURFACE_HOVER);
                runBtn.setBackground(runHover);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                card.setBackground(SURFACE);
                runBtn.setBackground(runBase);
            }
        };
        card.addMouseListener(hover);
        runBtn.addMouseListener(hover);

        card.add(left, BorderLayout.CENTER);
        card.add(runBtn, BorderLayout.EAST);
        return card;
    }

    private JPanel createPerformanceToggleCard() {
        JPanel card = new JPanel(new BorderLayout(10, 8));
        card.setBackground(SURFACE);
        card.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)));
        card.setPreferredSize(new Dimension(460, 72));

        JLabel title = new JLabel("Performance Monitor Overlay");
        title.setForeground(TEXT);
        title.setFont(new Font("Inter", Font.BOLD, 14));

        JTextArea sub = createWrappedTextArea(
                "Toggle real-time FPS and runtime metrics overlay.",
                TEXT_DIM,
                Font.PLAIN,
                11,
                34);

        JPanel text = new JPanel(new GridLayout(2, 1, 0, 3));
        text.setOpaque(false);
        text.add(title);
        text.add(sub);

        boolean enabled = parentGui != null && parentGui.isPerformanceMonitorVisible();
        performanceToggle = new JToggleButton(enabled ? "ON" : "OFF", enabled);
        performanceToggle.setFont(new Font("Inter", Font.BOLD, 11));
        performanceToggle.setForeground(TEXT);
        performanceToggle.setBackground(enabled ? new Color(34, 104, 80) : new Color(98, 56, 61));
        performanceToggle.setFocusPainted(false);
        performanceToggle.setBorderPainted(false);
        performanceToggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        performanceToggle.setPreferredSize(new Dimension(78, 34));
        performanceToggle.addActionListener(e -> {
            boolean selected = performanceToggle.isSelected();
            performanceToggle.setText(selected ? "ON" : "OFF");
            performanceToggle.setBackground(selected ? new Color(34, 104, 80) : new Color(98, 56, 61));

            if (parentGui != null) {
                parentGui.setPerformanceMonitorVisible(selected);
                parentGui.showNotification("Performance Monitor " + (selected ? "enabled" : "disabled"));
            }
        });

        card.add(text, BorderLayout.CENTER);
        card.add(performanceToggle, BorderLayout.EAST);
        return card;
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
        area.setColumns(Math.max(16, columns));
        return area;
    }

    private void startNewSession() {
        if (parentGui == null) {
            return;
        }
        parentGui.resetSession();
        parentGui.showNotification("New session started");
        refreshData();
    }

    private void showSessionSnapshot() {
        if (parentGui == null) {
            return;
        }

        String stats = String.format(
                "Current Session\n\n" +
                        "Attempted: %d\n" +
                        "Correct: %d\n" +
                        "Accuracy: %d%%\n" +
                        "Streak: %d",
                parentGui.getSessionTotalAttempted(),
                parentGui.getSessionTotalCorrect(),
                parentGui.getCurrentAccuracyPercent(),
                parentGui.getCurrentStreakCount());

        JOptionPane.showMessageDialog(this, stats, "Session Snapshot", JOptionPane.INFORMATION_MESSAGE);
    }

    private void exportSnapshotReport() {
        if (parentGui == null) {
            return;
        }

        File logsDir = new File("logs");
        if (!logsDir.exists() && !logsDir.mkdirs()) {
            JOptionPane.showMessageDialog(this,
                    "Could not create logs directory for export.",
                    "Export Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
        File outFile = new File(logsDir, "tools_snapshot_" + stamp + ".txt");

        try (FileWriter writer = new FileWriter(outFile)) {
            writer.write(buildSnapshotReport());
            JOptionPane.showMessageDialog(this,
                    "Snapshot exported to:\n" + outFile.getAbsolutePath(),
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to export snapshot:\n" + ex.getMessage(),
                    "Export Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String buildSnapshotReport() {
        StatisticsTracker tracker = parentGui.getStatisticsTracker();
        StatisticsTracker.RecommendationResult rec = tracker.getLearningRecommendations();

        StringBuilder sb = new StringBuilder();
        sb.append("Aptitude Tools Snapshot\n");
        sb.append("Generated: ").append(LocalDateTime.now()).append("\n\n");

        sb.append("Session\n");
        sb.append("- Attempted: ").append(parentGui.getSessionTotalAttempted()).append("\n");
        sb.append("- Correct: ").append(parentGui.getSessionTotalCorrect()).append("\n");
        sb.append("- Accuracy: ").append(parentGui.getCurrentAccuracyPercent()).append("%\n");
        sb.append("- Current streak: ").append(parentGui.getCurrentStreakCount()).append("\n\n");

        sb.append("Global Statistics\n");
        sb.append("- Total attempted: ").append(tracker.getTotalAttempted()).append("\n");
        sb.append("- Total correct: ").append(tracker.getTotalCorrect()).append("\n");
        sb.append("- Overall accuracy: ").append(String.format("%.1f", tracker.getOverallAccuracy())).append("%\n");
        sb.append("- Best streak: ").append(tracker.getBestStreak()).append("\n");
        sb.append("- Avg response time: ").append(String.format("%.1f", tracker.getAverageTimeSeconds()))
                .append(" sec\n");
        sb.append("- Unique topics: ").append(tracker.getUniqueTopicCount()).append("\n\n");

        sb.append("Weak Areas\n");
        Map<String, Double> weak = tracker.getWeakAreas();
        if (weak.isEmpty()) {
            sb.append("- None\n");
        } else {
            weak.entrySet().stream()
                    .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                    .forEach(e -> sb.append("- ").append(e.getKey()).append(" : ")
                            .append((int) Math.round(e.getValue() * 100)).append("%\n"));
        }
        sb.append("\n");

        sb.append("Recommendations\n");
        if (rec.suggestedNextTopic != null) {
            sb.append("- Suggested next topic: ").append(rec.suggestedNextTopic).append("\n");
        }
        if (rec.weakAreasToReview != null && !rec.weakAreasToReview.isEmpty()) {
            sb.append("- Weak areas to review: ").append(String.join(", ", rec.weakAreasToReview)).append("\n");
        }
        if (rec.reviewDueTopics != null && !rec.reviewDueTopics.isEmpty()) {
            sb.append("- Review due topics: ").append(String.join(", ", rec.reviewDueTopics)).append("\n");
        }

        return sb.toString();
    }

    private void resetStatistics() {
        if (parentGui == null) {
            return;
        }

        int result = JOptionPane.showConfirmDialog(this,
                "This will clear recorded statistics and achievements. Continue?",
                "Reset Statistics",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        parentGui.resetAllStatistics();
        parentGui.showNotification("Statistics reset completed");
        refreshData();
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

    private Color brighten(Color color, int delta) {
        return new Color(
                Math.min(255, color.getRed() + delta),
                Math.min(255, color.getGreen() + delta),
                Math.min(255, color.getBlue() + delta));
    }
}
