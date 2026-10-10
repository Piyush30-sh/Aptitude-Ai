package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.LinearGradientPaint;
import javax.swing.Timer;

/**
 * LoginDialog — Premium Glassmorphism Edition
 * Precise match + upgraded: animated border glow, avatar badge,
 * shimmer button, layered depth card, smooth focus rings.
 */
public class LoginDialog extends JDialog {

    private AptitudeGui parentGui;

    // ── palette ───────────────────────────────────────────────────────────
    private static final Color CARD_TOP = new Color(30, 41, 68, 245);
    private static final Color CARD_BOT = new Color(20, 30, 55, 250);

    private static final Color BORDER_GLOW_1 = new Color(99, 179, 255, 180);
    private static final Color BORDER_GLOW_2 = new Color(147, 112, 255, 160);
    private static final Color BORDER_GLOW_3 = new Color(56, 189, 248, 140);

    private static final Color GLASS_SHEEN = new Color(255, 255, 255, 22);

    private static final Color FIELD_BG = new Color(15, 23, 42, 210);
    private static final Color FIELD_BG_FOCUS = new Color(15, 23, 55, 230);
    private static final Color FIELD_BORDER = new Color(100, 116, 139, 90);
    private static final Color FIELD_FOCUS_CLR = new Color(99, 179, 255, 220);
    private static final Color FIELD_GLOW = new Color(56, 189, 248, 40);

    private static final Color TEXT_PRIMARY = new Color(0xf1f5f9);
    private static final Color TEXT_SECONDARY = new Color(0xcbd5e1);
    private static final Color TEXT_MUTED = new Color(0x94a3b8);
    private static final Color TEXT_LINK = new Color(0x38bdf8);
    private static final Color TEXT_LABEL = new Color(0x7dd3fc);

    private static final Color CYAN_ACCENT = new Color(0x38bdf8);

    private static final Color BTN_START = new Color(0x2563eb);
    private static final Color BTN_END = new Color(0x7c3aed);
    private static final Color BTN_HOVER_START = new Color(0x3b82f6);
    private static final Color BTN_HOVER_END = new Color(0x9333ea);

    private static final Color CLOSE_BG = new Color(127, 29, 29, 200);
    private static final Color CLOSE_HOV = new Color(185, 28, 28, 230);

    private static final Color AVATAR_BG = new Color(30, 41, 68, 255);

    private static final Color EYE_BG = new Color(30, 41, 68, 200);
    private static final Color EYE_HOV = new Color(51, 65, 85, 220);
    private static final Color ERROR_RED = new Color(0xf87171);

    private static final Color GOOGLE_RED = new Color(0xea4335);
    private static final Color FB_BLUE = new Color(0x1877f2);
    private static final Color SOCIAL_BG = new Color(15, 23, 42, 220);
    private static final Color SOCIAL_HOV = new Color(30, 41, 68, 240);

    private static final Color DIVIDER_CLR = new Color(100, 116, 139, 80);

    // ── animated glow state ───────────────────────────────────────────────
    private float glowAngle = 0f;
    private Timer glowTimer;
    private boolean btnHovered = false;

    // ── performance caching ───────────────────────────────────────────────
    private BufferedImage cachedCardBg = null;
    private int cachedCardW = 0, cachedCardH = 0;

    public LoginDialog(AptitudeGui parent) {
        super(parent, "Login", true);
        this.parentGui = parent;
        initDialog();
    }

    private void initDialog() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setUndecorated(true);
        setSize(400, 620);
        setLocationRelativeTo(null);
        setBackground(new Color(0, 0, 0, 0));

        final int CW = 400;

        // ── animated border card ──────────────────────────────────────────
        JPanel card = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                int w = getWidth(), h = getHeight();
                int arc = 32;

                // Use cached background if dimensions match
                if (cachedCardBg == null || cachedCardW != w || cachedCardH != h) {
                    cachedCardBg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    cachedCardW = w;
                    cachedCardH = h;
                    Graphics2D g2c = cachedCardBg.createGraphics();
                    g2c.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    // ── fast shadow (1 pass) ──
                    g2c.setColor(new Color(0, 0, 0, 80));
                    g2c.fillRoundRect(6, 12, w - 12, h - 14, arc, arc);

                    // ── card body gradient ──
                    GradientPaint body = new GradientPaint(0, 0, CARD_TOP, w * 0.3f, h, CARD_BOT);
                    g2c.setPaint(body);
                    g2c.fillRoundRect(0, 0, w, h, arc, arc);

                    // ── top glass sheen ──
                    GradientPaint sheen1 = new GradientPaint(0, 0, GLASS_SHEEN, 0, (int) (h * 0.38f),
                            new Color(255, 255, 255, 0));
                    g2c.setPaint(sheen1);
                    g2c.fillRoundRect(1, 1, w - 2, (int) (h * 0.38f), arc - 2, arc - 2);

                    g2c.dispose();
                }

                // Draw cached background once
                g.drawImage(cachedCardBg, 0, 0, null);

                // Draw animated border only (minimal per-frame work)
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                double a = glowAngle;
                Color c1 = blendColor(BORDER_GLOW_1, BORDER_GLOW_2, (float) (Math.sin(a) * 0.5 + 0.5));
                Color c2 = blendColor(BORDER_GLOW_2, BORDER_GLOW_3, (float) (Math.sin(a + Math.PI * 0.66) * 0.5 + 0.5));

                g2.setStroke(new BasicStroke(2.2f));
                float cx = w / 2f, cy2 = h / 2f;
                float dx = (float) (Math.cos(a) * w * 0.7f);
                float dy = (float) (Math.sin(a) * h * 0.7f);
                GradientPaint borderGlow = new GradientPaint(
                        cx + dx, cy2 + dy, c1,
                        cx - dx, cy2 - dy, c2);
                g2.setPaint(borderGlow);
                g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);

                g2.dispose();
            }
        };
        card.setOpaque(false);

        // Start animated border glow (5ms for 200+ FPS capability)
        glowTimer = new Timer(5, e -> {
            glowAngle += 0.040f;
            if (glowAngle > Math.PI * 2)
                glowAngle -= Math.PI * 2;
            card.repaint(0, 0, card.getWidth(), card.getHeight());
        });
        glowTimer.start();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (glowTimer != null)
                    glowTimer.stop();
            }
        });

        // ── layout constants ──────────────────────────────────────────────
        final boolean[] signInMode = { true };
        final boolean[] showPass = { false };
        int px = 44;
        int cy = 16;

        // ── avatar badge ──────────────────────────────────────────────────
        JPanel avatarPanel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int s = getWidth();

                // Static outer glow (no animation needed - card already animates)
                g2.setColor(new Color(56, 189, 248, 25));
                g2.fillOval(0, 0, s, s);

                // simple static ring
                g2.setColor(new Color(99, 179, 255, 120));
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(3, 3, s - 7, s - 7);

                // inner circle bg
                g2.setColor(AVATAR_BG);
                g2.fillOval(6, 6, s - 13, s - 13);

                // simplified gradient fill
                GradientPaint avatarGrad = new GradientPaint(0, 0, new Color(37, 99, 235, 200), s, s,
                        new Color(124, 58, 237, 180));
                g2.setPaint(avatarGrad);
                g2.fillOval(6, 6, s - 13, s - 13);

                // person silhouette
                g2.setColor(new Color(255, 255, 255, 220));
                int cs = s - 13;
                int ox = 6;
                int oy = 6;
                // head
                g2.fillOval(ox + cs / 2 - 7, oy + 8, 14, 14);
                // body arc
                g2.fillArc(ox + cs / 2 - 11, oy + cs - 22, 22, 18, 0, 180);

                g2.dispose();
            }
        };
        int avSize = 62;
        avatarPanel.setBounds((CW - avSize) / 2, cy, avSize, avSize);
        avatarPanel.setOpaque(false);
        card.add(avatarPanel);
        cy += avSize + 12;

        // ── subtitle ──────────────────────────────────────────────────────
        JLabel subtitle = new JLabel("", SwingConstants.CENTER);
        subtitle.setFont(loadFont("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(TEXT_SECONDARY);
        subtitle.setBounds(px, cy, CW - px * 2, 36);
        card.add(subtitle);
        cy += 40;

        // ── close button ──────────────────────────────────────────────────
        JButton closeBtn = makeFlatBtn("X", new Font("Maiandra GD", Font.BOLD, 20), CLOSE_BG, Color.WHITE);
        closeBtn.setBounds(CW - 48, 14, 30, 30);
        addHover(closeBtn, CLOSE_BG, CLOSE_HOV);
        closeBtn.addActionListener(e -> {
            glowTimer.stop();
            dispose();
        });
        card.add(closeBtn);

        // ── tabs ──────────────────────────────────────────────────────────
        JButton tabSignIn = makeTabBtn("Sign in");
        JButton tabSignUp = makeTabBtn("Sign up");
        int tabW = 120, tabH = 34;
        int tabsX = (CW - tabW * 2 - 8) / 2;
        tabSignIn.setBounds(tabsX, cy, tabW, tabH);
        tabSignUp.setBounds(tabsX + tabW + 8, cy, tabW, tabH);
        card.add(tabSignIn);
        card.add(tabSignUp);
        cy += tabH;

        // underline panels
        JPanel ulSignIn = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                LinearGradientPaint gp = new LinearGradientPaint(
                        0, 0, getWidth(), 0,
                        new float[] { 0f, 0.5f, 1f },
                        new Color[] { new Color(56, 189, 248, 0), CYAN_ACCENT, new Color(56, 189, 248, 0) });
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 2, 2);
                g2.dispose();
            }
        };
        ulSignIn.setOpaque(false);
        ulSignIn.setBounds(tabsX + 16, cy + 2, tabW - 32, 3);

        JPanel ulSignUp = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                LinearGradientPaint gp = new LinearGradientPaint(
                        0, 0, getWidth(), 0,
                        new float[] { 0f, 0.5f, 1f },
                        new Color[] { new Color(56, 189, 248, 0), CYAN_ACCENT, new Color(56, 189, 248, 0) });
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 2, 2);
                g2.dispose();
            }
        };
        ulSignUp.setOpaque(false);
        ulSignUp.setBounds(tabsX + tabW + 8 + 16, cy + 2, tabW - 32, 3);
        ulSignUp.setVisible(false);
        card.add(ulSignIn);
        card.add(ulSignUp);
        cy += 14;

        // ── Email field ───────────────────────────────────────────────────
        card.add(makeFieldLabel("EMAIL ADDRESS")).setBounds(px, cy, 200, 13);
        cy += 16;
        JTextField emailField = makeTextField("xyz@gmail.com");
        emailField.setBounds(px, cy, CW - px * 2, 44);
        card.add(emailField);
        cy += 52;

        // ── Password field ────────────────────────────────────────────────
        card.add(makeFieldLabel("PASSWORD")).setBounds(px, cy, 200, 13);
        cy += 16;
        JPasswordField passField = makePassField("••••••••");
        char defaultEcho = passField.getEchoChar();
        passField.setBounds(px, cy, CW - px * 2 - 50, 44);

        JButton eyeBtn = makeIconBtn(showPass[0] ? "🙈" : "👁");
        eyeBtn.setBounds(CW - px - 44, cy + 2, 40, 40);
        eyeBtn.addActionListener(e -> {
            showPass[0] = !showPass[0];
            passField.setEchoChar(showPass[0] ? (char) 0 : defaultEcho);
            eyeBtn.setText(showPass[0] ? "🙈" : "👁");
        });
        card.add(passField);
        card.add(eyeBtn);
        cy += 52;

        // ── Options row ───────────────────────────────────────────────────
        JCheckBox rememberMe = makeStyledCheckbox("Remember me");
        rememberMe.setFont(new Font("Mongolian Baiti", Font.BOLD, 12));
        rememberMe.setBounds(px, cy, 103, 22);
        card.add(rememberMe);

        JButton forgotBtn = makeLinkBtn("Forgot password?");

        forgotBtn.setBounds(CW - px - 120, cy, 147, 32);
        forgotBtn.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Demo mode only.\nCredentials: demo / demo123", "Forgot Password",
                JOptionPane.INFORMATION_MESSAGE));
        card.add(forgotBtn);
        cy += 30;

        // ── Error label ───────────────────────────────────────────────────
        JLabel errorLbl = new JLabel(" ", SwingConstants.CENTER);
        errorLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        errorLbl.setForeground(ERROR_RED);
        errorLbl.setBounds(px, cy, CW - px * 2, 16);
        card.add(errorLbl);
        cy += 20;

        // ── Gradient auth button ──────────────────────────────────────────
        JButton authBtn = new JButton("Login") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                // simplified glow (2 layers instead of 10)
                if (btnHovered) {
                    g2.setColor(new Color(37, 99, 235, 40));
                    g2.fillRoundRect(-4, -4, w + 8, h + 8, 18, 18);
                }

                // gradient fill
                Color s = btnHovered ? BTN_HOVER_START : BTN_START;
                Color e2 = btnHovered ? BTN_HOVER_END : BTN_END;
                GradientPaint gp = new GradientPaint(0, 0, s, w, h, e2);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, w, h, 14, 14);

                // minimal shimmer (removed for max FPS)

                // border (optional, removed for max FPS)

                g2.dispose();
                super.paintComponent(g);
            }
        };
        authBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        authBtn.setForeground(Color.WHITE);
        authBtn.setFocusPainted(false);
        authBtn.setContentAreaFilled(false);
        authBtn.setBorderPainted(false);
        authBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        authBtn.setBounds(px, cy, CW - px * 2, 46);
        authBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btnHovered = true;
                authBtn.repaint(0, 0, authBtn.getWidth(), authBtn.getHeight());
            }

            public void mouseExited(MouseEvent e) {
                btnHovered = false;
                authBtn.repaint(0, 0, authBtn.getWidth(), authBtn.getHeight());
            }
        });
        card.add(authBtn);
        cy += 54;

        // ── Switch mode link ──────────────────────────────────────────────
        JButton switchBtn = new JButton();
        switchBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        switchBtn.setForeground(TEXT_MUTED);
        switchBtn.setContentAreaFilled(false);
        switchBtn.setBorderPainted(false);
        switchBtn.setFocusPainted(false);
        switchBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        switchBtn.setBounds(px, cy, CW - px * 2, 22);
        switchBtn.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(switchBtn);
        cy += 28;

        // ── Divider with OR ───────────────────────────────────────────────
        JPanel divider = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                int w = getWidth(), h = getHeight();
                int mid = h / 2;
                GradientPaint left = new GradientPaint(0, mid, new Color(0, 0, 0, 0), w / 2 - 18, mid, DIVIDER_CLR);
                GradientPaint right = new GradientPaint(w / 2 + 18, mid, DIVIDER_CLR, w, mid, new Color(0, 0, 0, 0));
                g2.setPaint(left);
                g2.fillRect(0, mid, w / 2 - 18, 1);
                g2.setPaint(right);
                g2.fillRect(w / 2 + 18, mid, w / 2 - 18, 1);
                g2.dispose();
            }
        };
        JLabel orLbl = new JLabel("OR", SwingConstants.CENTER);
        orLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        orLbl.setForeground(TEXT_MUTED);
        orLbl.setBounds(0, 0, CW - px * 2, 18);
        divider.setOpaque(false);
        divider.setBounds(px, cy, CW - px * 2, 18);
        divider.add(orLbl);
        card.add(divider);
        cy += 22;

        // ── Social buttons ────────────────────────────────────────────────
        JButton gBtn = makeSocialBtn("G", GOOGLE_RED, "Google");
        JButton fBtn = makeSocialBtn("f", FB_BLUE, "Facebook");
        int socialW = 130, socialH = 40;
        int socialSpacing = 12;
        int totalSocial = socialW * 2 + socialSpacing;
        int socialStartX = (CW - totalSocial) / 2;
        gBtn.setBounds(socialStartX, cy, socialW, socialH);
        fBtn.setBounds(socialStartX + socialW + socialSpacing, cy, socialW, socialH);
        gBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Demo mode.\nUse: demo / demo123", "Google",
                JOptionPane.INFORMATION_MESSAGE));
        fBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Demo mode.\nUse: demo / demo123", "Facebook",
                JOptionPane.INFORMATION_MESSAGE));
        card.add(gBtn);
        card.add(fBtn);
        cy += socialH + 10;

        // ── Hint ──────────────────────────────────────────────────────────
        JLabel hintLbl = new JLabel("Demo credentials: demo / demo123", SwingConstants.CENTER);
        hintLbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        hintLbl.setForeground(new Color(100, 150, 200, 160));
        hintLbl.setBounds(px, cy, CW - px * 2, 14);
        card.add(hintLbl);

        // ── Mode switching logic ──────────────────────────────────────────
        Runnable applyMode = () -> {
            boolean si = signInMode[0];
            tabSignIn.setForeground(si ? TEXT_PRIMARY : TEXT_MUTED);
            tabSignUp.setForeground(si ? TEXT_MUTED : TEXT_PRIMARY);
            ulSignIn.setVisible(si);
            ulSignUp.setVisible(!si);

            subtitle.setText(si
                    ? "<html><center>Welcome back!<br><font color='#94a3b8'>Sign in to continue your journey</font></center></html>"
                    : "<html><center>Join the community<br><font color='#94a3b8'>Create your free account</font></center></html>");
            authBtn.setText(si ? "Sign In" : "Create Account");
            switchBtn.setText(si
                    ? "<html><font color='#94a3b8'>New here?&nbsp;</font><u><font color='#38bdf8'>Create an account</font></u></html>"
                    : "<html><font color='#94a3b8'>Already a member?&nbsp;</font><u><font color='#38bdf8'>Sign in</font></u></html>");
            errorLbl.setText(" ");
        };

        tabSignIn.addActionListener(e -> {
            signInMode[0] = true;
            applyMode.run();
        });
        tabSignUp.addActionListener(e -> {
            signInMode[0] = false;
            applyMode.run();
        });
        switchBtn.addActionListener(e -> {
            signInMode[0] = !signInMode[0];
            applyMode.run();
        });

        // ── Auth logic ────────────────────────────────────────────────────
        authBtn.addActionListener(e -> {
            String user = emailField.getText().trim();
            String pass = new String(passField.getPassword());
            if (user.isEmpty() || pass.isEmpty()) {
                errorLbl.setText("Please enter email and password");
                return;
            }
            if (!SecurityManager.DEMO_USERNAME.equalsIgnoreCase(user) || !SecurityManager.DEMO_PASSWORD.equals(pass)) {
                errorLbl.setText("Invalid credentials — use: demo / demo123");
                passField.setText("");
                return;
            }
            parentGui.isLoggedIn = true;
            parentGui.loggedInUser = SecurityManager.DEMO_USERNAME;

            // Initialize sound effects for correct/wrong answers
            parentGui.setAnswerSoundPaths(
                    AppPaths.resourcePath("correct.wav"),
                    AppPaths.resourcePath("wrong.wav"));
            parentGui.setSoundEffects(true);
            parentGui.setEffectsVolume(75);

            glowTimer.stop();
            dispose();
            parentGui.updateSidebarForLogin();
            parentGui.showNotification(signInMode[0]
                    ? "✅ Signed in as demo user"
                    : "✅ Account created. Signed in as demo");
        });

        passField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                    authBtn.doClick();
            }
        });

        applyMode.run();

        // Drag to move (optimized with debouncing)
        int[] dragOffset = { 0, 0 };
        final long[] lastDragTime = { 0 };
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragOffset[0] = e.getX();
                dragOffset[1] = e.getY();
            }
        });
        card.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                long now = System.currentTimeMillis();
                if (now - lastDragTime[0] < 16)
                    return; // ~60 FPS throttle
                lastDragTime[0] = now;

                Point loc = getLocation();
                setLocation(loc.x + e.getX() - dragOffset[0], loc.y + e.getY() - dragOffset[1]);
            }
        });

        setContentPane(card);
        getRootPane().setDefaultButton(authBtn);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                setShape(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 32, 32));
            }
        });
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private Font loadFont(String name, int style, int size) {
        return new Font(name, style, size);
    }

    private Color blendColor(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    private JButton makeTabBtn(String text) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                // no background
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(getForeground());
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        b.setFont(new Font("Segoe UI", Font.BOLD, 19));
        b.setForeground(TEXT_PRIMARY);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JLabel makeFieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(TEXT_LABEL);
        return l;
    }

    private JTextField makeTextField(String placeholder) {
        JTextField f = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = isFocusOwner() ? FIELD_BG_FOCUS : FIELD_BG;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                if (isFocusOwner()) {
                    // inner glow
                    g2.setColor(FIELD_GLOW);
                    g2.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 11, 11);
                }
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isFocusOwner()) {
                    g2.setColor(FIELD_FOCUS_CLR);
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                } else {
                    g2.setColor(FIELD_BORDER);
                    g2.setStroke(new BasicStroke(0.8f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                }
                g2.dispose();
            }
        };
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setOpaque(false);
        f.setForeground(TEXT_PRIMARY);
        f.setCaretColor(new Color(0xe0e7ff));
        f.putClientProperty("JTextField.placeholderText", placeholder);
        f.setBorder(new EmptyBorder(10, 14, 10, 14));
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                f.repaint();
            }

            public void focusLost(FocusEvent e) {
                f.repaint();
            }
        });
        return f;
    }

    private JPasswordField makePassField(String placeholder) {
        JPasswordField f = new JPasswordField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = isFocusOwner() ? FIELD_BG_FOCUS : FIELD_BG;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                if (isFocusOwner()) {
                    g2.setColor(FIELD_GLOW);
                    g2.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 11, 11);
                }
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isFocusOwner()) {
                    g2.setColor(FIELD_FOCUS_CLR);
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                } else {
                    g2.setColor(FIELD_BORDER);
                    g2.setStroke(new BasicStroke(0.8f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                }
                g2.dispose();
            }
        };
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setOpaque(false);
        f.setForeground(TEXT_PRIMARY);
        f.setCaretColor(new Color(0xe0e7ff));
        f.putClientProperty("JTextField.placeholderText", placeholder);
        f.setBorder(new EmptyBorder(10, 14, 10, 14));
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                f.repaint();
            }

            public void focusLost(FocusEvent e) {
                f.repaint();
            }
        });
        return f;
    }

    private JButton makeFlatBtn(String text, Font font, Color bg, Color fg) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(font);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton makeIconBtn(String icon) {
        JButton b = new JButton(icon) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(FIELD_BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        b.setBackground(EYE_BG);
        b.setForeground(TEXT_SECONDARY);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addHover(b, EYE_BG, EYE_HOV);
        return b;
    }

    private JButton makeSocialBtn(String letter, Color accent, String label) {
        JButton b = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, w, h, 12, 12);

                // simplified icon + label rendering (no font metrics on every paint)
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                g2.setColor(accent);
                g2.drawString(letter + " " + label, 12, (h + 5) / 2);

                g2.dispose();
            }
        };
        b.setBackground(SOCIAL_BG);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addHover(b, SOCIAL_BG, SOCIAL_HOV);
        return b;
    }

    private JCheckBox makeStyledCheckbox(String text) {
        JCheckBox cb = new JCheckBox(text);
        cb.setOpaque(false);
        cb.setForeground(TEXT_MUTED);
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        cb.setFocusPainted(false);
        return cb;
    }

    private JButton makeLinkBtn(String html) {
        JButton b = new JButton("<html><u><font color='#38bdf8'>" + html + "</font></u></html>");
        b.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        b.setForeground(TEXT_LINK);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setHorizontalAlignment(SwingConstants.RIGHT);
        return b;
    }

    private void addHover(JButton b, Color normal, Color hover) {
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                b.setBackground(hover);
                b.repaint(0, 0, b.getWidth(), b.getHeight());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                b.setBackground(normal);
                b.repaint(0, 0, b.getWidth(), b.getHeight());
            }
        });
    }
}