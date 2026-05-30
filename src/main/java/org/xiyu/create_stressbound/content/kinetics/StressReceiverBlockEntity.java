package org.xiyu.create_stressbound.content.kinetics;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.KineticNetwork;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.xiyu.create_stressbound.StressboundConfig;
import org.xiyu.create_stressbound.content.link.LinkAnchor;
import org.xiyu.create_stressbound.content.link.ReceiverStatus;
import org.xiyu.create_stressbound.content.link.StressLinkColors;
import org.xiyu.create_stressbound.content.link.StressLinkSavedData;
import org.xiyu.create_stressbound.content.link.StressLinkService;
import org.xiyu.create_stressbound.registry.StressboundBlockEntities;

public class StressReceiverBlockEntity extends GeneratingKineticBlockEntity implements MenuProvider {
    public static final String ENDPOINT_ID_KEY = "EndpointId";
    private static final float MIN_RUNTIME_SPEED = 0.01F;
    private static final int LEGACY_DEFAULT_REQUESTED_STRESS = 256;
    private static final int UNSET_REQUESTED_STRESS = 0;
    private static final String LINK_ID_KEY = "LinkId";
    private static final String REQUESTED_STRESS_KEY = "RequestedStress";
    private static final String STATUS_KEY = "ReceiverStatus";
    private static final String TRANSMITTED_SPEED_KEY = "TransmittedSpeed";
    private static final String GRANTED_STRESS_KEY = "GrantedStress";
    private static final String REVERSE_OUTPUT_KEY = "ReverseOutput";
    private static final String TRANSMITTER_POS_KEY = "TransmitterPos";
    private static final String TRANSMITTER_DIM_KEY = "TransmitterDim";
    private static final String TRANSMITTER_ENDPOINT_KEY = "TransmitterEndpoint";
    private static final String TRANSMITTER_MOVING_KEY = "TransmitterMoving";
    private static final String LINK_COLOR_KEY = "LinkColor";

    private UUID endpointId;
    private UUID linkId;
    private int requestedStress = UNSET_REQUESTED_STRESS;
    private float transmittedSpeed;
    private int grantedStress;
    private ReceiverStatus status = ReceiverStatus.IDLE;
    private boolean reverseOutput = false;

    // Client-synced transmitter position for visual rendering
    private BlockPos transmitterPos = BlockPos.ZERO;
    private ResourceKey<Level> transmitterDimension;
    private UUID transmitterEndpointId;
    private boolean transmitterMoving;
    private int linkColor = StressLinkColors.DEFAULT;

    public StressReceiverBlockEntity(BlockPos pos, BlockState blockState) {
        super(StressboundBlockEntities.STRESS_RECEIVER.get(), pos, blockState);
    }

    @Override
    public void initialize() {
        super.initialize();
        endpointId = endpointId == null ? UUID.randomUUID() : endpointId;
        transmittedSpeed = 0.0F;
        grantedStress = 0;
        lastCapacityProvided = 0.0F;
        lastStressApplied = 0.0F;
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
        if (isRuntimeStopped(transmittedSpeed)) {
            return 0.0F;
        }
        float speed = reverseOutput ? -transmittedSpeed : transmittedSpeed;
        Direction facing = getBlockState().getValue(StressReceiverBlock.FACING);
        return convertToDirection(speed, facing);
    }

    @Override
    public float calculateAddedStressCapacity() {
        float capacity = grantedStress <= 0 || isRuntimeStopped(transmittedSpeed)
            ? 0.0F
            : grantedStress / Math.abs(transmittedSpeed);
        lastCapacityProvided = capacity;
        return capacity;
    }

    @Override
    public float calculateStressApplied() {
        lastStressApplied = 0.0F;
        return 0.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (shouldRefreshNativeOverstressState()) {
            refreshNativeOverstressState();
        }
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
        if (getRedstoneSignal() > 0) {
            tooltip.add(Component.translatable("goggle.create_stressbound.receiver.redstone_scale",
                    getRedstoneSignal(), Math.round(getRedstoneOutputScale() * 100.0F))
                .withStyle(isPoweredDisabled() ? ChatFormatting.RED : ChatFormatting.YELLOW));
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
        applyRuntime(runtimeLinkId, runtimeSpeed, runtimeGrantedStress, runtimeStatus, null);
    }

    public void applyRuntime(UUID runtimeLinkId, float runtimeSpeed, int runtimeGrantedStress,
                             ReceiverStatus runtimeStatus, LinkAnchor transmitterVisualAnchor) {
        UUID nextLinkId = runtimeLinkId != null ? runtimeLinkId : linkId;
        UUID previousLinkId = linkId;
        ReceiverStatus previousStatus = status;
        float previousGeneratedSpeed = getGeneratedSpeed();
        float normalizedSpeed = normalizeRuntimeSpeed(runtimeSpeed);
        int normalizedGrantedStress = normalizedSpeed == 0.0F ? 0 : Math.max(runtimeGrantedStress, 0);
        ReceiverStatus normalizedStatus = normalizedSpeed == 0.0F && runtimeStatus == ReceiverStatus.ACTIVE
            ? ReceiverStatus.IDLE
            : runtimeStatus;
        boolean enteredSpeedDeadzone = transmittedSpeed != 0.0F && normalizedSpeed == 0.0F;
        boolean changed = transmittedSpeed != normalizedSpeed || grantedStress != normalizedGrantedStress || status != normalizedStatus;
        boolean statusChanged = previousStatus != normalizedStatus;
        boolean linkChanged = !Objects.equals(previousLinkId, nextLinkId);
        linkId = nextLinkId;
        transmittedSpeed = normalizedSpeed;
        grantedStress = normalizedGrantedStress;
        status = normalizedStatus;
        float nextGeneratedSpeed = getGeneratedSpeed();
        boolean generatedDirectionChanged = hasActiveRemoteRuntime(normalizedSpeed, normalizedGrantedStress, normalizedStatus)
            && previousGeneratedSpeed != 0.0F
            && nextGeneratedSpeed != 0.0F
            && Math.signum(previousGeneratedSpeed) != Math.signum(nextGeneratedSpeed);

        if (level != null && !level.isClientSide) {
            changed |= transmitterVisualAnchor == null
                ? refreshTransmitterVisualInfo()
                : setTransmitterVisualInfo(transmitterVisualAnchor);
            changed |= refreshLinkColor();
        }

        if ((changed || enteredSpeedDeadzone) && level != null && !level.isClientSide) {
            updateGeneratedRotation();
            if (generatedDirectionChanged) {
                refreshNativeOverstressState();
            }
            setChanged();
            sendData();
            if (statusChanged || linkChanged) {
                StressLinkService.refreshTransmitterVisualsForLink(
                    ((net.minecraft.server.level.ServerLevel) level).getServer(),
                    nextLinkId != null ? nextLinkId : previousLinkId
                );
            }
        }
    }

    public void clearLink() {
        linkId = null;
        requestedStress = defaultRequestedStress();
        linkColor = StressLinkColors.DEFAULT;
        applyRuntime(null, 0.0F, 0, ReceiverStatus.IDLE);
        if (level != null && !level.isClientSide) {
            setChanged();
            sendData();
        }
    }

    private static String format(float value) {
        if (Math.abs(value - Math.round(value)) < 0.001F) {
            return Integer.toString(Math.round(value));
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static boolean isRuntimeStopped(float speed) {
        return Math.abs(speed) < MIN_RUNTIME_SPEED;
    }

    private static float normalizeRuntimeSpeed(float speed) {
        if (!Float.isFinite(speed) || isRuntimeStopped(speed)) {
            return 0.0F;
        }
        return speed;
    }

    private boolean shouldRefreshNativeOverstressState() {
        return level != null
            && !level.isClientSide
            && isOverStressed()
            && hasActiveRemoteRuntime(transmittedSpeed, grantedStress, status);
    }

    private boolean hasActiveRemoteRuntime(float speed, int stress, ReceiverStatus status) {
        return status == ReceiverStatus.ACTIVE && stress > 0 && !isRuntimeStopped(speed);
    }

    private void refreshNativeOverstressState() {
        if (level == null || level.isClientSide || !hasNetwork()) {
            return;
        }
        KineticNetwork network = getOrCreateNetwork();
        updateFromNetwork(network.calculateCapacity(), network.calculateStress(), network.getSize());
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
        return requestedStress > UNSET_REQUESTED_STRESS
            ? StressboundConfig.clampRequestedStress(requestedStress)
            : defaultRequestedStress();
    }

    public void setRequestedStress(int requestedStress) {
        this.requestedStress = StressboundConfig.clampRequestedStress(requestedStress);
        setChanged();
        if (level != null && !level.isClientSide) {
            sendData();
        }
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

    public int getLinkColor() {
        return StressLinkColors.normalize(linkColor);
    }

    public int getRedstoneSignal() {
        if (level == null || !StressboundConfig.receiverPoweredStops) {
            return 0;
        }
        return StressboundConfig.clampRedstoneSignal(level.getBestNeighborSignal(worldPosition));
    }

    public float getRedstoneOutputScale() {
        return StressboundConfig.redstoneOutputScale(getRedstoneSignal());
    }

    public float scaleIncomingSpeed(float speed) {
        return speed * getRedstoneOutputScale();
    }

    public int scaleGrantedStress(int stress) {
        return StressboundConfig.scaleStressByRedstone(stress, getRedstoneSignal());
    }

    public boolean isPoweredDisabled() {
        return getRedstoneSignal() >= 15;
    }

    public boolean isReverseOutput() {
        return reverseOutput;
    }

    public void setReverseOutput(boolean reverseOutput) {
        if (this.reverseOutput != reverseOutput) {
            this.reverseOutput = reverseOutput;
            if (level != null && !level.isClientSide) {
                updateGeneratedRotation();
                setChanged();
                sendData();
            }
        }
    }

    public void toggleReverseOutput() {
        setReverseOutput(!reverseOutput);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.create_stressbound.receiver.title");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new org.xiyu.create_stressbound.client.gui.ReceiverMenu(containerId, playerInventory, worldPosition);
    }

    public void refreshClientLinkVisuals() {
        if (level == null || level.isClientSide) {
            return;
        }
        boolean changed = refreshTransmitterVisualInfo();
        changed |= refreshLinkColor();
        if (changed) {
            setChanged();
            sendData();
        }
    }

    private boolean refreshTransmitterVisualInfo() {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel) || linkId == null) {
            return clearTransmitterVisualInfo();
        }
        Optional<org.xiyu.create_stressbound.content.link.StressLinkRecord> record =
            StressLinkSavedData.get(serverLevel.getServer()).get(linkId);
        if (record.isEmpty()) {
            return clearTransmitterVisualInfo();
        }
        return setTransmitterVisualInfo(record.get().transmitter());
    }

    private boolean refreshLinkColor() {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel) || linkId == null) {
            int previous = linkColor;
            linkColor = StressLinkColors.DEFAULT;
            return previous != linkColor;
        }
        Optional<org.xiyu.create_stressbound.content.link.StressLinkRecord> record =
            StressLinkSavedData.get(serverLevel.getServer()).get(linkId);
        int nextColor = record.map(value -> StressLinkColors.normalize(value.color()))
            .orElse(StressLinkColors.DEFAULT);
        boolean changed = linkColor != nextColor;
        linkColor = nextColor;
        return changed;
    }

    private boolean setTransmitterVisualInfo(LinkAnchor tx) {
        BlockPos nextPos = tx.pos();
        ResourceKey<Level> nextDimension = tx.dimensionKey();
        UUID nextEndpointId = tx.endpointId().orElse(null);
        boolean nextMoving = !tx.isStaticBlock();
        boolean changed = !Objects.equals(transmitterPos, nextPos)
            || !Objects.equals(transmitterDimension, nextDimension)
            || !Objects.equals(transmitterEndpointId, nextEndpointId)
            || transmitterMoving != nextMoving;
        transmitterPos = nextPos;
        transmitterDimension = nextDimension;
        transmitterEndpointId = nextEndpointId;
        transmitterMoving = nextMoving;
        return changed;
    }

    private boolean clearTransmitterVisualInfo() {
        boolean changed = !BlockPos.ZERO.equals(transmitterPos)
            || transmitterDimension != null
            || transmitterEndpointId != null
            || transmitterMoving;
        transmitterPos = BlockPos.ZERO;
        transmitterDimension = null;
        transmitterEndpointId = null;
        transmitterMoving = false;
        return changed;
    }

    public BlockPos getTransmitterPos() {
        return transmitterPos;
    }

    public ResourceKey<Level> getTransmitterDimension() {
        return transmitterDimension;
    }

    public UUID getTransmitterEndpointId() {
        return transmitterEndpointId;
    }

    public boolean isTransmitterMoving() {
        return transmitterMoving;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putUUID(ENDPOINT_ID_KEY, getEndpointId());
        if (linkId != null) {
            tag.putUUID(LINK_ID_KEY, linkId);
        }
        tag.putInt(REQUESTED_STRESS_KEY, getRequestedStress());
        tag.putString(STATUS_KEY, status.name());
        tag.putFloat(TRANSMITTED_SPEED_KEY, transmittedSpeed);
        tag.putInt(GRANTED_STRESS_KEY, grantedStress);
        tag.putBoolean(REVERSE_OUTPUT_KEY, reverseOutput);

        if (clientPacket && transmitterPos != null && !transmitterPos.equals(BlockPos.ZERO)) {
            tag.putLong(TRANSMITTER_POS_KEY, transmitterPos.asLong());
            if (transmitterDimension != null) {
                tag.putString(TRANSMITTER_DIM_KEY, transmitterDimension.location().toString());
            }
            if (transmitterEndpointId != null) {
                tag.putUUID(TRANSMITTER_ENDPOINT_KEY, transmitterEndpointId);
            }
            tag.putBoolean(TRANSMITTER_MOVING_KEY, transmitterMoving);
        }
        if (clientPacket && linkId != null) {
            tag.putInt(LINK_COLOR_KEY, getLinkColor());
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        endpointId = tag.hasUUID(ENDPOINT_ID_KEY) ? tag.getUUID(ENDPOINT_ID_KEY) : endpointId;
        linkId = tag.hasUUID(LINK_ID_KEY) ? tag.getUUID(LINK_ID_KEY) : null;
        requestedStress = tag.contains(REQUESTED_STRESS_KEY)
            ? Math.max(tag.getInt(REQUESTED_STRESS_KEY), UNSET_REQUESTED_STRESS)
            : UNSET_REQUESTED_STRESS;
        status = parseStatus(tag);
        transmittedSpeed = normalizeRuntimeSpeed(tag.getFloat(TRANSMITTED_SPEED_KEY));
        grantedStress = transmittedSpeed == 0.0F ? 0 : Math.max(tag.getInt(GRANTED_STRESS_KEY), 0);
        reverseOutput = tag.getBoolean(REVERSE_OUTPUT_KEY);

        if (tag.contains(TRANSMITTER_POS_KEY)) {
            transmitterPos = BlockPos.of(tag.getLong(TRANSMITTER_POS_KEY));
        } else {
            transmitterPos = BlockPos.ZERO;
        }
        transmitterDimension = parseTransmitterDimension(tag);
        transmitterEndpointId = tag.hasUUID(TRANSMITTER_ENDPOINT_KEY) ? tag.getUUID(TRANSMITTER_ENDPOINT_KEY) : null;
        transmitterMoving = tag.getBoolean(TRANSMITTER_MOVING_KEY);
        linkColor = tag.contains(LINK_COLOR_KEY)
            ? StressLinkColors.normalize(tag.getInt(LINK_COLOR_KEY))
            : StressLinkColors.DEFAULT;
    }

    private static ReceiverStatus parseStatus(CompoundTag tag) {
        if (!tag.contains(STATUS_KEY)) {
            return ReceiverStatus.IDLE;
        }
        try {
            return ReceiverStatus.valueOf(tag.getString(STATUS_KEY));
        } catch (IllegalArgumentException ignored) {
            return ReceiverStatus.IDLE;
        }
    }

    private static ResourceKey<Level> parseTransmitterDimension(CompoundTag tag) {
        if (!tag.contains(TRANSMITTER_DIM_KEY)) {
            return null;
        }
        try {
            return ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                ResourceLocation.parse(tag.getString(TRANSMITTER_DIM_KEY)));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static int defaultRequestedStress() {
        int configured = StressboundConfig.defaultRequestedStress > 0
            ? StressboundConfig.defaultRequestedStress
            : LEGACY_DEFAULT_REQUESTED_STRESS;
        return StressboundConfig.clampRequestedStress(configured);
    }
}
