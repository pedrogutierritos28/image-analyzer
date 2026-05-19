package IA;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * ImageAnalyzer — V1 + V2
 *
 * V1: Carga y muestra una imagen en pantalla.
 * V2: Histograma RGB con bandas de distribución por canal.
 *
 * Compilar: javac ImageAnalyzer.java
 * Ejecutar:  java ImageAnalyzer
 */
public class ImageAnalyzer extends JFrame {

    // ── Estado ──────────────────────────────────────────────
    private BufferedImage currentImage;

    // ── Paneles principales ─────────────────────────────────
    private ImagePanel    imagePanel;
    private HistogramPanel histogramPanel;

    // ── Barra de estado ─────────────────────────────────────
    private JLabel statusLabel;

    // ════════════════════════════════════════════════════════
    //  Constructor / Setup
    // ════════════════════════════════════════════════════════
    public ImageAnalyzer() {
        super("Image Analyzer — V1 + V2");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 650);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(800, 500));

        buildMenu();
        buildUI();
        setVisible(true);
    }

    // ════════════════════════════════════════════════════════
    //  Menú
    // ════════════════════════════════════════════════════════
    private void buildMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("Archivo");
        fileMenu.setMnemonic(KeyEvent.VK_A);

        JMenuItem openItem = new JMenuItem("Abrir imagen…", KeyEvent.VK_O);
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        openItem.addActionListener(e -> loadImage());

        JMenuItem exitItem = new JMenuItem("Salir", KeyEvent.VK_S);
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(openItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        JMenu viewMenu = new JMenu("Vista");
        JMenuItem clearItem = new JMenuItem("Limpiar");
        clearItem.addActionListener(e -> clearAll());
        viewMenu.add(clearItem);

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        setJMenuBar(menuBar);
    }

    // ════════════════════════════════════════════════════════
    //  UI principal
    // ════════════════════════════════════════════════════════
    private void buildUI() {
        // ── Toolbar ─────────────────────────────────────────
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton btnOpen = new JButton("📂 Abrir imagen");
        btnOpen.addActionListener(e -> loadImage());
        toolBar.add(btnOpen);
        toolBar.addSeparator();

        JButton btnClear = new JButton("🗑 Limpiar");
        btnClear.addActionListener(e -> clearAll());
        toolBar.add(btnClear);

        add(toolBar, BorderLayout.NORTH);

        // ── Split: imagen | histograma ───────────────────────
        imagePanel     = new ImagePanel();
        histogramPanel = new HistogramPanel();

        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                wrapWithTitle(imagePanel,     "Imagen"),
                wrapWithTitle(histogramPanel, "Histograma RGB")
        );
        split.setResizeWeight(0.55);
        split.setDividerSize(6);
        add(split, BorderLayout.CENTER);

        // ── Barra de estado ──────────────────────────────────
        statusLabel = new JLabel("  Ninguna imagen cargada.");
        statusLabel.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                new EmptyBorder(3, 8, 3, 8)
        ));
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JPanel wrapWithTitle(JComponent comp, String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180)),
                " " + title + " ",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));
        p.add(new JScrollPane(comp), BorderLayout.CENTER);
        return p;
    }

    // ════════════════════════════════════════════════════════
    //  Lógica: cargar imagen
    // ════════════════════════════════════════════════════════
    private void loadImage() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Seleccionar imagen");
        fc.setFileFilter(new FileNameExtensionFilter(
                "Imágenes (JPG, PNG, BMP, GIF)", "jpg", "jpeg", "png", "bmp", "gif"));

        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = fc.getSelectedFile();
        try {
            currentImage = ImageIO.read(file);
            if (currentImage == null) {
                JOptionPane.showMessageDialog(this,
                        "No se pudo leer la imagen.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // V1 — mostrar imagen
            imagePanel.setImage(currentImage);

            // V2 — calcular y mostrar histograma RGB
            int[][] histData = computeHistogram(currentImage);
            histogramPanel.setHistogram(histData);

            statusLabel.setText(String.format(
                    "  %s   |   %d × %d px",
                    file.getName(), currentImage.getWidth(), currentImage.getHeight()));

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al abrir la imagen:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearAll() {
        currentImage = null;
        imagePanel.setImage(null);
        histogramPanel.setHistogram(null);
        statusLabel.setText("  Ninguna imagen cargada.");
    }

    // ════════════════════════════════════════════════════════
    //  V2 — Cálculo del histograma (256 niveles × 3 canales)
    // ════════════════════════════════════════════════════════
    /**
     * @return int[3][256]  → [0]=R  [1]=G  [2]=B
     */
    private int[][] computeHistogram(BufferedImage img) {
        int[][] hist = new int[3][256];
        int w = img.getWidth(), h = img.getHeight();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                hist[0][(rgb >> 16) & 0xFF]++;   // R
                hist[1][(rgb >>  8) & 0xFF]++;   // G
                hist[2][ rgb        & 0xFF]++;   // B
            }
        }
        return hist;
    }

    // ════════════════════════════════════════════════════════
    //  Panel V1 — visualización de imagen
    // ════════════════════════════════════════════════════════
    static class ImagePanel extends JPanel {
        private BufferedImage image;

        ImagePanel() {
            setBackground(new Color(30, 30, 30));
            setPreferredSize(new Dimension(500, 500));
        }

        void setImage(BufferedImage img) {
            this.image = img;
            if (img != null)
                setPreferredSize(new Dimension(img.getWidth(), img.getHeight()));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image == null) {
                // Placeholder
                g.setColor(new Color(80, 80, 80));
                g.setFont(new Font("SansSerif", Font.ITALIC, 14));
                String msg = "Ninguna imagen cargada";
                FontMetrics fm = g.getFontMetrics();
                g.drawString(msg,
                        (getWidth()  - fm.stringWidth(msg)) / 2,
                        (getHeight() + fm.getAscent())       / 2);
                return;
            }
            // Escalar manteniendo proporción
            double scale = Math.min(
                    (double) getWidth()  / image.getWidth(),
                    (double) getHeight() / image.getHeight()
            );
            int dw = (int)(image.getWidth()  * scale);
            int dh = (int)(image.getHeight() * scale);
            int dx = (getWidth()  - dw) / 2;
            int dy = (getHeight() - dh) / 2;

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(image, dx, dy, dw, dh, null);
        }
    }

    // ════════════════════════════════════════════════════════
    //  Panel V2 — histograma RGB (bandas superpuestas)
    // ════════════════════════════════════════════════════════
    static class HistogramPanel extends JPanel {
        private int[][] histogram; // [3][256]

        private static final Color COLOR_R = new Color(220, 50,  50,  160);
        private static final Color COLOR_G = new Color(50,  200, 80,  160);
        private static final Color COLOR_B = new Color(60,  120, 230, 160);

        private static final int PAD_L = 50, PAD_R = 20, PAD_T = 20, PAD_B = 40;

        HistogramPanel() {
            setBackground(new Color(22, 22, 28));
            setPreferredSize(new Dimension(450, 400));
        }

        void setHistogram(int[][] hist) {
            this.histogram = hist;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth()  - PAD_L - PAD_R;
            int h = getHeight() - PAD_T  - PAD_B;

            // ── Ejes ────────────────────────────────────────
            g2.setColor(new Color(80, 80, 100));
            g2.drawLine(PAD_L, PAD_T, PAD_L, PAD_T + h);          // Y
            g2.drawLine(PAD_L, PAD_T + h, PAD_L + w, PAD_T + h);  // X

            if (histogram == null) {
                g2.setColor(new Color(100, 100, 120));
                g2.setFont(new Font("SansSerif", Font.ITALIC, 13));
                g2.drawString("Sin datos", PAD_L + w/2 - 30, PAD_T + h/2);
                return;
            }

            // ── Máximo global ────────────────────────────────
            int maxVal = 1;
            for (int c = 0; c < 3; c++)
                for (int i = 0; i < 256; i++)
                    if (histogram[c][i] > maxVal) maxVal = histogram[c][i];

            // ── Líneas de referencia ─────────────────────────
            g2.setColor(new Color(50, 50, 65));
            for (int pct = 25; pct < 100; pct += 25) {
                int yy = PAD_T + h - (int)((pct / 100.0) * h);
                g2.drawLine(PAD_L, yy, PAD_L + w, yy);
            }

            // ── Bandas RGB ───────────────────────────────────
            Color[] colors = { COLOR_R, COLOR_G, COLOR_B };
            double binW = (double) w / 256;

            for (int c = 0; c < 3; c++) {
                g2.setColor(colors[c]);
                for (int i = 0; i < 256; i++) {
                    int barH = (int)((double) histogram[c][i] / maxVal * h);
                    int x    = PAD_L + (int)(i * binW);
                    int bw   = Math.max(1, (int) binW);
                    g2.fillRect(x, PAD_T + h - barH, bw, barH);
                }
            }

            // ── Etiquetas eje X ──────────────────────────────
            g2.setColor(new Color(160, 160, 180));
            g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
            for (int i = 0; i <= 255; i += 64) {
                int x = PAD_L + (int)(i * binW);
                g2.drawString(String.valueOf(i),
                        x - (i == 255 ? 14 : 6),
                        PAD_T + h + 16);
                g2.drawLine(x, PAD_T + h, x, PAD_T + h + 4);
            }

            // ── Leyenda ──────────────────────────────────────
            int lx = PAD_L + w - 90, ly = PAD_T + 12;
            String[] labels = { "Rojo", "Verde", "Azul" };
            for (int c = 0; c < 3; c++) {
                g2.setColor(colors[c]);
                g2.fillRect(lx, ly + c * 18, 12, 10);
                g2.setColor(new Color(200, 200, 210));
                g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
                g2.drawString(labels[c], lx + 16, ly + c * 18 + 9);
            }
        }
    }

    // ════════════════════════════════════════════════════════
    //  Main
    // ════════════════════════════════════════════════════════
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(ImageAnalyzer::new);
    }
}