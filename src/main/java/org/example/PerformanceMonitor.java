package org.example;

import com.sun.management.OperatingSystemMXBean;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.PaintEvent;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Live performance monitor overlay.
 * Displays JVM metrics and app paint throughput in real-time.
 */
public class PerformanceMonitor extends JPanel {
    private final AtomicInteger paintEventsThisSecond = new AtomicInteger(0);
    private final AtomicInteger currentUiFps = new AtomicInteger(0);
    private long secondWindowStartNanos = System.nanoTime();
    private double smoothedUiFps = 0.0;
    private int peakUiFps = 0;

    private long heapUsedBytes = 0;
    private long heapMaxBytes = 0;
    private long heapAverageBytes = 0;
    private int heapSampleCount = 0;

    private double cpuUsagePercent = 0;
    private int threadCount = 0;
    private long gcEventsPerSecond = 0;
    private long gcTimePerSecondMs = 0;
    private long lastGcEventsTotal = 0;
    private long lastGcTimeTotalMs = 0;

    private double timerJitterMs = 0;
    private long lastTickNanos = System.nanoTime();

    private volatile boolean monitoringEnabled = false;
    private volatile boolean paintTrackingInstalled = false;

    private final javax.swing.Timer updateTimer;
    private final AWTEventListener paintEventListener;

    private final List<GarbageCollectorMXBean> gcBeans;
    private final MemoryMXBean memoryBean;
    private final ThreadMXBean threadBean;
    private final OperatingSystemMXBean osBean;
    private final Window monitoredWindow;

    // Dragging support
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private boolean isDragging = false;

    public PerformanceMonitor(Window monitoredWindow) {
        this.monitoredWindow = monitoredWindow;
        setOpaque(false);
        setPreferredSize(new Dimension(250, 144));
        setMaximumSize(new Dimension(250, 144));
        setMinimumSize(new Dimension(250, 144));

        gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        memoryBean = ManagementFactory.getMemoryMXBean();
        threadBean = ManagementFactory.getThreadMXBean();

        java.lang.management.OperatingSystemMXBean baseOsBean = ManagementFactory.getOperatingSystemMXBean();
        if (baseOsBean instanceof OperatingSystemMXBean) {
            osBean = (OperatingSystemMXBean) baseOsBean;
        } else {
            osBean = null;
        }

        captureInitialGcTotals();

        paintEventListener = this::onPaintEvent;
        // Update monitor every 200ms; aggregate metric sampling each second.
        updateTimer = new javax.swing.Timer(200, e -> onTick());
        updateTimer.setCoalesce(true);
        setSamplingActive(monitoringEnabled && isVisible());

        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                isDragging = true;
                dragOffsetX = e.getX();
                dragOffsetY = e.getY();
                setCursor(new Cursor(Cursor.MOVE_CURSOR));
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                isDragging = false;
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });

        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (isDragging) {
                    Component parent = getParent();
                    if (parent != null) {
                        int newX = getX() + (e.getX() - dragOffsetX);
                        int newY = getY() + (e.getY() - dragOffsetY);

                        // Keep within parent bounds
                        newX = Math.max(0, Math.min(newX, parent.getWidth() - getWidth()));
                        newY = Math.max(0, Math.min(newY, parent.getHeight() - getHeight()));

                        setLocation(newX, newY);
                    }
                }
            }
        });
    }

    private void onPaintEvent(AWTEvent event) {
        if (!monitoringEnabled || !(event instanceof PaintEvent)) {
            return;
        }
        Object source = event.getSource();
        if (!(source instanceof Component)) {
            return;
        }

        Component sourceComponent = (Component) source;
        Window sourceWindow = SwingUtilities.getWindowAncestor(sourceComponent);
        if (sourceWindow == monitoredWindow) {
            paintEventsThisSecond.incrementAndGet();
        }
    }

    private synchronized void setPaintTrackingEnabled(boolean enabled) {
        if (enabled && !paintTrackingInstalled) {
            try {
                Toolkit.getDefaultToolkit().addAWTEventListener(paintEventListener, AWTEvent.PAINT_EVENT_MASK);
                paintTrackingInstalled = true;
            } catch (SecurityException ignored) {
                paintTrackingInstalled = false;
            }
            return;
        }

        if (!enabled && paintTrackingInstalled) {
            try {
                Toolkit.getDefaultToolkit().removeAWTEventListener(paintEventListener);
            } catch (Exception ignored) {
            }
            paintTrackingInstalled = false;
        }
    }

    private synchronized void setSamplingActive(boolean active) {
        if (active) {
            if (!updateTimer.isRunning()) {
                long now = System.nanoTime();
                lastTickNanos = now;
                secondWindowStartNanos = now;
                updateTimer.start();
            }
            setPaintTrackingEnabled(true);
            return;
        }

        if (updateTimer.isRunning()) {
            updateTimer.stop();
        }
        paintEventsThisSecond.set(0);
        setPaintTrackingEnabled(false);
    }

    private void onTick() {
        long nowNanos = System.nanoTime();
        if (!monitoringEnabled || !isShowing()) {
            lastTickNanos = nowNanos;
            return;
        }

        double elapsedMs = (nowNanos - lastTickNanos) / 1_000_000.0;
        timerJitterMs = Math.max(0, Math.abs(elapsedMs - 200.0));
        lastTickNanos = nowNanos;

        if (nowNanos - secondWindowStartNanos >= 1_000_000_000L) {
            int sampledPaintEvents = paintEventsThisSecond.getAndSet(0);
            smoothedUiFps = smoothedUiFps == 0.0
                    ? sampledPaintEvents
                    : (smoothedUiFps * 0.70) + (sampledPaintEvents * 0.30);

            int fpsNow = (int) Math.round(smoothedUiFps);
            currentUiFps.set(fpsNow);
            peakUiFps = Math.max(peakUiFps, fpsNow);

            refreshJvmMetrics();
            secondWindowStartNanos = nowNanos;
        }

        repaint();
    }

    private void captureInitialGcTotals() {
        long totalCount = 0;
        long totalTime = 0;
        for (GarbageCollectorMXBean bean : gcBeans) {
            long count = bean.getCollectionCount();
            long time = bean.getCollectionTime();
            if (count > 0) {
                totalCount += count;
            }
            if (time > 0) {
                totalTime += time;
            }
        }
        lastGcEventsTotal = totalCount;
        lastGcTimeTotalMs = totalTime;
    }

    private void refreshJvmMetrics() {
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        heapUsedBytes = Math.max(0, heap.getUsed());

        long reportedMax = heap.getMax();
        heapMaxBytes = reportedMax > 0 ? reportedMax : Runtime.getRuntime().maxMemory();

        if (heapSampleCount == 0) {
            heapAverageBytes = heapUsedBytes;
        } else {
            heapAverageBytes = (heapAverageBytes * heapSampleCount + heapUsedBytes) / (heapSampleCount + 1);
        }
        heapSampleCount++;

        if (osBean != null) {
            double rawLoad = osBean.getProcessCpuLoad();
            if (rawLoad >= 0) {
                cpuUsagePercent = Math.min(100.0, Math.max(0.0, rawLoad * 100.0));
            }
        }

        threadCount = threadBean.getThreadCount();

        long totalCount = 0;
        long totalTime = 0;
        for (GarbageCollectorMXBean bean : gcBeans) {
            long count = bean.getCollectionCount();
            long time = bean.getCollectionTime();
            if (count > 0) {
                totalCount += count;
            }
            if (time > 0) {
                totalTime += time;
            }
        }

        gcEventsPerSecond = Math.max(0, totalCount - lastGcEventsTotal);
        gcTimePerSecondMs = Math.max(0, totalTime - lastGcTimeTotalMs);
        lastGcEventsTotal = totalCount;
        lastGcTimeTotalMs = totalTime;
    }

    /**
     * Manual frame hook if caller wants to push custom render/frame updates.
     */
    public void recordFrame() {
        if (monitoringEnabled) {
            paintEventsThisSecond.incrementAndGet();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!monitoringEnabled)
            return;

        Graphics2D g2d = (Graphics2D) g;

        // Enable text antialiasing only
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);

        int width = getWidth();
        int height = getHeight();

        // Subtle dark semi-transparent background with gradient
        g2d.setColor(new Color(0, 0, 0, 120));
        g2d.fillRoundRect(5, 5, width - 10, height - 10, 10, 10);

        // Gradient overlay
        GradientPaint gradient = new GradientPaint(5, 5, new Color(99, 102, 241, 40),
                width - 5, height - 5, new Color(99, 102, 241, 0));
        g2d.setPaint(gradient);
        g2d.fillRoundRect(5, 5, width - 10, height - 10, 10, 10);

        // Border with glow effect
        g2d.setColor(new Color(99, 102, 241, 200));
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawRoundRect(5, 5, width - 10, height - 10, 10, 10);

        int uiFpsValue = currentUiFps.get();
        Color fpsColor = getUiFpsColor(uiFpsValue);

        // Row 1: UI FPS and CPU
        g2d.setColor(fpsColor);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 13));
        g2d.drawString(String.format("UI FPS: %d", uiFpsValue), 15, 25);

        g2d.setColor(getCpuColor());
        g2d.drawString(String.format("CPU: %.1f%%", cpuUsagePercent), width - 108, 25);

        // Row 2: Heap usage
        g2d.setColor(getHeapColor());
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g2d.drawString(
                String.format("Heap: %dMB/%dMB", heapUsedBytes / 1024 / 1024, heapMaxBytes / 1024 / 1024),
                15,
                43);

        // Row 3: Peak and threads
        g2d.setColor(new Color(203, 213, 225));
        g2d.drawString(String.format("Peak: %d | Threads: %d", peakUiFps, threadCount), 15, 61);

        // Row 4: GC and jitter
        g2d.setColor(new Color(203, 213, 225));
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g2d.drawString(String.format("GC/s: %d  GC-ms/s: %d", gcEventsPerSecond, gcTimePerSecondMs), 15, 77);

        g2d.setColor(new Color(148, 163, 184));
        g2d.drawString(String.format("Jitter: %.1fms  AvgHeap: %dMB", timerJitterMs, heapAverageBytes / 1024 / 1024),
                15,
                92);

        // Row 5: Status indicator
        String status = getStatusLabel(uiFpsValue);
        Color statusColor = getStatusColor(uiFpsValue);

        g2d.setColor(statusColor);
        g2d.setFont(new Font("Inter", Font.BOLD, 11));
        g2d.drawString(status, 15, 109);

        g2d.setColor(new Color(99, 102, 241, 180));
        g2d.drawString("Monitor: ACTIVE", width - 118, 109);

        // Drag handle indicator.
        g2d.setColor(new Color(99, 102, 241, 100));
        g2d.setStroke(new BasicStroke(1.0f));
        for (int i = 0; i < 3; i++) {
            g2d.drawRect(width - 18 + i * 3, 10, 2, 2);
            g2d.drawRect(width - 18 + i * 3, 15, 2, 2);
        }
    }

    private Color getUiFpsColor(int fps) {
        if (fps >= 90) {
            return new Color(34, 197, 94);
        }
        if (fps >= 60) {
            return new Color(255, 193, 7);
        }
        return new Color(239, 68, 68);
    }

    private Color getCpuColor() {
        if (cpuUsagePercent < 35) {
            return new Color(34, 197, 94);
        }
        if (cpuUsagePercent < 65) {
            return new Color(255, 193, 7);
        }
        return new Color(239, 68, 68);
    }

    private Color getHeapColor() {
        if (heapMaxBytes <= 0) {
            return new Color(203, 213, 225);
        }
        double usagePercent = (double) heapUsedBytes / heapMaxBytes * 100.0;
        if (usagePercent < 50) {
            return new Color(34, 197, 94);
        }
        if (usagePercent < 75) {
            return new Color(255, 193, 7);
        }
        return new Color(239, 68, 68);
    }

    private String getStatusLabel(int fps) {
        if (fps >= 90 && cpuUsagePercent < 70) {
            return "Status: Excellent";
        }
        if (fps >= 60 && cpuUsagePercent < 85) {
            return "Status: Good";
        }
        if (cpuUsagePercent >= 85) {
            return "Status: CPU Bound";
        }
        return "Status: Fair";
    }

    private Color getStatusColor(int fps) {
        if (fps >= 90 && cpuUsagePercent < 70) {
            return new Color(34, 197, 94);
        }
        if (fps >= 60 && cpuUsagePercent < 85) {
            return new Color(255, 193, 7);
        }
        return new Color(239, 68, 68);
    }

    public void setMonitoringEnabled(boolean enabled) {
        this.monitoringEnabled = enabled;
        setSamplingActive(enabled && isVisible());
        if (!enabled) {
            repaint();
        }
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        setSamplingActive(monitoringEnabled && visible);
        if (visible && monitoringEnabled) {
            repaint();
        }
    }

    public int getCurrentFPS() {
        return currentUiFps.get();
    }

    public long getPeakFPS() {
        return peakUiFps;
    }

    public void stop() {
        setSamplingActive(false);
    }
}
