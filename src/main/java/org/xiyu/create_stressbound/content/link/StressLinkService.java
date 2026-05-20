package org.xiyu.create_stressbound.content.link;

import java.util.ArrayList;
import java.util.Collection;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.xiyu.create_stressbound.StressboundConfig;
import org.xiyu.create_stressbound.compat.MovingStructureSupport;
import org.xiyu.create_stressbound.content.kinetics.StressReceiverBlockEntity;
import org.xiyu.create_stressbound.content.kinetics.StressTransmitterBlockEntity;
import org.xiyu.create_stressbound.network.StressLinkVisualSyncPacket;

public final class StressLinkService {
    private static final float MIN_RUNTIME_SPEED = 0.01F;
    private static final int VISUAL_SYNC_INTERVAL_TICKS = 5;
    private static final double VISUAL_SYNC_DISTANCE = 128.0D;
    private static final double VISUAL_SYNC_DISTANCE_SQR = VISUAL_SYNC_DISTANCE * VISUAL_SYNC_DISTANCE;
    private static final int MAX_VISUAL_LINKS_PER_PLAYER = 128;

    private StressLinkService() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int tick = server.getTickCount();
        MovingEndpointRegistry.get(server).cleanup(tick);

        int interval = evaluationInterval();
        if (tick % interval == 0) {
            evaluate(server);
        }
        if (tick % VISUAL_SYNC_INTERVAL_TICKS == 0) {
            syncClientVisuals(server);
        }
    }

    private static int evaluationInterval() {
        int interval = Math.max(1, StressboundConfig.evaluationIntervalTicks);
        if (StressboundConfig.transmitterPoweredStops || StressboundConfig.receiverPoweredStops) {
            interval = Math.min(interval, Math.max(1, StressboundConfig.redstoneEvaluationIntervalTicks));
        }
        return interval;
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

        List<StressLinkRecord> transmitterRecords = data.findByTransmitter(transmitterAnchor);
        int color = StressLinkColors.transmitterColor(transmitterRecords);
        if (!StressLinkColors.isAssigned(color)) {
            color = StressLinkColors.nextTransmitterColor(data.all());
        }
        int requestedStress = receiver.getRequestedStress();
        StressLinkRecord record = new StressLinkRecord(
            UUID.randomUUID(),
            player.getUUID(),
            transmitterAnchor,
            receiverAnchor,
            requestedStress,
            color,
            receiver.getLevel().getGameTime()
        );
        data.put(record);
        receiver.setLinkId(record.id());
        receiver.setRequestedStress(record.requestedStress());
        receiver.applyRuntime(record.id(), 0.0F, 0, ReceiverStatus.IDLE);
        transmitter.refreshLinkedReceiverVisuals();
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
        transmitter.refreshLinkedReceiverVisuals();
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

        StressLinkRecord removed = record.get();
        data.remove(linkId);
        getStaticReceiver(server, removed.receiver()).ifPresent(StressReceiverBlockEntity::clearLink);
        getStaticTransmitter(server, removed.transmitter()).ifPresent(StressTransmitterBlockEntity::refreshLinkedReceiverVisuals);
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
        getStaticTransmitter(server, updated.transmitter()).ifPresent(StressTransmitterBlockEntity::refreshLinkedReceiverVisuals);
        return Optional.of(updated);
    }

    public static ColorUpdateResult setLinkColor(MinecraftServer server, UUID linkId, int color) {
        StressLinkSavedData data = StressLinkSavedData.get(server);
        Optional<StressLinkRecord> existing = data.get(linkId);
        if (existing.isEmpty()) {
            return ColorUpdateResult.notFound();
        }

        StressLinkRecord record = existing.get();
        int normalized = StressLinkColors.normalize(color);
        if (StressLinkColors.isUsedByOtherTransmitter(data.all(), record.transmitter(), normalized)) {
            return ColorUpdateResult.duplicate(record);
        }

        List<StressLinkRecord> updated = setTransmitterGroupColor(data, record.transmitter(), normalized);
        refreshTransmitterGroupVisuals(server, record.transmitter(), updated);
        return ColorUpdateResult.updated(updated.isEmpty() ? record : updated.getFirst());
    }

    public static ColorUpdateResult assignNextLinkColor(MinecraftServer server, UUID linkId) {
        StressLinkSavedData data = StressLinkSavedData.get(server);
        Optional<StressLinkRecord> existing = data.get(linkId);
        if (existing.isEmpty()) {
            return ColorUpdateResult.notFound();
        }

        StressLinkRecord record = existing.get();
        java.util.Set<Integer> used = new java.util.LinkedHashSet<>();
        java.util.Set<String> seenTransmitters = new java.util.LinkedHashSet<>();
        for (StressLinkRecord link : data.all()) {
            String transmitterKey = link.transmitter().key();
            if (transmitterKey.equals(record.transmitter().key()) || !seenTransmitters.add(transmitterKey)) {
                continue;
            }
            if (StressLinkColors.isAssigned(link.color())) {
                used.add(StressLinkColors.normalize(link.color()));
            }
        }

        int color = StressLinkColors.nextAvailableAfter(used, record.color());
        List<StressLinkRecord> updated = setTransmitterGroupColor(data, record.transmitter(), color);
        refreshTransmitterGroupVisuals(server, record.transmitter(), updated);
        return ColorUpdateResult.updated(updated.isEmpty() ? record : updated.getFirst());
    }

    public static int setAllRequestedStress(MinecraftServer server, int requestedStress) {
        StressLinkSavedData data = StressLinkSavedData.get(server);
        List<UUID> ids = data.all().stream()
            .map(StressLinkRecord::id)
            .toList();
        ids.forEach(id -> setRequestedStress(server, id, requestedStress));
        return ids.size();
    }

    private static List<StressLinkRecord> setTransmitterGroupColor(StressLinkSavedData data, LinkAnchor transmitter, int color) {
        List<StressLinkRecord> updatedRecords = new ArrayList<>();
        for (StressLinkRecord link : data.findByTransmitter(transmitter)) {
            StressLinkRecord updated = link.withColor(color);
            data.put(updated);
            updatedRecords.add(updated);
        }
        return updatedRecords;
    }

    private static void refreshTransmitterGroupVisuals(MinecraftServer server, LinkAnchor transmitter, List<StressLinkRecord> records) {
        for (StressLinkRecord record : records) {
            getStaticReceiver(server, record.receiver()).ifPresent(StressReceiverBlockEntity::refreshClientLinkVisuals);
        }
        getStaticTransmitter(server, transmitter).ifPresent(StressTransmitterBlockEntity::refreshLinkedReceiverVisuals);
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

    private static void syncClientVisuals(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            return;
        }

        List<StressLinkRecord> records = List.copyOf(StressLinkSavedData.get(server).all());
        if (records.isEmpty()) {
            StressLinkVisualSyncPacket packet = new StressLinkVisualSyncPacket(List.of());
            for (ServerPlayer player : players) {
                PacketDistributor.sendToPlayer(player, packet);
            }
            return;
        }

        for (ServerPlayer player : players) {
            PacketDistributor.sendToPlayer(player,
                new StressLinkVisualSyncPacket(buildVisualPayloadsForPlayer(server, player, records)));
        }
    }

    private static List<StressLinkVisualSyncPacket.LinkVisualPayload> buildVisualPayloadsForPlayer(
        MinecraftServer server,
        ServerPlayer player,
        Collection<StressLinkRecord> records
    ) {
        List<StressLinkVisualSyncPacket.LinkVisualPayload> payloads = new ArrayList<>();
        ResourceKey<Level> playerDimension = player.level().dimension();
        Vec3 playerPos = player.position();

        for (StressLinkRecord record : records) {
            Optional<VisualEndpoint> transmitter = resolveVisualEndpoint(server, record.transmitter(), EndpointRole.TRANSMITTER);
            Optional<VisualEndpoint> receiver = resolveVisualEndpoint(server, record.receiver(), EndpointRole.RECEIVER);
            if (transmitter.isEmpty() || receiver.isEmpty()) {
                continue;
            }

            VisualEndpoint transmitterEndpoint = transmitter.get();
            VisualEndpoint receiverEndpoint = receiver.get();
            if (!transmitterEndpoint.dimension().equals(playerDimension) || !receiverEndpoint.dimension().equals(playerDimension)) {
                continue;
            }
            if (transmitterEndpoint.position().distanceToSqr(playerPos) > VISUAL_SYNC_DISTANCE_SQR
                && receiverEndpoint.position().distanceToSqr(playerPos) > VISUAL_SYNC_DISTANCE_SQR) {
                continue;
            }

            ReceiverVisualRuntime runtime = resolveReceiverVisualRuntime(server, record);
            payloads.add(new StressLinkVisualSyncPacket.LinkVisualPayload(
                transmitterEndpoint.position().x,
                transmitterEndpoint.position().y,
                transmitterEndpoint.position().z,
                receiverEndpoint.position().x,
                receiverEndpoint.position().y,
                receiverEndpoint.position().z,
                runtime.speed(),
                runtime.grantedStress(),
                record.requestedStress(),
                runtime.status(),
                record.color()
            ));

            if (payloads.size() >= MAX_VISUAL_LINKS_PER_PLAYER) {
                break;
            }
        }

        return payloads;
    }

    private static Optional<VisualEndpoint> resolveVisualEndpoint(MinecraftServer server, LinkAnchor anchor, EndpointRole role) {
        Optional<UUID> endpointId = anchor.endpointId();
        if (endpointId.isPresent()) {
            Optional<MovingEndpointRegistry.RuntimeEndpoint> moving = MovingEndpointRegistry.get(server).get(endpointId.get())
                .filter(endpoint -> role == EndpointRole.TRANSMITTER ? endpoint.isTransmitter() : endpoint.isReceiver());
            if (moving.isPresent()) {
                return visualEndpointForAnchor(server, moving.get().anchor());
            }
        }

        if (!anchor.isStaticBlock()) {
            return Optional.empty();
        }

        ServerLevel level = server.getLevel(anchor.dimensionKey());
        if (level == null || !level.isLoaded(anchor.pos())) {
            return Optional.empty();
        }

        BlockEntity blockEntity = level.getBlockEntity(anchor.pos());
        if (role == EndpointRole.TRANSMITTER) {
            if (!(blockEntity instanceof StressTransmitterBlockEntity transmitter)) {
                return Optional.empty();
            }
            if (endpointId.isPresent() && !endpointId.get().equals(transmitter.getEndpointId())) {
                return Optional.empty();
            }
        } else {
            if (!(blockEntity instanceof StressReceiverBlockEntity receiver)) {
                return Optional.empty();
            }
            if (endpointId.isPresent() && !endpointId.get().equals(receiver.getEndpointId())) {
                return Optional.empty();
            }
        }

        return Optional.of(new VisualEndpoint(level.dimension(), MovingStructureSupport.projectBlockCenter(level, anchor.pos())));
    }

    private static Optional<VisualEndpoint> visualEndpointForAnchor(MinecraftServer server, LinkAnchor anchor) {
        ServerLevel level = server.getLevel(anchor.dimensionKey());
        if (level == null) {
            return Optional.empty();
        }
        return Optional.of(new VisualEndpoint(level.dimension(), MovingStructureSupport.projectBlockCenter(level, anchor.pos())));
    }

    private static ReceiverVisualRuntime resolveReceiverVisualRuntime(MinecraftServer server, StressLinkRecord record) {
        Optional<StressReceiverBlockEntity> receiver = getStaticReceiver(server, record.receiver());
        if (receiver.isPresent()) {
            StressReceiverBlockEntity receiverBlockEntity = receiver.get();
            return new ReceiverVisualRuntime(receiverBlockEntity.getTransmittedSpeed(), receiverBlockEntity.getGrantedStress(),
                receiverBlockEntity.getStatus());
        }

        if (record.receiver().endpointId().isPresent()
            && MovingEndpointRegistry.get(server).get(record.receiver().endpointId().get())
                .filter(MovingEndpointRegistry.RuntimeEndpoint::isReceiver)
                .isPresent()) {
            return new ReceiverVisualRuntime(0.0F, 0, ReceiverStatus.IDLE);
        }

        return new ReceiverVisualRuntime(0.0F, 0, ReceiverStatus.RECEIVER_UNLOADED);
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
            records.forEach(record -> applyToStaticReceiver(server, record, 0.0F, 0,
                ReceiverStatus.TRANSMITTER_DISABLED, brokenLinks, transmitter.visualAnchor()));
            return;
        }

        if (transmitter.remoteLoop()) {
            records.forEach(record -> applyToStaticReceiver(server, record, 0.0F, 0,
                ReceiverStatus.REMOTE_LOOP, brokenLinks, transmitter.visualAnchor()));
            return;
        }

        if (transmitter.speed() == 0.0F) {
            records.forEach(record -> applyToStaticReceiver(server, record, 0.0F, 0,
                ReceiverStatus.IDLE, brokenLinks, transmitter.visualAnchor()));
            return;
        }

        List<ActiveReceiver> activeReceivers = new ArrayList<>();
        for (StressLinkRecord record : records) {
            ReceiverRuntime receiver = resolveReceiver(server, record.receiver());
            if (receiver.staticReceiver().isPresent()) {
                StressReceiverBlockEntity receiverBlockEntity = receiver.staticReceiver().get();
                if (receiverBlockEntity.isPoweredDisabled()) {
                    receiverBlockEntity.applyRuntime(record.id(), 0.0F, 0,
                        ReceiverStatus.RECEIVER_DISABLED, transmitter.visualAnchor());
                    continue;
                }
                int reservedStress = StressboundConfig.clampRequestedStress(record.requestedStress());
                int scaledReservedStress = receiverBlockEntity.scaleGrantedStress(reservedStress);
                if (scaledReservedStress <= 0) {
                    receiverBlockEntity.applyRuntime(record.id(), 0.0F, 0,
                        ReceiverStatus.RECEIVER_DISABLED, transmitter.visualAnchor());
                    continue;
                }
                activeReceivers.add(new ActiveReceiver(record, receiverBlockEntity, scaledReservedStress));
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
            .mapToLong(ActiveReceiver::reservedStress)
            .sum();

        boolean transientGeneratedBudget = transmitter.transientGeneratedBudget();
        if (StressboundConfig.strictOverloadMode && !transientGeneratedBudget
            && totalReservedStress > transmitter.availableStress()) {
            activeReceivers.forEach(active -> active.receiver().applyRuntime(active.record().id(), 0.0F, 0,
                ReceiverStatus.OVERLOADED, transmitter.visualAnchor()));
            return;
        }

        // Simulated torsion spring output has speed while its Create network reports no normal SU capacity.
        long remainingStress = transientGeneratedBudget
            ? Math.max(totalReservedStress, transmitter.availableStress())
            : transmitter.availableStress();
        for (ActiveReceiver activeReceiver : activeReceivers) {
            int reservedStress = activeReceiver.reservedStress();
            if (reservedStress > remainingStress) {
                activeReceiver.receiver().applyRuntime(activeReceiver.record().id(), 0.0F, 0,
                    ReceiverStatus.OVERLOADED, transmitter.visualAnchor());
                continue;
            }
            activeReceiver.receiver().applyRuntime(activeReceiver.record().id(),
                activeReceiver.receiver().scaleIncomingSpeed(transmitter.speed()), reservedStress,
                ReceiverStatus.ACTIVE, transmitter.visualAnchor());
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
                return TransmitterRuntime.active(endpoint.latchedSpeed(), endpoint.latchedAvailableStress(),
                    endpoint.poweredDisabled(), endpoint.remoteLoop(), endpoint.anchor(), false);
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
            transmitter.getControlledSourceSpeed(),
            transmitter.getControlledAvailableStressBudget(),
            transmitter.isPoweredDisabled(),
            transmitter.isRemoteLoopSource(),
            transmitter.createAnchor(),
            transmitter.hasActiveSimulatedTorsionSpringOutput()
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
        applyToStaticReceiver(server, record, speed, grantedStress, status, brokenLinks, null);
    }

    private static void applyToStaticReceiver(MinecraftServer server, StressLinkRecord record, float speed, int grantedStress,
                                              ReceiverStatus status, List<UUID> brokenLinks, LinkAnchor transmitterVisualAnchor) {
        Optional<StressReceiverBlockEntity> receiver = getStaticReceiver(server, record.receiver());
        if (receiver.isPresent()) {
            receiver.get().applyRuntime(record.id(), speed, grantedStress, status, transmitterVisualAnchor);
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

    private static float normalizeRuntimeSpeed(float speed) {
        if (!Float.isFinite(speed) || Math.abs(speed) < MIN_RUNTIME_SPEED) {
            return 0.0F;
        }
        return speed;
    }

    public record BindResult(boolean success, Component message) {
        public static BindResult success(Component message) {
            return new BindResult(true, message);
        }

        public static BindResult failure(String message) {
            return new BindResult(false, Component.translatable(message).withStyle(ChatFormatting.RED));
        }
    }

    public record ColorUpdateResult(ColorUpdateStatus status, StressLinkRecord record) {
        public static ColorUpdateResult updated(StressLinkRecord record) {
            return new ColorUpdateResult(ColorUpdateStatus.UPDATED, record);
        }

        public static ColorUpdateResult duplicate(StressLinkRecord record) {
            return new ColorUpdateResult(ColorUpdateStatus.DUPLICATE, record);
        }

        public static ColorUpdateResult notFound() {
            return new ColorUpdateResult(ColorUpdateStatus.NOT_FOUND, null);
        }
    }

    public enum ColorUpdateStatus {
        UPDATED,
        DUPLICATE,
        NOT_FOUND
    }

    private record ActiveReceiver(StressLinkRecord record, StressReceiverBlockEntity receiver, int reservedStress) {
    }

    private record VisualEndpoint(ResourceKey<Level> dimension, Vec3 position) {
    }

    private record ReceiverVisualRuntime(float speed, int grantedStress, ReceiverStatus status) {
    }

    private record TransmitterRuntime(Optional<ReceiverStatus> failureStatus, float speed, int availableStress,
                                      boolean poweredDisabled, boolean remoteLoop, LinkAnchor visualAnchor,
                                      boolean transientGeneratedBudget) {
        static TransmitterRuntime active(float speed, int availableStress, boolean poweredDisabled,
                                         boolean remoteLoop, LinkAnchor visualAnchor,
                                         boolean transientGeneratedBudget) {
            float normalizedSpeed = normalizeRuntimeSpeed(speed);
            int normalizedStress = normalizedSpeed == 0.0F ? 0 : Math.max(availableStress, 0);
            return new TransmitterRuntime(Optional.empty(), normalizedSpeed, normalizedStress,
                poweredDisabled, remoteLoop, visualAnchor, transientGeneratedBudget && normalizedSpeed != 0.0F);
        }

        static TransmitterRuntime failure(ReceiverStatus status) {
            return new TransmitterRuntime(Optional.of(status), 0.0F, 0, false, false, null, false);
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
