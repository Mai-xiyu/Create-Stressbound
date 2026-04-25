package org.xiyu.create_stressbound.content.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.xiyu.create_stressbound.CreateStressbound;
import org.xiyu.create_stressbound.content.kinetics.StressReceiverBlockEntity;
import org.xiyu.create_stressbound.content.kinetics.StressTransmitterBlockEntity;
import org.xiyu.create_stressbound.content.link.StressLinkService;

public class KineticBinderItem extends Item {
    private static final String PLAYER_BINDER_DATA = "BinderSelection";
    private static final String DIMENSION_KEY = "Dimension";
    private static final String POSITION_KEY = "Pos";

    public KineticBinderItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = serverLevel.getBlockEntity(pos);
        if (blockEntity instanceof StressTransmitterBlockEntity transmitter) {
            storeSelection(serverPlayer, serverLevel.dimension(), pos);
            serverPlayer.sendSystemMessage(Component.translatable("message.create_stressbound.binder.transmitter_stored",
                    describe(serverLevel.dimension(), pos))
                .withStyle(ChatFormatting.GOLD));
            return InteractionResult.SUCCESS;
        }

        if (blockEntity instanceof StressReceiverBlockEntity receiver) {
            if (player.isShiftKeyDown()) {
                boolean removed = StressLinkService.clearLinkAt(serverLevel, pos, serverPlayer);
                if (!removed) {
                    serverPlayer.sendSystemMessage(Component.translatable("message.create_stressbound.binder.receiver_not_linked")
                        .withStyle(ChatFormatting.GRAY));
                }
                return InteractionResult.SUCCESS;
            }

            Selection selection = loadSelection(serverPlayer);
            if (selection == null) {
                serverPlayer.sendSystemMessage(Component.translatable("message.create_stressbound.binder.select_transmitter_first")
                    .withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }

            StressLinkService.BindResult result = StressLinkService.bind(serverPlayer, selection.dimension(), selection.pos(), receiver);
            serverPlayer.sendSystemMessage(result.message());
            return result.success() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player.isShiftKeyDown()) {
            clearSelection(player);
            player.sendSystemMessage(Component.translatable("message.create_stressbound.binder.selection_cleared")
                .withStyle(ChatFormatting.GRAY));
            return InteractionResultHolder.success(player.getItemInHand(hand));
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.create_stressbound.kinetic_binder.tooltip")
            .withStyle(ChatFormatting.GRAY));
    }

    private static void storeSelection(ServerPlayer player, ResourceKey<Level> dimension, BlockPos pos) {
        CompoundTag root = player.getPersistentData().getCompound(CreateStressbound.MODID);
        CompoundTag selection = new CompoundTag();
        selection.putString(DIMENSION_KEY, dimension.location().toString());
        selection.putLong(POSITION_KEY, pos.asLong());
        root.put(PLAYER_BINDER_DATA, selection);
        player.getPersistentData().put(CreateStressbound.MODID, root);
    }

    private static Selection loadSelection(ServerPlayer player) {
        CompoundTag root = player.getPersistentData().getCompound(CreateStressbound.MODID);
        if (!root.contains(PLAYER_BINDER_DATA, CompoundTag.TAG_COMPOUND)) {
            return null;
        }

        CompoundTag selection = root.getCompound(PLAYER_BINDER_DATA);
        ResourceLocation dimensionId = ResourceLocation.parse(selection.getString(DIMENSION_KEY));
        ResourceKey<Level> dimension = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimensionId);
        BlockPos pos = BlockPos.of(selection.getLong(POSITION_KEY));
        return new Selection(dimension, pos);
    }

    private static void clearSelection(Player player) {
        CompoundTag root = player.getPersistentData().getCompound(CreateStressbound.MODID);
        root.remove(PLAYER_BINDER_DATA);
        player.getPersistentData().put(CreateStressbound.MODID, root);
    }

    private static String describe(ResourceKey<Level> dimension, BlockPos pos) {
        return dimension.location() + " [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]";
    }

    private record Selection(ResourceKey<Level> dimension, BlockPos pos) {
    }
}
