package org.xiyu.create_stressbound.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.xiyu.create_stressbound.CreateStressbound;
import org.xiyu.create_stressbound.content.kinetics.StressReceiverBlockEntity;
import org.xiyu.create_stressbound.content.link.ReceiverStatus;
import org.xiyu.create_stressbound.network.ReverseTogglePacket;

public class ReceiverScreen extends AbstractContainerScreen<ReceiverMenu> {

    // Create-style parchment palette (shared via StressboundTheme)
    private static final int TITLE_COLOR = StressboundTheme.TITLE;
    private static final int LABEL_COLOR = StressboundTheme.LABEL;
    private static final int VALUE_COLOR = StressboundTheme.VALUE;
    private static final int GOOD_COLOR = StressboundTheme.GOOD;
    private static final int WARN_COLOR = StressboundTheme.WARN;
    private static final int ERROR_COLOR = StressboundTheme.ERROR;
    private static final int DIM_COLOR = StressboundTheme.DIM;
    private static final int PANEL_W = 176;
    private static final int PANEL_H = 164;
    private static final int PAD = 8;
    private static final int LINE_H = 12;
    private static final int ICON_BUTTON_SIZE = 18;
    private static final ResourceLocation CLOCKWISE_ICON =
        CreateStressbound.id("textures/gui/rotation_clockwise.png");
    private static final ResourceLocation COUNTERCLOCKWISE_ICON =
        CreateStressbound.id("textures/gui/rotation_counterclockwise.png");

    private final BlockPos blockPos;
    private DirectionIconButton reverseButton;

    public ReceiverScreen(ReceiverMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.blockPos = menu.getBlockPos();
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int btnX = leftPos + (PANEL_W - ICON_BUTTON_SIZE) / 2;
        int btnY = directionButtonY();
        reverseButton = (DirectionIconButton) Button.builder(reverseButtonText(), b ->
                PacketDistributor.sendToServer(new ReverseTogglePacket(blockPos)))
            .pos(btnX, btnY)
            .size(ICON_BUTTON_SIZE, ICON_BUTTON_SIZE)
            .tooltip(Tooltip.create(Component.translatable("gui.create_stressbound.receiver.reverse.tooltip")))
            .build(DirectionIconButton::new);
        reverseButton.setReversed(isReverseOutput());
        addRenderableWidget(reverseButton);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        updateReverseButton();
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        drawPanel(g);
        drawContent(g);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
    }

    private void drawPanel(GuiGraphics g) {
        StressboundTheme.drawPanel(g, font, leftPos, topPos, PANEL_W, PANEL_H,
            Component.translatable("gui.create_stressbound.receiver.title"));
    }

    private void drawContent(GuiGraphics g) {
        StressReceiverBlockEntity receiver = getReceiver();
        if (receiver == null) return;

        int x = leftPos + PAD;
        int y = topPos + 22;

        ReceiverStatus status = receiver.getStatus();
        int statusColor = switch (status) {
            case ACTIVE -> GOOD_COLOR;
            case IDLE -> DIM_COLOR;
            case OVERLOADED -> ERROR_COLOR;
            case TRANSMITTER_DISABLED, RECEIVER_DISABLED -> WARN_COLOR;
            default -> ERROR_COLOR;
        };
        drawLine(g, x, y, "hud.create_stressbound.status",
            Component.translatable(status.translationKey()).getString(), statusColor);
        y += LINE_H + 4;

        if (status == ReceiverStatus.ACTIVE) {
            drawLine(g, x, y, "hud.create_stressbound.speed",
                format(receiver.getTransmittedSpeed()) + " RPM", VALUE_COLOR);
            y += LINE_H;
            drawLine(g, x, y, "hud.create_stressbound.budget",
                receiver.getGrantedStress() + " / " + receiver.getRequestedStress() + " SU", VALUE_COLOR);
            y += LINE_H;
        } else if (receiver.getLinkId() != null) {
            drawLine(g, x, y, "hud.create_stressbound.requested",
                receiver.getRequestedStress() + " SU", VALUE_COLOR);
            y += LINE_H;
        } else {
            g.drawString(font, Component.translatable("hud.create_stressbound.unlinked"), x, y, DIM_COLOR, false);
            y += LINE_H;
        }

        int rs = receiver.getRedstoneSignal();
        if (rs > 0) {
            drawLine(g, x, y, "hud.create_stressbound.redstone",
                rs + "/15 (" + Math.round(receiver.getRedstoneOutputScale() * 100) + "%)",
                rs >= 15 ? ERROR_COLOR : WARN_COLOR);
            y += LINE_H;
        }

        BlockPos txPos = receiver.getTransmitterPos();
        if (txPos != null && !txPos.equals(BlockPos.ZERO)) {
            String target = receiver.isTransmitterMoving()
                ? Component.translatable("hud.create_stressbound.target.moving").getString()
                : "[" + txPos.getX() + ", " + txPos.getY() + ", " + txPos.getZ() + "]";
            drawLine(g, x, y, "hud.create_stressbound.target",
                target, VALUE_COLOR);
        }

        drawDirectionControl(g, receiver);
    }

    private void drawDirectionControl(GuiGraphics g, StressReceiverBlockEntity receiver) {
        int centerX = leftPos + PANEL_W / 2;
        int buttonY = directionButtonY();
        StressboundTheme.drawCentered(g, font, Component.translatable("gui.create_stressbound.receiver.direction"),
            centerX, buttonY - 13, LABEL_COLOR);
        StressboundTheme.drawCentered(g, font, directionText(receiver.isReverseOutput()),
            centerX, buttonY + ICON_BUTTON_SIZE + 4, TITLE_COLOR);
    }

    private void drawLine(GuiGraphics g, int x, int y, String labelKey, String value, int valueColor) {
        g.drawString(font, Component.translatable(labelKey), x, y, LABEL_COLOR, false);
        g.drawString(font, value, x + font.width(Component.translatable(labelKey)) + 4, y, valueColor, false);
    }

    private void updateReverseButton() {
        if (reverseButton != null) {
            reverseButton.setReversed(isReverseOutput());
            reverseButton.setMessage(reverseButtonText());
        }
    }

    private int directionButtonY() {
        return topPos + PANEL_H - PAD - ICON_BUTTON_SIZE - 10;
    }

    private boolean isReverseOutput() {
        StressReceiverBlockEntity receiver = getReceiver();
        return receiver != null && receiver.isReverseOutput();
    }

    private Component reverseButtonText() {
        boolean reversed = isReverseOutput();
        return Component.translatable("gui.create_stressbound.receiver.reverse",
            Component.translatable(reversed
                ? "gui.create_stressbound.receiver.reverse.on"
                : "gui.create_stressbound.receiver.reverse.off"));
    }

    private Component directionText(boolean reversed) {
        return Component.translatable(reversed
            ? "gui.create_stressbound.receiver.direction.counterclockwise"
            : "gui.create_stressbound.receiver.direction.clockwise");
    }

    private StressReceiverBlockEntity getReceiver() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        BlockEntity be = mc.level.getBlockEntity(blockPos);
        return be instanceof StressReceiverBlockEntity r ? r : null;
    }

    private static String format(float v) {
        if (Math.abs(v - Math.round(v)) < 0.001F) return Integer.toString(Math.round(v));
        return String.format(java.util.Locale.ROOT, "%.2f", v);
    }

    private static class DirectionIconButton extends BrassButton {
        private boolean reversed;

        private DirectionIconButton(Button.Builder builder) {
            super(builder);
        }

        private void setReversed(boolean reversed) {
            this.reversed = reversed;
        }

        @Override
        protected void renderContent(GuiGraphics g, int x, int y, int width, int height) {
            ResourceLocation icon = reversed ? COUNTERCLOCKWISE_ICON : CLOCKWISE_ICON;
            g.blit(icon, x + (width - 16) / 2, y + (height - 16) / 2, 0, 0, 16, 16, 16, 16);
        }
    }
}
