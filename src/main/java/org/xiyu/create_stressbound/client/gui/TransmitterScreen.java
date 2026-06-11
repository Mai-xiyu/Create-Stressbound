package org.xiyu.create_stressbound.client.gui;

import java.util.ArrayList;
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
import org.xiyu.create_stressbound.network.SetLinkStressPacket;

public class TransmitterScreen extends AbstractContainerScreen<TransmitterMenu> {
    private static final int PANEL_W = 226;
    private static final int PAD = 8;
    private static final int ROW_START_Y = 36;
    private static final int ROW_H = 20;
    private static final int MIN_VISIBLE_ROWS = 2;
    private static final int MAX_VISIBLE_ROWS = 8;
    private static final int BOTTOM_AREA_H = 84;
    private static final int SWATCH = 8;
    private static final int SWATCH_GAP = 3;
    private static final int PALETTE_COLS = 12;
    private static final int STRESS_BUTTON_W = 44;
    private static final int STRESS_BUTTON_H = 16;
    private static final int STRESS_BUTTON_GAP = 4;
    private static final int AUTO_BUTTON_W = 66;
    private static final int AUTO_BUTTON_H = 16;

    private static final int DIVIDER = StressboundTheme.DIVIDER;
    private static final int LABEL_COLOR = StressboundTheme.LABEL;
    private static final int DIM_COLOR = StressboundTheme.DIM;
    private static final int VALUE_COLOR = StressboundTheme.VALUE;
    private static final int SELECTED_BG = StressboundTheme.ROW_SELECTED;
    private static final int ROW_BG = StressboundTheme.ROW_BG;
    private static final int ROW_HOVER = StressboundTheme.ROW_HOVER;

    private final BlockPos blockPos;
    private UUID selectedLinkId;
    private int scrollOffset;
    private int visibleRows = 4;
    private Button autoButton;
    private final List<Button> stressButtons = new ArrayList<>();

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
        stressButtons.clear();
        addStressButton(0, "-1k", -1024);
        addStressButton(1, "-256", -256);
        addStressButton(2, "+256", 256);
        addStressButton(3, "+1k", 1024);

        autoButton = Button.builder(Component.translatable("gui.create_stressbound.transmitter.auto_color"), button -> {
                StressTransmitterBlockEntity.LinkedReceiverInfo selected = selectedInfo();
                if (selected != null) {
                    PacketDistributor.sendToServer(new SetLinkColorPacket(blockPos, selected.linkId(), 0, true));
                }
            })
            .pos(leftPos + PANEL_W - PAD - AUTO_BUTTON_W, paletteLabelY() - 4)
            .size(AUTO_BUTTON_W, AUTO_BUTTON_H)
            .tooltip(Tooltip.create(Component.translatable("gui.create_stressbound.transmitter.auto_color.tooltip")))
            .build(BrassButton::new);
        addRenderableWidget(autoButton);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        updateSelection();
        boolean hasSelected = selectedInfo() != null;
        for (Button button : stressButtons) {
            button.active = hasSelected;
        }
        if (autoButton != null) {
            autoButton.active = hasSelected;
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
        StressboundTheme.drawPanel(g, font, leftPos, topPos, PANEL_W, imageHeight,
            Component.translatable("gui.create_stressbound.transmitter.title"));
    }

    private void drawContent(GuiGraphics g, int mx, int my) {
        List<StressTransmitterBlockEntity.LinkedReceiverInfo> infos = infos();
        int x = leftPos + PAD;
        int y = topPos + 23;

        g.drawString(font, Component.translatable("gui.create_stressbound.transmitter.links", infos.size()), x, y, LABEL_COLOR, false);
        if (infos.size() > visibleRows) {
            Component range = Component.translatable("gui.create_stressbound.transmitter.scroll",
                scrollOffset + 1, Math.min(infos.size(), scrollOffset + visibleRows), infos.size());
            g.drawString(font, range, leftPos + PANEL_W - PAD - font.width(range), y, DIM_COLOR, false);
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
            Component status = Component.translatable(info.status().translationKey());
            int statusX = leftPos + PANEL_W - PAD - font.width(status) - 4;
            drawTrimmedString(g, pos, rowX + 22, ry + 4, VALUE_COLOR, statusX - rowX - 26);
            g.drawString(font, status, statusX, ry + 4, statusColor(info.status()), false);
            g.drawString(font, Component.translatable("gui.create_stressbound.transmitter.requested", info.requestedStress()),
                rowX + 22, ry + 13, DIM_COLOR, false);
        }

        if (infos.isEmpty()) {
            StressboundTheme.drawCentered(g, font, Component.translatable("gui.create_stressbound.transmitter.no_links"),
                leftPos + PANEL_W / 2, topPos + ROW_START_Y + ROW_H * Math.max(1, visibleRows / 2), DIM_COLOR);
        }

        drawStressControls(g, selectedInfo());

        int listBottom = listBottomY();
        g.fill(x, listBottom + 4, leftPos + PANEL_W - PAD, listBottom + 5, DIVIDER);

        int paletteY = paletteLabelY();
        g.drawString(font, Component.translatable("gui.create_stressbound.transmitter.palette"),
            x, paletteY, LABEL_COLOR, false);
        drawPalette(g);
    }

    private void drawStressControls(GuiGraphics g, StressTransmitterBlockEntity.LinkedReceiverInfo selected) {
        int y = stressLabelY();
        g.drawString(font, Component.translatable("gui.create_stressbound.transmitter.stress"),
            leftPos + PAD, y, LABEL_COLOR, false);
        if (selected != null) {
            Component value = Component.translatable(
                "gui.create_stressbound.transmitter.stress.value", selected.requestedStress());
            g.drawString(font, value, leftPos + PANEL_W - PAD - font.width(value), y, VALUE_COLOR, false);
        }
    }

    private void drawPalette(GuiGraphics g) {
        int transmitterColor = transmitterColor();
        int startX = leftPos + PAD;
        int startY = paletteStartY();
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
        g.fill(x - 1, y - 1, x + SWATCH + 1, y + SWATCH + 1,
            selected ? StressboundTheme.SWATCH_SELECTED : StressboundTheme.SWATCH_BORDER);
        g.fill(x, y, x + SWATCH, y + SWATCH, 0xFF000000 | StressLinkColors.normalize(color));
        g.fill(x, y, x + SWATCH, y + 1, 0x77_FFFFFF);
        g.fill(x, y + SWATCH - 1, x + SWATCH, y + SWATCH, 0x66_000000);
    }

    private void drawTrimmedString(GuiGraphics g, String text, int x, int y, int color, int maxWidth) {
        if (maxWidth <= 0) {
            return;
        }
        String display = text;
        if (font.width(display) > maxWidth) {
            int ellipsisWidth = font.width("...");
            display = maxWidth <= ellipsisWidth
                ? ""
                : font.plainSubstrByWidth(display, maxWidth - ellipsisWidth) + "...";
        }
        if (!display.isEmpty()) {
            g.drawString(font, display, x, y, color, false);
        }
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
        int startY = paletteStartY();
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

    private void addStressButton(int index, String label, int delta) {
        int x = leftPos + PAD + index * (STRESS_BUTTON_W + STRESS_BUTTON_GAP);
        Button button = Button.builder(Component.literal(label), ignored -> adjustSelectedStress(delta))
            .pos(x, stressButtonY())
            .size(STRESS_BUTTON_W, STRESS_BUTTON_H)
            .tooltip(Tooltip.create(Component.translatable(
                "gui.create_stressbound.transmitter.stress.tooltip", signed(delta))))
            .build(BrassButton::new);
        stressButtons.add(button);
        addRenderableWidget(button);
    }

    private int stressButtonY() {
        return listBottomY() + 22;
    }

    private int stressLabelY() {
        return listBottomY() + 9;
    }

    private int paletteLabelY() {
        return listBottomY() + 47;
    }

    private int paletteStartY() {
        return listBottomY() + 61;
    }

    private int listBottomY() {
        return topPos + ROW_START_Y + visibleRows * ROW_H;
    }

    private void adjustSelectedStress(int delta) {
        StressTransmitterBlockEntity.LinkedReceiverInfo selected = selectedInfo();
        if (selected == null) {
            return;
        }
        PacketDistributor.sendToServer(new SetLinkStressPacket(blockPos, selected.linkId(),
            addStressDelta(selected.requestedStress(), delta)));
    }

    private static int addStressDelta(int requestedStress, int delta) {
        long adjusted = (long) requestedStress + delta;
        if (adjusted < 1L) {
            return 1;
        }
        if (adjusted > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) adjusted;
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
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
            case ACTIVE -> StressboundTheme.GOOD;
            case OVERLOADED, INVALID_RECEIVER, INVALID_TRANSMITTER, REMOTE_LOOP -> StressboundTheme.ERROR;
            case RECEIVER_DISABLED, TRANSMITTER_DISABLED -> StressboundTheme.WARN;
            default -> StressboundTheme.DIM;
        };
    }
}
