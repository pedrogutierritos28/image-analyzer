package IA;

import javax.swing.*;
import java.awt.*;

import static IA.AppTheme.*;
import static IA.AppTheme.ACCENT;

/**
 * Panel semitransparente con spinner animado que se superpone sobre el contenido
 * mientras se procesa una imagen.
 */
public class LoadingOverlay extends JPanel {

    private float      angle  = 0;
    private final Timer spinner;

    public LoadingOverlay() {
        setOpaque(false);
        spinner = new Timer(25, e -> { angle += 9; repaint(); });
        setVisible(false);
    }

    @Override
    public void setVisible(boolean v) {
        super.setVisible(v);
        if (v) spinner.start();
        else   spinner.stop();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fondo difuminado
        g2.setColor(new Color(8, 9, 12, 195));
        g2.fillRect(0, 0, getWidth(), getHeight());

        int cx = getWidth() / 2, cy = getHeight() / 2, r = 22;
        g2.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        for (int i = 0; i < 10; i++) {
            float alpha = (float)(i + 1) / 10f;
            g2.setColor(new Color(
                    ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(),
                    (int)(alpha * 230)
            ));
            double a  = Math.toRadians(angle + i * 36);
            int x1 = cx + (int)((r - 9) * Math.cos(a));
            int y1 = cy + (int)((r - 9) * Math.sin(a));
            int x2 = cx + (int)( r      * Math.cos(a));
            int y2 = cy + (int)( r      * Math.sin(a));
            g2.drawLine(x1, y1, x2, y2);
        }

        g2.setColor(TEXT_MUTED);
        g2.setFont(AppTheme.sansPlain(11));
        String msg  = "Procesando…";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, cx - fm.stringWidth(msg) / 2, cy + 48);
    }
}