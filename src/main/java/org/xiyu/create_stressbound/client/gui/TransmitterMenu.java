package org.xiyu.create_stressbound.client.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.xiyu.create_stressbound.content.kinetics.StressTransmitterBlockEntity;
import org.xiyu.create_stressbound.registry.StressboundMenuTypes;

public class TransmitterMenu extends AbstractContainerMenu {
    private final BlockPos blockPos;

    public TransmitterMenu(int containerId, Inventory playerInventory, BlockPos blockPos) {
        super(StressboundMenuTypes.TRANSMITTER.get(), containerId);
        this.blockPos = blockPos;
    }

    public TransmitterMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        super(StressboundMenuTypes.TRANSMITTER.get(), containerId);
        this.blockPos = extraData.readBlockPos();
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        BlockEntity blockEntity = player.level().getBlockEntity(blockPos);
        if (!(blockEntity instanceof StressTransmitterBlockEntity)) {
            return false;
        }
        return player.distanceToSqr(
            blockPos.getX() + 0.5D,
            blockPos.getY() + 0.5D,
            blockPos.getZ() + 0.5D
        ) <= 64.0D;
    }
}
