package org.xiyu.create_stressbound.network;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.xiyu.create_stressbound.CreateStressbound;
import org.xiyu.create_stressbound.content.link.ReceiverStatus;

public record StressLinkVisualSyncPacket(List<LinkVisualPayload> links) implements CustomPacketPayload {
    private static final int MAX_LINKS = 256;

    public static final Type<StressLinkVisualSyncPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(CreateStressbound.MODID, "stress_link_visual_sync"));

    public static final StreamCodec<FriendlyByteBuf, StressLinkVisualSyncPacket> CODEC =
        StreamCodec.of(
            (buf, packet) -> {
                int count = Math.min(packet.links.size(), MAX_LINKS);
                buf.writeVarInt(count);
                for (int i = 0; i < count; i++) {
                    packet.links.get(i).write(buf);
                }
            },
            buf -> {
                int count = Math.min(Math.max(buf.readVarInt(), 0), MAX_LINKS);
                List<LinkVisualPayload> links = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    links.add(LinkVisualPayload.read(buf));
                }
                return new StressLinkVisualSyncPacket(List.copyOf(links));
            }
        );

    public StressLinkVisualSyncPacket {
        links = List.copyOf(links);
    }

    @Override
    public Type<StressLinkVisualSyncPacket> type() {
        return TYPE;
    }

    public static void handle(StressLinkVisualSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientBridge.handle(packet.links);
            }
        });
    }

    public record LinkVisualPayload(
        double startX,
        double startY,
        double startZ,
        double endX,
        double endY,
        double endZ,
        float speed,
        int grantedStress,
        int requestedStress,
        ReceiverStatus status,
        int color
    ) {
        private void write(FriendlyByteBuf buf) {
            buf.writeDouble(startX);
            buf.writeDouble(startY);
            buf.writeDouble(startZ);
            buf.writeDouble(endX);
            buf.writeDouble(endY);
            buf.writeDouble(endZ);
            buf.writeFloat(speed);
            buf.writeVarInt(grantedStress);
            buf.writeVarInt(requestedStress);
            buf.writeVarInt(status.ordinal());
            buf.writeInt(color);
        }

        private static LinkVisualPayload read(FriendlyByteBuf buf) {
            return new LinkVisualPayload(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat(),
                buf.readVarInt(),
                buf.readVarInt(),
                statusByOrdinal(buf.readVarInt()),
                buf.readInt()
            );
        }

        private static ReceiverStatus statusByOrdinal(int ordinal) {
            ReceiverStatus[] values = ReceiverStatus.values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : ReceiverStatus.IDLE;
        }
    }

    private static final class ClientBridge {
        private ClientBridge() {
        }

        private static void handle(List<LinkVisualPayload> links) {
            try {
                Class<?> visualData = Class.forName("org.xiyu.create_stressbound.client.visual.StressLinkVisualData");
                visualData.getMethod("handleSync", List.class).invoke(null, links);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
                // Client-only visual data is optional for dedicated-server class loading.
            }
        }
    }
}
