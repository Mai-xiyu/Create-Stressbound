package org.xiyu.create_stressbound.content.kinetics;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.xiyu.create_stressbound.StressboundConfig;
import org.xiyu.create_stressbound.content.link.ReceiverStatus;
import org.xiyu.create_stressbound.registry.StressboundBlockEntities;

public class StressReceiverBlockEntity extends GeneratingKineticBlockEntity {
    public static final String ENDPOINT_ID_KEY = "EndpointId";
    private static final String LINK_ID_KEY = "LinkId";
    private static final String REQUESTED_STRESS_KEY = "RequestedStress";
    private static final String STATUS_KEY = "ReceiverStatus";

    private UUID endpointId;
    private UUID linkId;
    private int requestedStress = 256;
    private float transmittedSpeed;
    private int grantedStress;
    private ReceiverStatus status = ReceiverStatus.IDLE;

    public StressReceiverBlockEntity(BlockPos pos, BlockState blockState) {
        super(StressboundBlockEntities.STRESS_RECEIVER.get(), pos, blockState);
    }

    @Override
    public void initialize() {
        super.initialize();
        endpointId = endpointId == null ? UUID.randomUUID() : endpointId;
        transmittedSpeed = 0.0F;
        grantedStress = 0;
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel && linkId != null) {
            if (org.xiyu.create_stressbound.content.link.StressLinkSavedData.get(serverLevel.getServer()).get(linkId).isEmpty()) {
                clearLink();
            }
        }
        if (level instanceof net.minecraft.server.level.ServerLevel) {
            org.xiyu.create_stressbound.content.link.StressLinkService.refreshReceiverAnchor(this);
        }
        updateGeneratedRotation();
    }

    @Override
    public float getGeneratedSpeed() {
        if (transmittedSpeed == 0.0F) {
            return 0.0F;
        }
        Direction facing = getBlockState().getValue(StressReceiverBlock.FACING);
        return convertToDirection(transmittedSpeed, facing);
    }

    @Override
    public float calculateAddedStressCapacity() {
        if (grantedStress <= 0 || transmittedSpeed == 0.0F) {
            return 0.0F;
        }
        return grantedStress / Math.abs(transmittedSpeed);
    }

    @Override
    public float calculateStressApplied() {
        return 0.0F;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.translatable("goggle.create_stressbound.header")
            .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("goggle.create_stressbound.receiver.status",
                Component.translatable(status.translationKey()))
            .withStyle(status == ReceiverStatus.ACTIVE ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        if (linkId == null) {
            tooltip.add(Component.translatable("goggle.create_stressbound.receiver.unlinked")
                .withStyle(ChatFormatting.DARK_GRAY));
        } else if (status == ReceiverStatus.ACTIVE) {
            tooltip.add(Component.translatable("goggle.create_stressbound.receiver.speed", format(transmittedSpeed))
                .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("goggle.create_stressbound.receiver.budget", grantedStress, getRequestedStress())
                .withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("goggle.create_stressbound.receiver.requested", getRequestedStress())
                .withStyle(ChatFormatting.AQUA));
        }
        if (isPoweredDisabled()) {
            tooltip.add(Component.translatable("goggle.create_stressbound.receiver.powered_disabled")
                .withStyle(ChatFormatting.RED));
        }
        if (isPlayerSneaking) {
            tooltip.add(Component.translatable("goggle.create_stressbound.link",
                    linkId == null ? "-" : linkId.toString())
                .withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("goggle.create_stressbound.endpoint", getEndpointId().toString())
                .withStyle(ChatFormatting.DARK_GRAY));
        }
        return true;
    }

    public void applyRuntime(UUID runtimeLinkId, float runtimeSpeed, int runtimeGrantedStress, ReceiverStatus runtimeStatus) {
        UUID nextLinkId = runtimeLinkId != null ? runtimeLinkId : linkId;
        boolean changed = transmittedSpeed != runtimeSpeed || grantedStress != runtimeGrantedStress || status != runtimeStatus;
        linkId = nextLinkId;
        transmittedSpeed = runtimeSpeed;
        grantedStress = runtimeGrantedStress;
        status = runtimeStatus;

        if (changed && level != null && !level.isClientSide) {
            updateGeneratedRotation();
            setChanged();
            sendData();
        }
    }

    public void clearLink() {
        linkId = null;
        requestedStress = StressboundConfig.defaultRequestedStress > 0 ? StressboundConfig.defaultRequestedStress : 256;
        applyRuntime(null, 0.0F, 0, ReceiverStatus.IDLE);
    }

    private static String format(float value) {
        if (Math.abs(value - Math.round(value)) < 0.001F) {
            return Integer.toString(Math.round(value));
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    public UUID getEndpointId() {
        if (endpointId == null) {
            endpointId = UUID.randomUUID();
        }
        return endpointId;
    }

    public static UUID getEndpointIdFromTag(CompoundTag tag) {
        return tag.hasUUID(ENDPOINT_ID_KEY) ? tag.getUUID(ENDPOINT_ID_KEY) : null;
    }

    public org.xiyu.create_stressbound.content.link.LinkAnchor createAnchor() {
        return org.xiyu.create_stressbound.content.link.LinkAnchor.staticBlock(level.dimension(), worldPosition, getEndpointId());
    }

    public UUID getLinkId() {
        return linkId;
    }

    public void setLinkId(UUID linkId) {
        this.linkId = linkId;
        setChanged();
    }

    public int getRequestedStress() {
        return StressboundConfig.clampRequestedStress(requestedStress);
    }

    public void setRequestedStress(int requestedStress) {
        this.requestedStress = StressboundConfig.clampRequestedStress(requestedStress);
        setChanged();
    }

    public ReceiverStatus getStatus() {
        return status;
    }

    public float getTransmittedSpeed() {
        return transmittedSpeed;
    }

    public int getGrantedStress() {
        return grantedStress;
    }

    public boolean isPoweredDisabled() {
        return level != null && StressboundConfig.receiverPoweredStops && level.hasNeighborSignal(worldPosition);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putUUID(ENDPOINT_ID_KEY, getEndpointId());
        if (linkId != null) {
            tag.putUUID(LINK_ID_KEY, linkId);
        }
        tag.putInt(REQUESTED_STRESS_KEY, requestedStress);
        tag.putString(STATUS_KEY, status.name());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        endpointId = tag.hasUUID(ENDPOINT_ID_KEY) ? tag.getUUID(ENDPOINT_ID_KEY) : endpointId;
        linkId = tag.hasUUID(LINK_ID_KEY) ? tag.getUUID(LINK_ID_KEY) : null;
        requestedStress = tag.contains(REQUESTED_STRESS_KEY)
            ? tag.getInt(REQUESTED_STRESS_KEY)
            : (StressboundConfig.defaultRequestedStress > 0 ? StressboundConfig.defaultRequestedStress : 256);
        status = tag.contains(STATUS_KEY) ? ReceiverStatus.valueOf(tag.getString(STATUS_KEY)) : ReceiverStatus.IDLE;
    }
}
