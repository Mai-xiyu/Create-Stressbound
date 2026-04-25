package org.xiyu.create_stressbound.content.link;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.xiyu.create_stressbound.compat.MovingStructureSupport;
import org.xiyu.create_stressbound.content.kinetics.StressReceiverBlockEntity;
import org.xiyu.create_stressbound.content.kinetics.StressTransmitterBlockEntity;

public final class MovingEndpointRegistry {
    private static final Map<MinecraftServer, MovingEndpointRegistry> INSTANCES = new WeakHashMap<>();

    private final Map<UUID, RuntimeEndpoint> endpoints = new HashMap<>();

    private MovingEndpointRegistry() {
    }

    public static MovingEndpointRegistry get(MinecraftServer server) {
        synchronized (INSTANCES) {
            return INSTANCES.computeIfAbsent(server, ignored -> new MovingEndpointRegistry());
        }
    }

    public void capture(MovementContext context, EndpointRole role) {
        if (!(context.world instanceof ServerLevel serverLevel)) {
            return;
        }

        AbstractContraptionEntity contraptionEntity = context.contraption == null ? null : context.contraption.entity;
        if (contraptionEntity == null) {
            return;
        }

        UUID endpointId = role == EndpointRole.TRANSMITTER
            ? StressTransmitterBlockEntity.getEndpointIdFromTag(context.blockEntityData)
            : StressReceiverBlockEntity.getEndpointIdFromTag(context.blockEntityData);
        if (endpointId == null) {
            return;
        }

        AnchorKind kind = MovingStructureSupport.classify(contraptionEntity);
        BlockPos worldPos = currentWorldPos(context, contraptionEntity);
        LinkAnchor anchor = LinkAnchor.runtime(
            kind,
            serverLevel.dimension(),
            worldPos,
            endpointId,
            contraptionEntity.getUUID(),
            MovingStructureSupport.describeEntityHandle(contraptionEntity)
        );

        float latchedSpeed = role == EndpointRole.TRANSMITTER ? StressTransmitterBlockEntity.getLatchedSpeed(context.blockEntityData) : 0.0F;
        int latchedStress = role == EndpointRole.TRANSMITTER ? StressTransmitterBlockEntity.getLatchedAvailableStress(context.blockEntityData) : 0;
        boolean poweredDisabled = role == EndpointRole.TRANSMITTER && StressTransmitterBlockEntity.isLatchedPoweredDisabled(context.blockEntityData);
        boolean remoteLoop = role == EndpointRole.TRANSMITTER && StressTransmitterBlockEntity.isLatchedRemoteLoop(context.blockEntityData);

        endpoints.put(endpointId, new RuntimeEndpoint(anchor, role, latchedSpeed, latchedStress, poweredDisabled, remoteLoop, serverLevel.getServer().getTickCount()));
    }

    public void cleanup(long currentTick) {
        long cutoff = currentTick - 40L;
        endpoints.values().removeIf(endpoint -> endpoint.lastSeenTick() < cutoff);
    }

    public Optional<RuntimeEndpoint> get(UUID endpointId) {
        return Optional.ofNullable(endpoints.get(endpointId));
    }

    public boolean has(UUID endpointId) {
        return endpoints.containsKey(endpointId);
    }

    private static BlockPos currentWorldPos(MovementContext context, AbstractContraptionEntity contraptionEntity) {
        Vec3 position = context.position;
        if (position == null) {
            position = contraptionEntity.toGlobalVector(Vec3.atCenterOf(context.localPos), 1.0F);
        }
        return BlockPos.containing(position);
    }

    public record RuntimeEndpoint(
        LinkAnchor anchor,
        EndpointRole role,
        float latchedSpeed,
        int latchedAvailableStress,
        boolean poweredDisabled,
        boolean remoteLoop,
        long lastSeenTick
    ) {
        public boolean isTransmitter() {
            return role == EndpointRole.TRANSMITTER;
        }

        public boolean isReceiver() {
            return role == EndpointRole.RECEIVER;
        }
    }
}
