package IA;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;

import static IA.AppTheme.*;
import static IA.UIFactory.*;

/**
 * Ventana principal de Image Analyzer.
 * Orquesta la UI, carga de imágenes, ajuste de canales y paneles visuales.
 */
public class ImageAnalyzer extends JFrame {

    // ── Estado ────────────────────────────────────────────────
    private BufferedImage originalImage;   // imagen sin modificar
    private BufferedImage adjustedImage;   // imagen con ajuste de canales aplicado
    private boolean       isLoading = false;

    // ── Estado de filtro activo ─────────────────────────────
    private enum FilterType { NONE, GRAYSCALE, NEGATIVE, BINARIZE }
    private FilterType activeFilter = FilterType.NONE;

    // ── Componentes de filtros ───────────────────────────────
    private JButton btnGrayscale, btnNegative;
    private JSlider sliderBinarize;
    private JLabel valBinarize;

    // ── Sliders de canal ─────────────────────────────────────
    private JSlider sliderR, sliderG, sliderB;
    private JLabel  valR, valG, valB;

    // ── Paneles ───────────────────────────────────────────────
    private ImagePanel      imagePanel;
    private HistogramPanel  histogramPanel;
    private StatsPanel      statsPanel;
    private LoadingOverlay  loadingOverlay;

    // ── Labels de status bar ──────────────────────────────────
    private JLabel statusLabel;
    private JLabel filenameLabel;
    private JLabel dimensionsLabel;

    public ImageAnalyzer() {
        super("Image Analyzer  ·  v1.0");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1450, 820);
        setMinimumSize(new Dimension(960, 620));
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
        bar.setBorder(new MatteBorder(0, 0, 1, 0, DIVIDER));

        JMenu file = styledMenu("Archivo");
        JMenuItem open = styledItem("Abrir imagen…", KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK);
        open.addActionListener(e -> openFileDialog());
        JMenuItem save = styledItem("Guardar imagen…", KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK);
        save.addActionListener(e -> saveImageDialog());
        JMenuItem exit = styledItem("Salir");
        exit.addActionListener(e -> dispose());
        file.add(open); file.add(save); file.addSeparator(); file.add(exit);

        JMenu view = styledMenu("Vista");
        JMenuItem clear = styledItem("Limpiar");
        clear.addActionListener(e -> clearAll());
        view.add(clear);

        bar.add(file);
        bar.add(view);
        setJMenuBar(bar);
    }

    // ════════════════════════════════════════════════════════
    //  UI principal
    // ════════════════════════════════════════════════════════
    private void buildUI() {
        setLayout(new BorderLayout());
        add(buildToolbar(),       BorderLayout.NORTH);
        add(buildCenter(),        BorderLayout.CENTER);
        add(buildFilterPanel(),   BorderLayout.WEST);
        add(buildChannelSliders(), BorderLayout.SOUTH);
    }

    private JPanel buildCenter() {
        imagePanel     = new ImagePanel();
        histogramPanel = new HistogramPanel();
        statsPanel     = new StatsPanel();
        loadingOverlay = new LoadingOverlay();

        // Panel derecho: histograma + stats
        JPanel rightPanel = new JPanel(new BorderLayout(0, 0));
        rightPanel.setBackground(BG_DEEP);
        rightPanel.setBorder(new EmptyBorder(10, 6, 10, 10));
        rightPanel.add(card(histogramPanel, "Histograma RGB"), BorderLayout.CENTER);

        JPanel statsWrapper = new JPanel(new BorderLayout());
        statsWrapper.setBackground(BG_DEEP);
        statsWrapper.setBorder(new EmptyBorder(6, 0, 0, 0));
        statsWrapper.add(statsPanel, BorderLayout.CENTER);
        rightPanel.add(statsWrapper, BorderLayout.SOUTH);

        // Panel izquierdo con overlay de carga
        JLayeredPane layered = new JLayeredPane();
        layered.setLayout(new OverlayLayout(layered));
        JPanel imgWrapper = card(imagePanel, null);
        imgWrapper.setBorder(new EmptyBorder(10, 10, 10, 6));
        layered.add(imgWrapper,    JLayeredPane.DEFAULT_LAYER);
        layered.add(loadingOverlay, JLayeredPane.PALETTE_LAYER);

        // Split
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, layered, rightPanel);
        split.setResizeWeight(0.58);
        split.setDividerSize(3);
        split.setBackground(DIVIDER);
        split.setBorder(null);
        try {
            if (split.getUI() instanceof javax.swing.plaf.basic.BasicSplitPaneUI bspUI)
                bspUI.getDivider().setBackground(DIVIDER);
        } catch (Exception ignored) {}

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(BG_DEEP);
        center.add(split, BorderLayout.CENTER);
        return center;
    }

    // ── Panel de filtros (lado izquierdo) ─────────────────────
    private JPanel buildFilterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_PANEL);
        panel.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 0, 1, DIVIDER),
                new EmptyBorder(10, 10, 10, 10)
        ));
        panel.setPreferredSize(new Dimension(170, 0));

        // Título
        JLabel title = new JLabel("FILTROS");
        title.setForeground(new Color(70, 78, 108));
        title.setFont(sansBold(9));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(10));

        // ── Blanco y Negro ──
        btnGrayscale = filterButton("⚫  Blanco y Negro");
        btnGrayscale.setToolTipText("Convierte la imagen a escala de grises");
        btnGrayscale.addActionListener(e -> applyFilter(FilterType.GRAYSCALE));
        panel.add(btnGrayscale);
        panel.add(Box.createVerticalStrut(6));

        // ── Negativo ──
        btnNegative = filterButton("🔄  Negativo");
        btnNegative.setToolTipText("Invierte los colores de la imagen");
        btnNegative.addActionListener(e -> applyFilter(FilterType.NEGATIVE));
        panel.add(btnNegative);
        panel.add(Box.createVerticalStrut(12));

        // ── Binarización ──
        JLabel binLabel = new JLabel("Binarización");
        binLabel.setForeground(new Color(70, 78, 108));
        binLabel.setFont(sansBold(9));
        binLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(binLabel);
        panel.add(Box.createVerticalStrut(4));

        sliderBinarize = new JSlider(0, 255, 128);
        sliderBinarize.setBackground(BG_PANEL);
        sliderBinarize.setAlignmentX(Component.LEFT_ALIGNMENT);
        sliderBinarize.setMaximumSize(new Dimension(150, 26));
        sliderBinarize.setFocusable(false);
        valBinarize = new JLabel("Umbral: 128");
        valBinarize.setForeground(TEXT_MUTED);
        valBinarize.setFont(monoPlain(10));
        valBinarize.setAlignmentX(Component.LEFT_ALIGNMENT);

        sliderBinarize.addChangeListener(e -> {
            valBinarize.setText("Umbral: " + sliderBinarize.getValue());
            if (!sliderBinarize.getValueIsAdjusting()) {
                applyFilter(FilterType.BINARIZE);
            }
        });

        panel.add(sliderBinarize);
        panel.add(Box.createVerticalStrut(2));
        panel.add(valBinarize);
        panel.add(Box.createVerticalStrut(12));

        // ── Botón Original (reset) ──
        panel.add(Box.createVerticalGlue());
        JButton btnOriginal = ghostButton("↺  Original");
        btnOriginal.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnOriginal.setMaximumSize(new Dimension(150, 30));
        btnOriginal.setToolTipText("Restaurar imagen original");
        btnOriginal.addActionListener(e -> applyFilter(FilterType.NONE));
        panel.add(btnOriginal);
        panel.add(Box.createVerticalStrut(4));

        return panel;
    }

    private JButton filterButton(String text) {
        JButton b = new JButton(text);
        b.setForeground(TEXT_PRIMARY);
        b.setFont(sansPlain(12));
        b.setBackground(BG_CARD);
        b.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        b.setFocusPainted(false);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(160, 36));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setContentAreaFilled(true);

        // Hover effect
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setBackground(BG_HOVER);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(BG_CARD);
            }
        });
        return b;
    }

    // ════════════════════════════════════════════════════════
    //  Aplicar filtro
    // ════════════════════════════════════════════════════════
    private void applyFilter(FilterType filter) {
        if (originalImage == null) return;
        activeFilter = filter;

        switch (filter) {
            case NONE ->          adjustedImage = originalImage;
            case GRAYSCALE ->     adjustedImage = ImageProcessor.applyGrayscale(originalImage);
            case NEGATIVE ->      adjustedImage = ImageProcessor.applyNegative(originalImage);
            case BINARIZE ->      adjustedImage = ImageProcessor.applyBinarization(originalImage, sliderBinarize.getValue());
        }

        imagePanel.setImage(adjustedImage);
        int[][] hist = ImageProcessor.computeHistogram(adjustedImage);
        double[][] stats = ImageProcessor.computeStats(hist, (long) adjustedImage.getWidth() * adjustedImage.getHeight());
        histogramPanel.setHistogram(hist);
        statsPanel.setStats(stats);

        statusLabel.setText(switch (filter) {
            case NONE -> "Imagen original";
            case GRAYSCALE -> "Filtro: Blanco y negro";
            case NEGATIVE -> "Filtro: Negativo";
            case BINARIZE -> "Filtro: Binarización (umbral " + sliderBinarize.getValue() + ")";
        });
    }

    // ── Toolbar ───────────────────────────────────────────────
    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_TOOLBAR);
        bar.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, DIVIDER),
                new EmptyBorder(7, 10, 7, 10)
        ));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        left.setOpaque(false);

        JButton btnOpen = accentButton("⊕  Abrir imagen");
        btnOpen.addActionListener(e -> openFileDialog());
        left.add(btnOpen);
        left.add(vSeparator());

        left.add(dimLabel("CANALES"));
        String[] lbls = {"R", "G", "B", "L"};
        Color[]  cols = {CH_R, CH_G, CH_B, CH_L};
        for (int c = 0; c < 4; c++) {
            final int idx = c;
            JCheckBox cb  = channelCheckbox(lbls[c], cols[c]);
            cb.setSelected(true);
            cb.addActionListener(e -> histogramPanel.setChannelVisible(idx, cb.isSelected()));
            left.add(cb);
        }
        left.add(vSeparator());

        JCheckBox logCb = channelCheckbox("LOG", TEXT_MUTED);
        logCb.setToolTipText("Escala logarítmica en el histograma");
        logCb.addActionListener(e -> histogramPanel.setLogScale(logCb.isSelected()));
        left.add(logCb);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        right.setOpaque(false);
        JButton btnClear = ghostButton("Limpiar");
        btnClear.addActionListener(e -> clearAll());
        right.add(btnClear);

        bar.add(left,  BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ── Panel de sliders de canal ─────────────────────────────
    private JPanel buildChannelSliders() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(BG_TOOLBAR);
        outer.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, DIVIDER),
                new EmptyBorder(0, 0, 0, 0)
        ));

        // Status bar al fondo
        outer.add(buildStatusBar(), BorderLayout.SOUTH);

        // Barra de sliders
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 7));
        bar.setBackground(BG_TOOLBAR);
        bar.setBorder(new MatteBorder(0, 0, 1, 0, DIVIDER));

        bar.add(dimLabel("AJUSTE DE COLOR"));
        bar.add(vSeparator());

        // Crear los tres sliders R, G, B
        Color[] colors  = {CH_R, CH_G, CH_B};
        String[] names  = {"R", "G", "B"};
        sliderR = buildChannelSlider(); valR = sliderValueLabel();
        sliderG = buildChannelSlider(); valG = sliderValueLabel();
        sliderB = buildChannelSlider(); valB = sliderValueLabel();

        JSlider[] sliders = {sliderR, sliderG, sliderB};
        JLabel[]  vals    = {valR, valG, valB};

        for (int i = 0; i < 3; i++) {
            final int idx = i;
            // Etiqueta del canal
            JLabel lbl = new JLabel(names[i]);
            lbl.setForeground(colors[i]);
            lbl.setFont(AppTheme.monoBold(12));

            // Listener: actualiza imagen y paneles al soltar
            sliders[i].addChangeListener(e -> {
                vals[idx].setText(String.format("%+d", sliders[idx].getValue()));
                if (!sliders[idx].getValueIsAdjusting()) applyChannelAdjustment();
            });

            bar.add(lbl);
            bar.add(sliders[i]);
            bar.add(vals[i]);
            if (i < 2) bar.add(vSeparator());
        }

        bar.add(vSeparator());

        // Botón reset
        JButton btnReset = ghostButton("Reset");
        btnReset.setToolTipText("Restaurar canales originales");
        btnReset.addActionListener(e -> resetSliders());
        bar.add(btnReset);

        bar.add(vSeparator());

        // Botón guardar
        JButton btnSave = accentButton("⬇  Guardar");
        btnSave.setToolTipText("Guardar imagen con ajustes (Ctrl+S)");
        btnSave.addActionListener(e -> saveImageDialog());
        bar.add(btnSave);

        outer.add(bar, BorderLayout.CENTER);
        return outer;
    }

    private JSlider buildChannelSlider() {
        JSlider s = new JSlider(-255, 255, 0);
        s.setPreferredSize(new Dimension(160, 26));
        s.setBackground(BG_TOOLBAR);
        s.setFocusable(false);
        s.setPaintTicks(false);
        s.setPaintLabels(false);
        return s;
    }

    private JLabel sliderValueLabel() {
        JLabel l = new JLabel(" +0 ");
        l.setForeground(TEXT_MUTED);
        l.setFont(AppTheme.monoPlain(11));
        l.setPreferredSize(new Dimension(36, 16));
        return l;
    }

    // ── Status bar ────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_TOOLBAR);
        bar.setBorder(new EmptyBorder(5, 14, 5, 14));

        filenameLabel = new JLabel("Sin archivo abierto");
        filenameLabel.setForeground(TEXT_MUTED);
        filenameLabel.setFont(AppTheme.monoPlain(11));

        dimensionsLabel = new JLabel("");
        dimensionsLabel.setForeground(new Color(60, 68, 95));
        dimensionsLabel.setFont(AppTheme.monoPlain(11));

        statusLabel = new JLabel("Listo  ·  Arrastra una imagen aquí");
        statusLabel.setForeground(TEXT_DIM);
        statusLabel.setFont(AppTheme.sansPlain(11));

        JPanel leftSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftSection.setOpaque(false);

        JLabel dot = new JLabel("●");
        dot.setForeground(new Color(60, 180, 110));
        dot.setFont(AppTheme.sansPlain(9));

        leftSection.add(dot);
        leftSection.add(filenameLabel);
        leftSection.add(dimensionsLabel);

        bar.add(leftSection, BorderLayout.WEST);
        bar.add(statusLabel,  BorderLayout.EAST);
        return bar;
    }

    // ════════════════════════════════════════════════════════
    //  Ajuste de canales
    // ════════════════════════════════════════════════════════
    private void applyChannelAdjustment() {
        if (originalImage == null) return;

        int dR = sliderR.getValue();
        int dG = sliderG.getValue();
        int dB = sliderB.getValue();

        // Si todos en 0 usamos la original directamente
        adjustedImage = (dR == 0 && dG == 0 && dB == 0)
                ? originalImage
                : ImageProcessor.applyChannelAdjustment(originalImage, dR, dG, dB);

        // Actualizar imagen y recalcular histograma/stats
        imagePanel.setImage(adjustedImage);

        int[][]    hist  = ImageProcessor.computeHistogram(adjustedImage);
        double[][] stats = ImageProcessor.computeStats(hist, (long) adjustedImage.getWidth() * adjustedImage.getHeight());
        histogramPanel.setHistogram(hist);
        statsPanel.setStats(stats);

        statusLabel.setText(dR == 0 && dG == 0 && dB == 0
                ? "Listo"
                : String.format("Ajuste  R%+d  G%+d  B%+d", dR, dG, dB));
    }

    private void resetSliders() {
        sliderR.setValue(0); valR.setText(" +0 ");
        sliderG.setValue(0); valG.setText(" +0 ");
        sliderB.setValue(0); valB.setText(" +0 ");
        applyChannelAdjustment();
    }

    // ════════════════════════════════════════════════════════
    //  Guardar imagen
    // ════════════════════════════════════════════════════════
    private void saveImageDialog() {
        if (adjustedImage == null) {
            JOptionPane.showMessageDialog(this,
                    "No hay ninguna imagen cargada para guardar.",
                    "Sin imagen", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Guardar imagen");
        fc.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PNG (sin pérdida)", "png"));
        fc.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JPEG", "jpg"));
        fc.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("BMP", "bmp"));
        fc.setFileFilter(fc.getChoosableFileFilters()[1]); // PNG por defecto

        // Sugerir nombre basado en el archivo original
        String suggested = filenameLabel.getText();
        if (!suggested.isBlank() && !suggested.equals("Sin archivo abierto")) {
            int dot = suggested.lastIndexOf('.');
            suggested = (dot > 0 ? suggested.substring(0, dot) : suggested) + "_modificado.png";
        } else {
            suggested = "imagen_modificada.png";
        }
        fc.setSelectedFile(new File(suggested));

        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File dest = fc.getSelectedFile();

        // Determinar formato según filtro o extensión
        String fmt = "png";
        String name = dest.getName().toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) fmt = "jpg";
        else if (name.endsWith(".bmp")) fmt = "bmp";
        else if (!name.endsWith(".png")) dest = new File(dest.getAbsolutePath() + ".png");

        final File finalDest = dest;
        final String finalFmt = fmt;

        statusLabel.setText("Guardando…");

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                // JPEG no soporta alpha — convertir a RGB si es necesario
                BufferedImage toSave = adjustedImage;
                if (finalFmt.equals("jpg") && toSave.getType() != BufferedImage.TYPE_INT_RGB) {
                    BufferedImage rgb = new BufferedImage(
                            toSave.getWidth(), toSave.getHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2 = rgb.createGraphics();
                    g2.drawImage(toSave, 0, 0, null);
                    g2.dispose();
                    toSave = rgb;
                }
                if (!ImageIO.write(toSave, finalFmt, finalDest))
                    throw new Exception("Formato no soportado: " + finalFmt);
                return null;
            }

            @Override protected void done() {
                try {
                    get();
                    statusLabel.setText("Guardado: " + finalDest.getName());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ImageAnalyzer.this,
                            "Error al guardar:\n" + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    statusLabel.setText("Error al guardar");
                }
            }
        }.execute();
    }

    // ════════════════════════════════════════════════════════
    //  Drag & Drop
    // ════════════════════════════════════════════════════════
    private void setupDragDrop() {
        new DropTarget(this, new DropTargetAdapter() {
            @Override public void drop(DropTargetDropEvent ev) {
                try {
                    ev.acceptDrop(DnDConstants.ACTION_COPY);
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) ev.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) loadImageAsync(files.get(0));
                } catch (Exception ex) {
                    statusLabel.setText("No se pudo cargar el archivo arrastrado");
                }
            }
        });
    }

    // ════════════════════════════════════════════════════════
    //  Cargar imagen
    // ════════════════════════════════════════════════════════
    private void openFileDialog() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Seleccionar imagen");
        fc.setFileFilter(new FileNameExtensionFilter(
                "Imágenes (JPG, PNG, BMP, GIF, TIFF)",
                "jpg", "jpeg", "png", "bmp", "gif", "tiff", "tif"
        ));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            loadImageAsync(fc.getSelectedFile());
    }

    private void loadImageAsync(File file) {
        if (isLoading) return;
        isLoading = true;
        loadingOverlay.setVisible(true);
        statusLabel.setText("Cargando…");

        new SwingWorker<Object[], Void>() {
            @Override protected Object[] doInBackground() throws Exception {
                BufferedImage img = ImageIO.read(file);
                if (img == null) throw new Exception("Formato no soportado");
                int[][]    hist  = ImageProcessor.computeHistogram(img);
                double[][] stats = ImageProcessor.computeStats(hist, (long) img.getWidth() * img.getHeight());
                return new Object[]{img, hist, stats, file};
            }

            @Override protected void done() {
                loadingOverlay.setVisible(false);
                isLoading = false;
                try {
                    Object[] result  = get();
                    originalImage    = (BufferedImage) result[0];
                    adjustedImage    = originalImage;
                    int[][]    hist  = (int[][])       result[1];
                    double[][] stats = (double[][])    result[2];
                    File f           = (File)          result[3];

                    resetSliders();  // limpia ajustes anteriores
                    activeFilter = FilterType.NONE;
                    sliderBinarize.setValue(128);
                    imagePanel.setImage(originalImage);
                    histogramPanel.setHistogram(hist);
                    statsPanel.setStats(stats);

                    filenameLabel.setText(f.getName());
                    dimensionsLabel.setText(
                            "   " + originalImage.getWidth() + " × " + originalImage.getHeight() + " px");
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
        originalImage = null;
        adjustedImage = null;
        activeFilter = FilterType.NONE;
        sliderBinarize.setValue(128);
        resetSliders();
        imagePanel.setImage(null);
        histogramPanel.setHistogram(null);
        statsPanel.setStats(null);
        filenameLabel.setText("Sin archivo abierto");
        dimensionsLabel.setText("");
        statusLabel.setText("Listo  ·  Arrastra una imagen aquí");
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