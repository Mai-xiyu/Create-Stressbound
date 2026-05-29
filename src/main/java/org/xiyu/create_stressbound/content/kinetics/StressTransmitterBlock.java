package org.xiyu.create_stressbound.content.kinetics;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.xiyu.create_stressbound.content.link.StressLinkService;
import org.xiyu.create_stressbound.registry.StressboundBlockEntities;

public class StressTransmitterBlock extends DirectionalKineticBlock implements IBE<StressTransmitterBlockEntity> {
    public StressTransmitterBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return state.getValue(FACING).getAxis() == face.getAxis();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hideStressImpact() {
        return true;
    }

    @Override
    public Class<StressTransmitterBlockEntity> getBlockEntityClass() {
        return StressTransmitterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends StressTransmitterBlockEntity> getBlockEntityType() {
        return StressboundBlockEntities.STRESS_TRANSMITTER.get();
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            if (!isMoving && level instanceof ServerLevel serverLevel) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof StressTransmitterBlockEntity transmitter) {
                    StressLinkService.removeLinksForRemovedTransmitter(serverLevel, transmitter);
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
