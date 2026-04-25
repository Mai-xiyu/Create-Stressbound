package org.xiyu.create_stressbound.content.link;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.xiyu.create_stressbound.StressboundConfig;
import org.xiyu.create_stressbound.content.kinetics.StressReceiverBlockEntity;
import org.xiyu.create_stressbound.content.kinetics.StressTransmitterBlockEntity;

public final class StressLinkService {
    private StressLinkService() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        MovingEndpointRegistry.get(server).cleanup(server.getTickCount());

        int interval = Math.max(1, StressboundConfig.evaluationIntervalTicks);
        if (server.getTickCount() % interval != 0) {
            return;
        }
        evaluate(server);
    }

    public static BindResult bind(ServerPlayer player, ResourceKey<Level> transmitterDimension,
                                  BlockPos transmitterPos, StressReceiverBlockEntity receiver) {
        MinecraftServer server = player.server;
        ServerLevel transmitterLevel = server.getLevel(transmitterDimension);
        if (transmitterLevel == null) {
            return BindResult.failure("message.create_stressbound.bind.transmitter_dimension_missing");
        }

        if (!StressboundConfig.allowCrossDimensionTransmission && receiver.getLevel() != transmitterLevel) {
            return BindResult.failure("message.create_stressbound.bind.cross_dimension_disabled");
        }

        if (!transmitterLevel.isLoaded(transmitterPos)) {
            return BindResult.failure("message.create_stressbound.bind.transmitter_unloaded");
        }

        BlockEntity transmitterBe = transmitterLevel.getBlockEntity(transmitterPos);
        if (!(transmitterBe instanceof StressTransmitterBlockEntity transmitter)) {
            return BindResult.failure("message.create_stressbound.bind.not_transmitter");
        }

        if (transmitter.isRemoteLoopSource()) {
            return BindResult.failure("message.create_stressbound.bind.remote_loop");
        }

        StressLinkSavedData data = StressLinkSavedData.get(server);
        LinkAnchor transmitterAnchor = transmitter.createAnchor();
        LinkAnchor receiverAnchor = receiver.createAnchor();

        Optional<StressLinkRecord> existingReceiverLink = data.findByReceiver(receiverAnchor);
        int ownerCount = data.countByOwner(player.getUUID());
        if (existingReceiverLink.isEmpty() && ownerCount >= StressboundConfig.maxLinksPerPlayer) {
            return BindResult.failure("message.create_stressbound.bind.player_limit");
        }

        int transmitterLinks = data.findByTransmitter(transmitterAnchor).size();
        if (existingReceiverLink.isEmpty() && transmitterLinks >= StressboundConfig.maxReceiversPerTransmitter) {
            return BindResult.failure("message.create_stressbound.bind.transmitter_limit");
        }

        if (receiver.getLinkId() != null) {
            removeLink(server, receiver.getLinkId());
        } else {
            data.removeByReceiver(receiverAnchor);
        }

        int requestedStress = receiver.getRequestedStress();
        StressLinkRecord record = new StressLinkRecord(
            UUID.randomUUID(),
            player.getUUID(),
            transmitterAnchor,
            receiverAnchor,
            requestedStress,
            receiver.getLevel().getGameTime()
        );
        data.put(record);
        receiver.setLinkId(record.id());
        receiver.setRequestedStress(record.requestedStress());
        receiver.applyRuntime(record.id(), 0.0F, 0, ReceiverStatus.IDLE);
        return BindResult.success(Component.translatable("message.create_stressbound.bind.success", transmitterAnchor.describe())
            .withStyle(ChatFormatting.GREEN));
    }

    public static void refreshTransmitterAnchor(StressTransmitterBlockEntity transmitter) {
        if (!(transmitter.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        LinkAnchor newAnchor = transmitter.createAnchor();
        StressLinkSavedData data = StressLinkSavedData.get(serverLevel.getServer());
        List<StressLinkRecord> replacements = data.all().stream()
            .filter(record -> matchesSameEndpointOrStaticPosition(record.transmitter(), newAnchor))
            .map(record -> record.withTransmitter(newAnchor))
            .toList();
        replacements.forEach(data::put);
    }

    public static void refreshReceiverAnchor(StressReceiverBlockEntity receiver) {
        if (!(receiver.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        LinkAnchor newAnchor = receiver.createAnchor();
        StressLinkSavedData data = StressLinkSavedData.get(serverLevel.getServer());
        if (receiver.getLinkId() != null) {
            data.get(receiver.getLinkId()).ifPresent(record -> data.put(record.withReceiver(newAnchor)));
            return;
        }

        List<StressLinkRecord> replacements = data.all().stream()
            .filter(record -> matchesSameEndpointOrStaticPosition(record.receiver(), newAnchor))
            .map(record -> record.withReceiver(newAnchor))
            .toList();
        replacements.forEach(data::put);
    }

    public static boolean clearLinkAt(ServerLevel level, BlockPos pos, ServerPlayer actor) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof StressReceiverBlockEntity receiver)) {
            return false;
        }

        StressLinkSavedData data = StressLinkSavedData.get(level.getServer());
        boolean removed = false;
        if (receiver.getLinkId() != null) {
            removed = removeLink(level.getServer(), receiver.getLinkId());
        }
        if (!removed) {
            removed = data.removeByReceiver(receiver.createAnchor());
        }
        if (!removed) {
            removed = data.removeByReceiver(LinkAnchor.staticBlock(level.dimension(), pos));
        }

        receiver.clearLink();
        if (removed && actor != null) {
            actor.sendSystemMessage(Component.translatable("message.create_stressbound.receiver.link_cleared")
                .withStyle(ChatFormatting.YELLOW));
        }
        return removed;
    }

    public static boolean removeLink(MinecraftServer server, UUID linkId) {
        StressLinkSavedData data = StressLinkSavedData.get(server);
        Optional<StressLinkRecord> record = data.get(linkId);
        if (record.isEmpty()) {
            return false;
        }

        data.remove(linkId);
        getStaticReceiver(server, record.get().receiver()).ifPresent(StressReceiverBlockEntity::clearLink);
        return true;
    }

    public static int removeLinksByOwner(MinecraftServer server, UUID owner) {
        StressLinkSavedData data = StressLinkSavedData.get(server);
        List<UUID> ids = data.all().stream()
            .filter(record -> record.owner().equals(owner))
            .map(StressLinkRecord::id)
            .toList();

        ids.forEach(id -> removeLink(server, id));
        return ids.size();
    }

    public static Optional<StressLinkRecord> getLink(MinecraftServer server, UUID linkId) {
        return StressLinkSavedData.get(server).get(linkId);
    }

    public static Optional<StressLinkRecord> setRequestedStress(MinecraftServer server, UUID linkId, int requestedStress) {
        StressLinkSavedData data = StressLinkSavedData.get(server);
        Optional<StressLinkRecord> existing = data.get(linkId);
        if (existing.isEmpty()) {
            return Optional.empty();
        }

        int clampedStress = StressboundConfig.clampRequestedStress(requestedStress);
        StressLinkRecord updated = existing.get().withRequestedStress(clampedStress);
        data.put(updated);
        getStaticReceiver(server, updated.receiver()).ifPresent(receiver -> receiver.setRequestedStress(clampedStress));
        return Optional.of(updated);
    }

    public static int setAllRequestedStress(MinecraftServer server, int requestedStress) {
        StressLinkSavedData data = StressLinkSavedData.get(server);
        List<UUID> ids = data.all().stream()
            .map(StressLinkRecord::id)
            .toList();
        ids.forEach(id -> setRequestedStress(server, id, requestedStress));
        return ids.size();
    }

    private static void evaluate(MinecraftServer server) {
        StressLinkSavedData data = StressLinkSavedData.get(server);
        Map<String, List<StressLinkRecord>> groupedByTransmitter = new LinkedHashMap<>();
        List<UUID> brokenLinks = new ArrayList<>();

        for (StressLinkRecord record : data.all()) {
            groupedByTransmitter.computeIfAbsent(record.transmitter().key(), ignored -> new ArrayList<>()).add(record);
        }

        for (List<StressLinkRecord> group : groupedByTransmitter.values()) {
            evaluateGroup(server, group, brokenLinks);
        }

        for (UUID brokenLink : brokenLinks) {
            data.remove(brokenLink);
        }
    }

    private static void evaluateGroup(MinecraftServer server, List<StressLinkRecord> records, List<UUID> brokenLinks) {
        if (records.isEmpty()) {
            return;
        }

        TransmitterRuntime transmitter = resolveTransmitter(server, records.getFirst().transmitter());
        if (transmitter.failureStatus().isPresent()) {
            ReceiverStatus status = transmitter.failureStatus().get();
            records.forEach(record -> applyToStaticReceiver(server, record, 0.0F, 0, status, brokenLinks));
            if (status == ReceiverStatus.INVALID_TRANSMITTER && records.getFirst().transmitter().endpointId().isEmpty()) {
                records.forEach(record -> brokenLinks.add(record.id()));
            }
            return;
        }

        if (transmitter.poweredDisabled()) {
            records.forEach(record -> applyToStaticReceiver(server, record, 0.0F, 0, ReceiverStatus.TRANSMITTER_DISABLED, brokenLinks));
            return;
        }

        if (transmitter.remoteLoop()) {
            records.forEach(record -> applyToStaticReceiver(server, record, 0.0F, 0, ReceiverStatus.REMOTE_LOOP, brokenLinks));
            return;
        }

        if (transmitter.speed() == 0.0F) {
            records.forEach(record -> applyToStaticReceiver(server, record, 0.0F, 0, ReceiverStatus.IDLE, brokenLinks));
            return;
        }

        List<ActiveReceiver> activeReceivers = new ArrayList<>();
        for (StressLinkRecord record : records) {
            ReceiverRuntime receiver = resolveReceiver(server, record.receiver());
            if (receiver.staticReceiver().isPresent()) {
                StressReceiverBlockEntity receiverBlockEntity = receiver.staticReceiver().get();
                if (receiverBlockEntity.isPoweredDisabled()) {
                    receiverBlockEntity.applyRuntime(record.id(), 0.0F, 0, ReceiverStatus.RECEIVER_DISABLED);
                    continue;
                }
                activeReceivers.add(new ActiveReceiver(record, receiverBlockEntity));
                continue;
            }

            if (receiver.failureStatus().orElse(null) == ReceiverStatus.INVALID_RECEIVER && record.receiver().endpointId().isEmpty()) {
                brokenLinks.add(record.id());
            }
        }

        if (activeReceivers.isEmpty()) {
            return;
        }

        long totalReservedStress = activeReceivers.stream()
            .mapToLong(active -> StressboundConfig.clampRequestedStress(active.record().requestedStress()))
            .sum();

        if (StressboundConfig.strictOverloadMode && totalReservedStress > transmitter.availableStress()) {
            activeReceivers.forEach(active -> active.receiver().applyRuntime(active.record().id(), 0.0F, 0, ReceiverStatus.OVERLOADED));
            return;
        }

        long remainingStress = transmitter.availableStress();
        for (ActiveReceiver activeReceiver : activeReceivers) {
            int reservedStress = StressboundConfig.clampRequestedStress(activeReceiver.record().requestedStress());
            if (reservedStress > remainingStress) {
                activeReceiver.receiver().applyRuntime(activeReceiver.record().id(), 0.0F, 0, ReceiverStatus.OVERLOADED);
                continue;
            }
            activeReceiver.receiver().applyRuntime(activeReceiver.record().id(), transmitter.speed(), reservedStress, ReceiverStatus.ACTIVE);
            remainingStress -= reservedStress;
        }
    }

    private static TransmitterRuntime resolveTransmitter(MinecraftServer server, LinkAnchor anchor) {
        Optional<UUID> endpointId = anchor.endpointId();
        if (endpointId.isPresent()) {
            Optional<MovingEndpointRegistry.RuntimeEndpoint> moving = MovingEndpointRegistry.get(server).get(endpointId.get())
                .filter(MovingEndpointRegistry.RuntimeEndpoint::isTransmitter);
            if (moving.isPresent()) {
                MovingEndpointRegistry.RuntimeEndpoint endpoint = moving.get();
                return TransmitterRuntime.active(endpoint.latchedSpeed(), endpoint.latchedAvailableStress(), endpoint.poweredDisabled(), endpoint.remoteLoop());
            }
        }

        Optional<StressTransmitterBlockEntity> staticTransmitter = getStaticTransmitter(server, anchor);
        if (staticTransmitter.isEmpty()) {
            return TransmitterRuntime.failure(anchor.kind().isRuntimeImplemented()
                ? ReceiverStatus.TRANSMITTER_UNLOADED
                : ReceiverStatus.UNSUPPORTED_ANCHOR);
        }

        StressTransmitterBlockEntity transmitter = staticTransmitter.get();
        if (endpointId.isPresent() && !endpointId.get().equals(transmitter.getEndpointId())) {
            return TransmitterRuntime.failure(ReceiverStatus.INVALID_TRANSMITTER);
        }

        return TransmitterRuntime.active(
            transmitter.getSourceSpeed(),
            transmitter.getAvailableStressBudget(),
            transmitter.isPoweredDisabled(),
            transmitter.isRemoteLoopSource()
        );
    }

    private static ReceiverRuntime resolveReceiver(MinecraftServer server, LinkAnchor anchor) {
        Optional<UUID> endpointId = anchor.endpointId();
        if (endpointId.isPresent()) {
            Optional<MovingEndpointRegistry.RuntimeEndpoint> moving = MovingEndpointRegistry.get(server).get(endpointId.get())
                .filter(MovingEndpointRegistry.RuntimeEndpoint::isReceiver);
            if (moving.isPresent()) {
                return ReceiverRuntime.movingReceiver();
            }
        }

        Optional<StressReceiverBlockEntity> staticReceiver = getStaticReceiver(server, anchor);
        if (staticReceiver.isPresent()) {
            StressReceiverBlockEntity receiver = staticReceiver.get();
            if (endpointId.isPresent() && !endpointId.get().equals(receiver.getEndpointId())) {
                return ReceiverRuntime.failure(ReceiverStatus.INVALID_RECEIVER);
            }
            return ReceiverRuntime.staticReceiver(receiver);
        }

        return ReceiverRuntime.failure(anchor.kind().isRuntimeImplemented()
            ? ReceiverStatus.RECEIVER_UNLOADED
            : ReceiverStatus.UNSUPPORTED_ANCHOR);
    }

    private static void applyToStaticReceiver(MinecraftServer server, StressLinkRecord record, float speed, int grantedStress,
                                              ReceiverStatus status, List<UUID> brokenLinks) {
        Optional<StressReceiverBlockEntity> receiver = getStaticReceiver(server, record.receiver());
        if (receiver.isPresent()) {
            receiver.get().applyRuntime(record.id(), speed, grantedStress, status);
            return;
        }

        if (status == ReceiverStatus.INVALID_TRANSMITTER || status == ReceiverStatus.INVALID_RECEIVER) {
            brokenLinks.add(record.id());
        }
    }

    private static Optional<StressTransmitterBlockEntity> getStaticTransmitter(MinecraftServer server, LinkAnchor anchor) {
        if (!anchor.isStaticBlock()) {
            return Optional.empty();
        }

        ServerLevel level = server.getLevel(anchor.dimensionKey());
        if (level == null || !level.isLoaded(anchor.pos())) {
            return Optional.empty();
        }

        BlockEntity blockEntity = level.getBlockEntity(anchor.pos());
        return blockEntity instanceof StressTransmitterBlockEntity transmitter ? Optional.of(transmitter) : Optional.empty();
    }

    private static Optional<StressReceiverBlockEntity> getStaticReceiver(MinecraftServer server, LinkAnchor anchor) {
        if (!anchor.isStaticBlock()) {
            return Optional.empty();
        }

        ServerLevel level = server.getLevel(anchor.dimensionKey());
        if (level == null || !level.isLoaded(anchor.pos())) {
            return Optional.empty();
        }

        BlockEntity blockEntity = level.getBlockEntity(anchor.pos());
        return blockEntity instanceof StressReceiverBlockEntity receiver ? Optional.of(receiver) : Optional.empty();
    }

    private static boolean matchesSameEndpointOrStaticPosition(LinkAnchor stored, LinkAnchor fresh) {
        if (stored.endpointId().isPresent() && fresh.endpointId().isPresent()) {
            return stored.endpointId().get().equals(fresh.endpointId().get());
        }
        return stored.isStaticBlock()
            && stored.dimensionId().equals(fresh.dimensionId())
            && stored.pos().equals(fresh.pos());
    }

    public record BindResult(boolean success, Component message) {
        public static BindResult success(Component message) {
            return new BindResult(true, message);
        }

        public static BindResult failure(String message) {
            return new BindResult(false, Component.translatable(message).withStyle(ChatFormatting.RED));
        }
    }

    private record ActiveReceiver(StressLinkRecord record, StressReceiverBlockEntity receiver) {
    }

    private record TransmitterRuntime(Optional<ReceiverStatus> failureStatus, float speed, int availableStress,
                                      boolean poweredDisabled, boolean remoteLoop) {
        static TransmitterRuntime active(float speed, int availableStress, boolean poweredDisabled, boolean remoteLoop) {
            return new TransmitterRuntime(Optional.empty(), speed, Math.max(availableStress, 0), poweredDisabled, remoteLoop);
        }

        static TransmitterRuntime failure(ReceiverStatus status) {
            return new TransmitterRuntime(Optional.of(status), 0.0F, 0, false, false);
        }
    }

    private record ReceiverRuntime(Optional<StressReceiverBlockEntity> staticReceiver, Optional<ReceiverStatus> failureStatus,
                                   boolean moving) {
        static ReceiverRuntime staticReceiver(StressReceiverBlockEntity receiver) {
            return new ReceiverRuntime(Optional.of(receiver), Optional.empty(), false);
        }

        static ReceiverRuntime movingReceiver() {
            return new ReceiverRuntime(Optional.empty(), Optional.empty(), true);
        }

        static ReceiverRuntime failure(ReceiverStatus status) {
            return new ReceiverRuntime(Optional.empty(), Optional.of(status), false);
        }
    }
}
