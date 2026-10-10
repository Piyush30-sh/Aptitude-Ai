package org.example;

import javax.swing.*;
import java.awt.*;

public class ClaritySplash extends JWindow {
    private String currentMessage = "INITIALIZING NEURAL CORE...";
    private float alpha = 0.0f;

    private ImageIcon splashIcon;

    public ClaritySplash() {

        // 1. Load the image from project resources without hardcoded machine-specific
        // paths.
        String imgPath = AppPaths.resourcePath("upscalemedia-transformed.png");
        splashIcon = new ImageIcon(imgPath);

        // 2. Calculate Scaling (Fit to 70% of screen height)
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int targetH = (int) (screen.height * 0.7);
        double scale = (double) targetH / splashIcon.getIconHeight();
        int targetW = (int) (splashIcon.getIconWidth() * scale);

        // 3. Set Window size and center it
        setSize(targetW, targetH);
        setLocationRelativeTo(null);

        // 4. Force window to be transparent for the fade effect later
        setBackground(new Color(0, 0, 0, 0));
    }

    @Override
    public void paint(Graphics g) {
        // We override paint() directly to ensure the image and text are one layer
        Graphics2D g2 = (Graphics2D) g;
        // HIGH-PERFORMANCE RENDERING HINTS FOR 200 FPS
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_LCD_CONTRAST, 140);

        // A. Draw the Image scaled to window size
        g2.drawImage(splashIcon.getImage(), 0, 0, getWidth(), getHeight(), null);

        // B. Draw a subtle dark overlay at the bottom for text contrast
        g2.setPaint(new GradientPaint(0, getHeight() - 120, new Color(0, 0, 0, 0), 0, getHeight(),
                new Color(0, 0, 0, 180)));
        g2.fillRect(0, getHeight() - 120, getWidth(), 120);

        // C. Draw the Pulsing Text
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setColor(new Color(180, 160, 255)); // Futuristic Purple
        g2.setFont(new Font("Monospaced", Font.BOLD, 26));

        FontMetrics fm = g2.getFontMetrics();
        int tx = (getWidth() - fm.stringWidth(currentMessage)) / 2;
        int ty = getHeight() - 60;

        g2.drawString(currentMessage, tx, ty);
    }

    public void showSplash() {
        setVisible(true);
        String[] messages = { "BOOTING CORE...", "LOADING AI...", "CLARITY OPENING..." };

        // Each message blinks once with fade-in, hold, fade-out sequence
        for (String message : messages) {
            currentMessage = message;

            // FADE IN (0 → 1, 200ms)
            for (float f = 0f; f <= 1.0f; f += 0.05f) {
                alpha = f;
                repaint();
                try {
                    Thread.sleep(10); // 10ms per frame for smooth fade-in
                } catch (Exception e) {
                }
            }

            // HOLD visible (300ms)
            alpha = 1.0f;
            repaint();
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }

            // FADE OUT (1 → 0, 200ms)
            for (float f = 1.0f; f >= 0f; f -= 0.04f) {
                alpha = f;
                repaint();
                try {
                    Thread.sleep(10); // 10ms per frame for smooth fade-out
                } catch (Exception e) {
                }
            }
        }

        // ULTRA-SMOOTH FADE OUT window (200ms)
        for (float f = 1.0f; f >= 0; f -= 0.05f) {
            try {
                this.setOpacity(f);
                Thread.sleep(10); // 10ms intervals
            } catch (Exception e) {
                break;
            }
        }
        dispose();
    }
}