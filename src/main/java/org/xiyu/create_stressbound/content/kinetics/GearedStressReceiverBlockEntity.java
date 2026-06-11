package org.xiyu.create_stressbound.content.kinetics;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.xiyu.create_stressbound.registry.StressboundBlockEntities;

public class GearedStressReceiverBlockEntity extends StressReceiverBlockEntity {
    public GearedStressReceiverBlockEntity(BlockPos pos, BlockState blockState) {
        super(StressboundBlockEntities.GEARED_STRESS_RECEIVER.get(), pos, blockState);
    }

    @Override
    protected float convertOutputSpeed(float speed) {
        // Axis-based cogwheel output: no facing conversion, reverse toggle still applies upstream.
        return speed;
    }
}
