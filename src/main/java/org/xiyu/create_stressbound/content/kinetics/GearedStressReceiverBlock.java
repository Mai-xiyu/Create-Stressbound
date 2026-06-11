package org.xiyu.create_stressbound.content.kinetics;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.xiyu.create_stressbound.content.link.StressLinkService;
import org.xiyu.create_stressbound.registry.StressboundBlockEntities;

public class GearedStressReceiverBlock extends GearedStressLinkBlock implements IBE<StressReceiverBlockEntity> {
    public GearedStressReceiverBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public Class<StressReceiverBlockEntity> getBlockEntityClass() {
        return StressReceiverBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends StressReceiverBlockEntity> getBlockEntityType() {
        return StressboundBlockEntities.GEARED_STRESS_RECEIVER.get();
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            if (!isMoving && level instanceof ServerLevel serverLevel) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof StressReceiverBlockEntity receiver) {
                    StressLinkService.removeLinksForRemovedReceiver(serverLevel, receiver);
                }
            }
            IBE.onRemove(state, level, pos, newState);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (!player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        withBlockEntityDo(level, pos, be -> player.openMenu(be, pos));
        return InteractionResult.SUCCESS;
    }
}
