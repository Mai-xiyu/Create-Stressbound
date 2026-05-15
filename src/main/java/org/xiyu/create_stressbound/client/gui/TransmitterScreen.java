package org.xiyu.create_stressbound.client.gui;

import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.xiyu.create_stressbound.StressboundConfig;
import org.xiyu.create_stressbound.content.kinetics.StressTransmitterBlockEntity;
import org.xiyu.create_stressbound.content.link.StressLinkColors;
import org.xiyu.create_stressbound.network.SetLinkColorPacket;

public class TransmitterScreen extends AbstractContainerScreen<TransmitterMenu> {
    private static final int PANEL_W = 226;
    private static final int PAD = 8;
    private static final int ROW_START_Y = 36;
    private static final int ROW_H = 20;
    private static final int MIN_VISIBLE_ROWS = 2;
    private static final int MAX_VISIBLE_ROWS = 8;
    private static final int BOTTOM_AREA_H = 62;
    private static final int SWATCH = 8;
    private static final int SWATCH_GAP = 3;
    private static final int PALETTE_COLS = 12;

    private static final int BG_PANEL = 0xFF_2A2A3C;
    private static final int HEADER_BG = 0xFF_33334A;
    private static final int BORDER_AMBER = 0xFFFFD369;
    private static final int BORDER_DARK = 0xFF_111120;
    private static final int DIVIDER = 0xFF_44445A;
    private static final int TITLE_COLOR = 0xFFFFD369;
    private static final int LABEL_COLOR = 0xFF_BBBBBB;
    private static final int DIM_COLOR = 0xFF_777777;
    private static final int VALUE_COLOR = 0xFF_55CCEE;
    private static final int SELECTED_BG = 0x55_55CCEE;
    private static final int ROW_BG = 0x33_000000;
    private static final int ROW_HOVER = 0x44_FFD369;
    private static final int BADGE_BG = 0xFF_15151F;

    private final BlockPos blockPos;
    private UUID selectedLinkId;
    private int scrollOffset;
    private int visibleRows = 4;
    private Button autoButton;

    public TransmitterScreen(TransmitterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.blockPos = menu.getBlockPos();
        this.imageWidth = PANEL_W;
        this.imageHeight = ROW_START_Y + visibleRows * ROW_H + BOTTOM_AREA_H;
    }

    @Override
    protected void init() {
        visibleRows = computeVisibleRows();
        imageHeight = ROW_START_Y + visibleRows * ROW_H + BOTTOM_AREA_H;
        super.init();
        autoButton = Button.builder(Component.translatable("gui.create_stressbound.transmitter.auto_color"), button -> {
                StressTransmitterBlockEntity.LinkedReceiverInfo selected = selectedInfo();
                if (selected != null) {
                    PacketDistributor.sendToServer(new SetLinkColorPacket(blockPos, selected.linkId(), 0, true));
                }
            })
            .pos(leftPos + PANEL_W - 74, topPos + imageHeight - 23)
            .size(64, 16)
            .tooltip(Tooltip.create(Component.translatable("gui.create_stressbound.transmitter.auto_color.tooltip")))
            .build();
        addRenderableWidget(autoButton);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        updateSelection();
        if (autoButton != null) {
            autoButton.active = selectedInfo() != null;
        }
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        drawPanel(g);
        drawContent(g, mx, my);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<StressTransmitterBlockEntity.LinkedReceiverInfo> infos = infos();
        int row = rowAt(mouseX, mouseY);
        if (row >= 0) {
            int index = scrollOffset + row;
            if (index < infos.size()) {
                selectedLinkId = infos.get(index).linkId();
                return true;
            }
        }

        int color = paletteColorAt(mouseX, mouseY);
        StressTransmitterBlockEntity.LinkedReceiverInfo selected = selectedInfo();
        if (color >= 0 && selected != null) {
            PacketDistributor.sendToServer(new SetLinkColorPacket(blockPos, selected.linkId(), color, false));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxOffset = Math.max(0, infos().size() - visibleRows);
        if (maxOffset <= 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        scrollOffset = Math.max(0, Math.min(maxOffset, scrollOffset - (int) Math.signum(scrollY)));
        return true;
    }

    private void drawPanel(GuiGraphics g) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + PANEL_W, y + 1, BORDER_AMBER);
        g.fill(x, y + imageHeight - 1, x + PANEL_W, y + imageHeight, BORDER_AMBER);
        g.fill(x, y, x + 1, y + imageHeight, BORDER_AMBER);
        g.fill(x + PANEL_W - 1, y, x + PANEL_W, y + imageHeight, BORDER_AMBER);
        g.fill(x + 1, y + 1, x + PANEL_W - 1, y + 2, BORDER_DARK);
        g.fill(x + 1, y + imageHeight - 2, x + PANEL_W - 1, y + imageHeight - 1, BORDER_DARK);
        g.fill(x + 1, y + 1, x + 2, y + imageHeight - 1, BORDER_DARK);
        g.fill(x + PANEL_W - 2, y + 1, x + PANEL_W - 1, y + imageHeight - 1, BORDER_DARK);
        g.fill(x + 2, y + 2, x + PANEL_W - 2, y + imageHeight - 2, BG_PANEL);
        g.fill(x + 2, y + 2, x + PANEL_W - 2, y + 16, HEADER_BG);
        g.drawCenteredString(font, Component.translatable("gui.create_stressbound.transmitter.title"),
            x + PANEL_W / 2, y + 6, TITLE_COLOR);
        g.fill(x + PAD, y + 18, x + PANEL_W - PAD, y + 19, DIVIDER);
    }

    private void drawContent(GuiGraphics g, int mx, int my) {
        StressTransmitterBlockEntity transmitter = transmitter();
        List<StressTransmitterBlockEntity.LinkedReceiverInfo> infos = infos();
        int x = leftPos + PAD;
        int y = topPos + 23;

        g.drawString(font, Component.translatable("gui.create_stressbound.transmitter.links", infos.size()), x, y, LABEL_COLOR);
        if (infos.size() > visibleRows) {
            Component range = Component.translatable("gui.create_stressbound.transmitter.scroll",
                scrollOffset + 1, Math.min(infos.size(), scrollOffset + visibleRows), infos.size());
            g.drawString(font, range, leftPos + PANEL_W - PAD - font.width(range), y, DIM_COLOR);
        }

        int rowX = x;
        int rowY = topPos + ROW_START_Y;
        for (int i = 0; i < visibleRows; i++) {
            int index = scrollOffset + i;
            int ry = rowY + i * ROW_H;
            boolean hasInfo = index < infos.size();
            if (!hasInfo) {
                g.fill(rowX, ry, leftPos + PANEL_W - PAD, ry + ROW_H - 2, ROW_BG);
                continue;
            }

            StressTransmitterBlockEntity.LinkedReceiverInfo info = infos.get(index);
            boolean selected = info.linkId().equals(selectedLinkId);
            boolean hovered = mx >= rowX && mx < leftPos + PANEL_W - PAD && my >= ry && my < ry + ROW_H - 2;
            g.fill(rowX, ry, leftPos + PANEL_W - PAD, ry + ROW_H - 2,
                selected ? SELECTED_BG : hovered ? ROW_HOVER : ROW_BG);
            drawColorSwatch(g, rowX + 4, ry + 4, info.color(), selected);

            String pos = "[" + info.receiverPos().getX() + ", " + info.receiverPos().getY() + ", " + info.receiverPos().getZ() + "]";
            g.drawString(font, pos, rowX + 22, ry + 4, VALUE_COLOR);
            Component status = Component.translatable(info.status().translationKey());
            int statusX = leftPos + PANEL_W - PAD - font.width(status) - 4;
            g.drawString(font, status, statusX, ry + 4, statusColor(info.status()));
            g.drawString(font, Component.translatable("gui.create_stressbound.transmitter.requested", info.requestedStress()),
                rowX + 22, ry + 13, DIM_COLOR);
        }

        if (infos.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("gui.create_stressbound.transmitter.no_links"),
                leftPos + PANEL_W / 2, topPos + ROW_START_Y + ROW_H * Math.max(1, visibleRows / 2), DIM_COLOR);
        }

        int paletteY = topPos + imageHeight - 42;
        g.drawString(font, Component.translatable("gui.create_stressbound.transmitter.palette"),
            x, paletteY - 11, LABEL_COLOR);
        drawPalette(g);

        int transmitterColor = transmitterColor();
        if (transmitterColor >= 0 && transmitter != null) {
            Component color = Component.literal(StressLinkColors.hex(transmitterColor));
            g.fill(leftPos + PANEL_W - 70, paletteY - 12, leftPos + PANEL_W - 10, paletteY - 1, BADGE_BG);
            g.drawCenteredString(font, color, leftPos + PANEL_W - 40, paletteY - 10, 0xFF000000 | transmitterColor);
        }
    }

    private void drawPalette(GuiGraphics g) {
        int transmitterColor = transmitterColor();
        int startX = leftPos + PAD;
        int startY = topPos + imageHeight - 39;
        for (int i = 0; i < StressLinkColors.PALETTE.length; i++) {
            int color = StressLinkColors.normalize(StressLinkColors.PALETTE[i]);
            int col = i % PALETTE_COLS;
            int row = i / PALETTE_COLS;
            int x = startX + col * (SWATCH + SWATCH_GAP);
            int y = startY + row * (SWATCH + SWATCH_GAP);
            boolean selectedColor = transmitterColor >= 0 && StressLinkColors.normalize(transmitterColor) == color;
            drawColorSwatch(g, x, y, color, selectedColor);
        }
    }

    private void drawColorSwatch(GuiGraphics g, int x, int y, int color, boolean selected) {
        g.fill(x - 1, y - 1, x + SWATCH + 1, y + SWATCH + 1, selected ? BORDER_AMBER : BORDER_DARK);
        g.fill(x, y, x + SWATCH, y + SWATCH, 0xFF000000 | StressLinkColors.normalize(color));
        g.fill(x, y, x + SWATCH, y + 1, 0x77_FFFFFF);
        g.fill(x, y + SWATCH - 1, x + SWATCH, y + SWATCH, 0x66_000000);
    }

    private int rowAt(double mouseX, double mouseY) {
        int x0 = leftPos + PAD;
        int x1 = leftPos + PANEL_W - PAD;
        int y0 = topPos + ROW_START_Y;
        int y1 = y0 + ROW_H * visibleRows;
        if (mouseX < x0 || mouseX >= x1 || mouseY < y0 || mouseY >= y1) {
            return -1;
        }
        return (int) ((mouseY - y0) / ROW_H);
    }

    private int paletteColorAt(double mouseX, double mouseY) {
        int startX = leftPos + PAD;
        int startY = topPos + imageHeight - 39;
        for (int i = 0; i < StressLinkColors.PALETTE.length; i++) {
            int col = i % PALETTE_COLS;
            int row = i / PALETTE_COLS;
            int x = startX + col * (SWATCH + SWATCH_GAP);
            int y = startY + row * (SWATCH + SWATCH_GAP);
            if (mouseX >= x && mouseX < x + SWATCH && mouseY >= y && mouseY < y + SWATCH) {
                return StressLinkColors.normalize(StressLinkColors.PALETTE[i]);
            }
        }
        return -1;
    }

    private void updateSelection() {
        List<StressTransmitterBlockEntity.LinkedReceiverInfo> infos = infos();
        int maxOffset = Math.max(0, infos.size() - visibleRows);
        scrollOffset = Math.max(0, Math.min(maxOffset, scrollOffset));
        if (infos.isEmpty()) {
            selectedLinkId = null;
            return;
        }
        if (selectedInfo() == null) {
            selectedLinkId = infos.get(0).linkId();
        }
    }

    private StressTransmitterBlockEntity.LinkedReceiverInfo selectedInfo() {
        if (selectedLinkId == null) {
            return null;
        }
        for (StressTransmitterBlockEntity.LinkedReceiverInfo info : infos()) {
            if (info.linkId().equals(selectedLinkId)) {
                return info;
            }
        }
        return null;
    }

    private List<StressTransmitterBlockEntity.LinkedReceiverInfo> infos() {
        StressTransmitterBlockEntity transmitter = transmitter();
        return transmitter == null ? List.of() : transmitter.getLinkedReceiverInfos();
    }

    private StressTransmitterBlockEntity transmitter() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        BlockEntity be = mc.level.getBlockEntity(blockPos);
        return be instanceof StressTransmitterBlockEntity transmitter ? transmitter : null;
    }

    private int transmitterColor() {
        for (StressTransmitterBlockEntity.LinkedReceiverInfo info : infos()) {
            return StressLinkColors.normalize(info.color());
        }
        return -1;
    }

    private int computeVisibleRows() {
        int configured = StressboundConfig.maxReceiversPerTransmitter;
        if (configured <= 0) {
            configured = MIN_VISIBLE_ROWS;
        }
        int maxFit = Math.max(MIN_VISIBLE_ROWS, (height - 20 - ROW_START_Y - BOTTOM_AREA_H) / ROW_H);
        return Math.max(MIN_VISIBLE_ROWS, Math.min(Math.min(MAX_VISIBLE_ROWS, maxFit), configured));
    }

    private static int statusColor(org.xiyu.create_stressbound.content.link.ReceiverStatus status) {
        return switch (status) {
            case ACTIVE -> 0xFF_55FF77;
            case OVERLOADED, INVALID_RECEIVER, INVALID_TRANSMITTER, REMOTE_LOOP -> 0xFF_FF5555;
            case RECEIVER_DISABLED, TRANSMITTER_DISABLED -> 0xFF_FFAA33;
            default -> 0xFF_999999;
        };
    }
}
