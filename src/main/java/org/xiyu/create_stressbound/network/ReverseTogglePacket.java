package org.xiyu.create_stressbound.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.xiyu.create_stressbound.CreateStressbound;
import org.xiyu.create_stressbound.client.gui.ReceiverMenu;
import org.xiyu.create_stressbound.content.kinetics.StressReceiverBlockEntity;

public record ReverseTogglePacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<ReverseTogglePacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(CreateStressbound.MODID, "reverse_toggle"));

    public static final StreamCodec<FriendlyByteBuf, ReverseTogglePacket> CODEC =
        StreamCodec.of(
            (buf, pkt) -> buf.writeBlockPos(pkt.pos),
            buf -> new ReverseTogglePacket(buf.readBlockPos())
        );

    @Override
    public Type<ReverseTogglePacket> type() {
        return TYPE;
    }

    public static void handle(ReverseTogglePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                if (!(serverPlayer.containerMenu instanceof ReceiverMenu menu)
                    || !menu.getBlockPos().equals(packet.pos)
                    || !menu.stillValid(serverPlayer)) {
                    return;
                }
                BlockEntity be = serverPlayer.level().getBlockEntity(packet.pos);
                if (be instanceof StressReceiverBlockEntity receiver) {
                    receiver.toggleReverseOutput();
                }
            }
        });
    }
}
