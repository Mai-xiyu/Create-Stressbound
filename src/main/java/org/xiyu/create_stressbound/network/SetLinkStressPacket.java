package org.xiyu.create_stressbound.network;

import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.xiyu.create_stressbound.CreateStressbound;
import org.xiyu.create_stressbound.client.gui.TransmitterMenu;
import org.xiyu.create_stressbound.content.kinetics.StressTransmitterBlockEntity;
import org.xiyu.create_stressbound.content.link.StressLinkService;

public record SetLinkStressPacket(BlockPos transmitterPos, UUID linkId, int requestedStress)
    implements CustomPacketPayload {

    public static final Type<SetLinkStressPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(CreateStressbound.MODID, "set_link_stress"));

    public static final StreamCodec<FriendlyByteBuf, SetLinkStressPacket> CODEC =
        StreamCodec.of(
            (buf, pkt) -> {
                buf.writeBlockPos(pkt.transmitterPos);
                buf.writeUUID(pkt.linkId);
                buf.writeVarInt(pkt.requestedStress);
            },
            buf -> new SetLinkStressPacket(buf.readBlockPos(), buf.readUUID(), buf.readVarInt())
        );

    @Override
    public Type<SetLinkStressPacket> type() {
        return TYPE;
    }

    public static void handle(SetLinkStressPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }
            if (!(serverPlayer.containerMenu instanceof TransmitterMenu menu)
                || !menu.getBlockPos().equals(packet.transmitterPos)
                || !menu.stillValid(serverPlayer)) {
                return;
            }
            BlockEntity be = serverPlayer.level().getBlockEntity(packet.transmitterPos);
            if (!(be instanceof StressTransmitterBlockEntity transmitter)) {
                return;
            }

            StressLinkService.setRequestedStress(serverPlayer.server, transmitter.createAnchor(),
                    packet.linkId, packet.requestedStress)
                .ifPresentOrElse(updated -> {
                }, () -> serverPlayer.sendSystemMessage(Component.translatable(
                    "message.create_stressbound.stress.not_found").withStyle(ChatFormatting.RED)));
        });
    }
}
