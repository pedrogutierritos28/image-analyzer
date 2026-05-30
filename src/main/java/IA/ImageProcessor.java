package IA;

import java.awt.image.BufferedImage;

/**
 * Lógica pura de procesamiento de imágenes.
 * Sin dependencias de UI. Todos los métodos son estáticos.
 */
public final class ImageProcessor {

    private ImageProcessor() {}

    /**
     * Calcula el histograma de los canales R, G, B y Luminosidad.
     *
     * @param img imagen fuente
     * @return array [4][256]: índices 0=R, 1=G, 2=B, 3=Luminosidad
     */
    public static int[][] computeHistogram(BufferedImage img) {
        int[][] hist = new int[4][256];
        int w = img.getWidth(), h = img.getHeight();
        int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            img.getRGB(0, y, w, 1, row, 0, w);
            for (int rgb : row) {
                int r   = (rgb >> 16) & 0xFF;
                int g   = (rgb >>  8) & 0xFF;
                int b   =  rgb        & 0xFF;
                int lum = (int)(0.2126 * r + 0.7152 * g + 0.0722 * b);
                hist[0][r]++;
                hist[1][g]++;
                hist[2][b]++;
                hist[3][Math.min(lum, 255)]++;
            }
        }
        return hist;
    }

    /**
     * Aplica un ajuste de offset a los canales R, G y B de la imagen.
     * Los valores se recortan al rango [0, 255].
     *
     * @param src imagen original (no se modifica)
     * @param dR  delta para el canal rojo   (-255 a +255)
     * @param dG  delta para el canal verde  (-255 a +255)
     * @param dB  delta para el canal azul   (-255 a +255)
     * @return nueva imagen con los canales ajustados
     */
    public static BufferedImage applyChannelAdjustment(BufferedImage src, int dR, int dG, int dB) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            src.getRGB(0, y, w, 1, row, 0, w);
            for (int i = 0; i < w; i++) {
                int rgb = row[i];
                int r = clamp(((rgb >> 16) & 0xFF) + dR);
                int g = clamp(((rgb >>  8) & 0xFF) + dG);
                int b = clamp(( rgb        & 0xFF) + dB);
                row[i] = (r << 16) | (g << 8) | b;
            }
            out.setRGB(0, y, w, 1, row, 0, w);
        }
        return out;
    }

    /**
     * Convierte la imagen a escala de grises (luminancia perceptual ITU-R BT.709).
     *
     * @param src imagen original (no se modifica)
     * @return nueva imagen en blanco y negro
     */
    public static BufferedImage applyGrayscale(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            src.getRGB(0, y, w, 1, row, 0, w);
            for (int i = 0; i < w; i++) {
                int rgb = row[i];
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >>  8) & 0xFF;
                int b =  rgb        & 0xFF;
                int lum = (int)(0.2126 * r + 0.7152 * g + 0.0722 * b);
                lum = clamp(lum);
                row[i] = (lum << 16) | (lum << 8) | lum;
            }
            out.setRGB(0, y, w, 1, row, 0, w);
        }
        return out;
    }

    /**
     * Aplica el filtro negativo (inversión de color).
     * Cada canal se convierte en 255 - valor.
     *
     * @param src imagen original (no se modifica)
     * @return nueva imagen con colores invertidos
     */
    public static BufferedImage applyNegative(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            src.getRGB(0, y, w, 1, row, 0, w);
            for (int i = 0; i < w; i++) {
                int rgb = row[i];
                int r = 255 - ((rgb >> 16) & 0xFF);
                int g = 255 - ((rgb >>  8) & 0xFF);
                int b = 255 - ( rgb        & 0xFF);
                row[i] = (r << 16) | (g << 8) | b;
            }
            out.setRGB(0, y, w, 1, row, 0, w);
        }
        return out;
    }

    /**
     * Binariza la imagen usando un umbral sobre la luminancia.
     * Píxeles con luminancia >= umbral → blanco (255), resto → negro (0).
     *
     * @param src       imagen original (no se modifica)
     * @param threshold umbral de binarización [0–255]
     * @return nueva imagen binarizada (solo puros negros y blancos)
     */
    public static BufferedImage applyBinarization(BufferedImage src, int threshold) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            src.getRGB(0, y, w, 1, row, 0, w);
            for (int i = 0; i < w; i++) {
                int rgb = row[i];
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >>  8) & 0xFF;
                int b =  rgb        & 0xFF;
                int lum = (int)(0.2126 * r + 0.7152 * g + 0.0722 * b);
                int val = lum >= threshold ? 255 : 0;
                row[i] = (val << 16) | (val << 8) | val;
            }
            out.setRGB(0, y, w, 1, row, 0, w);
        }
        return out;
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    /**
     * Calcula media, desviación estándar y mediana por canal (R, G, B).
     *
     * @param hist        histograma calculado con {@link #computeHistogram}
     * @param totalPixels ancho × alto de la imagen
     * @return array [3][3]: [canal][0=media, 1=desv, 2=mediana]
     */
    public static double[][] computeStats(int[][] hist, long totalPixels) {
        double[][] stats = new double[3][3];
        for (int c = 0; c < 3; c++) {
            // Media
            double sum = 0;
            for (int i = 0; i < 256; i++) sum += (double) i * hist[c][i];
            double mean = sum / totalPixels;

            // Desviación estándar
            double varSum = 0;
            for (int i = 0; i < 256; i++) varSum += hist[c][i] * Math.pow(i - mean, 2);
            double std = Math.sqrt(varSum / totalPixels);

            // Mediana
            long half = totalPixels / 2, acc = 0;
            int median = 0;
            for (int i = 0; i < 256; i++) {
                acc += hist[c][i];
                if (acc >= half) { median = i; break; }
            }

            stats[c][0] = mean;
            stats[c][1] = std;
            stats[c][2] = median;
        }
        return stats;
    }
}