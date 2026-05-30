package IA;

import java.awt.Color;
import java.awt.Font;

/**
 * Paleta de colores, tipografías y constantes visuales de la aplicación.
 * Clase de utilidad: no se instancia.
 */
public final class AppTheme {

    private AppTheme() {}

    // ── Fondos ────────────────────────────────────────────────
    public static final Color BG_DEEP    = new Color(8,   9,  12);
    public static final Color BG_PANEL   = new Color(13,  15,  20);
    public static final Color BG_CARD    = new Color(18,  21,  29);
    public static final Color BG_HOVER   = new Color(26,  30,  42);
    public static final Color BG_TOOLBAR = new Color(11,  13,  18);

    // ── Acento ───────────────────────────────────────────────
    public static final Color ACCENT      = new Color(94, 114, 255);
    public static final Color ACCENT_GLOW = new Color(94, 114, 255, 35);
    public static final Color ACCENT_DIM  = new Color(94, 114, 255, 50);

    // ── Bordes y divisores ───────────────────────────────────
    public static final Color BORDER       = new Color(32,  36,  50);
    public static final Color BORDER_LIGHT = new Color(45,  50,  68);
    public static final Color DIVIDER      = new Color(28,  32,  44);

    // ── Texto ─────────────────────────────────────────────────
    public static final Color TEXT_PRIMARY = new Color(220, 224, 240);
    public static final Color TEXT_MUTED   = new Color(105, 112, 140);
    public static final Color TEXT_DIM     = new Color(52,  58,  82);

    // ── Canales RGB + Luminosidad ────────────────────────────
    public static final Color CH_R = new Color(255,  80,  95, 190);
    public static final Color CH_G = new Color( 45, 210, 115, 190);
    public static final Color CH_B = new Color( 70, 140, 255, 190);
    public static final Color CH_L = new Color(180, 185, 210, 120);

    // ── Fuentes de uso común ─────────────────────────────────
    public static Font sansPlain(int size)   { return new Font("SansSerif",  Font.PLAIN, size); }
    public static Font sansBold(int size)    { return new Font("SansSerif",  Font.BOLD,  size); }
    public static Font monoPlain(int size)   { return new Font("Monospaced", Font.PLAIN, size); }
    public static Font monoBold(int size)    { return new Font("Monospaced", Font.BOLD,  size); }
}