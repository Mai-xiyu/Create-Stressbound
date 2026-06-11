package org.xiyu.create_stressbound.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;

/** Button with Create-style brass chrome; subclasses may override {@link #renderContent}. */
public class BrassButton extends Button {
    public BrassButton(Button.Builder builder) {
        super(builder);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        boolean highlighted = active && isHoveredOrFocused();
        int border = highlighted ? StressboundTheme.BUTTON_HOVER_BORDER : StressboundTheme.BUTTON_BORDER;
        int fill = !active ? StressboundTheme.BUTTON_FILL_DISABLED
            : highlighted ? StressboundTheme.BUTTON_FILL_HOVER : StressboundTheme.BUTTON_FILL;

        g.fill(x + 1, y + 1, x + w + 1, y + h + 1, StressboundTheme.BUTTON_SHADOW);
        g.fill(x, y, x + w, y + h, StressboundTheme.BUTTON_DARK);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, border);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, StressboundTheme.BUTTON_INNER_BORDER);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, fill);
        g.fill(x + 3, y + 3, x + w - 3, y + 4, 0x88_FFFFFF);
        g.fill(x + 3, y + h - 4, x + w - 3, y + h - 3, 0x66_3A1D08);
        renderContent(g, x, y, w, h);
    }

    protected void renderContent(GuiGraphics g, int x, int y, int w, int h) {
        Font font = Minecraft.getInstance().font;
        int textX = x + (w - font.width(getMessage())) / 2;
        int textY = y + (h - 8) / 2 + 1;
        g.drawString(font, getMessage(), textX, textY,
            active ? StressboundTheme.BUTTON_TEXT : StressboundTheme.BUTTON_TEXT_DISABLED, false);
    }
}
