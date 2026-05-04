package org.xiyu.create_stressbound.client.visual;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.xiyu.create_stressbound.content.kinetics.StressReceiverBlockEntity;
import org.xiyu.create_stressbound.content.kinetics.StressTransmitterBlockEntity;
import org.xiyu.create_stressbound.content.link.ReceiverStatus;

public final class StressLinkVisualData {

    private static List<LinkVisual> cachedLinks = Collections.emptyList();
    private static long lastCollectTick = -1;
    private static final int COLLECT_INTERVAL = 10;
    private static final int CHUNK_RANGE = 4;
    private static final int MAX_LINKS = 64;
    private static ResourceKey<Level> cachedDimension;
    private static int cachedPlayerChunkX;
    private static int cachedPlayerChunkZ;

    private StressLinkVisualData() {
    }

    public record LinkVisual(
        BlockPos transmitterPos,
        BlockPos receiverPos,
        float speed,
        int grantedStress,
        int requestedStress,
        ReceiverStatus status,
        ResourceKey<Level> dimension
    ) {
    }

    public static List<LinkVisual> getLinks(Level level) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return Collections.emptyList();

        long tick = mc.player.tickCount;
        BlockPos playerPos = mc.player.blockPosition();
        int playerChunkX = SectionPos.blockToSectionCoord(playerPos.getX());
        int playerChunkZ = SectionPos.blockToSectionCoord(playerPos.getZ());
        boolean cacheValid = Objects.equals(cachedDimension, level.dimension())
            && cachedPlayerChunkX == playerChunkX
            && cachedPlayerChunkZ == playerChunkZ
            && tick - lastCollectTick < COLLECT_INTERVAL;
        if (cacheValid) {
            return cachedLinks;
        }
        lastCollectTick = tick;
        cachedDimension = level.dimension();
        cachedPlayerChunkX = playerChunkX;
        cachedPlayerChunkZ = playerChunkZ;
        cachedLinks = collectLinks(level);
        return cachedLinks;
    }

    private static List<LinkVisual> collectLinks(Level level) {
        if (level == null) return Collections.emptyList();

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return Collections.emptyList();

        List<LinkVisual> links = new ArrayList<>();
        BlockPos playerPos = mc.player.blockPosition();

        int cx0 = SectionPos.blockToSectionCoord(playerPos.getX()) - CHUNK_RANGE;
        int cx1 = SectionPos.blockToSectionCoord(playerPos.getX()) + CHUNK_RANGE;
        int cz0 = SectionPos.blockToSectionCoord(playerPos.getZ()) - CHUNK_RANGE;
        int cz1 = SectionPos.blockToSectionCoord(playerPos.getZ()) + CHUNK_RANGE;

        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                if (!mc.level.hasChunk(cx, cz)) continue;
                LevelChunk chunk = mc.level.getChunk(cx, cz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof StressReceiverBlockEntity receiver) {
                        BlockPos txPos = receiver.getTransmitterPos();
                        if (txPos == null || txPos.equals(BlockPos.ZERO)) continue;

                        ResourceKey<Level> txDim = receiver.getTransmitterDimension();
                        boolean sameDimension = txDim != null && txDim.equals(level.dimension());

                        links.add(new LinkVisual(
                            txPos.immutable(),
                            receiver.getBlockPos().immutable(),
                            receiver.getTransmittedSpeed(),
                            receiver.getGrantedStress(),
                            receiver.getRequestedStress(),
                            receiver.getStatus(),
                            sameDimension ? level.dimension() : null
                        ));
                        if (links.size() >= MAX_LINKS) {
                            return Collections.unmodifiableList(links);
                        }
                    }
                }
            }
        }
        return Collections.unmodifiableList(links);
    }

    public static Optional<BlockPos> findTransmitterFor(Level level, BlockPos receiverPos) {
        if (level == null) return Optional.empty();
        BlockEntity be = level.getBlockEntity(receiverPos);
        if (be instanceof StressReceiverBlockEntity receiver) {
            BlockPos txPos = receiver.getTransmitterPos();
            if (txPos != null && !txPos.equals(BlockPos.ZERO)) {
                return Optional.of(txPos);
            }
        }
        return Optional.empty();
    }

    public static List<BlockPos> findReceiversFor(Level level, BlockPos transmitterPos) {
        if (level == null) return Collections.emptyList();
        BlockEntity be = level.getBlockEntity(transmitterPos);
        if (be instanceof StressTransmitterBlockEntity transmitter) {
            return transmitter.getLinkedReceiverPositions();
        }
        return Collections.emptyList();
    }
}
