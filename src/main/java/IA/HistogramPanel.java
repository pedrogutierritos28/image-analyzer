package IA;

import javax.swing.*;
import java.awt.*;

import static IA.AppTheme.*;

/**
 * Panel que dibuja el histograma RGB + Luminosidad de una imagen.
 * Gestiona internamente la visibilidad de canales y la escala logarítmica.
 */
public class HistogramPanel extends JPanel {

    private static final int PAD_L = 44, PAD_R = 14, PAD_T = 18, PAD_B = 30;
    private static final Color[]  CHANNEL_COLORS = {CH_R, CH_G, CH_B, CH_L};
    private static final String[] CHANNEL_NAMES  = {"R", "G", "B", "L"};

    private int[][]  histogram;
    private boolean[] channelVisible = {true, true, true, true};
    private boolean   logScale       = false;

    public HistogramPanel() {
        setBackground(BG_CARD);
        setPreferredSize(new Dimension(420, 260));
    }

    // ── API pública ──────────────────────────────────────────

    public void setHistogram(int[][] hist)             { this.histogram = hist; repaint(); }
    public void setChannelVisible(int ch, boolean v)   { channelVisible[ch] = v; repaint(); }
    public void setLogScale(boolean log)               { this.logScale = log;    repaint(); }

    // ── Pintura ──────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth()  - PAD_L - PAD_R;
        int h = getHeight() - PAD_T  - PAD_B;

        paintGrid(g2, w, h);

        if (histogram == null) {
            paintNoData(g2, w, h);
            paintAxes(g2, w, h);
            return;
        }

        double maxVal = computeMax();
        double binW   = (double) w / 256;

        for (int c = 3; c >= 0; c--) {
            if (channelVisible[c]) paintChannelPolygon(g2, c, w, h, binW, maxVal);
        }

        paintAxes(g2, w, h);
        paintLegend(g2, w);
    }

    // ── Privados ─────────────────────────────────────────────

    private double computeMax() {
        double max = 1;
        for (int c = 0; c < 4; c++) {
            if (!channelVisible[c]) continue;
            for (int i = 0; i < 256; i++) {
                double v = logScale
                        ? (histogram[c][i] > 0 ? Math.log1p(histogram[c][i]) : 0)
                        : histogram[c][i];
                if (v > max) max = v;
            }
        }
        return max;
    }

    private void paintGrid(Graphics2D g2, int w, int h) {
        g2.setStroke(new BasicStroke(0.5f));
        for (int pct = 25; pct < 100; pct += 25) {
            int yy = PAD_T + h - (int)((pct / 100.0) * h);
            g2.setColor(new Color(28, 32, 46));
            g2.drawLine(PAD_L, yy, PAD_L + w, yy);
            g2.setColor(new Color(40, 46, 66));
            g2.setFont(AppTheme.monoPlain(8));
            g2.drawString(pct + "%", 2, yy + 4);
        }
        for (int i = 64; i < 256; i += 64) {
            int xx = PAD_L + (int)(i * (double) w / 256);
            g2.setColor(new Color(28, 32, 46));
            g2.drawLine(xx, PAD_T, xx, PAD_T + h);
        }
    }

    private void paintChannelPolygon(Graphics2D g2, int c, int w, int h,
                                     double binW, double maxVal) {
        int n = 256;
        int[] xs = new int[n + 2];
        int[] ys = new int[n + 2];

        for (int i = 0; i < n; i++) {
            double raw = histogram[c][i];
            double val = logScale ? (raw > 0 ? Math.log1p(raw) : 0) : raw;
            int barH   = (int)(val / maxVal * h);
            xs[i] = PAD_L + (int)(i * binW);
            ys[i] = PAD_T + h - barH;
        }
        xs[n]   = PAD_L + w; ys[n]   = PAD_T + h;
        xs[n+1] = PAD_L;     ys[n+1] = PAD_T + h;

        Color base = CHANNEL_COLORS[c];
        GradientPaint gp = new GradientPaint(
                0, PAD_T,     new Color(base.getRed(), base.getGreen(), base.getBlue(), 70),
                0, PAD_T + h, new Color(base.getRed(), base.getGreen(), base.getBlue(), 10)
        );
        g2.setPaint(gp);
        g2.fillPolygon(xs, ys, n + 2);

        g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 210));
        g2.setStroke(new BasicStroke(1.1f));
        for (int i = 0; i < n - 1; i++) g2.drawLine(xs[i], ys[i], xs[i+1], ys[i+1]);
    }

    private void paintAxes(Graphics2D g2, int w, int h) {
        g2.setColor(BORDER_LIGHT);
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(PAD_L, PAD_T, PAD_L, PAD_T + h);
        g2.drawLine(PAD_L, PAD_T + h, PAD_L + w, PAD_T + h);

        g2.setFont(AppTheme.monoPlain(9));
        double binW = (double) w / 256;
        for (int i = 0; i <= 255; i += 64) {
            int x = PAD_L + (int)(i * binW);
            g2.setColor(new Color(45, 52, 74));
            g2.drawString(String.valueOf(i), x - (i > 0 ? 8 : 0), PAD_T + h + 13);
            g2.setColor(BORDER);
            g2.drawLine(x, PAD_T + h, x, PAD_T + h + 3);
        }
    }

    private void paintLegend(Graphics2D g2, int w) {
        int lx = PAD_L + w - 36, ly = PAD_T + 6;
        g2.setFont(AppTheme.monoBold(9));
        for (int c = 0; c < 4; c++) {
            Color col   = CHANNEL_COLORS[c];
            int alpha   = channelVisible[c] ? col.getAlpha() : 40;
            Color drawn = new Color(col.getRed(), col.getGreen(), col.getBlue(), alpha);

            g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 50));
            g2.fillRoundRect(lx, ly + c * 16, 10, 9, 3, 3);
            g2.setColor(drawn);
            g2.drawRoundRect(lx, ly + c * 16, 10, 9, 3, 3);
            g2.drawString(CHANNEL_NAMES[c], lx + 14, ly + c * 16 + 9);
        }
    }

    private void paintNoData(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(40, 46, 66));
        g2.setFont(AppTheme.sansPlain(12));
        String msg  = "Sin datos";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, PAD_L + (w - fm.stringWidth(msg)) / 2, PAD_T + h / 2 + 4);
    }
}