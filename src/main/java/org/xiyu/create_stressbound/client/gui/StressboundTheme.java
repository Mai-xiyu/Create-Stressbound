package org.xiyu.create_stressbound.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Shared Create-style GUI palette and panel chrome: parchment body, beveled edges,
 * brass accents and shadowless dark text, matching Create's screen language.
 */
public final class StressboundTheme {
    // Panel chrome
    public static final int BORDER_OUT = 0xFF_2D2A21;
    public static final int EDGE_LIGHT = 0xFF_FDF7E4;
    public static final int EDGE_SHADOW = 0xFF_B0A584;
    public static final int BG = 0xFF_E7DFC8;
    public static final int HEADER_BG = 0xFF_D9CFAF;
    public static final int DIVIDER = 0xFF_BFB394;

    // Text
    public static final int TITLE = 0xFF_54452C;
    public static final int LABEL = 0xFF_575F7A;
    public static final int VALUE = 0xFF_2B6A8B;
    public static final int DIM = 0xFF_9A8F74;
    public static final int GOOD = 0xFF_3E7C43;
    public static final int WARN = 0xFF_A8741F;
    public static final int ERROR = 0xFF_A8403A;

    // List rows / swatches
    public static final int ROW_BG = 0x2E_5C4F33;
    public static final int ROW_HOVER = 0x55_D9A23C;
    public static final int ROW_SELECTED = 0x66_5C93B8;
    public static final int SWATCH_BORDER = 0xFF_5C4F33;
    public static final int SWATCH_SELECTED = 0xFF_8A5A18;

    // Brass button chrome (shared with BrassButton)
    public static final int BUTTON_SHADOW = 0x66_000000;
    public static final int BUTTON_DARK = 0xFF_4A2D10;
    public static final int BUTTON_BORDER = 0xFF_D48A25;
    public static final int BUTTON_HOVER_BORDER = 0xFF_FFE08A;
    public static final int BUTTON_INNER_BORDER = 0xFF_8B561E;
    public static final int BUTTON_FILL = 0xFF_D69A4A;
    public static final int BUTTON_FILL_HOVER = 0xFF_E4AB55;
    public static final int BUTTON_FILL_DISABLED = 0xFF_BCA376;
    public static final int BUTTON_TEXT = 0xFF_3A2510;
    public static final int BUTTON_TEXT_DISABLED = 0xFF_75643F;

    private StressboundTheme() {
    }

    /** Draws the standard parchment panel with bevel, header strip and centered title. */
    public static void drawPanel(GuiGraphics g, Font font, int x, int y, int w, int h, Component title) {
        g.fill(x, y, x + w, y + h, BORDER_OUT);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, EDGE_LIGHT);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, EDGE_LIGHT);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, EDGE_SHADOW);
        g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, EDGE_SHADOW);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, BG);
        g.fill(x + 2, y + 2, x + w - 2, y + 16, HEADER_BG);
        g.fill(x + 2, y + 16, x + w - 2, y + 17, DIVIDER);
        drawCentered(g, font, title, x + w / 2, y + 5, TITLE);
    }

    /** Centered text without shadow; vanilla drawCenteredString always casts a shadow. */
    public static void drawCentered(GuiGraphics g, Font font, Component text, int centerX, int y, int color) {
        g.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }
}
