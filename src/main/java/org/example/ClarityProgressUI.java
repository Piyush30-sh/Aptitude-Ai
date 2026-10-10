package org.example;
//package org.example;

import javax.swing.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;

public class ClarityProgressUI extends BasicProgressBarUI {
    @Override
    protected void paintDeterminate(Graphics g, JComponent c) {
        Graphics2D g2d = (Graphics2D) g.create(); // Use a copy of graphics
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = progressBar.getWidth();
        int h = progressBar.getHeight();
        double percent = progressBar.getPercentComplete();

        // Track - Use a semi-transparent white so we can see it against the dark image
        g2d.setColor(new Color(255, 255, 255, 50));
        g2d.fillRoundRect(0, 0, w, h, 10, 10);

        // Fill - Use a very bright Neon Purple/Cyan so it stands out
        int fillWidth = (int) (w * percent);
        if (fillWidth > 0) {
            // Neon Purple Gradient
            GradientPaint grad = new GradientPaint(0, 0, new Color(180, 100, 255),
                    fillWidth, 0, new Color(100, 200, 255));
            g2d.setPaint(grad);
            g2d.fillRoundRect(0, 0, fillWidth, h, 10, 10);
        }
        g2d.dispose();

    }
}
