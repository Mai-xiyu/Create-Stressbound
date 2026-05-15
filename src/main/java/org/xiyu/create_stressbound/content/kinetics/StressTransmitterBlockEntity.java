package org.xiyu.create_stressbound.content.kinetics;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.xiyu.create_stressbound.StressboundConfig;
import org.xiyu.create_stressbound.content.link.LinkAnchor;
import org.xiyu.create_stressbound.content.link.ReceiverStatus;
import org.xiyu.create_stressbound.content.link.StressLinkColors;
import org.xiyu.create_stressbound.content.link.StressLinkSavedData;
import org.xiyu.create_stressbound.content.link.StressLinkService;
import org.xiyu.create_stressbound.registry.StressboundBlockEntities;

public class StressTransmitterBlockEntity extends KineticBlockEntity implements MenuProvider {
    private static final int CREATIVE_SOURCE_STRESS_BUDGET = Integer.MAX_VALUE / 4;
    public static final String ENDPOINT_ID_KEY = "EndpointId";
    public static final String LATCHED_SPEED_KEY = "LatchedSpeed";
    public static final String LATCHED_AVAILABLE_STRESS_KEY = "LatchedAvailableStress";
    public static final String LATCHED_POWERED_DISABLED_KEY = "LatchedPoweredDisabled";
    public static final String LATCHED_REMOTE_LOOP_KEY = "LatchedRemoteLoop";

    private UUID endpointId;

    // Client-synced receiver positions for visual rendering
    private List<BlockPos> linkedReceiverPositions = Collections.emptyList();
    private List<LinkedReceiverInfo> linkedReceiverInfos = Collections.emptyList();

    public StressTransmitterBlockEntity(BlockPos pos, BlockState blockState) {
        super(StressboundBlockEntities.STRESS_TRANSMITTER.get(), pos, blockState);
    }

    @Override
    public void initialize() {
        super.initialize();
        endpointId = endpointId == null ? UUID.randomUUID() : endpointId;
        if (level instanceof net.minecraft.server.level.ServerLevel) {
            StressLinkService.refreshTransmitterAnchor(this);
            refreshLinkedReceiverPositions();
        }
    }

    public float getSourceSpeed() {
        return getTheoreticalSpeed();
    }

    public float getAvailableStressUnits() {
        return Math.max(capacity - stress, 0.0F);
    }

    public int getRedstoneSignal() {
        if (level == null || !StressboundConfig.transmitterPoweredStops) {
            return 0;
        }
        return StressboundConfig.clampRedstoneSignal(level.getBestNeighborSignal(worldPosition));
    }

    public float getRedstoneOutputScale() {
        return StressboundConfig.redstoneOutputScale(getRedstoneSignal());
    }

    public float getControlledSourceSpeed() {
        return getSourceSpeed() * getRedstoneOutputScale();
    }

    public int getControlledAvailableStressBudget() {
        return StressboundConfig.scaleStressByRedstone(getAvailableStressBudget(), getRedstoneSignal());
    }

    public boolean isPoweredDisabled() {
        return getRedstoneSignal() >= 15;
    }

    public boolean isRemoteLoopSource() {
        return hasNetwork() && getOrCreateNetwork().members.keySet().stream()
            .anyMatch(blockEntity -> blockEntity != this && blockEntity instanceof StressReceiverBlockEntity);
    }

    public List<BlockPos> getLinkedReceiverPositions() {
        return linkedReceiverPositions;
    }

    public List<LinkedReceiverInfo> getLinkedReceiverInfos() {
        return linkedReceiverInfos;
    }

    public void refreshLinkedReceiverVisuals() {
        if (level == null || level.isClientSide) {
            return;
        }
        refreshLinkedReceiverInfo();
        setChanged();
        sendData();
    }

    private void refreshLinkedReceiverPositions() {
        refreshLinkedReceiverInfo();
    }

    private void refreshLinkedReceiverInfo() {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            linkedReceiverPositions = Collections.emptyList();
            linkedReceiverInfos = Collections.emptyList();
            return;
        }
        LinkAnchor anchor = createAnchor();
        List<org.xiyu.create_stressbound.content.link.StressLinkRecord> records =
            StressLinkSavedData.get(serverLevel.getServer()).findByTransmitter(anchor);
        List<BlockPos> positions = new ArrayList<>();
        List<LinkedReceiverInfo> infos = new ArrayList<>();
        for (org.xiyu.create_stressbound.content.link.StressLinkRecord record : records) {
            LinkAnchor receiver = record.receiver();
            if (receiver.dimensionKey().equals(level.dimension())) {
                positions.add(receiver.pos());
            }
            infos.add(new LinkedReceiverInfo(
                record.id(),
                receiver.pos(),
                receiver.dimensionKey(),
                record.requestedStress(),
                StressLinkColors.normalize(record.color()),
                resolveReceiverStatus(serverLevel.getServer(), receiver)
            ));
        }
        linkedReceiverPositions = Collections.unmodifiableList(positions);
        linkedReceiverInfos = Collections.unmodifiableList(infos);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.translatable("goggle.create_stressbound.header")
            .withStyle(ChatFormatting.GOLD));
        if (getSourceSpeed() == 0.0F) {
            tooltip.add(Component.translatable("goggle.create_stressbound.transmitter.no_input")
                .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("goggle.create_stressbound.transmitter.speed", format(getControlledSourceSpeed()))
                .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("goggle.create_stressbound.transmitter.available",
                hasCreativeSource() && getRedstoneSignal() == 0
                    ? Component.translatable("goggle.create_stressbound.value.creative")
                    : Component.literal(format(getControlledAvailableStressBudget()) + " SU"))
            .withStyle(ChatFormatting.AQUA));
        if (!hasCreativeSource()) {
            tooltip.add(Component.translatable("goggle.create_stressbound.transmitter.network",
                    format(Math.round(stress)), format(Math.round(capacity)))
                .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (getRedstoneSignal() > 0) {
            tooltip.add(Component.translatable("goggle.create_stressbound.transmitter.redstone_scale",
                    getRedstoneSignal(), Math.round(getRedstoneOutputScale() * 100.0F))
                .withStyle(isPoweredDisabled() ? ChatFormatting.RED : ChatFormatting.YELLOW));
        }
        if (isPoweredDisabled()) {
            tooltip.add(Component.translatable("goggle.create_stressbound.transmitter.powered_disabled")
                .withStyle(ChatFormatting.RED));
        }
        if (isRemoteLoopSource()) {
            tooltip.add(Component.translatable("goggle.create_stressbound.transmitter.remote_loop")
                .withStyle(ChatFormatting.RED));
        }
        if (isPlayerSneaking) {
            tooltip.add(Component.translatable("goggle.create_stressbound.endpoint", getEndpointId().toString())
                .withStyle(ChatFormatting.DARK_GRAY));
        }
        return true;
    }

    public UUID getEndpointId() {
        if (endpointId == null) {
            endpointId = UUID.randomUUID();
        }
        return endpointId;
    }

    public LinkAnchor createAnchor() {
        return LinkAnchor.staticBlock(level.dimension(), worldPosition, getEndpointId());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.create_stressbound.transmitter.title");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new org.xiyu.create_stressbound.client.gui.TransmitterMenu(containerId, playerInventory, worldPosition);
    }

    public static UUID getEndpointIdFromTag(CompoundTag tag) {
        return tag.hasUUID(ENDPOINT_ID_KEY) ? tag.getUUID(ENDPOINT_ID_KEY) : null;
    }

    public static float getLatchedSpeed(CompoundTag tag) {
        return tag.getFloat(LATCHED_SPEED_KEY);
    }

    public static int getLatchedAvailableStress(CompoundTag tag) {
        return tag.getInt(LATCHED_AVAILABLE_STRESS_KEY);
    }

    public static boolean isLatchedPoweredDisabled(CompoundTag tag) {
        return tag.getBoolean(LATCHED_POWERED_DISABLED_KEY);
    }

    public static boolean isLatchedRemoteLoop(CompoundTag tag) {
        return tag.getBoolean(LATCHED_REMOTE_LOOP_KEY);
    }

    public int getAvailableStressBudget() {
        if (hasCreativeSource()) {
            return CREATIVE_SOURCE_STRESS_BUDGET;
        }
        return Math.max(Math.round(getAvailableStressUnits()), 0);
    }

    public float getNetworkStress() {
        return stress;
    }

    public float getNetworkCapacity() {
        return capacity;
    }

    private boolean hasCreativeSource() {
        return hasNetwork() && getOrCreateNetwork().sources.keySet().stream()
            .anyMatch(CreativeMotorBlockEntity.class::isInstance);
    }

    @Override
    public float calculateStressApplied() {
        return 0.0F;
    }

    private static String format(float value) {
        if (Math.abs(value - Math.round(value)) < 0.001F) {
            return Integer.toString(Math.round(value));
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static String format(int value) {
        return Integer.toString(value);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putUUID(ENDPOINT_ID_KEY, getEndpointId());
        tag.putFloat(LATCHED_SPEED_KEY, getControlledSourceSpeed());
        tag.putInt(LATCHED_AVAILABLE_STRESS_KEY, getControlledAvailableStressBudget());
        tag.putBoolean(LATCHED_POWERED_DISABLED_KEY, isPoweredDisabled());
        tag.putBoolean(LATCHED_REMOTE_LOOP_KEY, isRemoteLoopSource());

        if (clientPacket) {
            ListTag posList = new ListTag();
            for (BlockPos pos : linkedReceiverPositions) {
                posList.add(LongTag.valueOf(pos.asLong()));
            }
            tag.put("LinkedReceivers", posList);

            ListTag infoList = new ListTag();
            for (LinkedReceiverInfo info : linkedReceiverInfos) {
                CompoundTag infoTag = new CompoundTag();
                infoTag.putUUID("LinkId", info.linkId());
                infoTag.putLong("Pos", info.receiverPos().asLong());
                infoTag.putString("Dimension", info.dimension().location().toString());
                infoTag.putInt("RequestedStress", info.requestedStress());
                infoTag.putInt("Color", StressLinkColors.normalize(info.color()));
                infoTag.putString("Status", info.status().name());
                infoList.add(infoTag);
            }
            tag.put("LinkedReceiverInfos", infoList);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        endpointId = tag.hasUUID(ENDPOINT_ID_KEY) ? tag.getUUID(ENDPOINT_ID_KEY) : endpointId;

        // Read linked receiver positions
        if (tag.contains("LinkedReceivers", Tag.TAG_LIST)) {
            ListTag posList = tag.getList("LinkedReceivers", Tag.TAG_LONG);
            List<BlockPos> positions = new ArrayList<>(posList.size());
            for (Tag t : posList) {
                positions.add(BlockPos.of(((LongTag) t).getAsLong()));
            }
            linkedReceiverPositions = Collections.unmodifiableList(positions);
        } else {
            linkedReceiverPositions = Collections.emptyList();
        }

        if (tag.contains("LinkedReceiverInfos", Tag.TAG_LIST)) {
            ListTag infoList = tag.getList("LinkedReceiverInfos", Tag.TAG_COMPOUND);
            List<LinkedReceiverInfo> infos = new ArrayList<>(infoList.size());
            for (Tag infoValue : infoList) {
                CompoundTag infoTag = (CompoundTag) infoValue;
                infos.add(new LinkedReceiverInfo(
                    infoTag.getUUID("LinkId"),
                    BlockPos.of(infoTag.getLong("Pos")),
                    parseDimension(infoTag.getString("Dimension")),
                    infoTag.getInt("RequestedStress"),
                    StressLinkColors.normalize(infoTag.getInt("Color")),
                    parseStatus(infoTag.getString("Status"))
                ));
            }
            linkedReceiverInfos = Collections.unmodifiableList(infos);
        } else {
            linkedReceiverInfos = Collections.emptyList();
        }
    }

    private ReceiverStatus resolveReceiverStatus(net.minecraft.server.MinecraftServer server, LinkAnchor receiver) {
        if (!receiver.isStaticBlock()) {
            return ReceiverStatus.IDLE;
        }
        net.minecraft.server.level.ServerLevel serverLevel = server.getLevel(receiver.dimensionKey());
        if (serverLevel == null) {
            return ReceiverStatus.RECEIVER_UNLOADED;
        }
        if (!serverLevel.isLoaded(receiver.pos())) {
            return ReceiverStatus.RECEIVER_UNLOADED;
        }
        BlockEntity blockEntity = serverLevel.getBlockEntity(receiver.pos());
        if (blockEntity instanceof StressReceiverBlockEntity receiverBlockEntity) {
            return receiverBlockEntity.getStatus();
        }
        return ReceiverStatus.INVALID_RECEIVER;
    }

    private static ResourceKey<Level> parseDimension(String dimension) {
        return ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION,
            net.minecraft.resources.ResourceLocation.parse(dimension));
    }

    private static ReceiverStatus parseStatus(String status) {
        try {
            return ReceiverStatus.valueOf(status);
        } catch (IllegalArgumentException ignored) {
            return ReceiverStatus.IDLE;
        }
    }

    public record LinkedReceiverInfo(UUID linkId, BlockPos receiverPos, ResourceKey<Level> dimension,
                                     int requestedStress, int color, ReceiverStatus status) {
    }
}
