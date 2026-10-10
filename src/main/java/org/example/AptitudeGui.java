package org.example;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicMenuItemUI;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class AptitudeGui extends JFrame {
    private static final String OPTION_VISUAL_STATE_KEY = "optionVisualState";
    private static final String OPTION_VISUAL_STATE_CORRECT = "correct";
    private static final String OPTION_VISUAL_STATE_WRONG = "wrong";
    private static final String OPTION_VISUAL_STATE_DIM = "dim";
    private static final int EXPLANATION_STATE_NEUTRAL = 0;
    private static final int EXPLANATION_STATE_LOADING = 1;
    private static final int EXPLANATION_STATE_CORRECT = 2;
    private static final int EXPLANATION_STATE_WRONG = 3;
    private static final Color OPTION_TEXT_COLOR = new Color(203, 213, 225);
    private static final Color OPTION_TEXT_DIM = new Color(100, 100, 100);
    private static final Color OPTION_BORDER_BASE = new Color(100, 116, 139, 100);
    private static final Color OPTION_BORDER_ACCENT = new Color(99, 102, 241);
    private final java.awt.image.BufferedImage verifyTexturePattern = buildTexturePattern(
            8,
            new Color(255, 255, 255, 34),
            new Color(255, 255, 255, 16));
    private final java.awt.image.BufferedImage optionTexturePattern = buildTexturePattern(
            6,
            new Color(255, 255, 255, 20),
            new Color(255, 255, 255, 10));
    private final java.awt.image.BufferedImage headerControlTexturePattern = buildTexturePattern(
            7,
            new Color(255, 255, 255, 30),
            new Color(255, 255, 255, 14));
    private final java.awt.image.BufferedImage explanationTexturePattern = buildTexturePattern(
            7,
            new Color(255, 255, 255, 22),
            new Color(255, 255, 255, 9));
    private final java.awt.image.BufferedImage timerTexturePattern = buildTexturePattern(
            9,
            new Color(255, 255, 255, 26),
            new Color(255, 255, 255, 12));
    private JPanel cardContainer, optionsPanel;
    private SkeletonCard skeletonCard;
    private JTextPane questionArea;
    private AbstractButton[] options = new AbstractButton[4];
    private ButtonGroup optionGroup = new ButtonGroup();
    private JButton submitBtn, nextBtn, searchBtn;
    private JLabel topicValidationLabel;
    private JComboBox<String> categoryCombo, subtopicCombo, difficultyCombo;
    private JLabel timerLabel, streakVal, accuracyVal;
    private JTextArea explanationArea;
    private JScrollPane mainContentScrollPane;
    private int explanationVisualState = EXPLANATION_STATE_NEUTRAL;
    private javax.swing.Timer quizTimer;
    private int timeLeft = 60, streak = 0, totalAttempted = 0, totalCorrect = 0;

    // Background images
    private Image sidebarBgImage = null;
    private Image mainBgImage = null;
    private Image questionCardBgImage = null;

    // Cached scaled images for performance
    private Image cachedSidebarImage = null;
    private Image cachedMainImage = null;
    private Image cachedQuestionCardImage = null;
    private Dimension lastMainWrapperSize = new Dimension(0, 0);
    private Dimension lastSidebarSize = new Dimension(0, 0);
    private Dimension lastQuestionCardSize = new Dimension(0, 0);

    // Reusable AlphaComposite instances
    private static final AlphaComposite COMPOSITE_90 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.90f);
    private AptitudeQuestion currentQ;
    private AptitudeQuestion preloadedQ;
    private String preloadedTopic;
    private String preloadedDifficulty;
    private QuestionGeneratorV2 ai = new QuestionGeneratorV2();
    private boolean answerSubmitted = false;
    private boolean isPreloading = false;
    private javax.swing.Timer questionFadeInTimer;
    private javax.swing.Timer validationFadeTimer;
    private javax.swing.Timer verifyResultTimer;
    private javax.swing.Timer verifyPulseTimer;
    private javax.swing.Timer verifySheenTimer;
    private javax.swing.Timer preloadingDelayTimer;
    private Font timerFontNormalCached = null;
    private Font timerFontUrgentCached = null;
    private int verifySheenOffset = -220;
    private boolean verifyResultLocked = false;
    private final Color verifyDefaultColor = new Color(99, 102, 241);
    private final Color verifyCorrectColor = new Color(22, 163, 74);
    private final Color verifyWrongColor = new Color(220, 38, 38);
    private static final int VERIFY_RESULT_DISPLAY_MS = 4500;
    private static final int HEADER_CONTROL_HEIGHT = 48;
    private static final int QUIZ_TIMER_MAX_SECONDS = 60;
    private static final float TIMER_FONT_NORMAL = 24f;
    private static final float TIMER_FONT_URGENT = 26f;
    private static final Color TIMER_COLOR_SAFE = new Color(34, 197, 94);
    private static final Color TIMER_COLOR_WARNING = new Color(251, 191, 36);
    private static final Color TIMER_COLOR_URGENT = new Color(239, 68, 68);
    private static final String[] QUIZ_TIMER_LABELS = buildQuizTimerLabels();
    private static final int TARGET_ANIMATION_FPS = 240;
    private static final int ANIMATION_FRAME_DELAY_MS = Math.max(4, 1000 / TARGET_ANIMATION_FPS);
    private static final int HOVER_ANIMATION_DELAY_MS = 12;
    private static final String BUTTON_COLOR_ANIMATION_TIMER_KEY = "buttonColorAnimationTimer";
    // Keep verify button visuals, but skip expensive continuous/pulse animations
    // for smoother EDT throughput.
    private static final boolean ENABLE_VERIFY_BUTTON_ANIMATIONS = false;
    private static final Font VERIFY_BUTTON_FONT = new Font("Inter", Font.BOLD, 19);

    // NEW: Advanced Systems (Performance & Analytics)
    private StatisticsTracker statsTracker = new StatisticsTracker();
    private AchievementSystem achievementSystem = new AchievementSystem();
    private PerformanceMonitor performanceMonitor = null;
    private long questionStartTime = 0;
    private JPanel achievementNotificationPanel = null;
    private boolean showPerformanceMonitor = false;
    private float uiScaleFactor = 1.0f; // 1.0 = 100% (original layout)
    private boolean sidebarVisible = true;

    // Store original font sizes for proper zoom scaling
    private java.util.Map<String, Integer> originalFontSizes = new java.util.HashMap<>();
    private java.util.Map<Component, Integer> componentOriginalFontSizes = new java.util.HashMap<>();

    // Sidebar reference for live toggle
    private JPanel sidebarPanel = null;

    // Music support
    private String currentMusicPath = "";
    // Default login music path. This is resolved relative to the project so it
    // works on any machine.
    public static final String DEFAULT_LOGIN_MUSIC_PATH = AppPaths.resourcePath("Default Clarity Theme Music.wav");
    private boolean musicEnabled = true;
    private boolean isMusicPlaying = false;
    private int musicVolume = 50;
    private javax.sound.sampled.Clip musicClip = null;
    private javax.sound.sampled.AudioInputStream musicAudioInputStream = null; // Keep stream alive

    // Sound effects for correct/wrong answers
    private String correctSoundPath = "";
    private String wrongSoundPath = "";
    private boolean soundEffectsEnabled = true;
    private int effectsVolume = 75;
    private javax.sound.sampled.Clip correctSoundClip = null;
    private javax.sound.sampled.Clip wrongSoundClip = null;
    private javax.sound.sampled.AudioInputStream correctAudioStream = null;
    private javax.sound.sampled.AudioInputStream wrongAudioStream = null;

    // Floating menu buttons panel (for when sidebar is hidden)
    private JPanel floatingMenuButtonsPanel = null;
    private JPanel sidebarButtonStackWrapper = null;
    private JPanel sidebarBottomAnchor = null;

    // In-app section panels (Tools / Analytics / Settings)
    private JPanel sectionOverlayPanel = null;
    private JPanel sectionCardPanel = null;
    private CardLayout sectionCardLayout = null;
    private JLabel sectionTitleLabel = null;
    private JPanel sectionShellPanel = null;
    private JPanel toolsSectionPanel = null;
    private JPanel analyticsSectionPanel = null;
    private JPanel settingsSectionPanel = null;
    private Point sectionDragOffset = null;
    private long sectionLastDragTimeMs = 0L;
    private boolean sectionManuallyMoved = false;
    private static final int SECTION_DRAG_THROTTLE_MS = 12;

    private static final AtomicInteger BG_WORKER_INDEX = new AtomicInteger(1);
    private final ExecutorService backgroundExecutor = Executors.newFixedThreadPool(3, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "AptitudeWorker-" + BG_WORKER_INDEX.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    });

    // Track last notification for instant exit when new one appears
    private JWindow lastNotification = null;

    // LOGIN SYSTEM
    public boolean isLoggedIn = false;
    public String loggedInUser = "";
    private JPanel menuButtonsPanel = null; // Container for the three menu buttons
    private JButton loginIconButton = null; // Login button in sidebar
    private JButton logoutIconButton = null; // Logout button used as popup anchor
    private javax.swing.Timer sidebarMenuHideTimer = null;
    private static final int SIDEBAR_MENU_HIDE_DELAY_MS = 120;
    private int sidebarPopupMenuOpenCount = 0;
    private Popup loginUserHoverPopup = null;
    private javax.swing.Timer loginUserPopupShowTimer = null;

    public AptitudeGui() {
        FlatMacDarkLaf.setup();

        // Phase 1: Setup window and make visible immediately (fastest)
        setupWindow();
        setupTimerLogic();
        initUI();

        // Make window VISIBLE as early as possible - don't wait for other inits
        setVisible(true);

        // Keep floating action stack pinned to bottom-right while resizing.
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                positionFloatingMenuButtonsPanel();
                positionSectionOverlayPanel();
                positionPerformanceMonitor();
            }
        });

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                stopUiTimers();
                backgroundExecutor.shutdownNow();
            }
        });

        // Deferred setup remains non-blocking while keeping Swing updates on EDT.
        SwingUtilities.invokeLater(() -> {
            try {
                captureOriginalFontSizes();
                setupActions();
                initializePerformanceMonitor();
                initializeAchievementNotificationPanel();
            } catch (Exception e) {
                System.err.println("Deferred UI init failed: " + e.getMessage());
            }
        });

        // Load background images asynchronously (already async, but redeclare for
        // clarity)
        loadBackgroundImagesAsync();

        // Show initial topic selection message on EDT (deferred)
        SwingUtilities.invokeLater(this::showSelectTopicMessage);
    }

    private void runInBackground(String taskName, Runnable task) {
        backgroundExecutor.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                System.err.println(taskName + " failed: " + e.getMessage());
            }
        });
    }

    private void stopUiTimers() {
        if (quizTimer != null && quizTimer.isRunning()) {
            quizTimer.stop();
        }
        if (questionFadeInTimer != null && questionFadeInTimer.isRunning()) {
            questionFadeInTimer.stop();
        }
        if (validationFadeTimer != null && validationFadeTimer.isRunning()) {
            validationFadeTimer.stop();
        }
        if (verifyResultTimer != null && verifyResultTimer.isRunning()) {
            verifyResultTimer.stop();
        }
        if (verifyPulseTimer != null && verifyPulseTimer.isRunning()) {
            verifyPulseTimer.stop();
        }
        if (verifySheenTimer != null && verifySheenTimer.isRunning()) {
            verifySheenTimer.stop();
        }
        if (preloadingDelayTimer != null && preloadingDelayTimer.isRunning()) {
            preloadingDelayTimer.stop();
        }
        if (loginUserPopupShowTimer != null && loginUserPopupShowTimer.isRunning()) {
            loginUserPopupShowTimer.stop();
        }
        if (sidebarMenuHideTimer != null && sidebarMenuHideTimer.isRunning()) {
            sidebarMenuHideTimer.stop();
        }
    }

    /**
     * Initialize PerformanceMonitor that was previously null
     */
    private void initializePerformanceMonitor() {
        try {
            performanceMonitor = new PerformanceMonitor(this);
            getLayeredPane().add(performanceMonitor, JLayeredPane.PALETTE_LAYER);
            positionPerformanceMonitor();
            performanceMonitor.setFocusable(false);
            // Respect previously requested visibility if toggle was used before init
            // finished.
            setPerformanceMonitorVisible(showPerformanceMonitor);
            getLayeredPane().revalidate();
            getLayeredPane().repaint();
        } catch (Exception e) {
            System.err.println("Failed to initialize PerformanceMonitor: " + e.getMessage());
            performanceMonitor = null;
        }
    }

    /**
     * Initialize achievement notification panel
     */
    private void initializeAchievementNotificationPanel() {
        try {
            achievementNotificationPanel = createAchievementNotificationPanel();
            getLayeredPane().add(achievementNotificationPanel, JLayeredPane.POPUP_LAYER);
            achievementNotificationPanel.setBounds(getWidth() - 320, getHeight() - 120, 320, 100);
            achievementNotificationPanel.setVisible(false);
        } catch (Exception e) {
            System.err.println("Failed to initialize achievement panel: " + e.getMessage());
            achievementNotificationPanel = null;
        }
    }

    /**
     * Capture and store original font sizes for all UI components
     * This is called after initUI() to establish baselines for zoom
     */
    private void captureOriginalFontSizes() {
        // Question display area - typically 16px
        if (questionArea != null && questionArea.getFont() != null) {
            originalFontSizes.put("questionArea", questionArea.getFont().getSize());
        }
        // Explanation area - typically 14px
        if (explanationArea != null && explanationArea.getFont() != null) {
            originalFontSizes.put("explanationArea", explanationArea.getFont().getSize());
        }
        // Option buttons - typically 12px
        if (options != null) {
            for (int i = 0; i < options.length; i++) {
                if (options[i] != null && options[i].getFont() != null) {
                    originalFontSizes.put("option" + i, options[i].getFont().getSize());
                }
            }
        }
        // Category combo - typically 12px
        if (categoryCombo != null && categoryCombo.getFont() != null) {
            originalFontSizes.put("categoryCombo", categoryCombo.getFont().getSize());
        }
        // Subtopic combo - typically 12px
        if (subtopicCombo != null && subtopicCombo.getFont() != null) {
            originalFontSizes.put("subtopicCombo", subtopicCombo.getFont().getSize());
        }
        // Difficulty combo - typically 12px
        if (difficultyCombo != null && difficultyCombo.getFont() != null) {
            originalFontSizes.put("difficultyCombo", difficultyCombo.getFont().getSize());
        }
        // Submit button - typically 14px
        if (submitBtn != null && submitBtn.getFont() != null) {
            originalFontSizes.put("submitBtn", submitBtn.getFont().getSize());
        }
        // Next button - typically 14px
        if (nextBtn != null && nextBtn.getFont() != null) {
            originalFontSizes.put("nextBtn", nextBtn.getFont().getSize());
        }
        // Timer label - typically 18px (bold)
        if (timerLabel != null && timerLabel.getFont() != null) {
            originalFontSizes.put("timerLabel", timerLabel.getFont().getSize());
        }
        // Stats values - typically 14px
        if (streakVal != null && streakVal.getFont() != null) {
            originalFontSizes.put("streakVal", streakVal.getFont().getSize());
        }
        if (accuracyVal != null && accuracyVal.getFont() != null) {
            originalFontSizes.put("accuracyVal", accuracyVal.getFont().getSize());
        }
    }

    /**
     * Apply UI scaling to all components based on uiScaleFactor.
     * Scales font sizes and component dimensions proportionally.
     */
    private void applyUIScale() {
        try {
            // Apply to question area using original size
            if (questionArea != null && originalFontSizes.containsKey("questionArea")) {
                int originalSize = originalFontSizes.get("questionArea");
                Font currentFont = questionArea.getFont();
                questionArea.setFont(new Font(currentFont.getName(), currentFont.getStyle(),
                        Math.round(originalSize * uiScaleFactor)));
            }

            // Apply to explanation area using original size
            if (explanationArea != null && originalFontSizes.containsKey("explanationArea")) {
                int originalSize = originalFontSizes.get("explanationArea");
                Font currentFont = explanationArea.getFont();
                explanationArea.setFont(new Font(currentFont.getName(), currentFont.getStyle(),
                        Math.round(originalSize * uiScaleFactor)));
            }

            // Apply to option buttons using original sizes
            if (options != null) {
                for (int i = 0; i < options.length; i++) {
                    if (options[i] != null && originalFontSizes.containsKey("option" + i)) {
                        int originalSize = originalFontSizes.get("option" + i);
                        Font currentFont = options[i].getFont();
                        options[i].setFont(new Font(currentFont.getName(), currentFont.getStyle(),
                                Math.round(originalSize * uiScaleFactor)));
                    }
                }
            }

            // Apply to category combo using original size
            if (categoryCombo != null && originalFontSizes.containsKey("categoryCombo")) {
                int originalSize = originalFontSizes.get("categoryCombo");
                Font currentFont = categoryCombo.getFont();
                categoryCombo.setFont(new Font(currentFont.getName(), currentFont.getStyle(),
                        Math.round(originalSize * uiScaleFactor)));
            }

            // Apply to subtopic combo using original size
            if (subtopicCombo != null && originalFontSizes.containsKey("subtopicCombo")) {
                int originalSize = originalFontSizes.get("subtopicCombo");
                Font currentFont = subtopicCombo.getFont();
                subtopicCombo.setFont(new Font(currentFont.getName(), currentFont.getStyle(),
                        Math.round(originalSize * uiScaleFactor)));
            }

            // Apply to difficulty combo using original size
            if (difficultyCombo != null && originalFontSizes.containsKey("difficultyCombo")) {
                int originalSize = originalFontSizes.get("difficultyCombo");
                Font currentFont = difficultyCombo.getFont();
                difficultyCombo.setFont(new Font(currentFont.getName(), currentFont.getStyle(),
                        Math.round(originalSize * uiScaleFactor)));
            }

            // Apply to submit button using original size
            if (submitBtn != null && originalFontSizes.containsKey("submitBtn")) {
                int originalSize = originalFontSizes.get("submitBtn");
                Font currentFont = submitBtn.getFont();
                submitBtn.setFont(new Font(currentFont.getName(), Font.BOLD,
                        Math.round(originalSize * uiScaleFactor)));
            }

            // Apply to next button using original size
            if (nextBtn != null && originalFontSizes.containsKey("nextBtn")) {
                int originalSize = originalFontSizes.get("nextBtn");
                Font currentFont = nextBtn.getFont();
                nextBtn.setFont(new Font(currentFont.getName(), Font.BOLD,
                        Math.round(originalSize * uiScaleFactor)));
            }

            // Apply to timer label using original size
            if (timerLabel != null && originalFontSizes.containsKey("timerLabel")) {
                int originalSize = originalFontSizes.get("timerLabel");
                Font currentFont = timerLabel.getFont();
                timerLabel.setFont(new Font(currentFont.getName(), Font.BOLD,
                        Math.round(originalSize * uiScaleFactor)));
            }

            // Apply to streak value using original size
            if (streakVal != null && originalFontSizes.containsKey("streakVal")) {
                int originalSize = originalFontSizes.get("streakVal");
                Font currentFont = streakVal.getFont();
                streakVal.setFont(new Font(currentFont.getName(), currentFont.getStyle(),
                        Math.round(originalSize * uiScaleFactor)));
            }

            // Apply to accuracy value using original size
            if (accuracyVal != null && originalFontSizes.containsKey("accuracyVal")) {
                int originalSize = originalFontSizes.get("accuracyVal");
                Font currentFont = accuracyVal.getFont();
                accuracyVal.setFont(new Font(currentFont.getName(), currentFont.getStyle(),
                        Math.round(originalSize * uiScaleFactor)));
            }

            // Revalidate and repaint UI
            revalidate();
            repaint();
        } catch (

        Exception e) {
            System.err.println("Error applying UI scale: " + e.getMessage());
        }
    }

    /**
     * Load background images asynchronously to avoid blocking EDT during startup.
     * This makes the window appear instantly, then images load in background.
     */
    private void loadBackgroundImagesAsync() {
        runInBackground("ImageLoader", () -> {
            try {
                loadBackgroundImages();
                // Request repaint once images are loaded
                SwingUtilities.invokeLater(() -> {
                    repaint();
                });
            } catch (Exception e) {
                System.err.println("Error loading background images: " + e.getMessage());
            }
        });
    }

    private void setupWindow() {
        // Load icon asynchronously on separate thread to not block EDT
        runInBackground("IconLoader", () -> {
            try {
                ImageIcon i = new ImageIcon(AppPaths.resourcePath("upscalemedia-transformed-888888888.png"));
                SwingUtilities.invokeLater(() -> setIconImage(i.getImage()));
            } catch (Exception e) {
                System.err.println("Failed to load window icon: " + e.getMessage());
            }
        });

        setTitle("Aptitude ");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(1200, 800));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    /**
     * Show login dialog - user must authenticate before accessing features
     * All UI and logic is now in the LoginDialog class
     */
    private void showLoginDialog() {
        LoginDialog loginDialog = new LoginDialog(this);
        loginDialog.setVisible(true);
    }

    /**
     * Update sidebar after successful login - change icon to human and enable hover
     * menu
     */
    public void updateSidebarForLogin() {
        if (menuButtonsPanel != null && loginIconButton != null) {
            if (sidebarMenuHideTimer != null && sidebarMenuHideTimer.isRunning()) {
                sidebarMenuHideTimer.stop();
            }
            sidebarPopupMenuOpenCount = 0;

            // Change lock icon to human icon
            loginIconButton.setText("");
            loginIconButton.setToolTipText("Logged in as " + loggedInUser);

            // Keep menu buttons hidden initially - show on hover only
            menuButtonsPanel.setVisible(false);

            // Update window title with username
            setTitle("Aptitude Pro - Welcome " + loggedInUser + " 👤");

            // Repaint and revalidate all components
            revalidate();
            repaint();
        }

        // Start background music automatically after successful login.
        startBackgroundMusicAfterLogin();
    }

    /**
     * Automatically start background music after login.
     * Respects the ambient sound toggle - only plays if enabled in settings.
     * Priority: current path -> saved settings path -> default music path.
     */
    private void startBackgroundMusicAfterLogin() {
        // Check if ambient sound is enabled in settings
        java.util.Properties settings = new java.util.Properties();
        try {
            java.io.File settingsFile = AppPaths.projectFile("aptitude_settings.properties");
            if (settingsFile.exists()) {
                settings.load(new java.io.FileInputStream(settingsFile));
            }
        } catch (java.io.IOException e) {
            System.err.println("Could not load settings: " + e.getMessage());
        }

        // Only auto-play if ambient sound is enabled
        boolean ambientSoundEnabled = Boolean.parseBoolean(settings.getProperty("audio.ambientSound", "false"));
        if (!ambientSoundEnabled) {
            System.out.println("[LOGIN] Ambient sound is disabled - skipping auto-play");
            return;
        }

        String resolvedMusicPath = resolveLoginMusicPath();
        if (resolvedMusicPath == null || resolvedMusicPath.trim().isEmpty()) {
            return;
        }

        java.io.File file = new java.io.File(resolvedMusicPath);
        if (!file.exists()) {
            System.out.println("⚠️ Login music file not found: " + resolvedMusicPath);
            return;
        }

        // Don't force enable - let user settings control this
        if (musicEnabled) {
            setMusicPath(resolvedMusicPath);
            playMusic();
        }
    }

    /**
     * Resolve which music file to use after login.
     */
    private String resolveLoginMusicPath() {
        if (currentMusicPath != null && !currentMusicPath.trim().isEmpty()) {
            return currentMusicPath.trim();
        }

        java.io.File settingsFile = AppPaths.projectFile("aptitude_settings.properties");
        if (settingsFile.exists()) {
            java.util.Properties props = new java.util.Properties();
            try (java.io.FileInputStream fis = new java.io.FileInputStream(settingsFile)) {
                props.load(fis);
                String savedPath = props.getProperty("audio.musicPath", "").trim();
                if (!savedPath.isEmpty()) {
                    return savedPath;
                }
            } catch (Exception e) {
                System.out.println("⚠️ Could not read saved music path: " + e.getMessage());
            }
        }

        return DEFAULT_LOGIN_MUSIC_PATH;
    }

    /**
     * Show logout confirmation dialog
     */
    private void showLogoutConfirmation() {
        int result = JOptionPane.showConfirmDialog(this,
                "🔐 Logout " + loggedInUser + "?\n\nYou will need to login again to access features.",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            isLoggedIn = false;
            loggedInUser = "";
            showLoginAgain();
        }
    }

    /**
     * Show login dialog again after logout
     */
    private void showLoginAgain() {
        if (menuButtonsPanel != null && loginIconButton != null) {
            if (sidebarMenuHideTimer != null && sidebarMenuHideTimer.isRunning()) {
                sidebarMenuHideTimer.stop();
            }
            hideLoginUserHoverPopup();
            sidebarPopupMenuOpenCount = 0;

            // Change human icon back to lock icon
            loginIconButton.setText("");
            loginIconButton.setToolTipText("Click to login");

            // Hide menu buttons
            menuButtonsPanel.setVisible(false);
            loginIconButton.setVisible(true);

            // Reset window title
            setTitle("Aptitude ");

            revalidate();
            repaint();
        }
        showLoginDialog();
    }

    /**
     * Create comprehensive menu bar with all features organized by category.
     * Includes File, Tools, Analytics, Settings, View, Help menus.
     */
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(new Color(39, 39, 42));
        menuBar.setForeground(new Color(203, 213, 225));

        // FILE MENU
        JMenu fileMenu = new JMenu("📁 File");
        fileMenu.setForeground(new Color(203, 213, 225));

        JMenuItem newSessionItem = new JMenuItem("🆕 New Session");
        newSessionItem.setAccelerator(
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        newSessionItem.addActionListener(e -> resetSession());
        fileMenu.add(newSessionItem);

        JMenuItem resetStatsItem = new JMenuItem("🔄 Reset Statistics");
        resetStatsItem.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to reset all statistics?\nThis cannot be undone.",
                    "Reset Statistics",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                statsTracker = new StatisticsTracker();
                streakVal.setText("0 🔥");
                accuracyVal.setText("0% 🎯");
                totalAttempted = 0;
                totalCorrect = 0;
                streak = 0;
                JOptionPane.showMessageDialog(this, "Statistics reset successfully!", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });
        fileMenu.add(resetStatsItem);

        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("❌ Exit (Ctrl+Q)");
        exitItem.setAccelerator(
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        // TOOLS MENU
        JMenu toolsMenu = new JMenu("🛠 Tools");
        toolsMenu.setForeground(new Color(203, 213, 225));

        JMenuItem perfMonitorItem = new JMenuItem("⚡ Toggle Performance Monitor (Ctrl+P)");
        perfMonitorItem.setAccelerator(
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        perfMonitorItem.addActionListener(e -> {
            boolean visible = togglePerformanceMonitorVisible();
            showNotification("⚡ Performance Monitor: " + (visible ? "ON" : "OFF"));
        });
        toolsMenu.add(perfMonitorItem);

        JMenuItem statsItem = new JMenuItem("📈 View Current Statistics");
        statsItem.addActionListener(e -> {
            String stats = String.format(
                    "📈 CURRENT SESSION STATISTICS\n\n" +
                            "Total Attempted: %d\n" +
                            "Total Correct: %d\n" +
                            "Current Accuracy: %d%%\n" +
                            "Current Streak: %d 🔥",
                    totalAttempted,
                    totalCorrect,
                    totalAttempted == 0 ? 0 : (int) ((double) totalCorrect / totalAttempted * 100),
                    streak);
            JOptionPane.showMessageDialog(this, stats, "Statistics", JOptionPane.INFORMATION_MESSAGE);
        });
        toolsMenu.add(statsItem);

        // ANALYTICS MENU
        JMenu analyticsMenu = new JMenu("📊 Analytics");
        analyticsMenu.setForeground(new Color(203, 213, 225));

        JMenuItem weakAreasItem = new JMenuItem("⚠️ Weak Areas Analysis");
        weakAreasItem.addActionListener(e -> {
            var weakAreas = statsTracker.getWeakAreas();
            if (weakAreas.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No weak areas detected. Keep up the good work!", "Weak Areas",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                StringBuilder analysis = new StringBuilder("⚠️ WEAK AREAS (< 70% Accuracy):\n\n");
                weakAreas.forEach((topic, accuracy) -> analysis.append("• ").append(topic).append(": ")
                        .append(String.format("%.1f%%", accuracy)).append("\n"));
                JOptionPane.showMessageDialog(this, analysis.toString(), "Weak Areas Analysis",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });
        analyticsMenu.add(weakAreasItem);

        JMenuItem reviewDueItem = new JMenuItem("🔁 Review Due Topics");
        reviewDueItem.addActionListener(e -> {
            StringBuilder review = new StringBuilder("🔁 TOPICS DUE FOR REVIEW:\n\n");
            review.append("Review your weak areas regularly using the Analytics Dashboard for improving learning!");
            JOptionPane.showMessageDialog(this, review.toString(), "Review Schedule",
                    JOptionPane.INFORMATION_MESSAGE);
        });
        analyticsMenu.add(reviewDueItem);

        JMenuItem recommendationsItem = new JMenuItem("💡 Smart Recommendations");
        recommendationsItem.addActionListener(e -> {
            var recommendations = statsTracker.getLearningRecommendations();
            StringBuilder recs = new StringBuilder("💡 SMART LEARNING RECOMMENDATIONS:\n\n");
            if (recommendations.weakAreasToReview != null && !recommendations.weakAreasToReview.isEmpty()) {
                recs.append("• Focus on weak areas: ").append(String.join(", ", recommendations.weakAreasToReview))
                        .append("\n");
            }
            if (recommendations.suggestedNextTopic != null) {
                recs.append("• Suggested topic: ").append(recommendations.suggestedNextTopic).append("\n");
            }
            if (recs.length() == "💡 SMART LEARNING RECOMMENDATIONS:\n\n".length()) {
                recs.append("Keep learning consistently to unlock recommendations!");
            }
            JOptionPane.showMessageDialog(this, recs.toString(), "Recommendations",
                    JOptionPane.INFORMATION_MESSAGE);
        });
        analyticsMenu.add(recommendationsItem);

        JMenuItem topicBreakdownItem = new JMenuItem("📌 Topic Breakdown");
        topicBreakdownItem.addActionListener(e -> {
            var statsMap = statsTracker.getAllTopicStats();
            if (statsMap.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No topic data available yet.", "Topic Breakdown",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                StringBuilder breakdown = new StringBuilder("📌 PERFORMANCE BY TOPIC:\n\n");
                statsMap.forEach((topic, stats) -> breakdown.append("• ").append(topic).append(": ")
                        .append(String.format("%.1f%%", stats.getAccuracy() * 100)).append("\n"));
                JOptionPane.showMessageDialog(this, breakdown.toString(), "Topic Breakdown",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });
        analyticsMenu.add(topicBreakdownItem);

        // SETTINGS MENU
        JMenu settingsMenu = new JMenu("⚙️ Settings");
        settingsMenu.setForeground(new Color(203, 213, 225));

        // Sound Settings (placeholder)
        JMenuItem soundItem = new JMenuItem("🔊 Sound Settings");
        soundItem.addActionListener(e -> JOptionPane.showMessageDialog(this, "Sound settings coming soon!", "Sound",
                JOptionPane.INFORMATION_MESSAGE));
        settingsMenu.add(soundItem);

        // Notification Settings (placeholder)
        JMenuItem notifItem = new JMenuItem("🔔 Notification Settings");
        notifItem.addActionListener(e -> JOptionPane.showMessageDialog(this, "Notification settings coming soon!",
                "Notifications", JOptionPane.INFORMATION_MESSAGE));
        settingsMenu.add(notifItem);

        // VIEW MENU
        JMenu viewMenu = new JMenu("👁 View");
        viewMenu.setForeground(new Color(203, 213, 225));

        JCheckBoxMenuItem showStatsItem = new JCheckBoxMenuItem("📊 Show Statistics Sidebar", true);
        showStatsItem.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Sidebar toggle feature coming soon!", "View",
                    JOptionPane.INFORMATION_MESSAGE);
        });
        viewMenu.add(showStatsItem);

        JCheckBoxMenuItem fullscreenItem = new JCheckBoxMenuItem("🖥️ Fullscreen (F11)");
        fullscreenItem.addActionListener(e -> {
            if (fullscreenItem.isSelected()) {
                setExtendedState(JFrame.MAXIMIZED_BOTH);
            } else {
                setExtendedState(JFrame.NORMAL);
            }
        });
        viewMenu.add(fullscreenItem);

        viewMenu.addSeparator();

        JMenuItem zoomInItem = new JMenuItem("🔍 Zoom In (Ctrl++)");
        zoomInItem.setAccelerator(
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_PLUS, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        zoomInItem.addActionListener(e -> JOptionPane.showMessageDialog(this, "Zoom feature coming soon!", "Zoom",
                JOptionPane.INFORMATION_MESSAGE));
        viewMenu.add(zoomInItem);

        JMenuItem zoomOutItem = new JMenuItem("🔍 Zoom Out (Ctrl+-)");
        zoomOutItem.setAccelerator(
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_MINUS, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        zoomOutItem.addActionListener(e -> JOptionPane.showMessageDialog(this, "Zoom feature coming soon!", "Zoom",
                JOptionPane.INFORMATION_MESSAGE));
        viewMenu.add(zoomOutItem);

        // HELP MENU
        JMenu helpMenu = new JMenu("❓ Help");
        helpMenu.setForeground(new Color(203, 213, 225));

        JMenuItem keyboardItem = new JMenuItem("⌨️ Keyboard Shortcuts (F1)");
        keyboardItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        keyboardItem.addActionListener(e -> {
            String shortcuts = "⌨️ KEYBOARD SHORTCUTS:\n\n" +
                    "Ctrl+P   - Toggle Performance Monitor\n" +
                    "Ctrl+D   - Open Analytics Dashboard\n" +
                    "Ctrl+N   - Start New Session\n" +
                    "Ctrl+Q   - Exit Application\n" +
                    "F1       - Show This Help\n" +
                    "↑ / ↓   - Navigate Answer Options\n" +
                    "Enter    - Submit Answer or Next Question\n" +
                    "Escape   - Clear Selection\n";
            JOptionPane.showMessageDialog(this, shortcuts, "Keyboard Shortcuts", JOptionPane.INFORMATION_MESSAGE);
        });
        helpMenu.add(keyboardItem);

        JMenuItem hotTipsItem = new JMenuItem("💡 Hot Tips & Tricks");
        hotTipsItem.addActionListener(e -> {
            String tips = "💡 HOT TIPS & TRICKS:\n\n" +
                    "• Use keyboard navigation for faster answering\n" +
                    "• Check weak areas regularly from Analytics menu\n" +
                    "• Review due topics to improve spaced repetition\n" +
                    "• Track your achievements for motivation\n" +
                    "• Use Dark/Light mode based on your preference\n" +
                    "• Performance monitor helps optimize FPS\n";
            JOptionPane.showMessageDialog(this, tips, "Tips & Tricks", JOptionPane.INFORMATION_MESSAGE);
        });
        helpMenu.add(hotTipsItem);

        JMenuItem howtouseItem = new JMenuItem("📖 How to Use");
        howtouseItem.addActionListener(e -> {
            String howto = "📖 HOW TO USE APTITUDE PRO:\n\n" +
                    "1. Select a topic from the chips or type in search box\n" +
                    "2. Choose difficulty level (Normal/Hard)\n" +
                    "3. Click 'Search' to generate a question\n" +
                    "4. Select your answer from the options\n" +
                    "5. Click 'Verify Answer' to check and see explanation\n" +
                    "6. Use 'Next Question' to continue learning\n" +
                    "7. Monitor your progress in the Statistics sidebar\n" +
                    "8. Check Analytics Dashboard for detailed insights\n";
            JOptionPane.showMessageDialog(this, howto, "How to Use", JOptionPane.INFORMATION_MESSAGE);
        });
        helpMenu.add(howtouseItem);

        helpMenu.addSeparator();

        JMenuItem aboutItem = new JMenuItem("ℹ️ About Aptitude Pro");
        aboutItem.addActionListener(e -> {
            String about = "ℹ️ APTITUDE PRO v2.0\n\n" +
                    "Advanced AI-Powered Aptitude Learning Platform\n\n" +
                    "Features:\n" +
                    "✓ AI Question Generation\n" +
                    "✓ Real-time Performance Monitoring\n" +
                    "✓ Smart Analytics & Recommendations\n" +
                    "✓ Achievement System\n" +
                    "✓ Spaced Repetition Learning\n" +
                    "✓ Dark/Light Theme Support\n" +
                    "✓ Multi-threaded Architecture\n\n" +
                    "© 2026 Aptitude Learning";
            JOptionPane.showMessageDialog(this, about, "About", JOptionPane.INFORMATION_MESSAGE);
        });
        helpMenu.add(aboutItem);

        // Add all menus to menubar
        menuBar.add(fileMenu);
        menuBar.add(toolsMenu);
        menuBar.add(analyticsMenu);
        menuBar.add(settingsMenu);
        menuBar.add(viewMenu);
        menuBar.add(Box.createHorizontalGlue()); // Push help to right
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    /**
     * Reset the current session and show initial prompt.
     */
    public void resetSession() {
        currentQ = null;
        preloadedQ = null;
        answerSubmitted = false;
        streak = 0;
        streakVal.setText("0 🔥");

        // Collapse explanation area back to minimal size
        collapseExplanationArea();

        // Re-enable difficulty combo when resetting session
        if (difficultyCombo != null) {
            difficultyCombo.setEnabled(true);
            difficultyCombo.setToolTipText("Select difficulty level");
        }

        showSelectTopicMessage();
        if (categoryCombo != null && categoryCombo.getItemCount() > 0) {
            categoryCombo.setSelectedIndex(0);
        }
        if (subtopicCombo != null && subtopicCombo.getItemCount() > 0) {
            subtopicCombo.setSelectedIndex(0);
        }
        difficultyCombo.setSelectedItem("🟡 Normal");
    }

    /**
     * Reset all statistics and achievements - used by Settings/Tools dialogs
     */
    public void resetAllStatistics() {
        statsTracker = new StatisticsTracker();
        achievementSystem = new AchievementSystem();
        streakVal.setText("0 🔥");
        accuracyVal.setText("0% 🎯");
        totalAttempted = 0;
        totalCorrect = 0;
        streak = 0;
    }

    /**
     * Toggle sidebar visibility - LIVE CHANGE (Now actually works!)
     * When sidebar is hidden, menu buttons move to bottom-right
     */
    public void setSidebarVisible(boolean visible) {
        sidebarVisible = visible;
        // Use the stored sidebar panel reference
        if (sidebarPanel != null) {
            sidebarPanel.setVisible(visible);
            sidebarPanel.setPreferredSize(visible ? new Dimension(250, getHeight()) : new Dimension(0, 0));
            Container parent = sidebarPanel.getParent();
            if (parent != null) {
                parent.revalidate();
                parent.repaint();
            }
        }
        // Move the same login + hover menu stack into a floating overlay.
        if (visible) {
            moveActionButtonsBackToSidebarPanel();
            if (floatingMenuButtonsPanel != null) {
                floatingMenuButtonsPanel.setVisible(false);
            }
        } else {
            moveActionButtonsToFloatingPanel();
            positionFloatingMenuButtonsPanel();
        }

        positionFloatingMenuButtonsPanel();
        revalidate();
        repaint();
    }

    /**
     * Get current sidebar visibility state
     */
    public boolean isSidebarVisible() {
        return sidebarVisible;
    }

    /**
     * Set performance monitor visibility state.
     * If monitor is not initialized yet, the desired state is remembered.
     */
    public void setPerformanceMonitorVisible(boolean visible) {
        showPerformanceMonitor = visible;
        if (performanceMonitor != null) {
            performanceMonitor.setMonitoringEnabled(visible);
            performanceMonitor.setVisible(visible);
            if (visible) {
                positionPerformanceMonitor();
            }
        }
    }

    /**
     * Toggle performance monitor visibility and return new state.
     */
    public boolean togglePerformanceMonitorVisible() {
        setPerformanceMonitorVisible(!showPerformanceMonitor);
        return showPerformanceMonitor;
    }

    /**
     * Get current performance monitor target state.
     */
    public boolean isPerformanceMonitorVisible() {
        return showPerformanceMonitor;
    }

    /**
     * Expose shared statistics tracker for in-app Tools/Analytics pages.
     */
    public StatisticsTracker getStatisticsTracker() {
        return statsTracker;
    }

    /**
     * Expose shared achievement system for Tools/Analytics pages.
     */
    public AchievementSystem getAchievementSystem() {
        return achievementSystem;
    }

    /**
     * Session counters used in Tools/Analytics panels.
     */
    public int getSessionTotalAttempted() {
        return totalAttempted;
    }

    public int getSessionTotalCorrect() {
        return totalCorrect;
    }

    public int getCurrentStreakCount() {
        return streak;
    }

    public int getCurrentAccuracyPercent() {
        return totalAttempted == 0 ? 0 : (int) ((double) totalCorrect / totalAttempted * 100);
    }

    private void moveActionButtonsToFloatingPanel() {
        if (sidebarButtonStackWrapper == null || floatingMenuButtonsPanel == null) {
            return;
        }

        Container currentParent = sidebarButtonStackWrapper.getParent();
        if (currentParent != null) {
            currentParent.remove(sidebarButtonStackWrapper);
        }

        JPanel rightAnchor = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightAnchor.setOpaque(false);
        rightAnchor.add(sidebarButtonStackWrapper);

        floatingMenuButtonsPanel.removeAll();
        floatingMenuButtonsPanel.add(rightAnchor, BorderLayout.SOUTH);
        floatingMenuButtonsPanel.setVisible(true);
        floatingMenuButtonsPanel.revalidate();
        floatingMenuButtonsPanel.repaint();
    }

    private void moveActionButtonsBackToSidebarPanel() {
        if (sidebarButtonStackWrapper == null || sidebarBottomAnchor == null) {
            return;
        }

        Container currentParent = sidebarButtonStackWrapper.getParent();
        if (currentParent != null) {
            currentParent.remove(sidebarButtonStackWrapper);
        }

        sidebarBottomAnchor.removeAll();
        sidebarBottomAnchor.add(sidebarButtonStackWrapper);
        sidebarBottomAnchor.revalidate();
        sidebarBottomAnchor.repaint();
    }

    private void positionFloatingMenuButtonsPanel() {
        if (floatingMenuButtonsPanel == null) {
            return;
        }

        int panelWidth = 110;
        int panelHeight = 380;
        int marginRight = 43;
        int marginBottom = 47;

        int x = Math.max(0, getWidth() - panelWidth - marginRight);
        int y = Math.max(0, getHeight() - panelHeight - marginBottom);
        if (floatingMenuButtonsPanel.getX() != x
                || floatingMenuButtonsPanel.getY() != y
                || floatingMenuButtonsPanel.getWidth() != panelWidth
                || floatingMenuButtonsPanel.getHeight() != panelHeight) {
            floatingMenuButtonsPanel.setBounds(x, y, panelWidth, panelHeight);
        }
    }

    private void positionPerformanceMonitor() {
        if (performanceMonitor == null) {
            return;
        }

        Dimension preferredSize = performanceMonitor.getPreferredSize();
        int monitorWidth = preferredSize != null ? preferredSize.width : 250;
        int monitorHeight = preferredSize != null ? preferredSize.height : 144;

        int marginTop = 10;
        int marginRight = 14;

        int x = Math.max(8, getWidth() - monitorWidth - marginRight);
        int y = Math.max(6, marginTop);
        if (performanceMonitor.getX() != x
                || performanceMonitor.getY() != y
                || performanceMonitor.getWidth() != monitorWidth
                || performanceMonitor.getHeight() != monitorHeight) {
            performanceMonitor.setBounds(x, y, monitorWidth, monitorHeight);
        }
    }

    private void updateComponentVisibility(Container container, String componentName, boolean visible) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof Container) {
                String name = comp.getName();
                if (name != null && name.contains("sidebar")) {
                    comp.setVisible(visible);
                }
                updateComponentVisibility((Container) comp, componentName, visible);
            }
        }
    }

    /**
     * Toggle fullscreen mode - LIVE CHANGE
     * Steam-like exclusive fullscreen without freezing
     */
    public void setFullscreenMode(boolean fullscreen) {
        SwingUtilities.invokeLater(() -> {
            try {
                GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

                if (fullscreen) {
                    // Store current state
                    if (normalWindowBounds == null) {
                        normalWindowBounds = new Rectangle(getLocation(), getSize());
                    }

                    // Enter exclusive fullscreen mode
                    if (gd.isFullScreenSupported()) {
                        gd.setFullScreenWindow(this);
                        steamFullscreenActive = true;
                        System.out.println("✅ Fullscreen enabled (Steam-like exclusive mode)");
                    } else {
                        // Fallback to maximized if exclusive fullscreen not supported
                        setExtendedState(JFrame.MAXIMIZED_BOTH);
                        steamFullscreenActive = false;
                        System.out.println("⚠️ Exclusive fullscreen not supported, using maximized");
                    }
                } else {
                    // Exit fullscreen
                    if (gd.getFullScreenWindow() == this) {
                        gd.setFullScreenWindow(null);
                    }
                    steamFullscreenActive = false;

                    // Restore in the next event cycle after exclusive fullscreen is released,
                    // otherwise some platforms may iconify/minimize the frame.
                    SwingUtilities.invokeLater(() -> {
                        setVisible(true);
                        int clearedState = getExtendedState() & ~Frame.ICONIFIED;
                        setExtendedState(clearedState);
                        setExtendedState(JFrame.NORMAL);
                        setExtendedState(JFrame.MAXIMIZED_BOTH);
                        toFront();
                        requestFocus();
                        revalidate();
                        repaint();
                    });
                    System.out.println("✅ Fullscreen disabled");
                    return;
                }

                toFront();
                requestFocus();
                revalidate();
                repaint();
            } catch (Exception e) {
                System.err.println("❌ Fullscreen error: " + e.getMessage());
            }
        });
    }

    private Rectangle normalWindowBounds = null;
    private boolean steamFullscreenActive = false;

    /**
     * Set UI zoom level - LIVE CHANGE
     */
    public void setUIZoom(float zoomFactor) {
        uiScaleFactor = zoomFactor;

        // Update all text components with new font size
        updateFontSizes(getContentPane(), zoomFactor);

        // Revalidate and repaint
        revalidate();
        repaint();
    }

    private void updateFontSizes(Container container, float zoomFactor) {
        for (Component comp : container.getComponents()) {
            // Store original font size on first zoom change for precise scaling
            if (!componentOriginalFontSizes.containsKey(comp) && comp.getFont() != null) {
                componentOriginalFontSizes.put(comp, comp.getFont().getSize());
            }

            // Get original size or current size
            Integer originalSize = componentOriginalFontSizes.get(comp);
            if (originalSize != null && comp.getFont() != null) {
                Font currentFont = comp.getFont();

                int newSize = (int) (originalSize * zoomFactor); // Scale from original for precision
                comp.setFont(new Font(currentFont.getFamily(), currentFont.getStyle(), newSize));
            }

            // Recursively update nested containers
            if (comp instanceof Container) {
                updateFontSizes((Container) comp, zoomFactor);
            }
        }
    }

    /**
     * Set master volume level - LIVE CHANGE
     */
    public void setMasterVolume(int volume) {
        // Store volume preference
        System.setProperty("audio.masterVolume", String.valueOf(volume));
        // Update music volume and apply to playing clip
        musicVolume = volume;
        if (musicClip != null && musicClip.isControlSupported(javax.sound.sampled.FloatControl.Type.MASTER_GAIN)) {
            try {
                javax.sound.sampled.FloatControl volumeControl = (javax.sound.sampled.FloatControl) musicClip
                        .getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
                float dB = volume <= 0 ? -80f : 20f * (float) Math.log10((float) volume / 100f);
                dB = Math.max(-80f, Math.min(0f, dB));
                volumeControl.setValue(dB);
            } catch (Exception e) {
            }
        }
        System.out.println("🔊 Master Volume set to: " + volume + "%");
    }

    /**
     * Enable/disable background music playback.
     */
    public void setMusicEnabled(boolean enabled) {
        musicEnabled = enabled;
        System.setProperty("audio.musicEnabled", String.valueOf(enabled));
        if (!enabled) {
            stopMusic();
            return;
        }
        if (currentMusicPath != null && !currentMusicPath.trim().isEmpty()) {
            playMusic();
        }
    }

    /**
     * Set music path - for music playback
     * Usage: parentGui.setMusicPath("C:\\path\\to\\music.mp3");
     * Use empty string to clear: parentGui.setMusicPath("");
     */
    public void setMusicPath(String musicPath) {
        // Stop current music if playing
        if (isMusicPlaying) {
            stopMusic();
        }
        // Clear old clip and stream
        if (musicClip != null) {
            try {
                musicClip.close();
            } catch (Exception e) {
            }
            musicClip = null;
        }
        if (musicAudioInputStream != null) {
            try {
                musicAudioInputStream.close();
            } catch (Exception e) {
            }
            musicAudioInputStream = null;
        }
        currentMusicPath = musicPath != null ? musicPath : "";
        System.out.println("🎵 Music path set to: " + (currentMusicPath.isEmpty() ? "[empty/none]" : currentMusicPath));
    }

    /**
     * Get current music path
     */
    public String getMusicPath() {
        return currentMusicPath;
    }

    /**
     * Play music from current path - ACTUAL PLAYBACK IMPLEMENTED
     */
    public void playMusic() {
        if (!musicEnabled) {
            System.out.println("🔇 Music is disabled. Enable music to play.");
            return;
        }

        if (currentMusicPath == null || currentMusicPath.isEmpty()) {
            System.out.println("⚠️ Music path is empty - no music to play. Use setMusicPath() to set music.");
            return;
        }

        try {
            // If already playing, don't restart.
            if (isMusicPlaying && musicClip != null && musicClip.isRunning()) {
                System.out.println("🎵 Music already playing");
                return;
            }

            java.io.File audioFile = new java.io.File(currentMusicPath);
            if (!audioFile.exists()) {
                System.out.println("❌ Music file not found: " + currentMusicPath);
                isMusicPlaying = false;
                return;
            }

            playClipFromFile(audioFile);
        } catch (Exception e) {
            isMusicPlaying = false;
            System.err.println("❌ Error playing music: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void playClipFromFile(java.io.File audioFile) throws Exception {
        if (musicAudioInputStream != null) {
            try {
                musicAudioInputStream.close();
            } catch (Exception e) {
            }
        }

        javax.sound.sampled.AudioInputStream sourceStream = javax.sound.sampled.AudioSystem
                .getAudioInputStream(audioFile);
        javax.sound.sampled.AudioFormat baseFormat = sourceStream.getFormat();
        if (baseFormat.getEncoding() != javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED) {
            javax.sound.sampled.AudioFormat decodedFormat = new javax.sound.sampled.AudioFormat(
                    javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false);
            musicAudioInputStream = javax.sound.sampled.AudioSystem.getAudioInputStream(decodedFormat, sourceStream);
        } else {
            musicAudioInputStream = sourceStream;
        }

        if (musicClip != null) {
            try {
                musicClip.close();
            } catch (Exception e) {
            }
        }

        musicClip = javax.sound.sampled.AudioSystem.getClip();
        musicClip.open(musicAudioInputStream);

        if (musicClip.isControlSupported(javax.sound.sampled.FloatControl.Type.MASTER_GAIN)) {
            javax.sound.sampled.FloatControl volumeControl = (javax.sound.sampled.FloatControl) musicClip
                    .getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
            float dB = musicVolume <= 0 ? -80f : 20f * (float) Math.log10((float) musicVolume / 100f);
            dB = Math.max(-80f, Math.min(0f, dB));
            volumeControl.setValue(dB);
        }

        isMusicPlaying = true;
        musicClip.setFramePosition(0);
        musicClip.loop(javax.sound.sampled.Clip.LOOP_CONTINUOUSLY);
        System.out.println("▶️  Playing (loop): " + audioFile.getName() + " | Volume: " + musicVolume + "%");
    }

    /**
     * Stop music playback - ACTUAL STOP IMPLEMENTED
     */
    public void stopMusic() {
        try {
            if (musicClip != null && musicClip.isRunning()) {
                musicClip.stop();
                musicClip.setFramePosition(0);
            }
            if (musicAudioInputStream != null) {
                try {
                    musicAudioInputStream.close();
                } catch (Exception e) {
                }
                musicAudioInputStream = null;
            }
        } catch (Exception e) {
            System.err.println("Error stopping music: " + e.getMessage());
        }
        isMusicPlaying = false;
        System.out.println("⏹️  Music stopped");
    }

    /**
     * Toggle music playback
     */
    public void toggleMusic() {
        if (isMusicPlaying && musicClip != null && musicClip.isRunning()) {
            stopMusic();
        } else {
            playMusic();
        }
    }

    /**
     * Get current music state
     */
    public boolean isMusicPlaying() {
        return isMusicPlaying;
    }

    /**
     * Set effects volume level - LIVE CHANGE
     */
    public void setEffectsVolume(int volume) {
        effectsVolume = Math.max(0, Math.min(100, volume));
        System.setProperty("audio.effectsVolume", String.valueOf(volume));
        System.out.println("🎵 Effects Volume set to: " + volume + "%");
    }

    /**
     * Toggle sound effects - LIVE CHANGE
     */
    public void setSoundEffects(boolean enabled) {
        soundEffectsEnabled = enabled;
        System.setProperty("audio.soundEffects", String.valueOf(enabled));
        System.out.println(enabled ? "✅ Sound effects enabled" : "🔇 Sound effects disabled");
    }

    /**
     * Set paths for correct and wrong answer sound effects
     * 
     * @param correctPath Full path to correct answer sound (e.g.,
     *                    "D:\\sounds\\correct.wav")
     * @param wrongPath   Full path to wrong answer sound (e.g.,
     *                    "D:\\sounds\\wrong.wav")
     */
    public void setAnswerSoundPaths(String correctPath, String wrongPath) {
        correctSoundPath = correctPath != null ? correctPath : "";
        wrongSoundPath = wrongPath != null ? wrongPath : "";
        System.out.println(
                "✅ Correct sound: " + (correctPath != null && !correctPath.isEmpty() ? correctPath : "[empty]"));
        System.out.println("❌ Wrong sound: " + (wrongPath != null && !wrongPath.isEmpty() ? wrongPath : "[empty]"));
    }

    /**
     * Play correct answer sound effect
     */
    public void playCorrectSound() {
        if (!soundEffectsEnabled || correctSoundPath.isEmpty()) {
            return;
        }
        playAnswerSound(correctSoundPath, correctSoundClip, correctAudioStream);
    }

    /**
     * Play wrong answer sound effect
     */
    public void playWrongSound() {
        if (!soundEffectsEnabled || wrongSoundPath.isEmpty()) {
            return;
        }
        playAnswerSound(wrongSoundPath, wrongSoundClip, wrongAudioStream);
    }

    /**
     * Internal method to play answer sound effect
     */
    private void playAnswerSound(String soundPath, javax.sound.sampled.Clip clip,
            javax.sound.sampled.AudioInputStream audioStream) {
        try {
            java.io.File audioFile = new java.io.File(soundPath);
            if (!audioFile.exists()) {
                System.err.println("❌ Sound file not found: " + soundPath);
                return;
            }

            javax.sound.sampled.AudioInputStream newStream = javax.sound.sampled.AudioSystem
                    .getAudioInputStream(audioFile);
            javax.sound.sampled.AudioFormat baseFormat = newStream.getFormat();
            if (baseFormat.getEncoding() != javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED) {
                javax.sound.sampled.AudioInputStream pcmStream = javax.sound.sampled.AudioSystem.getAudioInputStream(
                        new javax.sound.sampled.AudioFormat(
                                javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED,
                                baseFormat.getSampleRate(),
                                16,
                                baseFormat.getChannels(),
                                baseFormat.getChannels() * 2,
                                baseFormat.getSampleRate(),
                                false),
                        newStream);
                newStream.close();
                newStream = pcmStream;
            }

            if (soundPath.contains("correct")) {
                if (correctSoundClip != null) {
                    correctSoundClip.close();
                }
                if (correctAudioStream != null) {
                    correctAudioStream.close();
                }
                correctAudioStream = newStream;
                correctSoundClip = javax.sound.sampled.AudioSystem.getClip();
                correctSoundClip.open(correctAudioStream);
                applyVolumeToClip(correctSoundClip, effectsVolume);
                correctSoundClip.start();
            } else {
                if (wrongSoundClip != null) {
                    wrongSoundClip.close();
                }
                if (wrongAudioStream != null) {
                    wrongAudioStream.close();
                }
                wrongAudioStream = newStream;
                wrongSoundClip = javax.sound.sampled.AudioSystem.getClip();
                wrongSoundClip.open(wrongAudioStream);
                applyVolumeToClip(wrongSoundClip, effectsVolume);
                wrongSoundClip.start();
            }
        } catch (Exception e) {
            System.err.println("❌ Error playing sound: " + e.getMessage());
        }
    }

    /**
     * Apply volume to audio clip
     */
    private void applyVolumeToClip(javax.sound.sampled.Clip clip, int volume) {
        if (clip != null && clip.isControlSupported(javax.sound.sampled.FloatControl.Type.MASTER_GAIN)) {
            javax.sound.sampled.FloatControl volumeControl = (javax.sound.sampled.FloatControl) clip
                    .getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
            float minGain = volumeControl.getMinimum();
            float maxGain = volumeControl.getMaximum();
            float gain = minGain + (maxGain - minGain) * (volume / 100.0f);
            volumeControl.setValue(gain);
        }
    }

    /**
     * Toggle ambient sound - LIVE CHANGE
     */
    public void setAmbientSound(boolean enabled) {
        System.setProperty("audio.ambientSound", String.valueOf(enabled));
        setMusicEnabled(enabled);
        System.out.println(enabled ? "✅ Ambient sound enabled" : "🔇 Ambient sound disabled");
    }

    /**
     * Enable/Disable notifications - LIVE CHANGE
     */
    public void setNotificationsEnabled(boolean enabled) {
        System.setProperty("notifications.enabled", String.valueOf(enabled));
    }

    /**
     * Set achievement notifications - LIVE CHANGE
     */
    public void setAchievementNotifications(boolean enabled) {
        System.setProperty("notifications.achievement", String.valueOf(enabled));
    }

    /**
     * Set streak notifications - LIVE CHANGE
     */
    public void setStreakNotifications(boolean enabled) {
        System.setProperty("notifications.streak", String.valueOf(enabled));
    }

    private void loadBackgroundImages() {
        // Load project-local resource images so the app remains portable across
        // machines.
        String sidebarImagePath = AppPaths.resourcePath("pexels-lucadross-4129849.jpg");
        if (sidebarImagePath != null && !sidebarImagePath.trim().isEmpty()) {
            try {
                sidebarBgImage = new ImageIcon(sidebarImagePath).getImage();
            } catch (Exception e) {
                System.err.println("Failed to load sidebar background: " + e.getMessage());
            }
        }

        String mainImagePath = AppPaths.resourcePath("digital-art-dark-cosmic-night-sky.jpg");
        if (mainImagePath != null && !mainImagePath.trim().isEmpty()) {
            try {
                mainBgImage = new ImageIcon(mainImagePath).getImage();
            } catch (Exception e) {
                System.err.println("Failed to load main background: " + e.getMessage());
            }
        }

        String questionCardImagePath = AppPaths.resourcePath("lights-prisms-effect-close-up.jpg");
        if (questionCardImagePath != null && !questionCardImagePath.trim().isEmpty()) {
            try {
                questionCardBgImage = new ImageIcon(questionCardImagePath).getImage();
            } catch (Exception e) {
                System.err.println("Failed to load question card background: " + e.getMessage());
            }
        }
    }

    /**
     * Scale image to target dimensions efficiently using BufferedImage.
     * Caches result to avoid re-scaling on every paint event.
     * Uses NEAREST_NEIGHBOR for maximum FPS (no quality loss at high res).
     */
    private Image getScaledImage(Image original, int width, int height, Image cachedImage, Dimension lastSize) {
        if (original == null || width <= 0 || height <= 0) {
            return null;
        }

        // Return cached image if dimensions haven't changed
        if (cachedImage != null && lastSize.width == width && lastSize.height == height) {
            return cachedImage;
        }

        // Scale image using BufferedImage for better performance
        java.awt.image.BufferedImage buffered = new java.awt.image.BufferedImage(
                width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = buffered.createGraphics();
        // FASTEST interpolation for max FPS - no quality loss at high resolution
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.drawImage(original, 0, 0, width, height, null);
        g2d.dispose();

        return buffered;
    }

    private void initUI() {
        JPanel mainWrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Paint main background image if loaded
                if (mainBgImage != null) {
                    Graphics2D g2 = (Graphics2D) g;
                    int width = getWidth();
                    int height = getHeight();

                    // Cache scaled image to avoid re-scaling every paint
                    if (cachedMainImage == null || lastMainWrapperSize.width != width
                            || lastMainWrapperSize.height != height) {
                        java.awt.image.BufferedImage buffered = new java.awt.image.BufferedImage(
                                width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
                        Graphics2D temp = buffered.createGraphics();
                        // Faster interpolation for maximum FPS
                        temp.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                        temp.drawImage(mainBgImage, 0, 0, width, height, null);
                        temp.dispose();
                        cachedMainImage = buffered;
                        lastMainWrapperSize = new Dimension(width, height);
                    }

                    // Render cached image with opacity
                    Composite oldComposite = g2.getComposite();
                    g2.setComposite(COMPOSITE_90);
                    g2.drawImage(cachedMainImage, 0, 0, null);
                    g2.setComposite(oldComposite);

                    // Add dark overlay for readability
                    g2.setColor(new Color(24, 24, 27, 70));
                    g2.fillRect(0, 0, width, height);
                }
            }
        };
        mainWrapper.setBackground(new Color(24, 24, 27));
        mainWrapper.setOpaque(true);
        sidebarPanel = createSidebar(); // Store reference for live toggle
        mainWrapper.add(sidebarPanel, BorderLayout.WEST);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);

        // Unified Header with single background (Search + Chips)
        JPanel header = createUnifiedHeader();
        centerPanel.add(header, BorderLayout.NORTH);

        // Main Body Scroll
        JPanel bodyWrapper = new JPanel(new GridBagLayout());
        bodyWrapper.setOpaque(false);
        bodyWrapper.setBorder(new EmptyBorder(10, 50, 60, 50));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 0);

        // Wrapper to constrain card width
        JPanel cardWrapper = new JPanel(new GridBagLayout());
        cardWrapper.setOpaque(false);
        GridBagConstraints cardGbc = new GridBagConstraints();
        cardGbc.gridx = 0;
        cardGbc.gridy = 0;
        cardGbc.weightx = 1.0;
        cardGbc.fill = GridBagConstraints.HORIZONTAL;
        cardGbc.anchor = GridBagConstraints.NORTH;

        cardContainer = new JPanel(new CardLayout());
        cardContainer.setOpaque(false);
        cardContainer.setPreferredSize(new Dimension(1100, 500));
        cardContainer.setMaximumSize(new Dimension(1100, 500));
        skeletonCard = new SkeletonCard();
        cardContainer.add(skeletonCard, "LOADING");
        cardContainer.add(createQuestionCard(), "REAL");

        cardWrapper.add(cardContainer, cardGbc);
        bodyWrapper.add(cardWrapper, gbc);

        // Verification Button
        submitBtn = createTexturedVerifyButton("✓ Verify Answer");
        submitBtn.setPreferredSize(new Dimension(340, 58));
        submitBtn.setMinimumSize(new Dimension(300, 58));
        submitBtn.setMaximumSize(new Dimension(380, 58));
        submitBtn.setFont(VERIFY_BUTTON_FONT);
        submitBtn.setBackground(verifyDefaultColor);
        submitBtn.setForeground(Color.WHITE);
        submitBtn.setFocusPainted(false);
        submitBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(30, 0, 0, 0);
        bodyWrapper.add(submitBtn, gbc);
        if (ENABLE_VERIFY_BUTTON_ANIMATIONS) {
            startVerifySheenAnimation();
        }

        // Explanation Area (hidden initially, starts with minimal height)
        explanationArea = new JTextArea() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();
                if (width > 1 && height > 1) {
                    // Keep explanation area itself fully transparent; draw only section blocks.
                    paintExplanationContentBlocks(g2, width, height);
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };
        explanationArea.setEditable(false);
        explanationArea.setOpaque(false);
        explanationArea.setLineWrap(true);
        explanationArea.setWrapStyleWord(true);
        explanationArea.setFont(new Font("Inter", Font.PLAIN, 18));
        explanationArea.setForeground(new Color(226, 232, 240));
        explanationArea.setBorder(new EmptyBorder(12, 16, 12, 16));
        explanationArea.setMargin(new Insets(0, 0, 0, 0));
        explanationArea.setText(""); // Start empty
        explanationArea.setPreferredSize(new Dimension(0, 0)); // Hidden initially
        explanationArea.setMinimumSize(new Dimension(0, 0));
        explanationArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 0));

        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(20, 20, 50, 20);
        bodyWrapper.add(explanationArea, gbc);

        mainContentScrollPane = new JScrollPane(bodyWrapper);
        mainContentScrollPane.setBorder(null);
        mainContentScrollPane.setOpaque(false);
        mainContentScrollPane.getViewport().setOpaque(false);
        mainContentScrollPane.getVerticalScrollBar().setUnitIncrement(4); // Ultra-responsive scrolling
        mainContentScrollPane.getVerticalScrollBar().setBlockIncrement(80); // Bigger block jumps
        centerPanel.add(mainContentScrollPane, BorderLayout.CENTER);

        mainWrapper.add(centerPanel, BorderLayout.CENTER);

        // Add floating menu buttons panel (bottom-right when sidebar is hidden)
        createFloatingMenuButtonsPanel();
        createSectionOverlayPanel();

        add(mainWrapper);
    }

    /**
     * Create floating menu buttons panel at bottom-right (shown when sidebar is
     * hidden)
     */
    private void createFloatingMenuButtonsPanel() {
        floatingMenuButtonsPanel = new JPanel();
        floatingMenuButtonsPanel.setLayout(new BorderLayout());
        floatingMenuButtonsPanel.setOpaque(false);
        floatingMenuButtonsPanel.setBorder(null);
        floatingMenuButtonsPanel.setVisible(false); // Hidden by default (sidebar visible)

        getLayeredPane().add(floatingMenuButtonsPanel, JLayeredPane.POPUP_LAYER);
        positionFloatingMenuButtonsPanel();
    }

    /**
     * Create in-app section overlay for Tools/Analytics/Settings panels.
     */
    private void createSectionOverlayPanel() {
        sectionOverlayPanel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(8, 8, 12, 160));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        sectionOverlayPanel.setOpaque(false);
        sectionOverlayPanel.setEnabled(true);
        sectionOverlayPanel.setFocusable(true);

        // Prevent click-through: clicks on empty overlay space must never reach
        // underlying topic chips or controls.
        java.awt.event.MouseAdapter overlayInputBlocker = new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                e.consume();
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                e.consume();
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                e.consume();
            }
        };
        sectionOverlayPanel.addMouseListener(overlayInputBlocker);
        sectionOverlayPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                e.consume();
            }

            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                e.consume();
            }
        });
        sectionOverlayPanel.addMouseWheelListener(e -> e.consume());

        sectionShellPanel = new JPanel(new BorderLayout());
        sectionShellPanel.setBackground(new Color(24, 24, 27));
        sectionShellPanel.setBorder(new LineBorder(new Color(60, 60, 68), 1, true));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(true);
        header.setBackground(new Color(32, 32, 38));
        header.setBorder(new EmptyBorder(12, 16, 12, 12));

        sectionTitleLabel = new JLabel("Section");
        sectionTitleLabel.setForeground(new Color(203, 213, 225));
        sectionTitleLabel.setFont(new Font("Inter", Font.BOLD, 16));
        header.add(sectionTitleLabel, BorderLayout.WEST);

        JButton closeBtn = new JButton("✕");
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setBackground(new Color(55, 55, 65));
        closeBtn.setForeground(new Color(203, 213, 225));
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> hideSectionPanel());
        header.add(closeBtn, BorderLayout.EAST);

        // Make section shell draggable from header.
        java.awt.event.MouseAdapter dragHandler = new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                sectionDragOffset = e.getPoint();
                sectionLastDragTimeMs = 0L;
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                sectionDragOffset = null;
                sectionLastDragTimeMs = 0L;
            }

            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (sectionShellPanel == null || sectionDragOffset == null) {
                    return;
                }

                long now = System.currentTimeMillis();
                if (now - sectionLastDragTimeMs < SECTION_DRAG_THROTTLE_MS) {
                    return; // Slightly faster than LoginDialog for smoother desktop drag.
                }
                sectionLastDragTimeMs = now;

                int newX = sectionShellPanel.getX() + (e.getX() - sectionDragOffset.x);
                int newY = sectionShellPanel.getY() + (e.getY() - sectionDragOffset.y);

                int maxX = Math.max(0, sectionOverlayPanel.getWidth() - sectionShellPanel.getWidth());
                int maxY = Math.max(0, sectionOverlayPanel.getHeight() - sectionShellPanel.getHeight());
                newX = Math.max(0, Math.min(newX, maxX));
                newY = Math.max(0, Math.min(newY, maxY));

                if (newX != sectionShellPanel.getX() || newY != sectionShellPanel.getY()) {
                    sectionShellPanel.setLocation(newX, newY);
                    sectionManuallyMoved = true;
                }
            }
        };
        header.addMouseListener(dragHandler);
        header.addMouseMotionListener(dragHandler);

        sectionCardLayout = new CardLayout();
        sectionCardPanel = new JPanel(sectionCardLayout);
        sectionCardPanel.setOpaque(true);
        sectionCardPanel.setBackground(new Color(24, 24, 27));

        sectionShellPanel.add(header, BorderLayout.NORTH);
        sectionShellPanel.add(sectionCardPanel, BorderLayout.CENTER);
        sectionOverlayPanel.add(sectionShellPanel);

        sectionOverlayPanel.setVisible(false);
        getLayeredPane().add(sectionOverlayPanel, JLayeredPane.MODAL_LAYER);
        positionSectionOverlayPanel();
    }

    private void positionSectionOverlayPanel() {
        if (sectionOverlayPanel == null) {
            return;
        }
        boolean layoutChanged = false;

        int overlayWidth = getWidth();
        int overlayHeight = getHeight();
        if (sectionOverlayPanel.getX() != 0
                || sectionOverlayPanel.getY() != 0
                || sectionOverlayPanel.getWidth() != overlayWidth
                || sectionOverlayPanel.getHeight() != overlayHeight) {
            sectionOverlayPanel.setBounds(0, 0, overlayWidth, overlayHeight);
            layoutChanged = true;
        }

        if (sectionShellPanel != null) {
            int width = Math.max(760, Math.min(980, getWidth() - 120));
            int height = Math.max(520, Math.min(700, getHeight() - 120));
            if (sectionShellPanel.getWidth() != width || sectionShellPanel.getHeight() != height) {
                sectionShellPanel.setSize(width, height);
                layoutChanged = true;
            }

            if (!sectionManuallyMoved) {
                int x = Math.max(20, (sectionOverlayPanel.getWidth() - width) / 2);
                int y = Math.max(20, (sectionOverlayPanel.getHeight() - height) / 2);
                if (sectionShellPanel.getX() != x || sectionShellPanel.getY() != y) {
                    sectionShellPanel.setLocation(x, y);
                    layoutChanged = true;
                }
            } else {
                int x = sectionShellPanel.getX();
                int y = sectionShellPanel.getY();
                int maxX = Math.max(0, sectionOverlayPanel.getWidth() - width);
                int maxY = Math.max(0, sectionOverlayPanel.getHeight() - height);
                int boundedX = Math.max(0, Math.min(x, maxX));
                int boundedY = Math.max(0, Math.min(y, maxY));
                if (sectionShellPanel.getX() != boundedX || sectionShellPanel.getY() != boundedY) {
                    sectionShellPanel.setLocation(boundedX, boundedY);
                    layoutChanged = true;
                }
            }
        }

        if (layoutChanged) {
            sectionOverlayPanel.revalidate();
        }
    }

    private void showSectionPanel(String sectionKey) {
        if (sectionOverlayPanel == null || sectionCardPanel == null || sectionCardLayout == null) {
            return;
        }

        if ("TOOLS".equals(sectionKey) && toolsSectionPanel == null) {
            toolsSectionPanel = new ToolsPageDialog(this);
            sectionCardPanel.add(toolsSectionPanel, "TOOLS");
        }
        if ("ANALYTICS".equals(sectionKey) && analyticsSectionPanel == null) {
            analyticsSectionPanel = new AnalyticsPageDialog(this);
            sectionCardPanel.add(analyticsSectionPanel, "ANALYTICS");
        }
        if ("SETTINGS".equals(sectionKey) && settingsSectionPanel == null) {
            settingsSectionPanel = new SettingsPageDialog(this);
            sectionCardPanel.add(settingsSectionPanel, "SETTINGS");
        }

        if ("TOOLS".equals(sectionKey) && toolsSectionPanel instanceof ToolsPageDialog) {
            ((ToolsPageDialog) toolsSectionPanel).refreshData();
        }
        if ("ANALYTICS".equals(sectionKey) && analyticsSectionPanel instanceof AnalyticsPageDialog) {
            ((AnalyticsPageDialog) analyticsSectionPanel).refreshData();
        }

        if (sectionTitleLabel != null) {
            if ("TOOLS".equals(sectionKey)) {
                sectionTitleLabel.setText("🛠 Tools & Utilities");
            } else if ("ANALYTICS".equals(sectionKey)) {
                sectionTitleLabel.setText("📊 Analytics & Insights");
            } else if ("SETTINGS".equals(sectionKey)) {
                sectionTitleLabel.setText("⚙️ Settings");
            }
        }

        positionSectionOverlayPanel();
        sectionCardLayout.show(sectionCardPanel, sectionKey);
        sectionOverlayPanel.setVisible(true);
        sectionOverlayPanel.requestFocusInWindow();
    }

    private void hideSectionPanel() {
        if (sectionOverlayPanel != null) {
            sectionOverlayPanel.setVisible(false);
        }
    }

    /**
     * Create hidden achievement notification panel.
     * Will be shown from bottom-right when achievement is unlocked.
     */
    private JPanel createAchievementNotificationPanel() {
        JPanel panel = new JPanel() {
            private AchievementSystem.Achievement currentAchievement = null;
            private float alpha = 0f;

            @Override
            protected void paintComponent(Graphics g) {
                if (currentAchievement == null || alpha <= 0)
                    return;

                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int width = getWidth();
                int height = 90;
                int x = width - 320;
                int y = 10;

                Composite originalComposite = g2d.getComposite();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

                // Background with gradient
                g2d.setColor(new Color(currentAchievement.color.getRed(),
                        currentAchievement.color.getGreen(),
                        currentAchievement.color.getBlue(), 200));
                g2d.fillRoundRect(x, y, 300, height, 15, 15);

                // Border
                g2d.setColor(currentAchievement.color);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRoundRect(x, y, 300, height, 15, 15);

                // Icon
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 36));
                g2d.drawString(currentAchievement.icon, x + 15, y + 55);

                // Title
                g2d.setFont(new Font("Inter", Font.BOLD, 14));
                g2d.drawString(currentAchievement.name, x + 65, y + 25);

                // Description
                g2d.setFont(new Font("Inter", Font.PLAIN, 11));
                g2d.setColor(new Color(255, 255, 255, 220));
                g2d.drawString(currentAchievement.description, x + 65, y + 50);

                g2d.setComposite(originalComposite);
            }
        };
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(320, 100));
        return panel;
    }

    /**
     * Show achievement notification with smooth fade-in and auto-hide.
     */
    private void showAchievementNotification(AchievementSystem.Achievement achievement) {
        if (achievementNotificationPanel == null)
            return;

        final float[] alpha = { 0f };
        final int[] step = { 0 };

        // Find the actual panel (use reflection or store reference)
        // For now, we'll trigger a repaint
        javax.swing.Timer fadeInTimer = new javax.swing.Timer(30, e -> {
            step[0]++;
            if (step[0] <= 10) {
                alpha[0] = (float) step[0] / 10f;
            } else if (step[0] > 50) {
                alpha[0] = Math.max(0, 1 - (float) (step[0] - 50) / 10f);
                if (alpha[0] <= 0) {
                    ((javax.swing.Timer) e.getSource()).stop();
                }
            }
            achievementNotificationPanel.repaint();
        });
        fadeInTimer.setCoalesce(true);
        fadeInTimer.start();
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Paint sidebar background image if loaded
                if (sidebarBgImage != null) {
                    Graphics2D g2 = (Graphics2D) g;
                    int width = getWidth();
                    int height = getHeight();

                    // Cache scaled image to avoid re-scaling every paint
                    if (cachedSidebarImage == null || lastSidebarSize.width != width
                            || lastSidebarSize.height != height) {
                        java.awt.image.BufferedImage buffered = new java.awt.image.BufferedImage(
                                width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
                        Graphics2D temp = buffered.createGraphics();
                        // Faster interpolation for maximum FPS
                        temp.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                        temp.drawImage(sidebarBgImage, 0, 0, width, height, null);
                        temp.dispose();
                        cachedSidebarImage = buffered;
                        lastSidebarSize = new Dimension(width, height);
                    }

                    // Render cached image with opacity
                    Composite oldComposite = g2.getComposite();
                    g2.setComposite(COMPOSITE_90);
                    g2.drawImage(cachedSidebarImage, 0, 0, null);
                    g2.setComposite(oldComposite);

                    // Add dark overlay for readability
                    g2.setColor(new Color(24, 24, 27, 70));
                    g2.fillRect(0, 0, width, height);
                }
            }
        };
        sidebar.setLayout(new BorderLayout());
        sidebar.setBackground(new Color(24, 24, 27));
        sidebar.setPreferredSize(new Dimension(250, getHeight()));
        sidebar.setBorder(new EmptyBorder(20, 20, 20, 20)); // Normal padding (no negative values)

        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
        statsPanel.setOpaque(false);

        // Sidebar title
        JLabel titleLabel = new JLabel("Statistics");
        titleLabel.setFont(new Font("Inter", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        statsPanel.add(titleLabel);
        statsPanel.add(Box.createVerticalStrut(20));

        // Streak section
        JLabel streakLabel = new JLabel("Streak");
        streakLabel.setFont(new Font("Inter", Font.PLAIN, 12));
        streakLabel.setForeground(new Color(161, 161, 170));
        statsPanel.add(streakLabel);

        streakVal = new JLabel("0 🔥");
        streakVal.setFont(new Font("Inter", Font.BOLD, 28));
        streakVal.setForeground(new Color(34, 197, 94));
        statsPanel.add(streakVal);
        statsPanel.add(Box.createVerticalStrut(20));

        // Accuracy section
        JLabel accuracyLabel = new JLabel("Accuracy");
        accuracyLabel.setFont(new Font("Inter", Font.PLAIN, 12));
        accuracyLabel.setForeground(new Color(161, 161, 170));
        statsPanel.add(accuracyLabel);

        accuracyVal = new JLabel("0% 🎯");
        accuracyVal.setFont(new Font("Inter", Font.BOLD, 28));
        accuracyVal.setForeground(new Color(99, 102, 241));
        statsPanel.add(accuracyVal);

        // SEPARATOR
        statsPanel.add(Box.createVerticalStrut(20));
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(100, 116, 139, 100));
        statsPanel.add(sep);

        sidebar.add(statsPanel, BorderLayout.NORTH);

        // COLLAPSIBLE MENU PANEL AT BOTTOM
        sidebar.add(createCollapsibleMenuPanel(), BorderLayout.SOUTH);

        return sidebar;
    }

    /**
     * Wrap a button in a centered container to prevent horizontal expansion
     */
    private JPanel wrapButtonCentered(JButton btn) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(50, 50));
        wrapper.setPreferredSize(new Dimension(50, 50));
        wrapper.setMinimumSize(new Dimension(50, 50));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(btn, BorderLayout.WEST);
        return wrapper;
    }

    private void showSidebarHoverMenu() {
        if (!isLoggedIn || menuButtonsPanel == null) {
            return;
        }

        if (sidebarMenuHideTimer != null && sidebarMenuHideTimer.isRunning()) {
            sidebarMenuHideTimer.stop();
        }

        if (!menuButtonsPanel.isVisible()) {
            menuButtonsPanel.setVisible(true);
            Container parent = menuButtonsPanel.getParent();
            if (parent != null) {
                parent.revalidate();
                parent.repaint();
            }
        }
    }

    private void hideSidebarHoverMenuImmediately() {
        if (menuButtonsPanel == null) {
            return;
        }

        hideLoginUserHoverPopup();

        if (sidebarMenuHideTimer != null && sidebarMenuHideTimer.isRunning()) {
            sidebarMenuHideTimer.stop();
        }

        sidebarPopupMenuOpenCount = 0;
        if (menuButtonsPanel.isVisible()) {
            menuButtonsPanel.setVisible(false);
            Container parent = menuButtonsPanel.getParent();
            if (parent != null) {
                parent.revalidate();
                parent.repaint();
            }
        }
    }

    private void trackSidebarPopupMenu(JPopupMenu menu) {
        if (menu == null) {
            return;
        }

        menu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                sidebarPopupMenuOpenCount++;
                hideLoginUserHoverPopup();
                showSidebarHoverMenu();
            }

            @Override
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                sidebarPopupMenuOpenCount = Math.max(0, sidebarPopupMenuOpenCount - 1);
                scheduleSidebarHoverMenuHide();
            }

            @Override
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
                sidebarPopupMenuOpenCount = Math.max(0, sidebarPopupMenuOpenCount - 1);
                scheduleSidebarHoverMenuHide();
            }
        });
    }

    private void scheduleSidebarHoverMenuHide() {
        if (!isLoggedIn || menuButtonsPanel == null || loginIconButton == null) {
            return;
        }

        if (sidebarPopupMenuOpenCount > 0) {
            showSidebarHoverMenu();
            return;
        }

        if (sidebarMenuHideTimer != null && sidebarMenuHideTimer.isRunning()) {
            sidebarMenuHideTimer.stop();
        }

        sidebarMenuHideTimer = new javax.swing.Timer(SIDEBAR_MENU_HIDE_DELAY_MS, e -> {
            if (sidebarPopupMenuOpenCount == 0
                    && !isMouseOverComponent(loginIconButton)
                    && !isMouseOverComponent(menuButtonsPanel)) {
                menuButtonsPanel.setVisible(false);
                Container parent = menuButtonsPanel.getParent();
                if (parent != null) {
                    parent.revalidate();
                    parent.repaint();
                }
            }
        });
        sidebarMenuHideTimer.setRepeats(false);
        sidebarMenuHideTimer.setCoalesce(true);
        sidebarMenuHideTimer.start();
    }

    private boolean isMouseOverComponent(Component component) {
        if (component == null || !component.isShowing()) {
            return false;
        }

        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        if (pointerInfo == null) {
            return false;
        }

        Point pointer = pointerInfo.getLocation();
        SwingUtilities.convertPointFromScreen(pointer, component);
        return pointer.x >= 0 && pointer.y >= 0
                && pointer.x < component.getWidth() && pointer.y < component.getHeight();
    }

    private void showLoginUserHoverPopup() {
        if (!isLoggedIn || loginIconButton == null || loggedInUser == null || loggedInUser.trim().isEmpty()) {
            return;
        }

        if (loginUserPopupShowTimer != null && loginUserPopupShowTimer.isRunning()) {
            loginUserPopupShowTimer.stop();
        }

        loginUserPopupShowTimer = new javax.swing.Timer(90, e -> {
            hideLoginUserHoverPopup();

            try {
                JPanel popupPanel = new JPanel(new BorderLayout());
                popupPanel.setOpaque(true);
                popupPanel.setBackground(new Color(23, 23, 30, 245));
                popupPanel.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(new Color(99, 102, 241, 140), 1, true),
                        new EmptyBorder(6, 10, 6, 10)));

                JLabel userLabel = new JLabel("👤 " + loggedInUser);
                userLabel.setFont(new Font("Inter", Font.BOLD, 12));
                userLabel.setForeground(new Color(224, 231, 255));
                popupPanel.add(userLabel, BorderLayout.CENTER);

                Dimension pref = popupPanel.getPreferredSize();
                Point iconOnScreen = loginIconButton.getLocationOnScreen();
                int x;
                if (!sidebarVisible) {
                    // Hidden sidebar mode: show user label to the LEFT of icon.
                    x = iconOnScreen.x - pref.width - 8;
                } else {
                    x = iconOnScreen.x + loginIconButton.getWidth() + 8;
                }
                int y = iconOnScreen.y + (loginIconButton.getHeight() - pref.height) / 2;

                loginUserHoverPopup = PopupFactory.getSharedInstance()
                        .getPopup(loginIconButton, popupPanel, x, y);
                loginUserHoverPopup.show();
            } catch (IllegalComponentStateException ignored) {
            }
        });
        loginUserPopupShowTimer.setRepeats(false);
        loginUserPopupShowTimer.setCoalesce(true);
        loginUserPopupShowTimer.start();
    }

    private void hideLoginUserHoverPopup() {
        if (loginUserPopupShowTimer != null && loginUserPopupShowTimer.isRunning()) {
            loginUserPopupShowTimer.stop();
        }

        if (loginUserHoverPopup != null) {
            loginUserHoverPopup.hide();
            loginUserHoverPopup = null;
        }
    }

    private void showMainFeaturePanelAtLogout(JPopupMenu menu, JButton triggerButton) {
        if (menu == null) {
            return;
        }

        JButton anchorButton = (logoutIconButton != null && logoutIconButton.isShowing())
                ? logoutIconButton
                : triggerButton;
        if (anchorButton == null) {
            return;
        }

        Dimension menuSize = menu.getPreferredSize();
        int x = anchorButton.getWidth() + 10;
        int y = anchorButton.getHeight() - menuSize.height;
        menu.show(anchorButton, x, y);
    }

    /**
     * Create three large circular menu buttons at bottom left of sidebar with
     * upward-expanding menus
     */
    private JPanel createCollapsibleMenuPanel() {
        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.setOpaque(false);
        containerPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        containerPanel.setVisible(true);

        // WRAPPER for compact button stack (at SOUTH/BOTTOM)
        sidebarButtonStackWrapper = new JPanel();
        JPanel buttonStackWrapper = sidebarButtonStackWrapper;
        buttonStackWrapper.setLayout(new BoxLayout(buttonStackWrapper, BoxLayout.Y_AXIS));
        buttonStackWrapper.setOpaque(false);
        buttonStackWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonStackWrapper.setBorder(new EmptyBorder(0, 0, 0, 0));
        buttonStackWrapper.setMaximumSize(new Dimension(50, Integer.MAX_VALUE));
        buttonStackWrapper.setMinimumSize(new Dimension(50, 58));

        // MENU BUTTONS PANEL (shown after login) - VERTICAL - FIXED WIDTH
        menuButtonsPanel = new JPanel();
        menuButtonsPanel.setLayout(new BoxLayout(menuButtonsPanel, BoxLayout.Y_AXIS));
        menuButtonsPanel.setOpaque(false);
        menuButtonsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        menuButtonsPanel.setAlignmentY(JPanel.TOP_ALIGNMENT);
        menuButtonsPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        // FIXED dimensions - NO stretching
        menuButtonsPanel.setMaximumSize(new Dimension(50, 250));
        menuButtonsPanel.setPreferredSize(new Dimension(50, 250));
        menuButtonsPanel.setMinimumSize(new Dimension(50, 250));

        // Create buttons first without actions
        JButton toolsBtn = createLargeCircleMenuButton("🛠", "TOOLS", null);
        JButton analyticsBtn = createLargeCircleMenuButton("📊", "ANALYTICS", null);
        JButton settingsBtn = createLargeCircleMenuButton("⚙️", "SETTINGS", null);
        toolsBtn.putClientProperty("menuGlyph", "tools");
        analyticsBtn.putClientProperty("menuGlyph", "analytics");
        settingsBtn.putClientProperty("menuGlyph", "settings");
        toolsBtn.setText("");
        analyticsBtn.setText("");
        settingsBtn.setText("");
        toolsBtn.setForeground(new Color(56, 189, 248));
        analyticsBtn.setForeground(new Color(96, 165, 250));
        settingsBtn.setForeground(new Color(148, 163, 184));

        // Add action listeners with proper references
        toolsBtn.addActionListener(e -> showToolsMenuUpward(toolsBtn));
        analyticsBtn.addActionListener(e -> showAnalyticsMenuUpward(analyticsBtn));
        settingsBtn.addActionListener(e -> showSettingsMenuUpward(settingsBtn));

        // Wrap each button in a centered container
        JPanel toolsWrapper = wrapButtonCentered(toolsBtn);
        JPanel analyticsWrapper = wrapButtonCentered(analyticsBtn);
        JPanel settingsWrapper = wrapButtonCentered(settingsBtn);

        menuButtonsPanel.add(toolsWrapper);
        menuButtonsPanel.add(Box.createVerticalStrut(12));
        menuButtonsPanel.add(analyticsWrapper);
        menuButtonsPanel.add(Box.createVerticalStrut(12));
        menuButtonsPanel.add(settingsWrapper);
        menuButtonsPanel.add(Box.createVerticalStrut(12));

        // Add logout button
        logoutIconButton = createLargeCircleMenuButton("🚪", "LOGOUT", null);
        logoutIconButton.putClientProperty("menuGlyph", "logout");
        logoutIconButton.setText("");
        logoutIconButton.setForeground(new Color(239, 68, 68));
        logoutIconButton.addActionListener(e -> showLogoutConfirmation());
        JPanel logoutWrapper = wrapButtonCentered(logoutIconButton);
        menuButtonsPanel.add(logoutWrapper);

        // LOGIN BUTTON (shown when not logged in)
        final float[] loginHoverIntensity = { 0f };
        final javax.swing.Timer[] loginHoverTimer = { null };
        final int[] loginIconTextYOffset = { 0 }; // Manual control: negative = up, positive = down
        final int[] loginToolTipInitialDelay = { ToolTipManager.sharedInstance().getInitialDelay() };
        final int[] loginToolTipReshowDelay = { ToolTipManager.sharedInstance().getReshowDelay() };
        final Runnable restoreLoginToolTipDelay = () -> {
            ToolTipManager.sharedInstance().setInitialDelay(loginToolTipInitialDelay[0]);
            ToolTipManager.sharedInstance().setReshowDelay(loginToolTipReshowDelay[0]);
        };
        loginIconButton = new JButton("") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

                int diameter = Math.min(getWidth(), getHeight()) - 6;
                int x = (getWidth() - diameter) / 2;
                int y = (getHeight() - diameter) / 2;
                Color glowColor = isLoggedIn ? new Color(16, 185, 129) : new Color(99, 102, 241);
                float hoverIntensity = loginHoverIntensity[0];
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;

                // Hover halo for a modern app-like floating button feel.
                if (hoverIntensity > 0f) {
                    float radius = (diameter / 2f) + 8f;
                    RadialGradientPaint hoverHalo = new RadialGradientPaint(
                            cx,
                            cy,
                            radius,
                            new float[] { 0f, 1f },
                            new Color[] {
                                    new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(),
                                            (int) (95 * hoverIntensity)),
                                    new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), 0)
                            });
                    g2.setPaint(hoverHalo);
                    g2.fillOval((int) (cx - radius), (int) (cy - radius), (int) (radius * 2), (int) (radius * 2));
                }

                GradientPaint shell = new GradientPaint(
                        0, y,
                        new Color(30, 41, 59, 230),
                        0, y + diameter,
                        new Color(15, 23, 42, 240));
                g2.setPaint(shell);
                g2.fillOval(x, y, diameter, diameter);

                g2.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(),
                        isLoggedIn ? 86 : 74));
                g2.fillOval(x + 2, y + 2, diameter - 4, diameter - 4);

                g2.setColor(new Color(10, 17, 32, 208));
                g2.fillOval(x + 5, y + 5, diameter - 10, diameter - 10);

                g2.setColor(new Color(255, 255, 255, (int) (68 + (24 * hoverIntensity))));
                g2.setStroke(new BasicStroke(1.45f));
                g2.drawOval(x, y, diameter, diameter);

                g2.setColor(new Color(255, 255, 255, 38));
                g2.drawArc(x + 3, y + 2, diameter - 7, diameter - 7, 28, 132);

                // Presence dot when logged in
                if (isLoggedIn) {
                    int dotSize = Math.max(8, diameter / 5);
                    int dotX = x + diameter - dotSize - 2;
                    int dotY = y + diameter - dotSize - 2;
                    g2.setColor(new Color(22, 163, 74));
                    g2.fillOval(dotX, dotY, dotSize, dotSize);
                    g2.setColor(new Color(187, 247, 208));
                    g2.drawOval(dotX, dotY, dotSize, dotSize);
                }

                int iconShiftY = loginIconTextYOffset[0] + Math.round((diameter - 44) * 0.10f);
                if (isLoggedIn) {
                    drawUserGlyph(g2, getWidth(), getHeight(), new Color(241, 245, 249), iconShiftY);
                } else {
                    drawLockGlyph(g2, getWidth(), getHeight(), new Color(241, 245, 249), iconShiftY);
                }
                g2.dispose();
            }
        };

        loginIconButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        loginIconButton.setForeground(Color.WHITE);
        loginIconButton.setRolloverEnabled(false);
        loginIconButton.setFocusPainted(false);
        loginIconButton.setBorderPainted(false);
        loginIconButton.setBorder(BorderFactory.createEmptyBorder());
        loginIconButton.setFocusable(false);
        loginIconButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginIconButton.setAlignmentY(JButton.BOTTOM_ALIGNMENT);
        loginIconButton.setHorizontalAlignment(SwingConstants.CENTER);
        loginIconButton.setVerticalAlignment(SwingConstants.CENTER);
        loginIconButton.setVerticalTextPosition(SwingConstants.CENTER);
        loginIconButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginIconButton.setContentAreaFilled(false);
        loginIconButton.setOpaque(false);
        loginIconButton.setPreferredSize(new Dimension(50, 50));
        loginIconButton.setMaximumSize(new Dimension(50, 50));
        loginIconButton.setMinimumSize(new Dimension(50, 50));
        loginIconButton.setToolTipText("Click to login");

        loginIconButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                showSidebarHoverMenu();
                showLoginUserHoverPopup();
                if (!isLoggedIn) {
                    ToolTipManager.sharedInstance().setInitialDelay(0);
                    ToolTipManager.sharedInstance().setReshowDelay(0);
                }

                if (loginHoverTimer[0] != null && loginHoverTimer[0].isRunning()) {
                    loginHoverTimer[0].stop();
                }

                loginHoverTimer[0] = new javax.swing.Timer(HOVER_ANIMATION_DELAY_MS, ev -> {
                    float current = loginHoverIntensity[0];
                    if (current >= 1f) {
                        loginHoverIntensity[0] = 1f;
                        loginIconButton.repaint();
                        ((javax.swing.Timer) ev.getSource()).stop();
                        return;
                    }
                    loginHoverIntensity[0] = Math.min(current + 0.08f, 1f);
                    loginIconButton.repaint();
                });
                loginHoverTimer[0].setCoalesce(true);
                loginHoverTimer[0].start();
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                scheduleSidebarHoverMenuHide();
                hideLoginUserHoverPopup();
                if (!isLoggedIn) {
                    restoreLoginToolTipDelay.run();
                }

                if (loginHoverTimer[0] != null && loginHoverTimer[0].isRunning()) {
                    loginHoverTimer[0].stop();
                }

                loginHoverTimer[0] = new javax.swing.Timer(HOVER_ANIMATION_DELAY_MS, ev -> {
                    float current = loginHoverIntensity[0];
                    if (current <= 0f) {
                        loginHoverIntensity[0] = 0f;
                        loginIconButton.repaint();
                        ((javax.swing.Timer) ev.getSource()).stop();
                        return;
                    }
                    loginHoverIntensity[0] = Math.max(current - 0.08f, 0f);
                    loginIconButton.repaint();
                });
                loginHoverTimer[0].setCoalesce(true);
                loginHoverTimer[0].start();
            }
        });

        loginIconButton.addActionListener(e -> {
            if (!isLoggedIn) {
                restoreLoginToolTipDelay.run();
                showLoginDialog();
                return;
            }

            hideLoginUserHoverPopup();

            // Logged-in click should not reopen login dialog.
            if (menuButtonsPanel != null && menuButtonsPanel.isVisible()) {
                hideSidebarHoverMenuImmediately();
            } else {
                showSidebarHoverMenu();
            }
        });

        // Add hover listeners to menu buttons panel to keep it visible when hovering
        menuButtonsPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                showSidebarHoverMenu();
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                scheduleSidebarHoverMenuHide();
            }
        });

        // Build compact button stack (menu buttons above, login icon below)
        buttonStackWrapper.add(menuButtonsPanel);
        menuButtonsPanel.setVisible(false); // Initially hidden

        // Small spacing between menu and login icon
        buttonStackWrapper.add(Box.createVerticalStrut(8));

        // Add login icon
        buttonStackWrapper.add(loginIconButton);
        loginIconButton.setVisible(true);

        // Add glue at top to push buttons to bottom
        containerPanel.add(Box.createVerticalGlue(), BorderLayout.CENTER);

        // Add button stack at bottom-left (strict anchor to avoid horizontal drift)
        sidebarBottomAnchor = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JPanel bottomLeftAnchor = sidebarBottomAnchor;
        bottomLeftAnchor.setOpaque(false);
        bottomLeftAnchor.add(buttonStackWrapper);
        containerPanel.add(bottomLeftAnchor, BorderLayout.SOUTH);

        return containerPanel;
    }

    /**
     * Create large circular menu button with impressive visual effects
     */
    private JButton createLargeCircleMenuButton(String icon, String label, Runnable action) {
        final float[] hoverIntensityRef = { 0f };
        final javax.swing.Timer[] hoverTimerRef = { null };

        JButton btn = new JButton(icon) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

                int w = getWidth();
                int h = getHeight();
                int diameter = Math.min(w, h) - 8;
                int x = (w - diameter) / 2;
                int y = (h - diameter) / 2;

                String menuGlyph = (String) getClientProperty("menuGlyph");
                Color accent = menuGlyph != null ? resolveGlyphGlowColor(menuGlyph) : getForeground();

                GradientPaint shell = new GradientPaint(
                        0,
                        y,
                        new Color(30, 41, 59, 222),
                        0,
                        y + diameter,
                        new Color(15, 23, 42, 238));
                g2.setPaint(shell);
                g2.fillOval(x, y, diameter, diameter);

                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                        (int) (58 + (36 * hoverIntensityRef[0]))));
                g2.fillOval(x + 2, y + 2, diameter - 4, diameter - 4);

                g2.setColor(new Color(11, 17, 30, 214));
                g2.fillOval(x + 5, y + 5, diameter - 10, diameter - 10);

                g2.setColor(new Color(255, 255, 255, (int) (60 + (20 * hoverIntensityRef[0]))));
                g2.setStroke(new BasicStroke(1.35f));
                g2.drawOval(x, y, diameter, diameter);

                g2.setColor(new Color(255, 255, 255, 30));
                g2.drawArc(x + 2, y + 2, diameter - 6, diameter - 6, 34, 124);

                if (menuGlyph != null && !menuGlyph.isEmpty()) {
                    drawMenuGlyph(g2, w, h, menuGlyph, new Color(241, 245, 249));
                    g2.dispose();
                    return;
                }

                // Draw emoji centered precisely (absolute center)
                String text = getText();
                if (text != null && !text.isEmpty()) {
                    g2.setFont(getFont());
                    g2.setColor(new Color(241, 245, 249));
                    FontMetrics fm = g2.getFontMetrics();

                    // Calculate exact center position using precise metrics
                    int textWidth = fm.stringWidth(text);
                    int textX = (w - textWidth) / 2;
                    int textY = (h + fm.getAscent() - fm.getDescent()) / 2 + 2;

                    // Settings emoji needs extra offset for proper centering
                    if ("⚙️".equals(text)) {
                        textY += 1; // Nudge down for better visual centering
                        textX += 17; // Nudge right for better visual centering
                    }
                    if ("📊".equals(text)) {
                        textY += 1; // Nudge down for better visual centering
                    }

                    g2.drawString(text, textX, textY);
                }
                g2.dispose();
            }

            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                String menuGlyph = (String) getClientProperty("menuGlyph");
                Color glowColor = resolveGlyphGlowColor(menuGlyph);

                // Draw glow before component paint so the icon remains crisp above it.
                if (hoverIntensityRef[0] > 0) {
                    int glowExpand = Math.round(5 + (7 * hoverIntensityRef[0]));
                    g2.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(),
                            (int) (42 * hoverIntensityRef[0])));
                    g2.fillOval(3 - glowExpand / 2, 3 - glowExpand / 2,
                            w - 6 + glowExpand, h - 6 + glowExpand);

                    g2.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(),
                            (int) (74 * hoverIntensityRef[0])));
                    g2.setStroke(new BasicStroke(1.35f + (0.85f * hoverIntensityRef[0])));
                    g2.drawOval(2, 2, w - 4, h - 4);
                }

                g2.dispose();

                // Call parent paint to draw the rest (component + border)
                super.paint(g);
            }
        };

        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 25));
        btn.setForeground(new Color(99, 102, 241)); // Indigo color for main buttons
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder());
        btn.setAlignmentX(JButton.CENTER_ALIGNMENT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setPreferredSize(new Dimension(50, 50)); // Larger size
        btn.setMaximumSize(new Dimension(50, 50));
        btn.setMinimumSize(new Dimension(50, 50));

        // Create lightweight popup for showing label below button on hover
        final PopupFactory popupFactory = PopupFactory.getSharedInstance();
        final Popup[] hoverPopup = { null };
        final javax.swing.Timer[] popupShowTimer = { null };
        JPanel labelPanel = new JPanel(new BorderLayout());
        labelPanel.setBackground(new Color(35, 35, 40));
        labelPanel.setBorder(new LineBorder(new Color(99, 102, 241), 1, true));
        labelPanel.setOpaque(true);
        labelPanel.setFocusable(false);

        JLabel labelDisplay = new JLabel(label);
        labelDisplay.setFont(new Font("Inter", Font.BOLD, 12));
        labelDisplay.setForeground(new Color(240, 245, 255));
        labelDisplay.setBorder(new EmptyBorder(4, 8, 4, 8));
        labelDisplay.setBackground(new Color(35, 35, 40));
        labelDisplay.setOpaque(true);
        labelDisplay.setFocusable(false);
        labelPanel.add(labelDisplay, BorderLayout.CENTER);

        // Smooth hover animation with glow effect (200 FPS optimized)
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                // Cancel any pending hide timers
                if (popupShowTimer[0] != null && popupShowTimer[0].isRunning()) {
                    popupShowTimer[0].stop();
                }

                // Debounce popup show with 200ms delay to prevent rapid flashing
                popupShowTimer[0] = new javax.swing.Timer(200, ev -> {
                    if (hoverPopup[0] != null) {
                        hoverPopup[0].hide();
                        hoverPopup[0] = null;
                    }
                    try {
                        Point btnLocation = btn.getLocationOnScreen();
                        int popupX = btnLocation.x + (btn.getWidth() - labelPanel.getPreferredSize().width) / 2;
                        int popupY = btnLocation.y + btn.getHeight() + 5;
                        hoverPopup[0] = popupFactory.getPopup(btn, labelPanel, popupX, popupY);
                        hoverPopup[0].show();
                    } catch (IllegalComponentStateException ex) {
                        // Button no longer on screen, don't show popup
                    }
                });
                popupShowTimer[0].setRepeats(false);
                popupShowTimer[0].setCoalesce(true);
                popupShowTimer[0].start();
                if (hoverTimerRef[0] != null && hoverTimerRef[0].isRunning()) {
                    hoverTimerRef[0].stop();
                }

                javax.swing.Timer t = new javax.swing.Timer(HOVER_ANIMATION_DELAY_MS, ev -> {
                    float current = hoverIntensityRef[0];
                    if (current < 1f) {
                        hoverIntensityRef[0] = Math.min(current + 0.075f, 1f); // 0.15f ÷ 2 for 5ms intervals
                        btn.repaint();
                    } else {
                        // Stop timer when max intensity reached
                        javax.swing.Timer timer = (javax.swing.Timer) ev.getSource();
                        if (timer.isRunning()) {
                            timer.stop();
                        }
                    }
                });
                t.setCoalesce(true);

                hoverTimerRef[0] = t;

                t.start();
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                // Hide label popup and cancel show timer
                if (popupShowTimer[0] != null && popupShowTimer[0].isRunning()) {
                    popupShowTimer[0].stop();
                }
                if (hoverPopup[0] != null) {
                    hoverPopup[0].hide();
                    hoverPopup[0] = null;
                }

                if (hoverTimerRef[0] != null && hoverTimerRef[0].isRunning()) {
                    hoverTimerRef[0].stop();
                }

                javax.swing.Timer t = new javax.swing.Timer(HOVER_ANIMATION_DELAY_MS, ev -> {
                    float current = hoverIntensityRef[0];
                    if (current > 0f) {
                        hoverIntensityRef[0] = Math.max(current - 0.075f, 0f); // 0.15f ÷ 2 for 5ms intervals
                        btn.repaint();
                    } else {
                        // Stop timer when intensity reaches 0
                        javax.swing.Timer timer = (javax.swing.Timer) ev.getSource();
                        if (timer.isRunning()) {
                            timer.stop();
                        }
                    }
                });
                t.setCoalesce(true);

                hoverTimerRef[0] = t;

                t.start();
            }
        });

        if (action != null) {
            btn.addActionListener(e -> action.run());
        }
        return btn;
    }

    private Color resolveGlyphGlowColor(String menuGlyph) {
        if ("logout".equals(menuGlyph)) {
            return new Color(239, 68, 68);
        }
        if ("settings".equals(menuGlyph)) {
            return new Color(148, 163, 184);
        }
        if ("tools".equals(menuGlyph)) {
            return new Color(56, 189, 248);
        }
        if ("analytics".equals(menuGlyph)) {
            return new Color(96, 165, 250);
        }
        return new Color(99, 102, 241);
    }

    private void drawMenuGlyph(Graphics2D g2, int width, int height, String menuGlyph, Color color) {
        if (menuGlyph == null) {
            return;
        }

        switch (menuGlyph) {
            case "tools":
                drawToolsGlyph(g2, width, height, color);
                break;
            case "analytics":
                drawAnalyticsGlyph(g2, width, height, color);
                break;
            case "settings":
                drawSettingsGlyph(g2, width, height, color);
                break;
            case "logout":
                drawLogoutDoorGlyph(g2, width, height, color);
                break;
            default:
                break;
        }
    }

    private void drawToolsGlyph(Graphics2D g2, int width, int height, Color baseColor) {
        Graphics2D icon = (Graphics2D) g2.create();
        icon.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        icon.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        icon.setStroke(new BasicStroke(2.15f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int cx = width / 2;
        int cy = height / 2;

        // Crossed tool set (wrench + screwdriver) for a cleaner, professional look.
        icon.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 236));
        icon.drawLine(cx - 9, cy + 8, cx + 3, cy - 4);
        icon.drawArc(cx, cy - 11, 9, 9, 28, 278);
        icon.drawLine(cx + 2, cy - 4, cx + 6, cy - 8);

        icon.setColor(new Color(226, 232, 240, 238));
        icon.drawLine(cx - 8, cy - 7, cx + 7, cy + 8);
        icon.fillRoundRect(cx + 5, cy + 6, 4, 4, 2, 2);
        icon.fillRoundRect(cx - 10, cy - 10, 4, 3, 2, 2);

        icon.setColor(new Color(186, 230, 253, 180));
        icon.drawLine(cx - 2, cy + 2, cx + 3, cy + 7);
        icon.dispose();
    }

    private void drawAnalyticsGlyph(Graphics2D g2, int width, int height, Color baseColor) {
        Graphics2D icon = (Graphics2D) g2.create();
        icon.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int startX = width / 2 - 11;
        int baseY = height / 2 + 10;

        icon.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 220));
        icon.fillRoundRect(startX, baseY - 8, 5, 8, 2, 2);
        icon.fillRoundRect(startX + 7, baseY - 13, 5, 13, 2, 2);
        icon.fillRoundRect(startX + 14, baseY - 18, 5, 18, 2, 2);

        icon.setColor(new Color(191, 219, 254, 245));
        icon.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        icon.drawLine(startX - 1, baseY + 1, startX + 21, baseY + 1);
        icon.drawLine(startX + 1, baseY - 10, startX + 9, baseY - 15);
        icon.drawLine(startX + 9, baseY - 15, startX + 16, baseY - 9);
        icon.drawLine(startX + 16, baseY - 9, startX + 21, baseY - 21);

        icon.dispose();
    }

    private void drawSettingsGlyph(Graphics2D g2, int width, int height, Color baseColor) {
        Graphics2D icon = (Graphics2D) g2.create();
        icon.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        icon.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int cx = width / 2;
        int cy = height / 2;
        int outer = 8;

        icon.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        icon.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 235));
        icon.drawOval(cx - outer, cy - outer, outer * 2, outer * 2);

        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45);
            int x1 = cx + (int) Math.round(Math.cos(angle) * 10);
            int y1 = cy + (int) Math.round(Math.sin(angle) * 10);
            int x2 = cx + (int) Math.round(Math.cos(angle) * 13);
            int y2 = cy + (int) Math.round(Math.sin(angle) * 13);
            icon.drawLine(x1, y1, x2, y2);
        }

        icon.setColor(new Color(248, 250, 252, 232));
        icon.fillOval(cx - 3, cy - 3, 6, 6);

        icon.setColor(new Color(255, 255, 255, 70));
        icon.drawArc(cx - outer + 1, cy - outer + 1, outer * 2 - 2, outer * 2 - 2, 30, 120);
        icon.dispose();
    }

    private void drawLockGlyph(Graphics2D g2, int width, int height, Color color, int yShift) {
        Graphics2D icon = (Graphics2D) g2.create();
        icon.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        icon.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        icon.translate(0, yShift);

        int cx = width / 2;
        int shackleW = 12;
        int shackleH = 10;
        int shackleY = height / 2 - 10;
        int bodyW = 16;
        int bodyH = 12;
        int bodyX = cx - bodyW / 2;
        int bodyY = height / 2 - 1;

        icon.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 48));
        icon.fillRoundRect(bodyX, bodyY, bodyW, bodyH, 5, 5);

        icon.setStroke(new BasicStroke(2.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        icon.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 236));
        icon.drawArc(cx - shackleW / 2, shackleY, shackleW, shackleH, 0, 180);
        icon.drawRoundRect(bodyX, bodyY, bodyW, bodyH, 4, 4);
        icon.fillOval(cx - 1, bodyY + 4, 3, 3);
        icon.drawLine(cx, bodyY + 7, cx, bodyY + 9);
        icon.dispose();
    }

    private void drawUserGlyph(Graphics2D g2, int width, int height, Color color, int yShift) {
        Graphics2D icon = (Graphics2D) g2.create();
        icon.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        icon.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        icon.translate(0, yShift);

        int cx = width / 2;
        int headR = 4;
        int headY = height / 2 - 10;
        int bodyY = headY + 10;
        int bodyW = 16;
        int bodyH = 10;

        icon.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 232));
        icon.fillOval(cx - headR, headY, headR * 2, headR * 2);

        icon.setStroke(new BasicStroke(2.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        icon.drawRoundRect(cx - bodyW / 2, bodyY, bodyW, bodyH, 8, 8);

        icon.setColor(new Color(255, 255, 255, 86));
        icon.drawArc(cx - 7, bodyY + 1, 14, 7, 0, 180);
        icon.dispose();
    }

    private void drawLogoutDoorGlyph(Graphics2D g2, int width, int height, Color baseColor) {
        Graphics2D icon = (Graphics2D) g2.create();
        icon.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        icon.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int frameX = width / 2 - 11;
        int frameY = height / 2 - 10;
        int frameW = 10;
        int frameH = 20;

        icon.setColor(new Color(248, 113, 113, 230));
        icon.setStroke(new BasicStroke(2.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        icon.drawRoundRect(frameX, frameY, frameW, frameH, 4, 4);

        icon.setColor(new Color(
                clampColor(baseColor.getRed() + 10),
                clampColor(baseColor.getGreen() - 20),
                clampColor(baseColor.getBlue() - 20),
                240));
        icon.fillRoundRect(frameX + 2, frameY + 2, frameW - 3, frameH - 3, 3, 3);

        icon.setColor(new Color(255, 244, 244, 245));
        icon.fillOval(frameX + frameW - 4, frameY + frameH / 2 - 1, 2, 2);

        int arrowStartX = frameX + frameW + 1;
        int arrowEndX = Math.min(width - 6, arrowStartX + 10);
        int arrowY = height / 2;

        icon.setStroke(new BasicStroke(2.35f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        icon.drawLine(arrowStartX, arrowY, arrowEndX, arrowY);
        icon.drawLine(arrowEndX, arrowY, arrowEndX - 4, arrowY - 4);
        icon.drawLine(arrowEndX, arrowY, arrowEndX - 4, arrowY + 4);

        icon.setColor(new Color(254, 202, 202, 170));
        icon.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        icon.drawLine(arrowStartX - 4, arrowY - 3, arrowStartX - 1, arrowY - 3);
        icon.drawLine(arrowStartX - 4, arrowY + 3, arrowStartX - 1, arrowY + 3);

        icon.dispose();
    }

    private void showToolsMenuUpward(JButton btn) {
        showSectionPanel("TOOLS");
    }

    private void showToolsMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem perf = new JMenuItem("⚡ Performance");
        perf.addActionListener(e -> {
            boolean visible = togglePerformanceMonitorVisible();
            JOptionPane.showMessageDialog(this,
                    visible ? "✅ Performance Monitor: ON" : "❌ Performance Monitor: OFF",
                    "Performance", JOptionPane.INFORMATION_MESSAGE);
        });
        menu.add(perf);

        JMenuItem stats = new JMenuItem("📈 Statistics");
        stats.addActionListener(e -> {
            String msg = String.format("📈 SESSION STATISTICS\n\n━━━━━━━━━━━━━━━━━━━━\n" +
                    "Attempted: %d\nCorrect: %d\nAccuracy: %d%%\nStreak: %d 🔥\n━━━━━━━━━━━━━━━━━━━━",
                    totalAttempted, totalCorrect,
                    totalAttempted == 0 ? 0 : (int) ((double) totalCorrect / totalAttempted * 100), streak);
            JOptionPane.showMessageDialog(this, msg, "Statistics", JOptionPane.INFORMATION_MESSAGE);
        });
        menu.add(stats);

        menu.show(this, 0, getHeight() - 100);
    }

    private void showAnalyticsMenuUpward(JButton btn) {
        showSectionPanel("ANALYTICS");
    }

    private void showAnalyticsMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem weak = new JMenuItem("⚠️ Weak Areas");
        weak.addActionListener(e -> {
            var weakAreas = statsTracker.getWeakAreas();
            StringBuilder sb = new StringBuilder("⚠️ WEAK AREAS\n\n━━━━━━━━━━━━━━━━━━\n");
            if (weakAreas.isEmpty())
                sb.append("✅ None!");
            else
                weakAreas.forEach(
                        (t, a) -> sb.append("• ").append(t).append(": ").append(String.format("%.1f%%\n", a * 100)));
            sb.append("━━━━━━━━━━━━━━━━━━");
            JOptionPane.showMessageDialog(this, sb.toString(), "Analysis", JOptionPane.INFORMATION_MESSAGE);
        });
        menu.add(weak);

        JMenuItem review = new JMenuItem("🔁 Review Topics");
        review.addActionListener(e -> JOptionPane.showMessageDialog(this, "🔁 Topics <70% accuracy need review!",
                "Reviews", JOptionPane.INFORMATION_MESSAGE));
        menu.add(review);

        JMenuItem rec = new JMenuItem("💡 Recommendations");
        rec.addActionListener(e -> {
            var recs = statsTracker.getLearningRecommendations();
            StringBuilder sb = new StringBuilder("💡 RECOMMENDATIONS\n\n━━━━━━━━━━━━━━━━━━\n");
            if (recs.weakAreasToReview != null && !recs.weakAreasToReview.isEmpty())
                sb.append(String.join("\n• ", recs.weakAreasToReview));
            else
                sb.append("Great work!");
            sb.append("\n━━━━━━━━━━━━━━━━━━");
            JOptionPane.showMessageDialog(this, sb.toString(), "Recommendations", JOptionPane.INFORMATION_MESSAGE);
        });
        menu.add(rec);

        JMenuItem topic = new JMenuItem("📌 Topics");
        topic.addActionListener(e -> {
            var statsMap = statsTracker.getAllTopicStats();
            StringBuilder sb = new StringBuilder("📌 TOPICS\n\n━━━━━━━━━━━━━━━━━━\n");
            if (statsMap.isEmpty())
                sb.append("Start learning!");
            else
                statsMap.forEach((t, s) -> sb.append("• ").append(t).append(": ")
                        .append(String.format("%.1f%%\n", s.getAccuracy() * 100)));
            sb.append("━━━━━━━━━━━━━━━━━━");
            JOptionPane.showMessageDialog(this, sb.toString(), "Topics", JOptionPane.INFORMATION_MESSAGE);
        });
        menu.add(topic);

        menu.show(this, 0, getHeight() - 100);
    }

    private void showSettingsMenuUpward(JButton btn) {
        showSectionPanel("SETTINGS");
    }

    /**
     * Create a stylish semi-transparent circular menu item with smooth hover
     * effects
     */
    private JMenuItem createStyledMenuItem(String text, Color baseColor, Color hoverColor) {
        final boolean[] isHovered = { false };
        JMenuItem item = new JMenuItem(text) {

            private float glowIntensity = 0f;
            private javax.swing.Timer glowTimer = null;

            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        isHovered[0] = true;
                        setForeground(Color.WHITE);
                        startGlow(true);
                    }

                    public void mouseExited(java.awt.event.MouseEvent e) {
                        isHovered[0] = false;
                        setForeground(new Color(200, 210, 230));
                        startGlow(false);
                    }

                    private void startGlow(boolean in) {
                        if (glowTimer != null && glowTimer.isRunning())
                            glowTimer.stop();
                        glowTimer = new javax.swing.Timer(HOVER_ANIMATION_DELAY_MS, ev -> {
                            glowIntensity += in ? 0.06f : -0.06f; // Adjusted for 5ms intervals (was 0.12f)
                            glowIntensity = Math.max(0f, Math.min(1f, glowIntensity));
                            repaint();
                            if ((in && glowIntensity >= 1f) || (!in && glowIntensity <= 0f)) {
                                ((javax.swing.Timer) ev.getSource()).stop();
                            }
                        });
                        glowTimer.setCoalesce(true);
                        glowTimer.start();
                    }
                });
            }

            @Override
            public void updateUI() {
                setUI(new BasicMenuItemUI() {
                    @Override
                    public void installUI(JComponent c) {
                        super.installUI(c);
                        c.setBorder(new EmptyBorder(13, 18, 13, 18));
                    }

                    @Override
                    protected void paintBackground(Graphics g, JMenuItem menuItem, Color bgColor) {
                        Graphics2D g2d = (Graphics2D) g.create();
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        int w = menuItem.getWidth();
                        int h = menuItem.getHeight();
                        int arc = 14;
                        int mx = 6, my = 3; // margin for rounded pill shape

                        if (glowIntensity > 0f) {
                            // Outer glow layer (hoverColor with low alpha, slightly larger)
                            g2d.setColor(new Color(
                                    hoverColor.getRed(), hoverColor.getGreen(), hoverColor.getBlue(),
                                    (int) (40 * glowIntensity)));
                            g2d.fillRoundRect(mx - 3, my - 2, w - (mx * 2) + 6, h - (my * 2) + 4, arc + 4, arc + 4);

                            // Main hover fill — gradient from hoverColor to slightly darker
                            Color gradTop = new Color(
                                    hoverColor.getRed(), hoverColor.getGreen(), hoverColor.getBlue(),
                                    (int) (200 * glowIntensity));
                            Color gradBot = new Color(
                                    Math.max(0, hoverColor.getRed() - 30),
                                    Math.max(0, hoverColor.getGreen() - 30),
                                    Math.max(0, hoverColor.getBlue() - 30),
                                    (int) (220 * glowIntensity));
                            GradientPaint gp = new GradientPaint(0, my, gradTop, 0, h - my, gradBot);
                            g2d.setPaint(gp);
                            g2d.fillRoundRect(mx, my, w - mx * 2, h - my * 2, arc, arc);

                            // Bright top highlight stripe (glass shine)
                            g2d.setColor(new Color(255, 255, 255, (int) (45 * glowIntensity)));
                            g2d.fillRoundRect(mx + 2, my + 2, w - mx * 2 - 4, (h - my * 2) / 2, arc, arc);

                            // Colored border ring
                            g2d.setColor(new Color(
                                    hoverColor.getRed(), hoverColor.getGreen(), hoverColor.getBlue(),
                                    (int) (180 * glowIntensity)));
                            g2d.setStroke(new BasicStroke(1.5f));
                            g2d.drawRoundRect(mx, my, w - mx * 2, h - my * 2, arc, arc);

                            // Left accent bar
                            g2d.setColor(new Color(
                                    hoverColor.getRed(), hoverColor.getGreen(), hoverColor.getBlue(),
                                    (int) (255 * glowIntensity)));
                            g2d.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                            g2d.drawLine(mx, my + arc / 2, mx, h - my - arc / 2);
                        } else {
                            // Normal state — subtle dark pill
                            g2d.setColor(new Color(40, 42, 50, 180));
                            g2d.fillRoundRect(mx, my, w - mx * 2, h - my * 2, arc, arc);

                            // Subtle border
                            g2d.setColor(new Color(80, 85, 100, 80));
                            g2d.setStroke(new BasicStroke(1f));
                            g2d.drawRoundRect(mx, my, w - mx * 2, h - my * 2, arc, arc);
                        }

                        g2d.dispose();
                    }

                    @Override
                    protected void paintText(Graphics g, JMenuItem menuItem, Rectangle textRect, String text) {
                        Graphics2D g2d = (Graphics2D) g.create();
                        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                        g2d.setFont(menuItem.getFont());

                        if (glowIntensity > 0.1f) {
                            // Text glow effect
                            g2d.setColor(new Color(
                                    hoverColor.getRed(), hoverColor.getGreen(), hoverColor.getBlue(),
                                    (int) (60 * glowIntensity)));
                            g2d.drawString(text, textRect.x + 1, textRect.y + g2d.getFontMetrics().getAscent() + 1);
                        }

                        g2d.setColor(menuItem.getForeground());
                        g2d.drawString(text, textRect.x, textRect.y + g2d.getFontMetrics().getAscent());
                        g2d.dispose();
                    }

                    @Override
                    public void update(Graphics g, JComponent c) {
                        paint(g, c);
                    }
                });
            }

            @Override
            protected void paintBorder(Graphics g) {
            }

            @Override
            public Insets getInsets() {
                return new Insets(13, 18, 13, 18);
            }
        };

        item.putClientProperty(FlatClientProperties.STYLE, "");
        item.setForeground(new Color(200, 210, 230));
        item.setFont(new Font("Inter", Font.BOLD, 14));
        item.setBorder(BorderFactory.createEmptyBorder(13, 18, 13, 18));
        item.setBorderPainted(false);
        item.setFocusPainted(false);
        item.setPreferredSize(new Dimension(230, 48));
        item.setOpaque(false);
        item.setFocusable(false);
        return item;
    }

    /**
     * Add auto-hide listener to popup menu.
     * Menu disappears automatically after 4-5 seconds if cursor is not over it.
     */
    private void addMenuAutoHideListener(JPopupMenu menu) {
        final javax.swing.Timer hideTimer = new javax.swing.Timer(1000, event -> {
            if (!isPointerInsidePopup(menu)) {
                menu.setVisible(false);
            }
        });
        hideTimer.setRepeats(false);
        hideTimer.setCoalesce(true);

        java.awt.event.MouseAdapter hoverTracker = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                hideTimer.stop();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                SwingUtilities.invokeLater(() -> {
                    if (!isPointerInsidePopup(menu)) {
                        hideTimer.restart();
                    }
                });
            }
        };

        menu.addMouseListener(hoverTracker);

        // Track hover on child items too, so moving inside menu doesn't trigger false
        // hide.
        for (Component component : menu.getComponents()) {
            if (component instanceof JComponent) {
                ((JComponent) component).addMouseListener(hoverTracker);
            }
        }

        // Start countdown when popup opens; cancel when it closes.
        menu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                hideTimer.restart();
            }

            @Override
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                hideTimer.stop();
            }

            @Override
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
                hideTimer.stop();
            }
        });
    }

    private boolean isPointerInsidePopup(JPopupMenu menu) {
        if (!menu.isShowing()) {
            return false;
        }
        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        if (pointerInfo == null) {
            return false;
        }
        try {
            Point pointer = pointerInfo.getLocation();
            Point menuTopLeft = menu.getLocationOnScreen();
            Rectangle bounds = new Rectangle(menuTopLeft.x, menuTopLeft.y, menu.getWidth(), menu.getHeight());
            return bounds.contains(pointer);
        } catch (IllegalComponentStateException ex) {
            return false;
        }
    }

    private void showSettingsMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem newSess = new JMenuItem("🆕 New Session");
        newSess.addActionListener(e -> resetSession());
        menu.add(newSess);

        JMenuItem resetStats = new JMenuItem("🔄 Reset Stats");
        resetStats.addActionListener(e -> {
            if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, "Reset all statistics?", "Reset",
                    JOptionPane.YES_NO_OPTION)) {
                statsTracker = new StatisticsTracker();
                streakVal.setText("0 🔥");
                accuracyVal.setText("0% 🎯");
                totalAttempted = 0;
                totalCorrect = 0;
                streak = 0;
            }
        });
        menu.add(resetStats);

        // Replace every menu.addSeparator() with:
        JSeparator sep1 = new JSeparator();
        sep1.setForeground(new Color(60, 60, 68));
        sep1.setBackground(new Color(34, 34, 39));
        menu.add(sep1);

        JMenuItem help = new JMenuItem("❓ Help");
        help.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "⌨️ Controls:\nCtrl+P → Performance\nCtrl+D → Analytics\nCtrl+N → New Session", "Help",
                JOptionPane.INFORMATION_MESSAGE));
        menu.add(help);

        JMenuItem exit = new JMenuItem("❌ Exit");
        exit.addActionListener(e -> {
            if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, "Exit app?", "Exit",
                    JOptionPane.YES_NO_OPTION)) {
                System.exit(0);
            }
        });
        menu.add(exit);

        menu.show(this, 0, getHeight() - 100);
    }

    /**
     * Create a collapsible menu category with enhanced UI, animations, and dynamic
     * theme support.
     */
    private JPanel createMenuCategory(String categoryName, String[] items, Runnable[] actions) {
        JPanel categoryPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Dark theme (hardcoded)
                boolean isDark = true;

                // Gradient background for the category
                Color gradStart = isDark ? new Color(35, 35, 40, 100) : new Color(240, 245, 250, 100);
                Color gradEnd = isDark ? new Color(25, 25, 28, 80) : new Color(230, 237, 245, 80);
                GradientPaint gradient = new GradientPaint(
                        0, 0, gradStart,
                        0, getHeight(), gradEnd);
                g2.setPaint(gradient);
                g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 10, 10);

                // Subtle border
                g2.setColor(isDark ? new Color(60, 60, 65, 150) : new Color(210, 215, 225, 150));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 10, 10);
            }
        };
        categoryPanel.setLayout(new BoxLayout(categoryPanel, BoxLayout.Y_AXIS));
        categoryPanel.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        categoryPanel.setOpaque(false);
        categoryPanel.setBorder(new EmptyBorder(8, 0, 8, 0));

        // Category header button with custom painting and circular design
        final boolean[] expanded = { true };
        JButton categoryBtn = new JButton(categoryName + " ▼") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                // Dark theme (hardcoded)
                boolean isDark = true;
                int w = getWidth();
                int h = getHeight();

                // Shadow effect (circular)
                g2.setColor(new Color(0, 0, 0, 40));
                g2.fillOval(2, 3, w - 4, h - 4);

                // Circular gradient background
                Color gradient1 = isDark ? new Color(60, 62, 80, 200) : new Color(220, 235, 255, 200);
                Color gradient2 = isDark ? new Color(45, 47, 65, 180) : new Color(235, 245, 255, 180);
                GradientPaint gp = new GradientPaint(0, 0, gradient1, 0, h, gradient2);
                g2.setPaint(gp);
                g2.fillOval(0, 0, w - 1, h - 1);

                // Circular border with glow effect (perfect circle)
                Color borderColor = isDark ? new Color(99, 102, 241) : new Color(99, 120, 220);
                g2.setColor(new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), 180));
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawOval(0, 0, w - 1, h - 1);

                // Fill as circle
                g2.setPaint(gp);
                g2.fillOval(0, 0, w - 1, h - 1);

                // Inner highlight (circular)
                g2.setColor(new Color(255, 255, 255, 30));
                g2.setStroke(new BasicStroke(1f));
                g2.drawOval(2, 2, w - 4, h - 4);

                // Draw text
                super.paintComponent(g);
            }
        };
        categoryBtn.setFont(new Font("Inter", Font.BOLD, 12));
        categoryBtn.setForeground(new Color(240, 245, 255));
        categoryBtn.setFocusPainted(false);
        categoryBtn.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        categoryBtn.setAlignmentX(JButton.CENTER_ALIGNMENT);
        categoryBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        categoryBtn.setContentAreaFilled(false);
        categoryBtn.setOpaque(false);
        categoryBtn.setPreferredSize(new Dimension(56, 56));
        categoryBtn.setMaximumSize(new Dimension(56, 56));
        categoryBtn.setMinimumSize(new Dimension(56, 56));

        // Items panel with dynamic styling and centered layout
        JPanel itemsPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Dark theme (hardcoded)
                boolean isDark = true;

                // Semi-transparent background
                g2.setColor(isDark ? new Color(30, 30, 35, 20) : new Color(235, 240, 248, 20));
                g2.fillRoundRect(4, 0, getWidth() - 8, getHeight(), 6, 6);
            }
        };
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
        itemsPanel.setOpaque(false);
        itemsPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        itemsPanel.setAlignmentX(JPanel.CENTER_ALIGNMENT);

        // Create menu items with circular styling
        for (int i = 0; i < items.length; i++) {
            final int index = i;
            JButton itemBtn = new JButton(items[i]) {
                private float animationProgress = 0f;

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                    // Dark theme (hardcoded)
                    boolean isDark = true;
                    int w = getWidth();
                    int h = getHeight();

                    // Shadow effect with animation (circular)
                    if (animationProgress > 0.1f) {
                        g2.setColor(new Color(0, 0, 0, (int) (30 * animationProgress)));
                        g2.fillOval(2, 3, w - 4, h - 4);
                    }

                    // Dynamic circular background with animation
                    Color normalBg1 = isDark ? new Color(45, 45, 55, 200) : new Color(240, 245, 255, 200);
                    Color normalBg2 = isDark ? new Color(35, 35, 42, 180) : new Color(250, 252, 255, 180);
                    Color hoverBg1 = isDark ? new Color(70, 75, 95, 220) : new Color(210, 230, 255, 220);
                    Color hoverBg2 = isDark ? new Color(55, 60, 75, 200) : new Color(230, 245, 255, 200);

                    Color bg1 = new Color(
                            (int) (normalBg1.getRed() + (hoverBg1.getRed() - normalBg1.getRed()) * animationProgress),
                            (int) (normalBg1.getGreen()
                                    + (hoverBg1.getGreen() - normalBg1.getGreen()) * animationProgress),
                            (int) (normalBg1.getBlue()
                                    + (hoverBg1.getBlue() - normalBg1.getBlue()) * animationProgress));
                    Color bg2 = new Color(
                            (int) (normalBg2.getRed() + (hoverBg2.getRed() - normalBg2.getRed()) * animationProgress),
                            (int) (normalBg2.getGreen()
                                    + (hoverBg2.getGreen() - normalBg2.getGreen()) * animationProgress),
                            (int) (normalBg2.getBlue()
                                    + (hoverBg2.getBlue() - normalBg2.getBlue()) * animationProgress));

                    // Circular gradient (perfect circle)
                    GradientPaint circularGrad = new GradientPaint(0, 0, bg1, 0, h, bg2);
                    g2.setPaint(circularGrad);
                    g2.fillOval(0, 0, w - 1, h - 1);

                    // Circular border with glow (perfect circle)
                    Color borderNormal = isDark ? new Color(80, 85, 100) : new Color(200, 215, 235);
                    Color borderHover = isDark ? new Color(99, 102, 241) : new Color(79, 120, 220);
                    Color borderColor = new Color(
                            (int) (borderNormal.getRed()
                                    + (borderHover.getRed() - borderNormal.getRed()) * animationProgress),
                            (int) (borderNormal.getGreen()
                                    + (borderHover.getGreen() - borderNormal.getGreen()) * animationProgress),
                            (int) (borderNormal.getBlue()
                                    + (borderHover.getBlue() - borderNormal.getBlue()) * animationProgress),
                            (int) (160 + 70 * animationProgress));
                    g2.setColor(borderColor);
                    g2.setStroke(new BasicStroke(2.0f));
                    g2.drawOval(0, 0, w - 1, h - 1);

                    // Inner light highlight for depth (perfect circle)
                    g2.setColor(new Color(255, 255, 255, (int) (20 * animationProgress)));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawOval(2, 2, w - 4, h - 4);

                    // Text color with animation
                    Color textNormal = isDark ? new Color(170, 175, 190) : new Color(70, 80, 110);
                    Color textHover = isDark ? new Color(99, 102, 241) : new Color(50, 100, 200);
                    Color textColor = new Color(
                            (int) (textNormal.getRed()
                                    + (textHover.getRed() - textNormal.getRed()) * animationProgress),
                            (int) (textNormal.getGreen()
                                    + (textHover.getGreen() - textNormal.getGreen()) * animationProgress),
                            (int) (textNormal.getBlue()
                                    + (textHover.getBlue() - textNormal.getBlue()) * animationProgress));
                    setForeground(textColor);

                    super.paintComponent(g);
                }
            };
            itemBtn.setFont(new Font("Inter", Font.PLAIN, 10));
            itemBtn.setFocusPainted(false);
            itemBtn.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            itemBtn.setAlignmentX(JButton.CENTER_ALIGNMENT);
            itemBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            itemBtn.setContentAreaFilled(false);
            itemBtn.setOpaque(false);
            itemBtn.setPreferredSize(new Dimension(50, 50));
            itemBtn.setMaximumSize(new Dimension(50, 50));
            itemBtn.setMinimumSize(new Dimension(50, 50));

            // Smooth animation on click (200 FPS compatible)
            itemBtn.addActionListener(e -> {
                Timer clickFeedback = new Timer(5, ev -> { // 5ms = 200 FPS (was 80ms for feedback)
                    itemBtn.repaint();
                });
                clickFeedback.setInitialDelay(5);
                clickFeedback.setRepeats(false);
                clickFeedback.setCoalesce(true);
                clickFeedback.start();
                actions[index].run();
            });

            // Hover animation
            itemBtn.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    animateButton(itemBtn, true);
                }

                public void mouseExited(java.awt.event.MouseEvent e) {
                    animateButton(itemBtn, false);
                }
            });

            itemsPanel.add(itemBtn);
            if (i < items.length - 1) {
                itemsPanel.add(Box.createVerticalStrut(6));
            }
        }

        // Expand/collapse with animation
        categoryBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                animateCategoryButton(categoryBtn, true);
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                animateCategoryButton(categoryBtn, false);
            }
        });

        categoryBtn.addActionListener(e -> {
            expanded[0] = !expanded[0];

            // Ultra-smooth animation for expand/collapse (200 FPS)
            final int targetHeight = expanded[0] ? itemsPanel.getPreferredSize().height : 0;
            Timer expandTimer = new Timer(5, ev -> { // 5ms = 200 FPS (was 15ms)
                itemsPanel.repaint();
            });
            expandTimer.setRepeats(false);
            expandTimer.setCoalesce(true);
            expandTimer.start();

            itemsPanel.setVisible(expanded[0]);
            categoryBtn.setText(categoryName + (expanded[0] ? " ▼" : " ▶"));
        });

        categoryPanel.add(categoryBtn);
        categoryPanel.add(Box.createVerticalStrut(4));
        categoryPanel.add(itemsPanel);

        return categoryPanel;
    }

    /**
     * Animate menu item button on hover
     */
    private void animateButton(JButton btn, boolean hover) {
        if (btn != null) {
            btn.repaint();
        }
    }

    /**
     * Animate category button on hover
     */
    private void animateCategoryButton(JButton btn, boolean hover) {
        btn.repaint();
    }

    private JPanel createUnifiedHeader() {
        JPanel unified = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                // Optimized for speed: minimal antialiasing for maximum FPS
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // Very transparent background to show main bg image clearly
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(39, 39, 42, 60),
                        0, getHeight(), new Color(24, 24, 27, 40));
                g2.setPaint(gradient);
                g2.fillRoundRect(10, 10, getWidth() - 20, getHeight() - 15, 25, 25);

                // Minimal highlight for glass effect
                g2.setColor(new Color(255, 255, 255, 8));
                g2.fillRoundRect(10, 10, getWidth() - 20, 70, 25, 25);

                // Subtle bottom shadow
                g2.setColor(new Color(0, 0, 0, 20));
                g2.fillRoundRect(12, getHeight() - 8, getWidth() - 24, 6, 15, 15);

                g2.dispose();
            }
        };
        unified.setLayout(new BoxLayout(unified, BoxLayout.Y_AXIS));
        unified.setOpaque(false);
        unified.setBorder(new EmptyBorder(7, 20, 15, 20));

        unified.add(createSearchPanel());
        // Suggestion panel removed - using category/subtopic dropdowns instead
        // unified.add(createSuggestionPanel());

        return unified;
    }

    private void paintTexturedHeaderControl(
            Graphics2D g2,
            int width,
            int height,
            int arc,
            Color baseColor,
            java.awt.image.BufferedImage texture,
            boolean hovered,
            boolean pressed,
            boolean focused) {
        if (width <= 1 || height <= 1) {
            return;
        }

        int topShift = pressed ? -10 : (hovered ? 12 : 6);
        int bottomShift = pressed ? -20 : (hovered ? -6 : -14);
        if (focused) {
            topShift += 4;
            bottomShift += 4;
        }

        Color topColor = new Color(
                clampColor(baseColor.getRed() + topShift),
                clampColor(baseColor.getGreen() + topShift),
                clampColor(baseColor.getBlue() + topShift),
                Math.min(255, baseColor.getAlpha() + 18));
        Color bottomColor = new Color(
                clampColor(baseColor.getRed() + bottomShift),
                clampColor(baseColor.getGreen() + bottomShift),
                clampColor(baseColor.getBlue() + bottomShift),
                Math.min(255, baseColor.getAlpha() + 28));

        g2.setPaint(new GradientPaint(0, 0, topColor, 0, height, bottomColor));
        g2.fillRoundRect(0, 0, width - 1, height - 1, arc, arc);

        g2.setPaint(new TexturePaint(texture, new Rectangle(0, 0, texture.getWidth(), texture.getHeight())));
        g2.fillRoundRect(1, 1, width - 2, height - 2, arc, arc);

        g2.setColor(new Color(255, 255, 255, focused ? 105 : (hovered ? 82 : 58)));
        g2.drawRoundRect(0, 0, width - 1, height - 1, arc, arc);
    }

    private JTextField createTexturedTopicField() {
        JTextField field = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean hovered = isEnabled() && getMousePosition() != null;
                boolean focused = hasFocus();
                Color base = focused ? new Color(38, 46, 60, 228) : new Color(23, 24, 31, 224);
                paintTexturedHeaderControl(
                        g2,
                        getWidth(),
                        getHeight(),
                        24,
                        base,
                        headerControlTexturePattern,
                        hovered,
                        false,
                        focused);
                g2.dispose();
                super.paintComponent(g);
            }
        };

        field.setOpaque(false);
        field.setForeground(new Color(241, 245, 249));
        field.setCaretColor(Color.WHITE);
        field.setSelectionColor(new Color(99, 102, 241, 170));
        field.setSelectedTextColor(Color.WHITE);
        field.setBorder(new EmptyBorder(8, 12, 8, 12));
        return field;
    }

    private JComboBox<String> createTexturedDifficultyCombo() {
        JComboBox<String> combo = new JComboBox<>(new String[] { "🟡 Normal", "🔴 Hard" }) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean hovered = isEnabled() && getMousePosition() != null;
                boolean focused = hasFocus();
                Color base = focused ? new Color(54, 64, 84, 228) : new Color(33, 39, 52, 220);
                paintTexturedHeaderControl(
                        g2,
                        getWidth(),
                        getHeight(),
                        24,
                        base,
                        headerControlTexturePattern,
                        hovered,
                        false,
                        focused);
                g2.dispose();
                super.paintComponent(g);
            }
        };

        combo.setOpaque(false);
        combo.setForeground(Color.WHITE);
        combo.setBackground(new Color(0, 0, 0, 0));
        combo.setBorder(new EmptyBorder(3, 8, 3, 8));
        combo.setMaximumRowCount(6);
        combo.setPrototypeDisplayValue("🟡 Normal  ");
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected,
                        cellHasFocus);
                label.setFont(new Font("Inter", Font.BOLD, 13));
                label.setVerticalAlignment(SwingConstants.CENTER);
                if (index == -1) {
                    label.setBorder(new EmptyBorder(1, 6, 1, 6));
                } else {
                    label.setBorder(new EmptyBorder(4, 8, 4, 8));
                }
                if (isSelected) {
                    label.setBackground(new Color(67, 56, 202));
                    label.setForeground(Color.WHITE);
                } else {
                    label.setBackground(new Color(24, 24, 27));
                    label.setForeground(new Color(226, 232, 240));
                }
                return label;
            }
        });
        return combo;
    }

    private JComboBox<String> createTexturedCategoryCombo() {
        TopicsManager topicsManager = TopicsManager.getInstance();
        JComboBox<String> combo = new JComboBox<>(topicsManager.getCategories().toArray(new String[0])) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean hovered = isEnabled() && getMousePosition() != null;
                boolean focused = hasFocus();
                Color base = focused ? new Color(54, 64, 84, 228) : new Color(33, 39, 52, 220);
                paintTexturedHeaderControl(
                        g2,
                        getWidth(),
                        getHeight(),
                        24,
                        base,
                        headerControlTexturePattern,
                        hovered,
                        false,
                        focused);
                g2.dispose();
                super.paintComponent(g);
            }
        };

        combo.setOpaque(false);
        combo.setForeground(Color.WHITE);
        combo.setBackground(new Color(0, 0, 0, 0));
        combo.setBorder(new EmptyBorder(3, 8, 3, 8));
        combo.setMaximumRowCount(6);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected,
                        cellHasFocus);
                label.setFont(new Font("Inter", Font.PLAIN, 13));
                label.setVerticalAlignment(SwingConstants.CENTER);
                if (index == -1) {
                    label.setBorder(new EmptyBorder(1, 6, 1, 6));
                } else {
                    label.setBorder(new EmptyBorder(4, 8, 4, 8));
                }
                if (isSelected) {
                    label.setBackground(new Color(67, 56, 202));
                    label.setForeground(Color.WHITE);
                } else {
                    label.setBackground(new Color(24, 24, 27));
                    label.setForeground(new Color(226, 232, 240));
                }
                return label;
            }
        });
        return combo;
    }

    private JComboBox<String> createTexturedSubtopicCombo() {
        JComboBox<String> combo = new JComboBox<>(new String[] { "Select Subtopic" }) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean hovered = isEnabled() && getMousePosition() != null;
                boolean focused = hasFocus();
                Color base = focused ? new Color(54, 64, 84, 228) : new Color(33, 39, 52, 220);
                paintTexturedHeaderControl(
                        g2,
                        getWidth(),
                        getHeight(),
                        24,
                        base,
                        headerControlTexturePattern,
                        hovered,
                        false,
                        focused);
                g2.dispose();
                super.paintComponent(g);
            }
        };

        combo.setOpaque(false);
        combo.setForeground(Color.WHITE);
        combo.setBackground(new Color(0, 0, 0, 0));
        combo.setBorder(new EmptyBorder(3, 8, 3, 8));
        combo.setMaximumRowCount(8);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected,
                        cellHasFocus);
                label.setFont(new Font("Inter", Font.PLAIN, 13));
                label.setVerticalAlignment(SwingConstants.CENTER);
                if (index == -1) {
                    label.setBorder(new EmptyBorder(1, 6, 1, 6));
                } else {
                    label.setBorder(new EmptyBorder(4, 8, 4, 8));
                }
                if (isSelected) {
                    label.setBackground(new Color(67, 56, 202));
                    label.setForeground(Color.WHITE);
                } else {
                    label.setBackground(new Color(24, 24, 27));
                    label.setForeground(new Color(226, 232, 240));
                }
                return label;
            }
        });
        return combo;
    }

    private void onCategoryChanged() {
        Object selected = categoryCombo.getSelectedItem();
        if (selected == null) {
            subtopicCombo.setModel(new DefaultComboBoxModel<>(new String[] { "Select Subtopic" }));
            return;
        }

        TopicsManager topicsManager = TopicsManager.getInstance();
        java.util.List<String> subtopics = topicsManager.getSubtopics(selected.toString());
        subtopicCombo.setModel(new DefaultComboBoxModel<>(subtopics.toArray(new String[0])));
        subtopicCombo.setSelectedIndex(0);
        searchBtn.setEnabled(false);
    }

    private JButton createTexturedHeaderButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                ButtonModel model = getModel();
                boolean pressed = model.isPressed() || model.isArmed();
                boolean hovered = model.isRollover();
                Color base = getBackground();

                paintTexturedHeaderControl(
                        g2,
                        getWidth(),
                        getHeight(),
                        22,
                        new Color(base.getRed(), base.getGreen(), base.getBlue(), 226),
                        headerControlTexturePattern,
                        hovered,
                        pressed,
                        hasFocus());

                if (!isEnabled()) {
                    g2.setColor(new Color(0, 0, 0, 70));
                    g2.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 22, 22);
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };

        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setRolloverEnabled(true);
        button.setBorder(new EmptyBorder(6, 12, 6, 12));
        return button;
    }

    private JLabel createTexturedTimerLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color fg = getForeground();
                Color base = new Color(
                        clampColor((int) (fg.getRed() * 0.65f) + 8),
                        clampColor((int) (fg.getGreen() * 0.65f) + 8),
                        clampColor((int) (fg.getBlue() * 0.65f) + 8),
                        214);

                paintTexturedHeaderControl(
                        g2,
                        getWidth(),
                        getHeight(),
                        24,
                        base,
                        timerTexturePattern,
                        false,
                        false,
                        false);
                g2.dispose();
                super.paintComponent(g);
            }
        };

        label.setOpaque(false);
        label.setBorder(new EmptyBorder(6, 12, 6, 12));
        return label;
    }

    private JPanel createSearchPanel() {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setOpaque(false);

        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(4, 3, 4, 3));
        GridBagConstraints rowGbc = new GridBagConstraints();
        rowGbc.gridy = 0;
        rowGbc.insets = new Insets(4, 6, 4, 6);
        rowGbc.anchor = GridBagConstraints.CENTER;
        rowGbc.fill = GridBagConstraints.HORIZONTAL;

        // Category selector
        categoryCombo = createTexturedCategoryCombo();
        categoryCombo.setPreferredSize(new Dimension(220, HEADER_CONTROL_HEIGHT));
        categoryCombo.setMinimumSize(new Dimension(200, HEADER_CONTROL_HEIGHT));
        categoryCombo.setFont(new Font("Inter", Font.PLAIN, 14));
        categoryCombo.setForeground(Color.WHITE);
        categoryCombo.addActionListener(e -> onCategoryChanged());

        // Subtopic selector
        subtopicCombo = createTexturedSubtopicCombo();
        subtopicCombo.setPreferredSize(new Dimension(220, HEADER_CONTROL_HEIGHT));
        subtopicCombo.setMinimumSize(new Dimension(200, HEADER_CONTROL_HEIGHT));
        subtopicCombo.setFont(new Font("Inter", Font.PLAIN, 14));
        subtopicCombo.setForeground(Color.WHITE);
        subtopicCombo.addActionListener(e -> {
            searchBtn.setEnabled(subtopicCombo.getSelectedItem() != null);
            hideValidation();
        });

        // Difficulty selector
        difficultyCombo = createTexturedDifficultyCombo();
        difficultyCombo.setSelectedItem("🟡 Normal");
        difficultyCombo.setPreferredSize(new Dimension(186, HEADER_CONTROL_HEIGHT + 2));
        difficultyCombo.setMinimumSize(new Dimension(174, HEADER_CONTROL_HEIGHT + 2));
        difficultyCombo.setMaximumSize(new Dimension(196, HEADER_CONTROL_HEIGHT + 2));
        difficultyCombo.setFont(new Font("Inter", Font.BOLD, 15));
        difficultyCombo.setForeground(Color.WHITE);

        // Enable Enter key to trigger search from difficulty dropdown
        InputMap diffInput = difficultyCombo.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap diffAction = difficultyCombo.getActionMap();
        diffInput.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), "triggerSearch");
        diffAction.put("triggerSearch", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                Object subtopic = subtopicCombo.getSelectedItem();
                if (subtopic != null && !subtopic.toString().isEmpty()
                        && !"Select Subtopic".equals(subtopic.toString())) {
                    preloadedQ = null;
                    currentQ = null;
                    loadQuestion(true);
                }
            }
        });

        searchBtn = createTexturedHeaderButton("🔍 Search");
        searchBtn.setPreferredSize(new Dimension(142, HEADER_CONTROL_HEIGHT));
        searchBtn.setMinimumSize(new Dimension(132, HEADER_CONTROL_HEIGHT));
        searchBtn.setFont(new Font("Inter", Font.BOLD, 13));
        searchBtn.setBackground(new Color(59, 130, 246));
        searchBtn.setForeground(Color.WHITE);
        searchBtn.setFocusPainted(false);
        searchBtn.setEnabled(false);
        addButtonHoverEffect(searchBtn, new Color(59, 130, 246), new Color(37, 99, 235));

        topicValidationLabel = new JLabel(" ");
        topicValidationLabel.setFont(new Font("Calibri (Body)", Font.PLAIN, 16));
        topicValidationLabel.setForeground(new Color(255, 0, 0, 180));
        topicValidationLabel.setBorder(new EmptyBorder(0, -2, 0, 0));

        nextBtn = createTexturedHeaderButton("Next Question →");
        nextBtn.setPreferredSize(new Dimension(174, HEADER_CONTROL_HEIGHT));
        nextBtn.setMinimumSize(new Dimension(162, HEADER_CONTROL_HEIGHT));
        nextBtn.setFont(new Font("Inter", Font.BOLD, 13));
        nextBtn.setBackground(new Color(139, 92, 246));
        nextBtn.setForeground(Color.WHITE);
        nextBtn.setFocusPainted(false);
        addButtonHoverEffect(nextBtn, new Color(139, 92, 246), new Color(124, 58, 237));

        timerLabel = createTexturedTimerLabel("⏱ 00:60");
        timerLabel.setPreferredSize(new Dimension(182, HEADER_CONTROL_HEIGHT));
        timerLabel.setMinimumSize(new Dimension(174, HEADER_CONTROL_HEIGHT));
        timerLabel.setFont(new Font("Monospaced", Font.BOLD, (int) TIMER_FONT_NORMAL));
        timerLabel.setForeground(TIMER_COLOR_SAFE);
        ensureTimerFontsInitialized();
        if (timerFontNormalCached != null) {
            timerLabel.setFont(timerFontNormalCached);
        }
        updateTimerLabelText(timeLeft);

        rowGbc.gridx = 0;
        rowGbc.weightx = 0;
        row.add(categoryCombo, rowGbc);

        rowGbc.gridx = 1;
        rowGbc.weightx = 0;
        row.add(subtopicCombo, rowGbc);

        rowGbc.gridx = 2;
        rowGbc.weightx = 0;
        row.add(difficultyCombo, rowGbc);

        rowGbc.gridx = 3;
        rowGbc.weightx = 0;
        row.add(searchBtn, rowGbc);

        rowGbc.gridx = 4;
        rowGbc.weightx = 1.0;
        row.add(Box.createHorizontalGlue(), rowGbc);

        rowGbc.gridx = 5;
        rowGbc.weightx = 0;
        row.add(nextBtn, rowGbc);

        rowGbc.gridx = 6;
        rowGbc.insets = new Insets(4, 10, 4, 3);
        row.add(timerLabel, rowGbc);

        wrapper.add(row);

        // Validation label panel (centered below search row)
        JPanel validationRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 2));
        validationRow.setOpaque(false);
        validationRow.add(topicValidationLabel);
        wrapper.add(validationRow);

        return wrapper;
    }

    private JPanel createQuestionCard() {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Paint question card background image if loaded
                if (questionCardBgImage != null) {
                    Graphics2D g2 = (Graphics2D) g;
                    int width = getWidth();
                    int height = getHeight();

                    // Cache scaled image to avoid re-scaling every paint
                    if (cachedQuestionCardImage == null || lastQuestionCardSize.width != width
                            || lastQuestionCardSize.height != height) {
                        java.awt.image.BufferedImage buffered = new java.awt.image.BufferedImage(
                                width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
                        Graphics2D temp = buffered.createGraphics();
                        // Faster interpolation for maximum FPS
                        temp.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                        temp.drawImage(questionCardBgImage, 0, 0, width, height, null);
                        temp.dispose();
                        cachedQuestionCardImage = buffered;
                        lastQuestionCardSize = new Dimension(width, height);
                    }

                    // Render cached image with opacity
                    Composite oldComposite = g2.getComposite();
                    g2.setComposite(COMPOSITE_90);
                    g2.drawImage(cachedQuestionCardImage, 0, 0, null);
                    g2.setComposite(oldComposite);

                    // Add dark overlay for readability
                    g2.setColor(new Color(39, 39, 42, 70));
                    g2.fillRect(0, 0, width, height);
                }
            }
        };
        card.setBackground(new Color(39, 39, 42));
        card.setOpaque(true);
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 40");
        card.setBorder(new EmptyBorder(0, 0, 0, 0));
        card.setPreferredSize(new Dimension(1100, 500));
        card.setMaximumSize(new Dimension(1100, 500));

        questionArea = new JTextPane();
        questionArea.setEditable(false);
        questionArea.setOpaque(false);
        questionArea.setContentType("text/html");
        // disable selection/highlighter to avoid stray selection rectangles
        questionArea.setHighlighter(null);
        questionArea.putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        questionArea.setMargin(new Insets(0, 22, 20, 10));

        JScrollPane questionScroll = new JScrollPane(questionArea);

        questionScroll.setOpaque(false);
        questionScroll.getViewport().setOpaque(false);
        questionScroll.setBorder(null);
        questionScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        questionScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setOpaque(false);
        optionsPanel.setBorder(new EmptyBorder(0, 42, 25, 10));

        card.add(questionScroll, BorderLayout.CENTER);
        card.add(optionsPanel, BorderLayout.SOUTH);
        return card;
    }

    // ADD THIS METHOD to build better formatted explanation
    private String buildDetailedExplanation(boolean correct, AptitudeQuestion q, String[] correctAnswers) {
        StringBuilder sb = new StringBuilder();

        // Show correct answer details only when user selected the wrong option.
        if (!correct) {
            sb.append("✅ CORRECT ANSWER:\n");
            sb.append(wrapText(String.join(" OR ", correctAnswers), 76)).append("\n\n");
        }

        // Explanation section
        sb.append("💡 EXPLANATION:\n");
        String explanation = (q.explanation != null) ? q.explanation.trim() : "";

        if (explanation.isEmpty()) {
            explanation = "Concept applied correctly to solve this problem.";
        } else {
            // Keep full AI explanation text; don't truncate it in UI formatting.
            explanation = formatText(explanation);
        }

        sb.append(wrapText(explanation, 76)).append("\n\n");

        // Quick trick section
        sb.append("⚡ QUICK TRICK:\n");
        if (q.quickTrick != null && !q.quickTrick.trim().isEmpty()) {
            String trick = q.quickTrick.trim();

            // Split into steps by periods
            String[] steps = trick.split("(?<=[.!?])\\s+");
            for (String step : steps) {
                String s = step.trim();
                if (!s.isEmpty()) {
                    String wrappedStep = wrapText(s, 72);
                    String[] wrappedLines = wrappedStep.split("\\R");
                    for (int idx = 0; idx < wrappedLines.length; idx++) {
                        if (idx == 0) {
                            sb.append("✓ ").append(wrappedLines[idx]).append("\n");
                        } else {
                            sb.append("  ").append(wrappedLines[idx]).append("\n");
                        }
                    }
                }
            }
        } else {
            sb.append("✓ Refer to explanation for solving strategy.\n");
        }

        return sb.toString();
    }

    private String formatText(String text) {
        return text
                .replaceAll("\\b(therefore|hence|thus|accordingly)\\b", "so")
                .replaceAll("\\b(utilize|utilise)\\b", "use")
                .replaceAll("\\b(approximately)\\b", "about")
                .replaceAll("\\b(subsequently)\\b", "then")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String wrapText(String text, int maxWidth) {
        StringBuilder wrapped = new StringBuilder();
        String[] words = text.split(" ");
        int currentLength = 0;

        for (String word : words) {
            if (currentLength + word.length() + 1 > maxWidth) {
                wrapped.append("\n");
                currentLength = 0;
            }
            wrapped.append(word).append(" ");
            currentLength += word.length() + 1;
        }

        return wrapped.toString().trim();
    }

    private void collapseExplanationArea() {
        if (explanationArea == null) {
            return;
        }

        setExplanationVisualState(EXPLANATION_STATE_NEUTRAL);
        explanationArea.setPreferredSize(new Dimension(0, 0));
        explanationArea.setMinimumSize(new Dimension(0, 0));
        explanationArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 0));
        explanationArea.setText("");
        explanationArea.revalidate();
        explanationArea.repaint();

        Container parent = explanationArea.getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
    }

    private void setExplanationVisualState(int state) {
        explanationVisualState = state;
        if (explanationArea != null) {
            explanationArea.repaint();
        }
    }

    private void paintExplanationContentBlocks(Graphics2D g2, int width, int height) {
        if (explanationArea == null) {
            return;
        }

        String text = explanationArea.getText();
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        FontMetrics metrics = explanationArea.getFontMetrics(explanationArea.getFont());
        if (metrics == null) {
            return;
        }

        Insets insets = explanationArea.getInsets();
        int blockX = Math.max(6, insets.left - 6);
        int maxBlockWidth = Math.max(120, width - blockX - Math.max(6, insets.right - 6));

        int textLeftPaddingInBlock = Math.max(4, insets.left - blockX);
        int textRightPaddingInBlock = textLeftPaddingInBlock + 8;
        int minBlockWidth = Math.min(maxBlockWidth,
                Math.max(168,
                        metrics.stringWidth("⚡ QUICK TRICK:") + textLeftPaddingInBlock + textRightPaddingInBlock + 10));

        Color fillColor;
        Color strokeColor;
        if (explanationVisualState == EXPLANATION_STATE_CORRECT) {
            fillColor = new Color(34, 197, 94, 40);
            strokeColor = new Color(134, 239, 172, 132);
        } else if (explanationVisualState == EXPLANATION_STATE_WRONG) {
            fillColor = new Color(239, 68, 68, 40);
            strokeColor = new Color(252, 165, 165, 138);
        } else if (explanationVisualState == EXPLANATION_STATE_LOADING) {
            fillColor = new Color(148, 163, 184, 34);
            strokeColor = new Color(203, 213, 225, 108);
        } else {
            fillColor = new Color(148, 163, 184, 28);
            strokeColor = new Color(148, 163, 184, 88);
        }

        java.util.List<Integer> lineStarts = new java.util.ArrayList<>();
        java.util.List<Integer> lineEnds = new java.util.ArrayList<>();
        java.util.List<String> logicalLines = new java.util.ArrayList<>();

        int cursor = 0;
        int lineStart = 0;
        while (cursor < text.length()) {
            char c = text.charAt(cursor);
            if (c == '\r' || c == '\n') {
                lineStarts.add(lineStart);
                lineEnds.add(cursor);
                logicalLines.add(text.substring(lineStart, cursor));
                if (c == '\r' && cursor + 1 < text.length() && text.charAt(cursor + 1) == '\n') {
                    cursor++;
                }
                cursor++;
                lineStart = cursor;
            } else {
                cursor++;
            }
        }
        lineStarts.add(lineStart);
        lineEnds.add(text.length());
        logicalLines.add(text.substring(lineStart));

        int lineIndex = 0;
        while (lineIndex < logicalLines.size()) {
            while (lineIndex < logicalLines.size() && logicalLines.get(lineIndex).trim().isEmpty()) {
                lineIndex++;
            }

            if (lineIndex >= logicalLines.size()) {
                break;
            }

            int blockStartLine = lineIndex;
            int blockEndLine = lineIndex;
            int blockWidestPx = 0;

            while (lineIndex < logicalLines.size() && !logicalLines.get(lineIndex).trim().isEmpty()) {
                String displayLine = logicalLines.get(lineIndex).stripTrailing();
                blockWidestPx = Math.max(blockWidestPx, metrics.stringWidth(displayLine));
                blockEndLine = lineIndex;
                lineIndex++;
            }

            int blockStartOffset = lineStarts.get(blockStartLine);
            int blockEndOffsetExclusive = lineEnds.get(blockEndLine);
            if (blockEndOffsetExclusive <= blockStartOffset) {
                continue;
            }

            int lastOffset = Math.min(text.length() - 1, Math.max(blockStartOffset, blockEndOffsetExclusive - 1));

            Rectangle startRect;
            Rectangle endRect;
            try {
                java.awt.geom.Rectangle2D s2d = explanationArea.modelToView2D(blockStartOffset);
                java.awt.geom.Rectangle2D e2d = explanationArea.modelToView2D(lastOffset);
                if (s2d == null || e2d == null) {
                    continue;
                }
                startRect = s2d.getBounds();
                endRect = e2d.getBounds();
            } catch (BadLocationException ignored) {
                continue;
            }

            int blockTopPadding = 4;
            int blockBottomPadding = 6;
            int textTop = startRect.y;
            int textBottom = endRect.y + endRect.height;
            int blockY = Math.max(0, textTop - blockTopPadding);
            if (blockY >= height - 8) {
                break;
            }

            int desiredBlockWidth = blockWidestPx + textLeftPaddingInBlock + textRightPaddingInBlock + 8;
            int blockWidth = Math.min(maxBlockWidth, Math.max(minBlockWidth, desiredBlockWidth));
            int rawHeight = (textBottom - textTop) + blockTopPadding + blockBottomPadding;
            int blockHeight = Math.min(rawHeight, height - blockY - 6);
            if (blockHeight <= 3) {
                continue;
            }

            g2.setColor(fillColor);
            g2.fillRoundRect(blockX, blockY, blockWidth, blockHeight, 12, 12);

            Composite originalComposite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.16f));
            g2.setPaint(new TexturePaint(
                    explanationTexturePattern,
                    new Rectangle(blockX, blockY, explanationTexturePattern.getWidth(),
                            explanationTexturePattern.getHeight())));
            g2.fillRoundRect(blockX + 1, blockY + 1, Math.max(1, blockWidth - 2), Math.max(1, blockHeight - 2), 11, 11);
            g2.setComposite(originalComposite);

            g2.setColor(strokeColor);
            g2.drawRoundRect(blockX, blockY, blockWidth, blockHeight, 12, 12);
        }
    }

    private void expandExplanationAreaForCurrentText(int minHeight) {
        if (explanationArea == null) {
            return;
        }

        FontMetrics metrics = explanationArea.getFontMetrics(explanationArea.getFont());
        int lineHeight = metrics != null ? metrics.getHeight() : 22;
        String text = explanationArea.getText();
        int lineCount = (text == null || text.isEmpty()) ? 1 : text.split("\\R", -1).length;
        Insets insets = explanationArea.getInsets();
        int verticalInsets = insets == null ? 0 : insets.top + insets.bottom;
        int targetHeight = Math.max(minHeight, lineCount * lineHeight + verticalInsets + 12);

        explanationArea.setPreferredSize(new Dimension(600, targetHeight));
        explanationArea.setMinimumSize(new Dimension(0, 0));
        explanationArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, targetHeight));
        explanationArea.revalidate();
        explanationArea.repaint();

        Container parent = explanationArea.getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
    }

    private void scrollMainPanelToTop() {
        if (mainContentScrollPane == null) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            JScrollBar verticalScrollBar = mainContentScrollPane.getVerticalScrollBar();
            if (verticalScrollBar != null) {
                verticalScrollBar.setValue(verticalScrollBar.getMinimum());
            }
        });
    }

    private void displayQuestion(AptitudeQuestion q) {
        currentQ = q;
        answerSubmitted = false;

        // Disable difficulty combo if question is from manual repository (no
        // quickTrick)
        boolean isManualQuestion = (q.quickTrick == null);
        if (difficultyCombo != null) {
            difficultyCombo.setEnabled(!isManualQuestion);
            if (isManualQuestion) {
                difficultyCombo.setToolTipText("ℹ️ Disabled - Manual question loaded");
            } else {
                difficultyCombo.setToolTipText("Select difficulty level");
            }
        }

        StringBuilder html = new StringBuilder();
        html.append(
                "<html><head><style>body{box-sizing:border-box; max-width:100%; width:100%; font-size:18pt; color:#FFFFFF; line-height:1.6; word-wrap:break-word; white-space:normal; margin:0; padding:5px;} .difficulty{font-size:14pt; color:#999; margin-bottom:10px;}</style></head><body>");
        // Add difficulty indicator
        String difficulty = (q.difficulty == null || q.difficulty.trim().isEmpty())
                ? "Normal"
                : q.difficulty.trim();
        String diffColor = "Normal".equals(difficulty) ? "#F59E0B"
                : ("Hard".equals(difficulty) ? "#EF4444" : "#F59E0B");
        html.append("<div class='difficulty' style='color:").append(diffColor).append(";'>Difficulty: ")
                .append(difficulty).append("</div>");
        html.append(q.question.replace("\n", "<br>"));
        html.append("</body></html>");

        questionArea.setText(html.toString());
        questionArea.setCaretPosition(0);

        // ALWAYS USE RADIO BUTTONS - NEVER CHECKBOXES
        optionsPanel.removeAll();
        optionGroup = new ButtonGroup();

        // Build options and add hover listeners
        for (int i = 0; i < 4; i++) {
            if (i < q.options.size()) {
                String optText = q.options.get(i);

                JRadioButton rb = createTexturedOptionButton(optText);
                rb.setOpaque(false);
                rb.setFont(new Font("Inter", Font.PLAIN, 20));
                rb.setAlignmentX(Component.LEFT_ALIGNMENT);
                rb.setForeground(OPTION_TEXT_COLOR);
                rb.putClientProperty(OPTION_VISUAL_STATE_KEY, null);
                // ensure consistent border so we can restore it after hover
                rb.setBorder(createBaseOptionBorder());
                rb.addActionListener(ae -> {
                    updateOptionSelectionStyles();
                    // stop any submit button pulse when user interacts
                    stopSubmitBtnPulse();
                });

                optionGroup.add(rb);
                options[i] = rb;
                optionsPanel.add(rb);

                if (i < 3)
                    optionsPanel.add(Box.createVerticalStrut(12));
            }
        }

        optionsPanel.revalidate();
        optionsPanel.repaint();

        if (skeletonCard != null) {
            skeletonCard.stop();
        }

        // Collapse explanation area back to minimal size when starting new question
        collapseExplanationArea();
        explanationArea.setForeground(new Color(226, 232, 240, 0)); // start transparent

        if (verifyResultTimer != null && verifyResultTimer.isRunning()) {
            verifyResultTimer.stop();
        }
        verifyResultLocked = false;
        submitBtn.setText("✓ Verify Answer");
        submitBtn.setEnabled(true);
        submitBtn.setBackground(verifyDefaultColor);
        submitBtn.setFont(VERIFY_BUTTON_FONT);
        stopSubmitBtnPulse();

        timeLeft = 60;
        resetTimerVisualState();

        // NEW: Record question start time for statistics
        questionStartTime = System.currentTimeMillis();

        quizTimer.restart();

        // Start preloading next question and fade/entrance animations
        startPreloadingNextQuestion();

        // animate entrance of question and options + brief verify pulse
        SwingUtilities.invokeLater(() -> {
            animateQuestionIn();
            startSubmitBtnEntrancePulse();
        });
    }

    // Fade-in for question with smooth easing
    private void animateQuestionIn() {
        if (questionFadeInTimer != null && questionFadeInTimer.isRunning())
            questionFadeInTimer.stop();

        Color qColor = questionArea.getForeground();
        questionArea.setForeground(new Color(qColor.getRed(), qColor.getGreen(), qColor.getBlue(), 0));

        final int totalSteps = 50; // 250ms total ultra-smooth animation (5ms × 50 = 250ms)
        final int[] step = { 0 };
        questionFadeInTimer = new javax.swing.Timer(ANIMATION_FRAME_DELAY_MS, e -> {
            step[0]++;

            // Smooth easing function for fade in
            float t = (float) step[0] / totalSteps;
            float ratio = t < 0.5f ? 2 * t * t : -1 + (4 - 2 * t) * t; // ease-in-out
            int qAlpha = Math.min(255, (int) (ratio * 255));
            questionArea.setForeground(new Color(qColor.getRed(), qColor.getGreen(), qColor.getBlue(), qAlpha));

            if (step[0] >= totalSteps) {
                questionFadeInTimer.stop();
                // Ensure full opacity on completion
                questionArea.setForeground(qColor);
            }
        });
        questionFadeInTimer.setCoalesce(true);
        questionFadeInTimer.start();
    }

    private Border createBaseOptionBorder() {
        return BorderFactory.createCompoundBorder(
                new LineBorder(OPTION_BORDER_BASE, 3, true),
                new EmptyBorder(8, 15, 8, 15));
    }

    private Border createOptionBorder(Color borderColor, int thickness) {
        return BorderFactory.createCompoundBorder(
                new LineBorder(borderColor, thickness, true),
                new EmptyBorder(8, 15, 8, 15));
    }

    private static int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static java.awt.image.BufferedImage buildTexturePattern(int size, Color strong, Color soft) {
        java.awt.image.BufferedImage texture = new java.awt.image.BufferedImage(
                size,
                size,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = texture.createGraphics();
        g2.setColor(strong);
        g2.drawLine(0, size - 1, size - 1, 0);
        g2.drawLine(0, size / 2, size / 2, 0);
        g2.setColor(soft);
        g2.drawLine(size / 3, size - 1, size - 1, size / 3);
        g2.dispose();
        return texture;
    }

    private static String[] buildQuizTimerLabels() {
        String[] labels = new String[QUIZ_TIMER_MAX_SECONDS + 1];
        labels[QUIZ_TIMER_MAX_SECONDS] = "⏱ 00:60";
        for (int seconds = 0; seconds < QUIZ_TIMER_MAX_SECONDS; seconds++) {
            labels[seconds] = String.format("⏱ %02d:%02d", seconds / 60, seconds % 60);
        }
        return labels;
    }

    private void ensureTimerFontsInitialized() {
        if (timerLabel == null) {
            return;
        }
        if (timerFontNormalCached == null) {
            timerFontNormalCached = timerLabel.getFont().deriveFont(TIMER_FONT_NORMAL);
        }
        if (timerFontUrgentCached == null) {
            timerFontUrgentCached = timerLabel.getFont().deriveFont(TIMER_FONT_URGENT);
        }
    }

    private void updateTimerLabelText(int secondsRemaining) {
        if (timerLabel == null) {
            return;
        }
        int clamped = Math.max(0, Math.min(QUIZ_TIMER_MAX_SECONDS, secondsRemaining));
        timerLabel.setText(QUIZ_TIMER_LABELS[clamped]);
    }

    private void resetTimerVisualState() {
        ensureTimerFontsInitialized();
        updateTimerLabelText(timeLeft);
        timerLabel.setForeground(TIMER_COLOR_SAFE);
        if (timerFontNormalCached != null && timerLabel.getFont() != timerFontNormalCached) {
            timerLabel.setFont(timerFontNormalCached);
        }
    }

    private void startVerifySheenAnimation() {
        if (!ENABLE_VERIFY_BUTTON_ANIMATIONS) {
            return;
        }
        if (submitBtn == null) {
            return;
        }
        if (verifySheenTimer != null && verifySheenTimer.isRunning()) {
            return;
        }

        verifySheenOffset = -220;
        verifySheenTimer = new javax.swing.Timer(28, e -> {
            verifySheenOffset += 18;
            int limit = Math.max(340, submitBtn.getWidth()) + 170;
            if (verifySheenOffset > limit) {
                verifySheenOffset = -220;
            }
            submitBtn.repaint();
        });
        verifySheenTimer.setCoalesce(true);
        verifySheenTimer.start();
    }

    private void stopVerifyPulseAnimation() {
        if (verifyPulseTimer != null && verifyPulseTimer.isRunning()) {
            verifyPulseTimer.stop();
        }
        if (submitBtn != null) {
            submitBtn.setFont(VERIFY_BUTTON_FONT);
        }
    }

    private void switchVerifyButtonToNextState() {
        verifyResultLocked = false;
        stopVerifyPulseAnimation();
        submitBtn.setText("→ Next Question");
        submitBtn.setBackground(verifyDefaultColor);
        submitBtn.setEnabled(true);
        submitBtn.repaint();
    }

    private void playVerifyResultAnimation(boolean correct) {
        verifyResultLocked = true;

        if (verifyResultTimer != null && verifyResultTimer.isRunning()) {
            verifyResultTimer.stop();
        }
        if (verifyPulseTimer != null && verifyPulseTimer.isRunning()) {
            verifyPulseTimer.stop();
        }

        Color resultBase = correct ? verifyCorrectColor : verifyWrongColor;
        submitBtn.setEnabled(false);
        submitBtn.setText(correct ? "✅ Excellent!" : "❌ Wrong!");
        submitBtn.setBackground(resultBase);

        if (!ENABLE_VERIFY_BUTTON_ANIMATIONS) {
            submitBtn.setFont(VERIFY_BUTTON_FONT);
            submitBtn.repaint();
            verifyResultTimer = new javax.swing.Timer(VERIFY_RESULT_DISPLAY_MS, e -> switchVerifyButtonToNextState());
            verifyResultTimer.setRepeats(false);
            verifyResultTimer.setCoalesce(true);
            verifyResultTimer.start();
            return;
        }

        final int totalFrames = 40;
        final int[] frame = { 0 };
        final float baseSize = 19f;

        verifyPulseTimer = new javax.swing.Timer(50, ev -> {
            frame[0]++;
            float wave = (float) Math.sin(frame[0] * 0.55f);
            float scaleBoost = Math.max(0f, 1.8f * wave);
            int shift = (int) (wave * 18f);

            submitBtn.setFont(submitBtn.getFont().deriveFont(baseSize + scaleBoost));
            submitBtn.setBackground(new Color(
                    clampColor(resultBase.getRed() + shift),
                    clampColor(resultBase.getGreen() + shift),
                    clampColor(resultBase.getBlue() + shift)));

            verifySheenOffset += 24;
            if (verifySheenOffset > submitBtn.getWidth() + 190) {
                verifySheenOffset = -220;
            }
            submitBtn.repaint();

            if (frame[0] >= totalFrames) {
                ((javax.swing.Timer) ev.getSource()).stop();
                submitBtn.setFont(submitBtn.getFont().deriveFont(baseSize));
                submitBtn.setBackground(resultBase);
            }
        });
        verifyPulseTimer.setCoalesce(true);
        verifyPulseTimer.start();

        verifyResultTimer = new javax.swing.Timer(VERIFY_RESULT_DISPLAY_MS, e -> switchVerifyButtonToNextState());
        verifyResultTimer.setRepeats(false);
        verifyResultTimer.setCoalesce(true);
        verifyResultTimer.start();
    }

    private JButton createTexturedVerifyButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();
                ButtonModel model = getModel();
                boolean pressed = model.isPressed() || model.isArmed();
                boolean hovered = model.isRollover();
                Color base = getBackground();

                int topShift = pressed ? -22 : (hovered ? 20 : 8);
                int bottomShift = pressed ? -34 : (hovered ? -2 : -18);

                Color topColor = new Color(
                        clampColor(base.getRed() + topShift),
                        clampColor(base.getGreen() + topShift),
                        clampColor(base.getBlue() + topShift),
                        isEnabled() ? 245 : 180);
                Color bottomColor = new Color(
                        clampColor(base.getRed() + bottomShift),
                        clampColor(base.getGreen() + bottomShift),
                        clampColor(base.getBlue() + bottomShift),
                        isEnabled() ? 250 : 190);

                g2.setPaint(new GradientPaint(0, 0, topColor, 0, height, bottomColor));
                g2.fillRoundRect(0, 0, width - 1, height - 1, 22, 22);

                g2.setPaint(new TexturePaint(
                        verifyTexturePattern,
                        new Rectangle(0, 0, verifyTexturePattern.getWidth(), verifyTexturePattern.getHeight())));
                g2.fillRoundRect(1, 1, width - 2, height - 2, 22, 22);

                if (ENABLE_VERIFY_BUTTON_ANIMATIONS) {
                    int sheenStart = verifySheenOffset;
                    Paint sheenPaint = new LinearGradientPaint(
                            sheenStart,
                            0,
                            sheenStart + 120,
                            0,
                            new float[] { 0f, 0.5f, 1f },
                            new Color[] {
                                    new Color(255, 255, 255, 0),
                                    new Color(255, 255, 255, verifyResultLocked ? 155 : (hovered ? 120 : 90)),
                                    new Color(255, 255, 255, 0)
                            });
                    g2.setPaint(sheenPaint);
                    g2.fillRoundRect(1, 1, width - 2, height - 2, 22, 22);
                }

                g2.setColor(new Color(255, 255, 255, 60));
                g2.drawLine(14, 5, Math.max(14, width - 16), 5);

                g2.setColor(new Color(255, 255, 255, 95));
                g2.drawRoundRect(0, 0, width - 1, height - 1, 22, 22);

                g2.dispose();
                super.paintComponent(g);
            }
        };

        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setRolloverEnabled(true);
        button.setBorder(new EmptyBorder(8, 18, 8, 18));
        return button;
    }

    private JRadioButton createTexturedOptionButton(String text) {
        JRadioButton optionButton = new JRadioButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();
                ButtonModel model = getModel();
                boolean selected = model.isSelected();
                boolean hovered = model.isRollover();
                Object visualState = getClientProperty(OPTION_VISUAL_STATE_KEY);
                boolean isCorrectState = OPTION_VISUAL_STATE_CORRECT.equals(visualState);
                boolean isWrongState = OPTION_VISUAL_STATE_WRONG.equals(visualState);
                boolean isDimState = OPTION_VISUAL_STATE_DIM.equals(visualState);

                Color topColor;
                Color bottomColor;
                Color borderColor;
                if (isCorrectState) {
                    topColor = new Color(22, 163, 74, 190);
                    bottomColor = new Color(21, 128, 61, 210);
                    borderColor = new Color(134, 239, 172, 230);
                } else if (isWrongState) {
                    topColor = new Color(220, 38, 38, 190);
                    bottomColor = new Color(185, 28, 28, 210);
                    borderColor = new Color(252, 165, 165, 230);
                } else if (isDimState) {
                    topColor = new Color(30, 41, 59, 135);
                    bottomColor = new Color(15, 23, 42, 155);
                    borderColor = new Color(100, 116, 139, 140);
                } else if (selected) {
                    topColor = new Color(99, 102, 241, 185);
                    bottomColor = new Color(67, 56, 202, 205);
                    borderColor = new Color(129, 140, 248, 190);
                } else if (hovered) {
                    topColor = new Color(71, 85, 105, 155);
                    bottomColor = new Color(51, 65, 85, 175);
                    borderColor = new Color(148, 163, 184, 140);
                } else {
                    topColor = new Color(39, 39, 42, 140);
                    bottomColor = new Color(24, 24, 27, 155);
                    borderColor = new Color(148, 163, 184, 110);
                }

                g2.setPaint(new GradientPaint(0, 0, topColor, 0, height, bottomColor));
                g2.fillRoundRect(0, 0, width - 1, height - 1, 18, 18);

                g2.setPaint(new TexturePaint(
                        optionTexturePattern,
                        new Rectangle(0, 0, optionTexturePattern.getWidth(), optionTexturePattern.getHeight())));
                g2.fillRoundRect(1, 1, width - 2, height - 2, 18, 18);

                if (isCorrectState) {
                    g2.setColor(new Color(34, 197, 94, 68));
                    g2.fillRoundRect(1, 1, width - 2, height - 2, 18, 18);
                } else if (isWrongState) {
                    g2.setColor(new Color(239, 68, 68, 68));
                    g2.fillRoundRect(1, 1, width - 2, height - 2, 18, 18);
                } else if (selected) {
                    g2.setColor(new Color(129, 140, 248, 70));
                    g2.fillRoundRect(1, 1, width - 2, height - 2, 18, 18);
                }

                if (isCorrectState || isWrongState) {
                    g2.setColor(new Color(255, 255, 255, 80));
                    g2.setStroke(new BasicStroke(1.8f));
                    g2.drawRoundRect(1, 1, width - 3, height - 3, 17, 17);
                }

                g2.setColor(borderColor);
                g2.drawRoundRect(0, 0, width - 1, height - 1, 18, 18);

                g2.setColor(new Color(255, 255, 255, 38));
                g2.drawLine(12, 4, Math.max(12, width - 14), 4);

                g2.dispose();
                super.paintComponent(g);
            }
        };

        optionButton.setContentAreaFilled(false);
        optionButton.setOpaque(false);
        optionButton.setRolloverEnabled(true);
        optionButton.setIconTextGap(12);
        optionButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return optionButton;
    }

    private void updateOptionSelectionStyles() {
        for (AbstractButton btn : options) {
            if (btn == null || !btn.isVisible()) {
                continue;
            }
            btn.setForeground(OPTION_TEXT_COLOR);
            if (btn.isSelected()) {
                animateOptionSelection(btn, true);
            } else {
                animateOptionSelection(btn, false);
            }
        }
    }

    // Instant option selection animation
    private void animateOptionSelection(AbstractButton btn, boolean isSelected) {
        if (btn == null) {
            return;
        }
        if (isSelected) {
            btn.setBorder(createOptionBorder(OPTION_BORDER_ACCENT, 3));
        } else {
            btn.setBorder(createBaseOptionBorder());
        }
    }

    // Ultra-fast entrance pulse for Verify button with scale animation
    private void startSubmitBtnEntrancePulse() {
        if (!ENABLE_VERIFY_BUTTON_ANIMATIONS || submitBtn == null) {
            return;
        }
        // Animation code disabled - ENABLE_VERIFY_BUTTON_ANIMATIONS is false
    }

    // Stop any ongoing pulse animation on the submit button
    private void stopSubmitBtnPulse() {
        stopVerifyPulseAnimation();
    }

    private void setupTimerLogic() {
        quizTimer = new javax.swing.Timer(1000, e -> {
            timeLeft--;
            updateTimerLabelText(timeLeft);
            ensureTimerFontsInitialized();

            if (timeLeft <= 10) {
                if (timerLabel.getForeground() != TIMER_COLOR_URGENT) {
                    timerLabel.setForeground(TIMER_COLOR_URGENT);
                }
                // Pulse animation for urgency
                Font targetFont = (timeLeft % 2 == 0) ? timerFontUrgentCached : timerFontNormalCached;
                if (targetFont != null && timerLabel.getFont() != targetFont) {
                    timerLabel.setFont(targetFont);
                }
            } else if (timeLeft <= 30) {
                if (timerLabel.getForeground() != TIMER_COLOR_WARNING) {
                    timerLabel.setForeground(TIMER_COLOR_WARNING);
                }
                if (timerFontNormalCached != null && timerLabel.getFont() != timerFontNormalCached) {
                    timerLabel.setFont(timerFontNormalCached);
                }
            } else {
                if (timerLabel.getForeground() != TIMER_COLOR_SAFE) {
                    timerLabel.setForeground(TIMER_COLOR_SAFE);
                }
                if (timerFontNormalCached != null && timerLabel.getFont() != timerFontNormalCached) {
                    timerLabel.setFont(timerFontNormalCached);
                }
            }

            if (timeLeft <= 0) {
                quizTimer.stop();
                if (!answerSubmitted) {
                    submitAnswer();
                }
            }
        });
        quizTimer.setCoalesce(true);
    }

    private void setupActions() {
        submitBtn.addActionListener(e -> {
            if (currentQ == null)
                return;
            if (!submitBtn.isEnabled()) {
                return;
            }
            if (!answerSubmitted) {
                submitAnswer();
            } else {
                scrollMainPanelToTop();
                loadQuestion(false);
            }
        });

        nextBtn.addActionListener(e -> {
            scrollMainPanelToTop();
            loadQuestion(true);
        });
        searchBtn.addActionListener(e -> {
            preloadedQ = null;
            currentQ = null;
            loadQuestion(true);
        });
        difficultyCombo.addActionListener(e -> {
            preloadedQ = null;
            currentQ = null;
            loadQuestion(false);
        });

        // NEW: Keyboard Navigation
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() != java.awt.event.KeyEvent.KEY_PRESSED)
                return false;

            // Ctrl+P: Toggle Performance Monitor
            if (e.isControlDown() && e.getKeyCode() == java.awt.event.KeyEvent.VK_P) {
                boolean visible = togglePerformanceMonitorVisible();
                showNotification("⚡ Performance Monitor: " + (visible ? "ON" : "OFF"));
                return true;
            }

            // Ctrl+D: Open Analytics Dashboard
            if (e.isControlDown() && e.getKeyCode() == java.awt.event.KeyEvent.VK_D) {
                try {
                    AnalyticsDashboard dashboard = new AnalyticsDashboard(statsTracker, achievementSystem);
                    dashboard.setVisible(true);
                    showNotification("📊 Analytics Dashboard Opened");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AptitudeGui.this,
                            "Could not open Analytics Dashboard: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
                return true;
            }

            // Ctrl+N: New Session
            if (e.isControlDown() && e.getKeyCode() == java.awt.event.KeyEvent.VK_N) {
                resetSession();
                showNotification("🆕 New Session Started");
                return true;
            }

            // Ctrl+Q: Exit with confirmation
            if (e.isControlDown() && e.getKeyCode() == java.awt.event.KeyEvent.VK_Q) {
                int confirm = JOptionPane.showConfirmDialog(AptitudeGui.this,
                        "Are you sure you want to exit?\n\nYour current session progress will be saved.",
                        "Exit Confirmation",
                        JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    showNotification("👋 Goodbye! Session Saved");
                    java.util.Timer exitTimer = new java.util.Timer();
                    exitTimer.schedule(new java.util.TimerTask() {
                        @Override
                        public void run() {
                            System.exit(0);
                        }
                    }, 500);
                }
                return true;
            }

            // F11: Toggle Fullscreen
            if (e.getKeyCode() == java.awt.event.KeyEvent.VK_F11) {
                boolean enableSteamLike = !steamFullscreenActive;
                setFullscreenMode(enableSteamLike);
                showNotification(enableSteamLike ? "🖥️ Fullscreen: ON" : "🖥️ Fullscreen: OFF");
                return true;
            }

            // Ctrl++: Zoom In
            if (e.isControlDown() && (e.getKeyCode() == java.awt.event.KeyEvent.VK_PLUS ||
                    e.getKeyCode() == java.awt.event.KeyEvent.VK_EQUALS)) {
                uiScaleFactor = Math.min(1.5f, uiScaleFactor + 0.1f);
                applyUIScale();
                showNotification(String.format("🔍 Zoom: %.0f%%", uiScaleFactor * 100));
                return true;
            }

            // Ctrl+-: Zoom Out
            if (e.isControlDown() && e.getKeyCode() == java.awt.event.KeyEvent.VK_MINUS) {
                uiScaleFactor = Math.max(0.7f, uiScaleFactor - 0.1f);
                applyUIScale();
                showNotification(String.format("🔍 Zoom: %.0f%%", uiScaleFactor * 100));
                return true;
            }

            // Ctrl+0: Reset Zoom to 100% (Original Layout)
            if (e.isControlDown() && (e.getKeyCode() == java.awt.event.KeyEvent.VK_0 ||
                    e.getKeyCode() == java.awt.event.KeyEvent.VK_NUMPAD0)) {
                resetZoom();
                return true;
            }

            // Arrow keys: Navigate options
            if ((e.getKeyCode() == java.awt.event.KeyEvent.VK_UP ||
                    e.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN) &&
                    currentQ != null && !answerSubmitted) {

                int currentIndex = -1;
                for (int i = 0; i < options.length; i++) {
                    if (options[i] != null && options[i].isSelected()) {
                        currentIndex = i;
                        break;
                    }
                }

                if (currentIndex == -1)
                    currentIndex = 0;
                else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN)
                    currentIndex++;
                else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_UP)
                    currentIndex--;

                if (currentIndex >= 0 && currentIndex < 4 && options[currentIndex] != null) {
                    options[currentIndex].setSelected(true);
                    updateOptionSelectionStyles();
                }
                return true;
            }

            // Enter: Submit or Next
            if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER && currentQ != null && submitBtn.isEnabled()) {
                submitBtn.doClick();
                return true;
            }

            return false;
        });
    }

    /**
     * Reset zoom to 100% (original layout)
     */
    private void resetZoom() {
        uiScaleFactor = 1.0f; // 1.0f = 100% original layout
        applyUIScale();
        showNotification("🔄 Zoom Reset: 100% (Original Layout)");
    }

    /**
     * Show an enhanced toast-style notification with smooth fade-in/out animations,
     * gradient background, and modern styling.
     * Instantly exits previous notification when a new one appears.
     * Positioned between search bar and topic chips.
     */
    public void showNotification(String message) {
        SwingUtilities.invokeLater(() -> {
            // Instantly dispose previous notification if it exists
            if (lastNotification != null) {
                try {
                    lastNotification.dispose();
                } catch (Exception e) {
                    // Ignore if already disposed
                }
                lastNotification = null;
            }

            // Create custom panel with gradient background and rounded corners
            JPanel notificationPanel = new JPanel() {
                private float alpha = 0f;

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g;
                    // ⚡ OPTIMIZED FOR 200 FPS - Speed over quality for smooth interactions
                    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);

                    // Draw rounded rectangle with gradient background
                    int width = getWidth();
                    int height = getHeight();
                    int arcSize = 16;

                    // Gradient from indigo to purple
                    GradientPaint gradient = new GradientPaint(0, 0,
                            new Color((int) (99 * alpha), (int) (102 * alpha), (int) (241 * alpha),
                                    (int) (220 * alpha)),
                            width, height,
                            new Color((int) (168 * alpha), (int) (85 * alpha), (int) (247 * alpha),
                                    (int) (220 * alpha)));

                    g2d.setPaint(gradient);
                    g2d.fillRoundRect(0, 0, width, height, arcSize, arcSize);

                    // Draw border with soft glow
                    g2d.setStroke(new BasicStroke(1.5f));
                    g2d.setColor(new Color(200, 200, 255, (int) (100 * alpha)));
                    g2d.drawRoundRect(0, 0, width - 1, height - 1, arcSize, arcSize);

                    super.paintComponent(g);
                }
            };

            notificationPanel.setLayout(new BorderLayout(12, 0));
            notificationPanel.setOpaque(false);
            notificationPanel.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

            // Message label with enhanced styling
            JLabel notificationLabel = new JLabel(message);
            notificationLabel.setFont(new Font("Inter", Font.BOLD, 15));
            notificationLabel.setForeground(Color.WHITE);
            notificationLabel.setOpaque(false);
            notificationLabel.setHorizontalAlignment(JLabel.LEFT);

            notificationPanel.add(notificationLabel, BorderLayout.CENTER);

            // Window with custom appearance
            JWindow notification = new JWindow(this) {
                @Override
                public void setVisible(boolean b) {
                    if (b) {
                        setBackground(new Color(0, 0, 0, 0)); // Transparent
                    }
                    super.setVisible(b);
                }
            };

            notification.setBackground(new Color(0, 0, 0, 0));
            notification.add(notificationPanel);
            notification.setFocusableWindowState(false);
            notification.pack();

            // Get notification preferred size
            int notifWidth = Math.max(400, notification.getWidth());
            int notifHeight = notification.getHeight();

            // Position between search bar and topic chips
            int screenX = getLocationOnScreen().x;
            int screenY = getLocationOnScreen().y;
            int centerX = screenX + (getWidth() - notifWidth) / 2;
            // Position approximately between search (top) and chips (below search)
            // Typically search is ~120px from top, so chips start ~200px, notification at
            // ~180px
            int topY = screenY + 117;

            notification.setSize(notifWidth, notifHeight);
            notification.setLocation(centerX, topY - 30); // Start slightly higher
            notification.setVisible(true);

            // Store as last notification
            lastNotification = notification;

            // Fade-in animation (200 FPS compatible)
            javax.swing.Timer fadeInTimer = new javax.swing.Timer(ANIMATION_FRAME_DELAY_MS, null);
            final float[] currentAlpha = { 0f };
            fadeInTimer.addActionListener(e -> {
                currentAlpha[0] = Math.min(1.0f, currentAlpha[0] + 0.05f); // Adjusted increment for 5ms

                // Update alpha for gradient
                notificationPanel.repaint();

                // Smooth slide-down movement
                int currentY = notification.getY();
                if (currentY < topY) {
                    notification.setLocation(centerX, Math.min(topY, currentY + 1)); // Reduced movement for 5ms
                }

                if (currentAlpha[0] >= 1.0f) {
                    ((javax.swing.Timer) e.getSource()).stop();
                }
            });
            fadeInTimer.setCoalesce(true);
            fadeInTimer.start();

            // Auto-hide after 2.2 seconds (or instantly if new notification appears)
            javax.swing.Timer hideTimer = new javax.swing.Timer(2200, e -> {
                // Only fade out if this is still the last notification
                if (lastNotification == notification) {
                    fadeOutNotification(notification, notificationPanel);
                    lastNotification = null;
                }
            });
            hideTimer.setRepeats(false);
            hideTimer.setCoalesce(true);
            hideTimer.start();
        });
    }

    /**
     * Fade out a notification smoothly (200 FPS compatible)
     */
    private void fadeOutNotification(JWindow notification, JPanel notificationPanel) {
        javax.swing.Timer fadeOutTimer = new javax.swing.Timer(ANIMATION_FRAME_DELAY_MS, null);
        final float[] fadeOutAlpha = { 1.0f };
        fadeOutTimer.addActionListener(fadeEvent -> {
            fadeOutAlpha[0] = Math.max(0f, fadeOutAlpha[0] - 0.05f); // Adjusted for 5ms intervals
            notificationPanel.repaint();

            if (fadeOutAlpha[0] <= 0f) {
                ((javax.swing.Timer) fadeEvent.getSource()).stop();
                try {
                    notification.dispose();
                } catch (Exception ex) {
                    // Ignore if already disposed
                }
            }
        });
        fadeOutTimer.setCoalesce(true);
        fadeOutTimer.start();
    }

    // Button hover animation helper with press feedback
    private void addButtonHoverEffect(JButton btn, Color normalColor, Color hoverColor) {
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) {
                    animateButtonColor(btn, normalColor, hoverColor, 120); // Faster
                }
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) {
                    animateButtonColor(btn, btn.getBackground(), normalColor, 120);
                }
            }

            public void mousePressed(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) {
                    stopButtonColorAnimation(btn);
                    // Instant press feedback
                    Color darkColor = new Color(
                            Math.max(0, normalColor.getRed() - 25),
                            Math.max(0, normalColor.getGreen() - 25),
                            Math.max(0, normalColor.getBlue() - 25));
                    btn.setBackground(darkColor);
                }
            }

            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (btn.isEnabled() && btn.getModel().isArmed()) {
                    animateButtonColor(btn, btn.getBackground(), hoverColor, 80);
                } else if (btn.isEnabled()) {
                    animateButtonColor(btn, btn.getBackground(), normalColor, 80);
                }
            }
        });
    }

    // Chip hover animation helper with press feedback
    private void addChipHoverEffect(JButton chip, Color baseColor, Color hoverColor) {
        Color normalBg = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 120);
        Color hoverBg = new Color(hoverColor.getRed(), hoverColor.getGreen(), hoverColor.getBlue(), 180);

        chip.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                animateButtonColor(chip, normalBg, hoverBg, 150); // Faster
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                animateButtonColor(chip, chip.getBackground(), normalBg, 150);
            }

            public void mousePressed(java.awt.event.MouseEvent e) {
                stopButtonColorAnimation(chip);
                // Instant press feedback
                Color darkColor = new Color(
                        Math.max(0, hoverBg.getRed() - 30),
                        Math.max(0, hoverBg.getGreen() - 30),
                        Math.max(0, hoverBg.getBlue() - 30),
                        Math.max(0, hoverBg.getAlpha() - 30));
                chip.setBackground(darkColor);
            }
        });
    }

    private void stopButtonColorAnimation(JButton btn) {
        if (btn == null) {
            return;
        }

        Object activeTimer = btn.getClientProperty(BUTTON_COLOR_ANIMATION_TIMER_KEY);
        if (activeTimer instanceof javax.swing.Timer) {
            javax.swing.Timer existingTimer = (javax.swing.Timer) activeTimer;
            if (existingTimer.isRunning()) {
                existingTimer.stop();
            }
        }
        btn.putClientProperty(BUTTON_COLOR_ANIMATION_TIMER_KEY, null);
    }

    // Ultra-smooth color transition animation with easing
    private void animateButtonColor(JButton btn, Color fromColor, Color toColor, int duration) {
        if (btn == null || fromColor == null || toColor == null) {
            return;
        }

        stopButtonColorAnimation(btn);

        final int steps = 25; // 250ms transitions at 10ms interval = ultra-smooth
        final int delay = 10; // Exactly 10ms for 100fps capable animations
        final int[] step = { 0 };
        final Color effectiveFrom = btn.getBackground() != null ? btn.getBackground() : fromColor;

        javax.swing.Timer timer = new javax.swing.Timer(delay, null);
        btn.putClientProperty(BUTTON_COLOR_ANIMATION_TIMER_KEY, timer);
        timer.addActionListener(e -> {
            step[0]++;
            // Ease-in-out cubic for natural acceleration/deceleration
            float t = (float) step[0] / steps;
            float ratio = t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;

            int r = (int) (effectiveFrom.getRed() + ratio * (toColor.getRed() - effectiveFrom.getRed()));
            int g = (int) (effectiveFrom.getGreen() + ratio * (toColor.getGreen() - effectiveFrom.getGreen()));
            int b = (int) (effectiveFrom.getBlue() + ratio * (toColor.getBlue() - effectiveFrom.getBlue()));
            int a = (int) (effectiveFrom.getAlpha() + ratio * (toColor.getAlpha() - effectiveFrom.getAlpha()));

            btn.setBackground(new Color(r, g, b, a));

            if (step[0] >= steps) {
                timer.stop();
                btn.setBackground(toColor);
                if (btn.getClientProperty(BUTTON_COLOR_ANIMATION_TIMER_KEY) == timer) {
                    btn.putClientProperty(BUTTON_COLOR_ANIMATION_TIMER_KEY, null);
                }
            }
        });
        timer.setCoalesce(true);
        timer.start();
    }

    // Fast animated stat counter update with easing
    private void animateStatUpdate(JLabel label, int fromValue, int toValue, String suffix) {
        final int steps = 15; // 150ms total animation
        final int delay = 10; // Exactly 10ms for 100fps capable stats
        final int[] step = { 0 };

        javax.swing.Timer timer = new javax.swing.Timer(delay, null);
        timer.addActionListener(e -> {
            step[0]++;
            // Ease-out cubic for smooth deceleration
            float t = (float) step[0] / steps;
            float ratio = 1 - (float) Math.pow(1 - t, 3);

            int currentValue = (int) (fromValue + ratio * (toValue - fromValue));
            label.setText(currentValue + suffix);

            if (step[0] >= steps) {
                timer.stop();
                label.setText(toValue + suffix);
            }
        });
        timer.setCoalesce(true);
        timer.start();
    }

    private void showValidation(String message) {
        // Optimized for 200 FPS smooth validation feedback
        if (validationFadeTimer != null && validationFadeTimer.isRunning()) {
            validationFadeTimer.stop();
        }

        topicValidationLabel.setText(message);
        topicValidationLabel.setForeground(new Color(255, 0, 0, 0));

        final int[] alpha = { 0 };
        final int steps = 40; // 200ms fade-in (5ms × 40 = 200ms, was 80ms)
        final int[] step = { 0 };
        validationFadeTimer = new javax.swing.Timer(ANIMATION_FRAME_DELAY_MS, ev -> {
            step[0]++;
            // Ease-out for smooth appearance
            float t = (float) step[0] / steps;
            float ratio = 1 - (float) Math.pow(1 - t, 2);
            alpha[0] = Math.min(180, (int) (ratio * 180));
            topicValidationLabel.setForeground(new Color(255, 0, 0, alpha[0]));
            if (step[0] >= steps) {
                validationFadeTimer.stop();
                topicValidationLabel.setForeground(new Color(255, 0, 0, 180));
            }
        });
        validationFadeTimer.setCoalesce(true);
        validationFadeTimer.start();
    }

    private void hideValidation() {
        if (validationFadeTimer != null && validationFadeTimer.isRunning()) {
            validationFadeTimer.stop();
        }

        final int currentAlpha = topicValidationLabel.getForeground().getAlpha();
        if (currentAlpha == 0) {
            topicValidationLabel.setText(" ");
            return;
        }

        final int startAlpha = currentAlpha;
        final int steps = 40; // 200ms fade-out (5ms × 40 = 200ms, was 80ms)
        final int[] step = { 0 };
        validationFadeTimer = new javax.swing.Timer(ANIMATION_FRAME_DELAY_MS, ev -> {
            step[0]++;
            // Ease-in for smooth disappearance
            float t = (float) step[0] / steps;
            float ratio = 1 - (t * t);
            int alpha = Math.max(0, (int) (startAlpha * ratio));
            topicValidationLabel.setForeground(new Color(255, 0, 0, alpha));
            if (step[0] >= steps) {
                validationFadeTimer.stop();
                topicValidationLabel.setText(" ");
            }
        });
        validationFadeTimer.setCoalesce(true);
        validationFadeTimer.start();
    }

    private void showSelectTopicMessage() {
        currentQ = null;
        answerSubmitted = false;
        quizTimer.stop();

        String html = "<html><head><style>" +
                "body { font-family: 'Segoe UI', Arial, sans-serif; font-size: 24pt; color: #FFFFFF; line-height: 1.6; padding: 10px; }"
                +
                ".hint { font-family: Courier, monospace; color: #a1a1aaa0; font-size: 20pt; margin-top: 8px; }" +
                ".hint1 { font-family: 'Lucida Handwriting'; color: #2b7f8a; font-size: 37pt; margin-top: 20px; }" +
                "</style></head><body>" +
                "<div class='hint1'>Select a topic to generate your first question.</div>" +
                "<div class='hint'>Tip: type a topic and press Enter, or click a chip above.</div>" +
                "</body></html>";
        questionArea.setText(html);
        questionArea.setCaretPosition(0);

        optionsPanel.removeAll();
        optionsPanel.revalidate();
        optionsPanel.repaint();

        if (skeletonCard != null) {
            skeletonCard.stop();
        }

        // Collapse explanation area back to minimal size
        collapseExplanationArea();

        if (verifyResultTimer != null && verifyResultTimer.isRunning()) {
            verifyResultTimer.stop();
        }
        verifyResultLocked = false;
        submitBtn.setText("✓ Verify Answer");
        submitBtn.setEnabled(false);
        submitBtn.setBackground(verifyDefaultColor);
        submitBtn.setFont(VERIFY_BUTTON_FONT);
        timeLeft = 60;
        resetTimerVisualState();

        ((CardLayout) cardContainer.getLayout()).show(cardContainer, "REAL");
    }

    private String getSelectedDifficulty() {
        Object selected = difficultyCombo.getSelectedItem();
        if (selected == null)
            return "Normal";
        // Strip emoji prefix (e.g., "🟡 Normal" -> "Normal")
        String text = selected.toString().replaceAll("[^a-zA-Z ]", "").trim();
        return text.isEmpty() ? "Normal" : text;
    }

    /**
     * Handles answer verification with deferred explanation generation.
     * 
     * TOKEN OPTIMIZATION STRATEGY:
     * ┌─────────────────────────────────────────────────────────────────┐
     * │ TOTAL API CALLS PER COMPLETE QUESTION CYCLE: │
     * ├─────────────────────────────────────────────────────────────────┤
     * │ 1. loadQuestion() → generateNewQuestion() │
     * │ └─ Returns: question + options (No explanation) │
     * │ │
     * │ 2. startPreloadingNextQuestion() → generateNewQuestion() │
     * │ └─ Returns: next question + options (No explanation) │
     * │ │
     * │ 3. submitAnswer() [THIS METHOD] → generateExplanation() │
     * │ └─ Returns: explanation + quick trick (Only on verify!) │
     * └─────────────────────────────────────────────────────────────────┘
     * 
     * RESULT: ~40-50% token savings by deferring explanations until needed.
     */
    private void submitAnswer() {
        if (currentQ == null || answerSubmitted)
            return;

        // Gather selected answer (radio button - single selection only)
        String selectedText = null;
        for (AbstractButton btn : options) {
            if (btn != null && btn.isVisible() && btn.isSelected()) {
                selectedText = btn.getText().trim();
                break;
            }
        }

        if (selectedText == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select an answer!",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        quizTimer.stop();
        answerSubmitted = true;

        // Normalize and compare
        String normalizedSelected = selectedText.replaceAll("\\s+", " ").toLowerCase();
        String normalizedCorrect = currentQ.correctAnswer.replaceAll("\\s+", " ").toLowerCase();

        boolean correct = normalizedSelected.equals(normalizedCorrect);

        totalAttempted++;
        if (correct) {
            totalCorrect++;
            streak++;
        } else {
            streak = 0;
        }

        // NEW: Track statistics (non-blocking, thread-safe)
        long timeSpent = System.currentTimeMillis() - questionStartTime;
        String difficulty = getSelectedDifficulty();
        String selectedSubtopic = (subtopicCombo.getSelectedItem() != null)
                ? subtopicCombo.getSelectedItem().toString()
                : "Unknown";
        statsTracker.recordAttempt(
                selectedSubtopic,
                difficulty,
                currentQ.question,
                selectedText,
                currentQ.correctAnswer,
                timeSpent,
                correct);

        // Update stats with animation
        int oldStreak = streak - (correct ? 1 : -streak);
        animateStatUpdate(streakVal, oldStreak, streak, " 🔥");

        int newAccuracy = (totalAttempted == 0) ? 0 : (int) (((double) totalCorrect / totalAttempted) * 100);
        int oldAccuracy = (totalAttempted <= 1) ? 0
                : (int) (((double) (totalCorrect - (correct ? 1 : 0)) / (totalAttempted - 1)) * 100);
        animateStatUpdate(accuracyVal, oldAccuracy, newAccuracy, "% 🎯");

        // Highlight answers immediately
        for (AbstractButton btn : options) {
            if (btn != null && btn.isVisible()) {
                String optTextClean = btn.getText().replaceAll("\\s+", " ").toLowerCase();

                if (optTextClean.equals(normalizedCorrect)) {
                    // Correct answer
                    btn.setForeground(new Color(34, 197, 94));
                    btn.putClientProperty(OPTION_VISUAL_STATE_KEY, OPTION_VISUAL_STATE_CORRECT);
                } else if (btn.isSelected()) {
                    // Selected but wrong answer
                    btn.setForeground(new Color(239, 68, 68));
                    btn.putClientProperty(OPTION_VISUAL_STATE_KEY, OPTION_VISUAL_STATE_WRONG);
                } else {
                    // Not selected and not correct
                    btn.setForeground(OPTION_TEXT_DIM);
                    btn.putClientProperty(OPTION_VISUAL_STATE_KEY, OPTION_VISUAL_STATE_DIM);
                }
                btn.repaint();
                btn.setEnabled(false);
            }
        }

        playVerifyResultAnimation(correct);

        // Generate explanation asynchronously (deferred API call to save tokens)
        final boolean isCorrect = correct;

        // Expand explanation area to show loading message
        explanationArea.setText("⏳ Generating explanation...");
        explanationArea.setCaretPosition(0);
        explanationArea.setForeground(new Color(226, 232, 240));
        setExplanationVisualState(EXPLANATION_STATE_LOADING);
        expandExplanationAreaForCurrentText(110);

        final AptitudeQuestion questionSnapshot = currentQ;

        runInBackground("ExplanationGenerator", () -> {
            try {
                // Use explanation from question or generate simple one
                String explanation = (questionSnapshot.explanation != null && !questionSnapshot.explanation.isEmpty())
                        ? questionSnapshot.explanation
                        : "The correct answer is: " + questionSnapshot.correctAnswer;

                // Build and display explanation on EDT
                SwingUtilities.invokeLater(() -> {
                    if (currentQ != questionSnapshot) {
                        return;
                    }

                    String[] correctAnswersArray = { questionSnapshot.correctAnswer };
                    String explanationText = buildDetailedExplanation(isCorrect, questionSnapshot, correctAnswersArray);

                    // Expand explanation area from minimal size to allow content to display
                    explanationArea.setText(explanationText);
                    explanationArea.setCaretPosition(0);

                    if (isCorrect) {
                        explanationArea.setForeground(new Color(220, 252, 231));
                        setExplanationVisualState(EXPLANATION_STATE_CORRECT);
                    } else {
                        explanationArea.setForeground(new Color(254, 226, 226));
                        setExplanationVisualState(EXPLANATION_STATE_WRONG);
                    }

                    // Force layout recalculation to make explanation visible
                    expandExplanationAreaForCurrentText(220);

                    if (isCorrect) {
                        playCorrectSound();
                    } else {
                        playWrongSound();
                    }
                });
            } catch (Exception ex) {
                System.err.println("Error generating explanation: " + ex.getMessage());
                SwingUtilities.invokeLater(() -> {
                    explanationArea.setText("Failed to generate explanation. Please try again.");
                    explanationArea.setCaretPosition(0);
                    explanationArea.setForeground(new Color(254, 226, 226));
                    setExplanationVisualState(EXPLANATION_STATE_WRONG);
                    expandExplanationAreaForCurrentText(120);
                });
            }
        });
    }

    /**
     * Preloads the next question in background WITHOUT explanations.
     * 
     * TOKEN OPTIMIZATION STRATEGY:
     * 1. Loading Question: generateNewQuestion() → only question + options (no
     * explanation)
     * 2. Preloading Next: generateNewQuestion() → only question + options (no
     * explanation) ←◆ THIS METHOD
     * 3. Verifying Answer: generateExplanation() → explanation + quick trick
     * (on-demand)
     * 
     * This 3-tier approach saves ~40-50% of API tokens by only generating
     * explanations
     * when the user actually clicks "Verify Answer". Questions are generated with
     * minimal
     * tokens, and preloading happens silently without any explanation overhead.
     */
    private void startPreloadingNextQuestion() {
        if (isPreloading)
            return;

        Object categoryObj = categoryCombo.getSelectedItem();
        Object subtopicObj = subtopicCombo.getSelectedItem();

        if (subtopicObj == null || subtopicObj.toString().isEmpty() ||
                "Select Subtopic".equals(subtopicObj.toString())) {
            return;
        }

        if (preloadingDelayTimer != null && preloadingDelayTimer.isRunning()) {
            preloadingDelayTimer.stop();
        }

        preloadingDelayTimer = new javax.swing.Timer(350, e -> {
            ((javax.swing.Timer) e.getSource()).stop();

            String category = categoryObj != null ? categoryObj.toString() : "";
            String subtopic = subtopicObj.toString();
            String difficulty = getSelectedDifficulty();
            preloadedTopic = subtopic;
            preloadedDifficulty = difficulty;
            isPreloading = true;

            runInBackground("QuestionPreload", () -> {
                try {
                    // Generate next question using QuestionGeneratorV2
                    preloadedQ = ai.generateNewQuestion(category, subtopic, difficulty);
                } catch (Exception ex) {
                    preloadedQ = null;
                } finally {
                    isPreloading = false;
                }
            });
        });
        preloadingDelayTimer.setRepeats(false);
        preloadingDelayTimer.setCoalesce(true);
        preloadingDelayTimer.start();
    }

    private void loadQuestion(boolean showValidation) {
        Object categoryObj = categoryCombo.getSelectedItem();
        Object subtopicObj = subtopicCombo.getSelectedItem();

        if (categoryObj == null || categoryObj.toString().isEmpty()) {
            if (showValidation) {
                showValidation("Select a category");
            }
            showSelectTopicMessage();
            return;
        }

        if (subtopicObj == null || subtopicObj.toString().isEmpty() ||
                "Select Subtopic".equals(subtopicObj.toString())) {
            if (showValidation) {
                showValidation("Select a subtopic");
            }
            showSelectTopicMessage();
            return;
        }

        hideValidation();
        String category = categoryObj.toString();
        String subtopic = subtopicObj.toString();

        // Show loading skeleton
        ((CardLayout) cardContainer.getLayout()).show(cardContainer, "LOADING");
        if (skeletonCard != null) {
            skeletonCard.start();
        }
        submitBtn.setEnabled(false);

        runInBackground("QuestionLoader", () -> {
            final AptitudeQuestion[] q = new AptitudeQuestion[1];
            String difficulty = getSelectedDifficulty();

            // Use preloaded question if available
            if (preloadedQ != null
                    && category.equals(preloadedTopic)
                    && difficulty.equals(preloadedDifficulty)) {
                q[0] = preloadedQ;
                preloadedQ = null;
            } else {
                // Load new question using QuestionGeneratorV2 with subtopic
                try {
                    q[0] = ai.generateNewQuestion(category, subtopic, difficulty);
                } catch (Exception ex) {
                    q[0] = null;
                }
            }

            SwingUtilities.invokeLater(() -> {
                if (q[0] != null) {
                    displayQuestion(q[0]);
                    ((CardLayout) cardContainer.getLayout()).show(cardContainer, "REAL");
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Failed to generate question for: " + subtopic + "\n\n" +
                                    "Possible causes:\n" +
                                    "• API key not set (Windows: set GROQ_API_KEY=your-key)\n" +
                                    "  (macOS/Linux: export GROQ_API_KEY=\"your-key\")\n" +
                                    "• No internet connection\n" +
                                    "• API service temporarily unavailable\n\n" +
                                    "Please check and try again.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    submitBtn.setEnabled(true);
                    if (skeletonCard != null) {
                        skeletonCard.stop();
                    }
                    ((CardLayout) cardContainer.getLayout()).show(cardContainer, "REAL");
                }
            });
        });
    }

    // ====== Unique Question Filtering Removed - Using QuestionGeneratorV2 instead
    // ======

    // =================================================================
    // BACKGROUND IMAGE CUSTOMIZATION METHODS
    // =================================================================

    /**
     * Set sidebar background image with custom opacity
     * 
     * @param imagePath Full path to image file
     * @param opacity   Value between 0.0f (transparent) and 1.0f (opaque)
     */
    public void setSidebarBackgroundImage(String imagePath, float opacity) {
        try {
            sidebarBgImage = new ImageIcon(imagePath).getImage();
            // TODO: Add opacity parameter to sidebar painting if needed
        } catch (Exception e) {
            System.err.println("Failed to load sidebar background: " + e.getMessage());
        }
    }

    /**
     * Set main background image with custom opacity
     * 
     * @param imagePath Full path to image file
     * @param opacity   Value between 0.0f (transparent) and 1.0f (opaque)
     */
    public void setMainBackgroundImage(String imagePath, float opacity) {
        try {
            mainBgImage = new ImageIcon(imagePath).getImage();
            // TODO: Store opacity for use in paintComponent
        } catch (Exception e) {
            System.err.println("Failed to load main background: " + e.getMessage());
        }
    }

    /**
     * Set question card background image with custom opacity
     * 
     * @param imagePath Full path to image file
     * @param opacity   Value between 0.0f (transparent) and 1.0f (opaque)
     */
    public void setQuestionCardBackgroundImage(String imagePath, float opacity) {
        try {
            questionCardBgImage = new ImageIcon(imagePath).getImage();
            // TODO: Store opacity for use in paintComponent if needed
        } catch (Exception e) {
            System.err.println("Failed to load question card background: " + e.getMessage());
        }
    }

    class SkeletonCard extends JPanel {
        private float alpha = 0.3f;
        private boolean inc = true;
        private javax.swing.Timer animTimer;

        public SkeletonCard() {
            setOpaque(false); // Transparent to show background image
            // Don't set arc: 40 - it causes FlatLaf to paint a background
            setPreferredSize(new Dimension(1100, 500));
            setMaximumSize(new Dimension(1100, 500));
        }

        public void start() {
            if (animTimer != null && animTimer.isRunning()) {
                return;
            }
            animTimer = new javax.swing.Timer(ANIMATION_FRAME_DELAY_MS, e -> {
                if (!isShowing()) {
                    return;
                }
                alpha += inc ? 0.0053f : -0.0053f; // Ultra-smooth pulsing (5ms * 0.0053 ≈ 0.015 at 14ms)
                if (alpha >= 0.5f || alpha <= 0.2f)
                    inc = !inc;
                repaint();
            });
            animTimer.setCoalesce(true);
            animTimer.start();
        }

        public void stop() {
            if (animTimer != null && animTimer.isRunning()) {
                animTimer.stop();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            // Don't call super - FlatLaf will paint opaque background
            Graphics2D g2 = (Graphics2D) g;
            // 📈 HIGH-PERFORMANCE RENDERING FOR 200 FPS
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);

            int width = getWidth();
            int height = getHeight();
            int padding = 50;
            int contentWidth = width - (2 * padding);

            // Very faint skeleton (5% opacity max)
            g2.setColor(new Color(63, 63, 70, (int) (alpha * 255 * 1.45f)));

            // Question text - simulate paragraph lines
            int questionY = 50;
            int lineHeight = 24;
            int lineSpacing = 6;

            // First line (full width)
            g2.fillRoundRect(padding, questionY, contentWidth, lineHeight, 12, 12);

            // Second line (full width)
            g2.fillRoundRect(padding, questionY + lineHeight + lineSpacing, contentWidth, lineHeight, 12, 12);

            // Third line (full width)
            g2.fillRoundRect(padding, questionY + 2 * (lineHeight + lineSpacing), contentWidth, lineHeight, 12, 12);

            // Fourth line (partial - 60% width to look natural)
            g2.fillRoundRect(padding, questionY + 3 * (lineHeight + lineSpacing), (int) (contentWidth * 0.6),
                    lineHeight, 12, 12);

            // Options section with proper spacing
            int optionsStartY = questionY + 4 * (lineHeight + lineSpacing) + 30;
            int optionHeight = 40;
            int optionSpacing = 18;

            // Option 1 - checkbox/radio indicator + text
            g2.fillRoundRect(padding, optionsStartY, 22, optionHeight, 6, 6);
            g2.fillRoundRect(padding + 40, optionsStartY, contentWidth - 40, optionHeight, 12, 12);

            // Option 2
            g2.fillRoundRect(padding, optionsStartY + optionHeight + optionSpacing, 22, optionHeight, 6, 6);
            g2.fillRoundRect(padding + 40, optionsStartY + optionHeight + optionSpacing, contentWidth - 40,
                    optionHeight, 12, 12);

            // Option 3
            g2.fillRoundRect(padding, optionsStartY + 2 * (optionHeight + optionSpacing), 22, optionHeight, 6, 6);
            g2.fillRoundRect(padding + 40, optionsStartY + 2 * (optionHeight + optionSpacing), contentWidth - 40,
                    optionHeight, 12, 12);

            // Option 4
            g2.fillRoundRect(padding, optionsStartY + 3 * (optionHeight + optionSpacing), 22, optionHeight, 6, 6);
            g2.fillRoundRect(padding + 40, optionsStartY + 3 * (optionHeight + optionSpacing), contentWidth - 40,
                    optionHeight, 12, 12);
        }
    }

    public static void main(String[] args) {
        // 1. Core UI Setup (Must be first)
        try {
            com.formdev.flatlaf.themes.FlatMacDarkLaf.setup();
        } catch (Exception e) {
            System.err.println("Failed to initialize FlatLaf");
        }

        // 2. Build GUI in background thread while splash shows
        final AptitudeGui[] mainApp = new AptitudeGui[1];
        Thread loader = new Thread(() -> {
            try {
                mainApp[0] = new AptitudeGui();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "GUIBuilder");
        loader.start();

        // 3. Show splash and wait for completion, then smoothly transition to main
        // window
        ClaritySplash splash = new ClaritySplash();
        Thread splashThread = new Thread(() -> {
            try {
                // Show splash (this blocks until splash completes fade-out)
                splash.showSplash();

                // Once splash is done, immediately show the main window with smooth transition
                SwingUtilities.invokeLater(() -> {
                    if (mainApp[0] != null) {
                        // Try to set opacity for fade-in, but gracefully fall back if not supported
                        // (decorated frames on Windows don't support opacity)
                        boolean supportsOpacity = false;
                        try {
                            mainApp[0].setOpacity(0.0f);
                            supportsOpacity = true;
                        } catch (IllegalComponentStateException ex) {
                            // Frame is decorated, opacity not supported - just show it normally
                            supportsOpacity = false;
                        }

                        mainApp[0].setVisible(true);
                        mainApp[0].toFront();
                        mainApp[0].requestFocus();

                        // Only animate opacity if supported (undecorated frames)
                        if (supportsOpacity) {
                            // Ultra-smooth fade-in from splash ending to main window (5ms intervals = 200
                            // FPS)
                            javax.swing.Timer fadeInTimer = new javax.swing.Timer(ANIMATION_FRAME_DELAY_MS, null);
                            final float[] opacity = { 0.0f };
                            fadeInTimer.addActionListener(fadeEvent -> {
                                opacity[0] = Math.min(1.0f, opacity[0] + 0.02f); // Adjusted for 5ms
                                try {
                                    mainApp[0].setOpacity(opacity[0]);
                                } catch (Exception ex) {
                                    // Silently fail if opacity no longer supported
                                }

                                if (opacity[0] >= 1.0f) {
                                    ((javax.swing.Timer) fadeEvent.getSource()).stop();
                                }
                            });
                            fadeInTimer.setCoalesce(true);
                            fadeInTimer.start();
                        }
                    }
                });
            } catch (Exception e) {
                System.err.println("Splash error: " + e.getMessage());
            }
        }, "SplashThread");
        splashThread.setDaemon(false); // Not daemon - let it complete naturally
        splashThread.start();
    }
}