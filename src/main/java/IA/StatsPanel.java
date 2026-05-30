package IA;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

import static IA.AppTheme.*;

/**
 * Panel que muestra las estadísticas por canal (media, desviación estándar, mediana).
 */
public class StatsPanel extends JPanel {

    private static final String[] CH_LABELS = {"R", "G", "B"};
    private static final Color[]  CH_COLORS = {CH_R, CH_G, CH_B};

    private double[][] stats;

    public StatsPanel() {
        setBackground(BG_CARD);
        setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(12, 14, 12, 14)
        ));
        setPreferredSize(new Dimension(0, 118));
    }

    /** @param s array [3][3]: [canal R/G/B][media, desv, mediana] — null para limpiar */
    public void setStats(double[][] s) { this.stats = s; repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Header
        g2.setColor(new Color(50, 56, 80));
        g2.setFont(AppTheme.sansBold(8));
        g2.drawString("ESTADÍSTICAS  POR  CANAL", 0, 12);

        g2.setColor(BORDER);
        g2.drawLine(0, 18, getWidth() - 28, 18);

        if (stats == null) {
            g2.setColor(new Color(40, 46, 66));
            g2.setFont(AppTheme.sansPlain(11));
            g2.drawString("Carga una imagen para ver estadísticas", 0, 46);
            return;
        }

        int colW = (getWidth() - 28) / 3;
        String[] rowLabels = {"Media", "Desv.", "Mediana"};

        for (int c = 0; c < 3; c++) {
            int cx = c * colW;

            // Fondo sutil por canal
            Color cc = CH_COLORS[c];
            g2.setColor(new Color(cc.getRed(), cc.getGreen(), cc.getBlue(), 12));
            g2.fillRoundRect(cx, 24, colW - 4, 82, 4, 4);

            // Barra de acento + nombre
            g2.setColor(cc);
            g2.fillRoundRect(cx, 26, 3, 16, 2, 2);
            g2.setFont(AppTheme.monoBold(13));
            g2.drawString(CH_LABELS[c], cx + 8, 39);

            // Filas de datos
            for (int r = 0; r < 3; r++) {
                g2.setFont(AppTheme.monoPlain(10));
                g2.setColor(new Color(60, 68, 95));
                g2.drawString(rowLabels[r], cx + 6, 58 + r * 17);

                g2.setFont(AppTheme.monoBold(10));
                g2.setColor(TEXT_PRIMARY);
                g2.drawString(String.format("%.1f", stats[c][r]), cx + 50, 58 + r * 17);
            }
        }
    }
}