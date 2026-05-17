package org.xiyu.create_stressbound.client.visual;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.xiyu.create_stressbound.content.link.ReceiverStatus;
import org.xiyu.create_stressbound.content.link.StressLinkColors;
import org.xiyu.create_stressbound.network.StressLinkVisualSyncPacket;

public final class StressLinkVisualData {
    private static final int STALE_AFTER_TICKS = 40;

    private static List<LinkVisual> syncedLinks = Collections.emptyList();
    private static long lastSyncGameTime = -1;
    private static ResourceKey<Level> syncedDimension;

    private StressLinkVisualData() {
    }

    public record LinkVisual(
        Vec3 transmitterPos,
        Vec3 receiverPos,
        float speed,
        int grantedStress,
        int requestedStress,
        ReceiverStatus status,
        int color
    ) {
    }

    public static List<LinkVisual> getLinks(Level level) {
        if (level == null || !Objects.equals(syncedDimension, level.dimension())) {
            return Collections.emptyList();
        }
        if (lastSyncGameTime >= 0 && level.getGameTime() - lastSyncGameTime > STALE_AFTER_TICKS) {
            return Collections.emptyList();
        }
        return syncedLinks;
    }

    public static void handleSync(List<StressLinkVisualSyncPacket.LinkVisualPayload> payloads) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            clear();
            return;
        }

        List<LinkVisual> links = new ArrayList<>(payloads.size());
        for (StressLinkVisualSyncPacket.LinkVisualPayload payload : payloads) {
            links.add(new LinkVisual(
                new Vec3(payload.startX(), payload.startY(), payload.startZ()),
                new Vec3(payload.endX(), payload.endY(), payload.endZ()),
                payload.speed(),
                payload.grantedStress(),
                payload.requestedStress(),
                payload.status(),
                StressLinkColors.normalize(payload.color())
            ));
        }
        syncedLinks = Collections.unmodifiableList(links);
        syncedDimension = mc.level.dimension();
        lastSyncGameTime = mc.level.getGameTime();
    }

    public static void clear() {
        syncedLinks = Collections.emptyList();
        syncedDimension = null;
        lastSyncGameTime = -1;
    }
}
