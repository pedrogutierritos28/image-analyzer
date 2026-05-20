package IA;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;

public class ImageAnalyzer extends JFrame {

    // ── Paleta de colores ────────────────────────────────────
    private static final Color BG_DEEP      = new Color(10,  11,  14);
    private static final Color BG_PANEL     = new Color(16,  18,  22);
    private static final Color BG_CARD      = new Color(22,  25,  32);
    private static final Color BG_HOVER     = new Color(30,  34,  44);
    private static final Color ACCENT       = new Color(99,  120, 255);
    private static final Color ACCENT_DIM   = new Color(99,  120, 255, 60);
    private static final Color BORDER       = new Color(40,  44,  58);
    private static final Color TEXT_PRIMARY = new Color(225, 228, 240);
    private static final Color TEXT_MUTED   = new Color(110, 118, 148);
    private static final Color TEXT_DIM     = new Color(60,  66,  90);

    private static final Color CH_R = new Color(255, 85,  100, 180);
    private static final Color CH_G = new Color(50,  210, 120, 180);
    private static final Color CH_B = new Color(80,  140, 255, 180);
    private static final Color CH_L = new Color(200, 200, 210, 130);

    // ── Estado ────────────────────────────────────────────────
    private BufferedImage currentImage;
    private BufferedImage scaledCache;
    private Dimension    scaledCacheSize;
    private int[][]      histogramData;   // [4][256] R,G,B,L
    private boolean      isLoading = false;

    // ── Paneles ───────────────────────────────────────────────
    private ImagePanel     imagePanel;
    private HistogramPanel histogramPanel;
    private StatsPanel     statsPanel;

    // ── UI extras ─────────────────────────────────────────────
    private JLabel   statusLabel;
    private JLabel   filenameLabel;
    private JLabel   dimensionsLabel;
    private LoadingOverlay loadingOverlay;
    private boolean[] channelVisible = {true, true, true, true}; // R,G,B,L
    private boolean   logScale = false;

    // ════════════════════════════════════════════════════════
    //  Constructor
    // ════════════════════════════════════════════════════════
    public ImageAnalyzer() {
        super("Image Analyzer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 720);
        setMinimumSize(new Dimension(900, 560));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DEEP);

        buildMenu();
        buildUI();
        setupDragDrop();
        setVisible(true);
    }

    // ════════════════════════════════════════════════════════
    //  Menú
    // ════════════════════════════════════════════════════════
    private void buildMenu() {
        JMenuBar bar = new JMenuBar();
        bar.setBackground(BG_PANEL);
        bar.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));

        JMenu file = styledMenu("Archivo");
        JMenuItem open = styledItem("Abrir imagen…", KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK);
        open.addActionListener(e -> openFileDialog());
        JMenuItem exit = styledItem("Salir");
        exit.addActionListener(e -> System.exit(0));
        file.add(open); file.addSeparator(); file.add(exit);

        JMenu view = styledMenu("Vista");
        JMenuItem clear = styledItem("Limpiar");
        clear.addActionListener(e -> clearAll());
        view.add(clear);

        bar.add(file);
        bar.add(view);
        setJMenuBar(bar);
    }

    private JMenu styledMenu(String text) {
        JMenu m = new JMenu(text);
        m.setForeground(TEXT_PRIMARY);
        m.setFont(new Font("SansSerif", Font.PLAIN, 13));
        return m;
    }

    private JMenuItem styledItem(String text) {
        JMenuItem i = new JMenuItem(text);
        i.setBackground(BG_PANEL);
        i.setForeground(TEXT_PRIMARY);
        i.setFont(new Font("SansSerif", Font.PLAIN, 13));
        return i;
    }

    private JMenuItem styledItem(String text, int key, int mod) {
        JMenuItem i = styledItem(text);
        i.setAccelerator(KeyStroke.getKeyStroke(key, mod));
        return i;
    }

    // ════════════════════════════════════════════════════════
    //  UI principal
    // ════════════════════════════════════════════════════════
    private void buildUI() {
        setLayout(new BorderLayout());

        // ── Toolbar ─────────────────────────────────────────
        add(buildToolbar(), BorderLayout.NORTH);

        // ── Contenido central ────────────────────────────────
        imagePanel     = new ImagePanel();
        histogramPanel = new HistogramPanel();
        statsPanel     = new StatsPanel();
        loadingOverlay = new LoadingOverlay();

        // Panel derecho: histograma + stats
        JPanel rightPanel = new JPanel(new BorderLayout(0, 8));
        rightPanel.setBackground(BG_DEEP);
        rightPanel.setBorder(new EmptyBorder(10, 0, 10, 10));
        rightPanel.add(card(histogramPanel, "Histograma RGB"), BorderLayout.CENTER);
        rightPanel.add(statsPanel, BorderLayout.SOUTH);

        // Panel izquierdo: imagen
        JLayeredPane layered = new JLayeredPane();
        layered.setLayout(new OverlayLayout(layered));
        JPanel imgWrapper = card(imagePanel, null);
        imgWrapper.setBorder(new EmptyBorder(10, 10, 10, 0));
        layered.add(imgWrapper, JLayeredPane.DEFAULT_LAYER);
        layered.add(loadingOverlay, JLayeredPane.PALETTE_LAYER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, layered, rightPanel);
        split.setResizeWeight(0.58);
        split.setDividerSize(4);
        split.setBackground(BG_DEEP);
        split.setBorder(null);
        if (split.getUI() instanceof javax.swing.plaf.basic.BasicSplitPaneUI bspUI) {
            bspUI.getDivider().setBackground(BORDER);
        }
        add(split, BorderLayout.CENTER);

        // ── Barra de estado ──────────────────────────────────
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        bar.setBackground(BG_PANEL);
        bar.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(8, 10, 8, 10)
        ));

        // Botón abrir
        JButton btnOpen = accentButton("Abrir imagen");
        btnOpen.addActionListener(e -> openFileDialog());
        bar.add(btnOpen);

        bar.add(separator());

        // Checkboxes de canal
        bar.add(label("Canales:"));
        String[] labels = {"R", "G", "B", "L"};
        Color[] colors = {CH_R, CH_G, CH_B, CH_L};
        for (int c = 0; c < 4; c++) {
            final int idx = c;
            JCheckBox cb = channelCheckbox(labels[c], colors[c]);
            cb.setSelected(true);
            cb.addActionListener(e -> {
                channelVisible[idx] = cb.isSelected();
                histogramPanel.repaint();
            });
            bar.add(cb);
        }

        bar.add(separator());

        // Toggle escala log
        JCheckBox logCb = channelCheckbox("Log", TEXT_MUTED);
        logCb.setToolTipText("Escala logarítmica");
        logCb.addActionListener(e -> {
            logScale = logCb.isSelected();
            histogramPanel.repaint();
        });
        bar.add(logCb);

        bar.add(separator());

        JButton btnClear = ghostButton("Limpiar");
        btnClear.addActionListener(e -> clearAll());
        bar.add(btnClear);

        return bar;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_PANEL);
        bar.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, BORDER),
                new EmptyBorder(5, 12, 5, 12)
        ));

        filenameLabel = new JLabel("Ningún archivo");
        filenameLabel.setForeground(TEXT_MUTED);
        filenameLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));

        dimensionsLabel = new JLabel("");
        dimensionsLabel.setForeground(TEXT_DIM);
        dimensionsLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));

        statusLabel = new JLabel("Listo  •  Arrastra una imagen aquí");
        statusLabel.setForeground(TEXT_DIM);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        left.add(filenameLabel);
        left.add(dimensionsLabel);

        bar.add(left, BorderLayout.WEST);
        bar.add(statusLabel, BorderLayout.EAST);
        return bar;
    }

    private JPanel card(JComponent content, String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_CARD);
        p.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(title != null ? 0 : 0, 0, 0, 0)
        ));
        if (title != null) {
            JLabel header = new JLabel("  " + title);
            header.setForeground(TEXT_MUTED);
            header.setFont(new Font("SansSerif", Font.PLAIN, 11));
            header.setBorder(new CompoundBorder(
                    new MatteBorder(0, 0, 1, 0, BORDER),
                    new EmptyBorder(6, 8, 6, 8)
            ));
            header.setBackground(new Color(18, 20, 28));
            header.setOpaque(true);
            p.add(header, BorderLayout.NORTH);
        }
        p.add(content, BorderLayout.CENTER);
        return p;
    }

    // ── Helpers de UI ────────────────────────────────────────
    private JButton accentButton(String text) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) g2.setColor(ACCENT.darker());
                else if (getModel().isRollover()) g2.setColor(ACCENT.brighter());
                else g2.setColor(ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setBorder(new EmptyBorder(6, 14, 6, 14));
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton ghostButton(String text) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) g2.setColor(BG_HOVER);
                else g2.setColor(new Color(0,0,0,0));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setForeground(TEXT_MUTED);
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));
        b.setBorder(new EmptyBorder(6, 12, 6, 12));
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JCheckBox channelCheckbox(String text, Color color) {
        JCheckBox cb = new JCheckBox(text);
        cb.setForeground(color);
        cb.setBackground(BG_PANEL);
        cb.setFont(new Font("Monospaced", Font.BOLD, 12));
        cb.setFocusPainted(false);
        cb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return cb;
    }

    private JSeparator separator() {
        JSeparator s = new JSeparator(JSeparator.VERTICAL);
        s.setPreferredSize(new Dimension(1, 20));
        s.setForeground(BORDER);
        return s;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_DIM);
        l.setFont(new Font("SansSerif", Font.PLAIN, 11));
        return l;
    }

    // ════════════════════════════════════════════════════════
    //  Drag & Drop
    // ════════════════════════════════════════════════════════
    private void setupDragDrop() {
        new DropTarget(this, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent ev) {
                try {
                    ev.acceptDrop(DnDConstants.ACTION_COPY);
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) ev.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) loadImageAsync(files.get(0));
                } catch (Exception ex) { /* ignorar */ }
            }
        });
    }

    // ════════════════════════════════════════════════════════
    //  Cargar imagen — SwingWorker (no bloquea EDT)
    // ════════════════════════════════════════════════════════
    private void openFileDialog() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Seleccionar imagen");
        fc.setFileFilter(new FileNameExtensionFilter(
                "Imágenes (JPG, PNG, BMP, GIF, TIFF)", "jpg","jpeg","png","bmp","gif","tiff","tif"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            loadImageAsync(fc.getSelectedFile());
    }

    private void loadImageAsync(File file) {
        if (isLoading) return;
        isLoading = true;
        loadingOverlay.setVisible(true);
        statusLabel.setText("Cargando…");

        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                BufferedImage img = ImageIO.read(file);
                if (img == null) throw new Exception("Formato no soportado");
                int[][] hist = computeHistogram(img);
                double[][] stats = computeStats(hist, img.getWidth() * (long) img.getHeight());
                return new Object[]{img, hist, stats, file};
            }

            @Override
            protected void done() {
                loadingOverlay.setVisible(false);
                isLoading = false;
                try {
                    Object[] result = get();
                    currentImage  = (BufferedImage) result[0];
                    histogramData = (int[][])       result[1];
                    double[][] stats = (double[][]) result[2];
                    File f        = (File)          result[3];

                    scaledCache     = null; // invalidar caché
                    scaledCacheSize = null;

                    imagePanel.setImage(currentImage);
                    histogramPanel.setHistogram(histogramData);
                    statsPanel.setStats(stats);

                    filenameLabel.setText(f.getName());
                    dimensionsLabel.setText(
                            "  " + currentImage.getWidth() + " × " + currentImage.getHeight() + " px");
                    statusLabel.setText("Listo");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ImageAnalyzer.this,
                            "Error al abrir la imagen:\n" + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    statusLabel.setText("Error al cargar");
                }
            }
        }.execute();
    }

    private void clearAll() {
        currentImage  = null;
        scaledCache   = null;
        histogramData = null;
        imagePanel.setImage(null);
        histogramPanel.setHistogram(null);
        statsPanel.setStats(null);
        filenameLabel.setText("Ningún archivo");
        dimensionsLabel.setText("");
        statusLabel.setText("Listo  •  Arrastra una imagen aquí");
    }

    // ════════════════════════════════════════════════════════
    //  Cálculo histograma [4][256]: R, G, B, Luminosidad
    // ════════════════════════════════════════════════════════
    private int[][] computeHistogram(BufferedImage img) {
        int[][] hist = new int[4][256];
        int w = img.getWidth(), h = img.getHeight();
        int[] row = new int[w];

        for (int y = 0; y < h; y++) {
            img.getRGB(0, y, w, 1, row, 0, w); // lectura fila completa — más rápido
            for (int rgb : row) {
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >>  8) & 0xFF;
                int b =  rgb        & 0xFF;
                int lum = (int)(0.2126 * r + 0.7152 * g + 0.0722 * b);
                hist[0][r]++;
                hist[1][g]++;
                hist[2][b]++;
                hist[3][Math.min(lum, 255)]++;
            }
        }
        return hist;
    }

    // Devuelve [canal][0=media, 1=stddev, 2=mediana]
    private double[][] computeStats(int[][] hist, long totalPixels) {
        double[][] stats = new double[3][3];
        for (int c = 0; c < 3; c++) {
            double sum = 0;
            for (int i = 0; i < 256; i++) sum += (double) i * hist[c][i];
            double mean = sum / totalPixels;

            double varSum = 0;
            for (int i = 0; i < 256; i++) varSum += hist[c][i] * Math.pow(i - mean, 2);
            double std = Math.sqrt(varSum / totalPixels);

            long half = totalPixels / 2, acc = 0;
            int median = 0;
            for (int i = 0; i < 256; i++) { acc += hist[c][i]; if (acc >= half) { median = i; break; } }

            stats[c][0] = mean;
            stats[c][1] = std;
            stats[c][2] = median;
        }
        return stats;
    }

    // ════════════════════════════════════════════════════════
    //  Panel imagen — con caché de escala
    // ════════════════════════════════════════════════════════
    class ImagePanel extends JPanel {
        private BufferedImage image;
        private static final String PLACEHOLDER = "Arrastra una imagen o usa Abrir";

        ImagePanel() {
            setBackground(BG_CARD);
            setPreferredSize(new Dimension(500, 500));
            addComponentListener(new ComponentAdapter() {
                @Override public void componentResized(ComponentEvent e) {
                    if (image != null) {
                        scaledCache = null; // invalidar al redimensionar
                        repaint();
                    }
                }
            });
        }

        void setImage(BufferedImage img) {
            this.image  = img;
            scaledCache = null;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (image == null) {
                paintPlaceholder(g2);
                return;
            }

            // Recalcular escala solo si necesario
            Dimension current = getSize();
            if (scaledCache == null || !current.equals(scaledCacheSize)) {
                scaledCache     = buildScaledImage(image, current);
                scaledCacheSize = new Dimension(current);
            }

            int dx = (getWidth()  - scaledCache.getWidth())  / 2;
            int dy = (getHeight() - scaledCache.getHeight()) / 2;

            // Sombra sutil
            g2.setColor(new Color(0, 0, 0, 60));
            g2.fillRoundRect(dx + 4, dy + 4, scaledCache.getWidth(), scaledCache.getHeight(), 4, 4);

            g2.drawImage(scaledCache, dx, dy, null);
        }

        private void paintPlaceholder(Graphics2D g2) {
            int cx = getWidth() / 2, cy = getHeight() / 2;

            // Ícono de upload estilizado
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(TEXT_DIM);
            int bx = cx - 28, by = cy - 36, bw = 56, bh = 50;
            g2.drawRoundRect(bx, by, bw, bh, 8, 8);
            g2.drawLine(cx, by + 14, cx, by + 36);
            g2.drawLine(cx - 10, by + 22, cx, by + 12);
            g2.drawLine(cx + 10, by + 22, cx, by + 12);
            g2.drawLine(bx + 8, by + 44, bx + bw - 8, by + 44);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g2.setColor(TEXT_DIM);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(PLACEHOLDER, cx - fm.stringWidth(PLACEHOLDER) / 2, cy + 40);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.setColor(new Color(55, 60, 80));
            String sub = "JPG, PNG, BMP, GIF, TIFF";
            g2.drawString(sub, cx - fm.stringWidth(sub) / 2 + 4, cy + 58);
        }

        private BufferedImage buildScaledImage(BufferedImage src, Dimension container) {
            double sx = (container.width  - 40.0) / src.getWidth();
            double sy = (container.height - 40.0) / src.getHeight();
            double scale = Math.min(sx, sy);
            if (scale > 1) scale = 1; // no ampliar
            int tw = (int)(src.getWidth()  * scale);
            int th = (int)(src.getHeight() * scale);
            BufferedImage out = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = out.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(src, 0, 0, tw, th, null);
            g2.dispose();
            return out;
        }
    }

    // ════════════════════════════════════════════════════════
    //  Panel histograma
    // ════════════════════════════════════════════════════════
    class HistogramPanel extends JPanel {
        private int[][] histogram;

        private static final int PAD_L = 48, PAD_R = 16, PAD_T = 16, PAD_B = 32;
        private static final Color[] CHANNEL_COLORS = {CH_R, CH_G, CH_B, CH_L};
        private static final String[] CHANNEL_NAMES = {"R", "G", "B", "L"};

        HistogramPanel() {
            setBackground(BG_CARD);
            setPreferredSize(new Dimension(420, 260));
        }

        void setHistogram(int[][] hist) { this.histogram = hist; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth()  - PAD_L - PAD_R;
            int h = getHeight() - PAD_T  - PAD_B;

            paintGrid(g2, w, h);

            if (histogram == null) {
                g2.setColor(TEXT_DIM);
                g2.setFont(new Font("SansSerif", Font.ITALIC, 12));
                String msg = "Sin datos";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, PAD_L + (w - fm.stringWidth(msg))/2, PAD_T + h/2);
                paintAxes(g2, w, h);
                return;
            }

            // Máximo (con/sin log)
            double maxVal = 1;
            for (int c = 0; c < 4; c++) {
                if (!channelVisible[c]) continue;
                for (int i = 0; i < 256; i++) {
                    double v = logScale ? (histogram[c][i] > 0 ? Math.log1p(histogram[c][i]) : 0)
                            : histogram[c][i];
                    if (v > maxVal) maxVal = v;
                }
            }

            double binW = (double) w / 256;

            // Dibujar canales con polígono relleno
            for (int c = 3; c >= 0; c--) {
                if (!channelVisible[c]) continue;
                paintChannelPolygon(g2, c, w, h, binW, maxVal);
            }

            paintAxes(g2, w, h);
            paintLegend(g2, w);
        }

        private void paintGrid(Graphics2D g2, int w, int h) {
            g2.setColor(new Color(35, 40, 55));
            g2.setStroke(new BasicStroke(0.5f));
            for (int pct = 25; pct < 100; pct += 25) {
                int yy = PAD_T + h - (int)((pct / 100.0) * h);
                g2.drawLine(PAD_L, yy, PAD_L + w, yy);
            }
            for (int i = 64; i < 256; i += 64) {
                int xx = PAD_L + (int)(i * (double) w / 256);
                g2.drawLine(xx, PAD_T, xx, PAD_T + h);
            }
        }

        private void paintChannelPolygon(Graphics2D g2, int c, int w, int h, double binW, double maxVal) {
            int n = 256;
            int[] xs = new int[n + 2];
            int[] ys = new int[n + 2];

            for (int i = 0; i < n; i++) {
                double raw = histogram[c][i];
                double val = logScale ? (raw > 0 ? Math.log1p(raw) : 0) : raw;
                int barH = (int)(val / maxVal * h);
                xs[i] = PAD_L + (int)(i * binW);
                ys[i] = PAD_T + h - barH;
            }
            xs[n]   = PAD_L + w; ys[n]   = PAD_T + h;
            xs[n+1] = PAD_L;     ys[n+1] = PAD_T + h;

            Color base = CHANNEL_COLORS[c];
            g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 55));
            g2.fillPolygon(xs, ys, n + 2);

            g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 200));
            g2.setStroke(new BasicStroke(1.2f));
            for (int i = 0; i < n - 1; i++) {
                g2.drawLine(xs[i], ys[i], xs[i+1], ys[i+1]);
            }
        }

        private void paintAxes(Graphics2D g2, int w, int h) {
            g2.setColor(BORDER);
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(PAD_L, PAD_T, PAD_L, PAD_T + h);
            g2.drawLine(PAD_L, PAD_T + h, PAD_L + w, PAD_T + h);

            g2.setColor(TEXT_DIM);
            g2.setFont(new Font("Monospaced", Font.PLAIN, 9));
            double binW = (double) w / 256;
            for (int i = 0; i <= 255; i += 64) {
                int x = PAD_L + (int)(i * binW);
                g2.drawString(String.valueOf(i), x - (i > 0 ? 8 : 0), PAD_T + h + 14);
                g2.setColor(new Color(50, 55, 75));
                g2.drawLine(x, PAD_T + h, x, PAD_T + h + 3);
                g2.setColor(TEXT_DIM);
            }
        }

        private void paintLegend(Graphics2D g2, int w) {
            int lx = PAD_L + w - 100, ly = PAD_T + 6;
            g2.setFont(new Font("Monospaced", Font.BOLD, 10));
            for (int c = 0; c < 4; c++) {
                Color col = channelVisible[c] ? CHANNEL_COLORS[c]
                        : new Color(CHANNEL_COLORS[c].getRed(), CHANNEL_COLORS[c].getGreen(),
                        CHANNEL_COLORS[c].getBlue(), 60);
                g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 80));
                g2.fillRoundRect(lx, ly + c * 16, 10, 9, 2, 2);
                g2.setColor(col);
                g2.drawRoundRect(lx, ly + c * 16, 10, 9, 2, 2);
                g2.drawString(CHANNEL_NAMES[c], lx + 14, ly + c * 16 + 9);
            }
        }
    }

    // ════════════════════════════════════════════════════════
    //  Panel de estadísticas
    // ════════════════════════════════════════════════════════
    class StatsPanel extends JPanel {
        private double[][] stats; // [canal][media, std, mediana]
        private static final String[] CH_LABELS = {"R", "G", "B"};
        private static final Color[] CH_COLORS  = {CH_R, CH_G, CH_B};

        StatsPanel() {
            setBackground(BG_PANEL);
            setBorder(new CompoundBorder(
                    new LineBorder(BORDER, 1, true),
                    new EmptyBorder(10, 12, 10, 12)
            ));
            setPreferredSize(new Dimension(0, 110));
        }

        void setStats(double[][] s) { this.stats = s; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(TEXT_DIM);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g2.drawString("ESTADÍSTICAS", 0, 14);

            if (stats == null) {
                g2.setColor(new Color(50, 55, 75));
                g2.setFont(new Font("SansSerif", Font.ITALIC, 11));
                g2.drawString("Sin datos", 0, 40);
                return;
            }

            int colW = (getWidth() - 20) / 3;
            String[] rowLabels = {"Media", "Std", "Mediana"};

            for (int c = 0; c < 3; c++) {
                int cx = c * colW;
                // Header canal
                g2.setColor(CH_COLORS[c]);
                g2.setFont(new Font("Monospaced", Font.BOLD, 12));
                g2.drawString(CH_LABELS[c], cx, 34);

                g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
                for (int r = 0; r < 3; r++) {
                    g2.setColor(TEXT_DIM);
                    g2.drawString(rowLabels[r], cx, 50 + r * 17);
                    g2.setColor(TEXT_PRIMARY);
                    g2.drawString(String.format("%.1f", stats[c][r]), cx + 52, 50 + r * 17);
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════
    //  Overlay de carga
    // ════════════════════════════════════════════════════════
    class LoadingOverlay extends JPanel {
        private float angle = 0;
        private Timer spinner;

        LoadingOverlay() {
            setOpaque(false);
            spinner = new Timer(30, e -> { angle += 8; repaint(); });
            setVisible(false);
        }

        @Override
        public void setVisible(boolean v) {
            super.setVisible(v);
            if (v) spinner.start(); else spinner.stop();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(10, 11, 14, 180));
            g2.fillRect(0, 0, getWidth(), getHeight());

            int cx = getWidth() / 2, cy = getHeight() / 2, r = 20;
            g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            for (int i = 0; i < 8; i++) {
                float alpha = (float)(i + 1) / 8f;
                g2.setColor(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(),
                        (int)(alpha * 220)));
                double a = Math.toRadians(angle + i * 45);
                int x1 = cx + (int)((r - 8) * Math.cos(a));
                int y1 = cy + (int)((r - 8) * Math.sin(a));
                int x2 = cx + (int)(r * Math.cos(a));
                int y2 = cy + (int)(r * Math.sin(a));
                g2.drawLine(x1, y1, x2, y2);
            }

            g2.setColor(TEXT_MUTED);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            String msg = "Procesando…";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, cx - fm.stringWidth(msg)/2, cy + 44);
        }
    }

    // ════════════════════════════════════════════════════════
    //  Main
    // ════════════════════════════════════════════════════════
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(ImageAnalyzer::new);
    }
}