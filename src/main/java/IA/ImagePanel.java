package IA;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

import static IA.AppTheme.*;

/**
 * Panel que muestra una imagen escalada centrada con sombra.
 * Gestiona su propia caché de escalado internamente.
 */
public class ImagePanel extends JPanel {

    private static final String PLACEHOLDER  = "Arrastra una imagen o usa Abrir";
    private static final String PLACEHOLDER2 = "JPG · PNG · BMP · GIF · TIFF";

    private BufferedImage image;
    private BufferedImage scaledCache;
    private Dimension     scaledCacheSize;

    public ImagePanel() {
        setBackground(BG_CARD);
        setPreferredSize(new Dimension(500, 500));
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                if (image != null) { scaledCache = null; repaint(); }
            }
        });
    }

    public void setImage(BufferedImage img) {
        this.image       = img;
        this.scaledCache = null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);

        if (image == null) { paintPlaceholder(g2); return; }

        Dimension current = getSize();
        if (scaledCache == null || !current.equals(scaledCacheSize)) {
            scaledCache     = buildScaledImage(image, current);
            scaledCacheSize = new Dimension(current);
        }

        int dx = (getWidth()  - scaledCache.getWidth())  / 2;
        int dy = (getHeight() - scaledCache.getHeight()) / 2;

        // Sombra multi-capa
        for (int i = 4; i >= 1; i--) {
            g2.setColor(new Color(0, 0, 0, 18 * i));
            g2.fillRoundRect(dx + i * 2, dy + i * 2,
                    scaledCache.getWidth(), scaledCache.getHeight(), 4, 4);
        }
        g2.drawImage(scaledCache, dx, dy, null);
    }

    // ── Privados ─────────────────────────────────────────────

    private void paintPlaceholder(Graphics2D g2) {
        int cx = getWidth() / 2, cy = getHeight() / 2;

        Paint radial = new RadialGradientPaint(
                cx, cy, Math.min(getWidth(), getHeight()) * 0.4f,
                new float[]{0f, 1f},
                new Color[]{new Color(20, 24, 36), BG_CARD}
        );
        g2.setPaint(radial);
        g2.fillRect(0, 0, getWidth(), getHeight());

        int bx = cx - 26, by = cy - 40, bw = 52, bh = 48;
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(45, 52, 74));
        g2.drawRoundRect(bx, by, bw, bh, 8, 8);

        g2.setColor(new Color(60, 68, 96));
        g2.drawLine(cx,      by + 12, cx,      by + 36);
        g2.drawLine(cx - 10, by + 20, cx,      by + 10);
        g2.drawLine(cx + 10, by + 20, cx,      by + 10);
        g2.drawLine(bx + 8,  by + 42, bx + bw - 8, by + 42);

        g2.setFont(AppTheme.sansPlain(13));
        g2.setColor(new Color(65, 72, 100));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(PLACEHOLDER, cx - fm.stringWidth(PLACEHOLDER) / 2, cy + 38);

        g2.setFont(AppTheme.sansPlain(10));
        g2.setColor(new Color(40, 46, 66));
        FontMetrics fm2 = g2.getFontMetrics();
        g2.drawString(PLACEHOLDER2, cx - fm2.stringWidth(PLACEHOLDER2) / 2, cy + 56);
    }

    private BufferedImage buildScaledImage(BufferedImage src, Dimension container) {
        double sx    = (container.width  - 40.0) / src.getWidth();
        double sy    = (container.height - 40.0) / src.getHeight();
        double scale = Math.min(Math.min(sx, sy), 1.0);
        int tw = Math.max(1, (int)(src.getWidth()  * scale));
        int th = Math.max(1, (int)(src.getHeight() * scale));

        BufferedImage out = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
        g2.drawImage(src, 0, 0, tw, th, null);
        g2.dispose();
        return out;
    }
}