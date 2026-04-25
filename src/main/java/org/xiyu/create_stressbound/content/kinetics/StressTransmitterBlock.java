package org.xiyu.create_stressbound.content.kinetics;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.xiyu.create_stressbound.registry.StressboundBlockEntities;

public class StressTransmitterBlock extends DirectionalKineticBlock implements IBE<StressTransmitterBlockEntity> {
    public StressTransmitterBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return state.getValue(FACING) == face;
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
            IBE.onRemove(state, level, pos, newState);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
