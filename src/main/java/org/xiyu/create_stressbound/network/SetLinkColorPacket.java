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

public record SetLinkColorPacket(BlockPos transmitterPos, UUID linkId, int color, boolean autoAssign)
    implements CustomPacketPayload {

    public static final Type<SetLinkColorPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(CreateStressbound.MODID, "set_link_color"));

    public static final StreamCodec<FriendlyByteBuf, SetLinkColorPacket> CODEC =
        StreamCodec.of(
            (buf, pkt) -> {
                buf.writeBlockPos(pkt.transmitterPos);
                buf.writeUUID(pkt.linkId);
                buf.writeInt(pkt.color);
                buf.writeBoolean(pkt.autoAssign);
            },
            buf -> new SetLinkColorPacket(buf.readBlockPos(), buf.readUUID(), buf.readInt(), buf.readBoolean())
        );

    @Override
    public Type<SetLinkColorPacket> type() {
        return TYPE;
    }

    public static void handle(SetLinkColorPacket packet, IPayloadContext context) {
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
            if (!(be instanceof StressTransmitterBlockEntity)) {
                return;
            }

            StressLinkService.ColorUpdateResult result = packet.autoAssign
                ? StressLinkService.assignNextLinkColor(serverPlayer.server, packet.linkId)
                : StressLinkService.setLinkColor(serverPlayer.server, packet.linkId, packet.color);

            switch (result.status()) {
                case NOT_FOUND -> serverPlayer.sendSystemMessage(Component.translatable(
                    "message.create_stressbound.color.not_found").withStyle(ChatFormatting.RED));
                case UPDATED -> {
                }
            }
        });
    }
}
