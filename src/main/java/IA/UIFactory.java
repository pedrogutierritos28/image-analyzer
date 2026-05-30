package IA;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

import static IA.AppTheme.*;

/**
 * Fábrica de componentes Swing reutilizables con el estilo de la aplicación.
 * Todos los métodos son estáticos.
 */
public final class UIFactory {

    private UIFactory() {}

    // ── Botones ──────────────────────────────────────────────

    /** Botón primario con fondo de acento y efecto glow en hover. */
    public static JButton accentButton(String text) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(ACCENT_GLOW);
                    g2.fillRoundRect(-2, -2, getWidth() + 4, getHeight() + 4, 10, 10);
                }
                Color base = getModel().isPressed()  ? ACCENT.darker()
                        : getModel().isRollover() ? new Color(110, 130, 255)
                          : ACCENT;
                g2.setColor(base);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setForeground(Color.WHITE);
        b.setFont(sansBold(12));
        b.setBorder(new EmptyBorder(6, 16, 6, 16));
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** Botón secundario con borde sutil y sin relleno. */
    public static JButton ghostButton(String text) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? BG_HOVER : new Color(0, 0, 0, 0));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(getModel().isRollover() ? BORDER_LIGHT : BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setForeground(TEXT_MUTED);
        b.setFont(sansPlain(12));
        b.setBorder(new EmptyBorder(6, 12, 6, 12));
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ── Controles de barra ───────────────────────────────────

    /** Checkbox con color personalizado para selección de canal. */
    public static JCheckBox channelCheckbox(String text, Color color) {
        JCheckBox cb = new JCheckBox(text);
        cb.setForeground(color);
        cb.setBackground(BG_TOOLBAR);
        cb.setFont(monoBold(11));
        cb.setFocusPainted(false);
        cb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return cb;
    }

    /** Separador vertical para toolbars. */
    public static JSeparator vSeparator() {
        JSeparator s = new JSeparator(JSeparator.VERTICAL);
        s.setPreferredSize(new Dimension(1, 18));
        s.setForeground(BORDER);
        return s;
    }

    /** Etiqueta de sección en mayúsculas para la toolbar. */
    public static JLabel dimLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(45, 50, 70));
        l.setFont(sansBold(9));
        l.setBorder(new EmptyBorder(0, 4, 0, 2));
        return l;
    }

    // ── Card container ───────────────────────────────────────

    /**
     * Envuelve un componente en una tarjeta con borde y header opcional.
     *
     * @param content componente a envolver
     * @param title   texto del header (puede ser null para omitirlo)
     */
    public static JPanel card(JComponent content, String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_CARD);
        p.setBorder(new LineBorder(BORDER, 1, true));

        if (title != null) {
            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(new Color(14, 16, 22));
            header.setBorder(new CompoundBorder(
                    new MatteBorder(0, 0, 1, 0, BORDER),
                    new EmptyBorder(7, 10, 7, 10)
            ));

            JLabel lbl = new JLabel(title.toUpperCase());
            lbl.setForeground(new Color(70, 78, 108));
            lbl.setFont(sansBold(9));

            JPanel accentLine = new JPanel();
            accentLine.setBackground(ACCENT);
            accentLine.setPreferredSize(new Dimension(2, 14));
            accentLine.setOpaque(true);

            JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            titleRow.setOpaque(false);
            titleRow.add(accentLine);
            titleRow.add(lbl);

            header.add(titleRow, BorderLayout.WEST);
            p.add(header, BorderLayout.NORTH);
        }
        p.add(content, BorderLayout.CENTER);
        return p;
    }

    // ── Menú ─────────────────────────────────────────────────

    public static JMenu styledMenu(String text) {
        JMenu m = new JMenu(text);
        m.setForeground(TEXT_PRIMARY);
        m.setFont(sansPlain(13));
        return m;
    }

    public static JMenuItem styledItem(String text) {
        JMenuItem i = new JMenuItem(text);
        i.setBackground(BG_PANEL);
        i.setForeground(TEXT_PRIMARY);
        i.setFont(sansPlain(13));
        return i;
    }

    public static JMenuItem styledItem(String text, int key, int mod) {
        JMenuItem i = styledItem(text);
        i.setAccelerator(KeyStroke.getKeyStroke(key, mod));
        return i;
    }
}