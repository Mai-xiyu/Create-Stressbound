package org.xiyu.create_stressbound.content.kinetics;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.xiyu.create_stressbound.StressboundConfig;
import org.xiyu.create_stressbound.content.link.LinkAnchor;
import org.xiyu.create_stressbound.content.link.StressLinkService;
import org.xiyu.create_stressbound.registry.StressboundBlockEntities;

public class StressTransmitterBlockEntity extends KineticBlockEntity {
    private static final int CREATIVE_SOURCE_STRESS_BUDGET = Integer.MAX_VALUE / 4;
    public static final String ENDPOINT_ID_KEY = "EndpointId";
    public static final String LATCHED_SPEED_KEY = "LatchedSpeed";
    public static final String LATCHED_AVAILABLE_STRESS_KEY = "LatchedAvailableStress";
    public static final String LATCHED_POWERED_DISABLED_KEY = "LatchedPoweredDisabled";
    public static final String LATCHED_REMOTE_LOOP_KEY = "LatchedRemoteLoop";

    private UUID endpointId;

    public StressTransmitterBlockEntity(BlockPos pos, BlockState blockState) {
        super(StressboundBlockEntities.STRESS_TRANSMITTER.get(), pos, blockState);
    }

    @Override
    public void initialize() {
        super.initialize();
        endpointId = endpointId == null ? UUID.randomUUID() : endpointId;
        if (level instanceof net.minecraft.server.level.ServerLevel) {
            StressLinkService.refreshTransmitterAnchor(this);
        }
    }

    public float getSourceSpeed() {
        return getTheoreticalSpeed();
    }

    public float getAvailableStressUnits() {
        return Math.max(capacity - stress, 0.0F);
    }

    public boolean isPoweredDisabled() {
        return level != null && StressboundConfig.transmitterPoweredStops && level.hasNeighborSignal(worldPosition);
    }

    public boolean isRemoteLoopSource() {
        return hasNetwork() && getOrCreateNetwork().members.keySet().stream()
            .anyMatch(blockEntity -> blockEntity != this && blockEntity instanceof StressReceiverBlockEntity);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.translatable("goggle.create_stressbound.header")
            .withStyle(ChatFormatting.GOLD));
        if (getSourceSpeed() == 0.0F) {
            tooltip.add(Component.translatable("goggle.create_stressbound.transmitter.no_input")
                .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("goggle.create_stressbound.transmitter.speed", format(getSourceSpeed()))
                .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("goggle.create_stressbound.transmitter.available",
                hasCreativeSource()
                    ? Component.translatable("goggle.create_stressbound.value.creative")
                    : Component.literal(format(getAvailableStressBudget()) + " SU"))
            .withStyle(ChatFormatting.AQUA));
        if (!hasCreativeSource()) {
            tooltip.add(Component.translatable("goggle.create_stressbound.transmitter.network",
                    format(Math.round(stress)), format(Math.round(capacity)))
                .withStyle(ChatFormatting.DARK_GRAY));
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
        tag.putFloat(LATCHED_SPEED_KEY, getSourceSpeed());
        tag.putInt(LATCHED_AVAILABLE_STRESS_KEY, getAvailableStressBudget());
        tag.putBoolean(LATCHED_POWERED_DISABLED_KEY, isPoweredDisabled());
        tag.putBoolean(LATCHED_REMOTE_LOOP_KEY, isRemoteLoopSource());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        endpointId = tag.hasUUID(ENDPOINT_ID_KEY) ? tag.getUUID(ENDPOINT_ID_KEY) : endpointId;
    }
}
