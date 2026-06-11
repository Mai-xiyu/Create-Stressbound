package org.xiyu.create_stressbound.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fallback (non-Flywheel) renderer for the geared stress link blocks, mirroring Create's
 * EncasedCogRenderer for small cogs: a shaftless cogwheel core plus optional shaft stubs
 * on the axis ends.
 */
public class GearedStressLinkRenderer extends KineticBlockEntityRenderer<KineticBlockEntity> {
    public GearedStressLinkRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(KineticBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
        if (VisualizationManager.supportsVisualization(be.getLevel())) {
            return;
        }

        BlockState blockState = be.getBlockState();
        if (!(blockState.getBlock() instanceof IRotate def)) {
            return;
        }

        Axis axis = getRotationAxisOf(be);
        BlockPos pos = be.getBlockPos();
        float angle = getAngleForBe(be, pos, axis);

        for (Direction d : Iterate.directionsInAxis(axis)) {
            if (!def.hasShaftTowards(be.getLevel(), pos, blockState, d)) {
                continue;
            }
            SuperByteBuffer shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, blockState, d);
            kineticRotationTransform(shaft, be, axis, angle, light);
            shaft.renderInto(ms, buffer.getBuffer(RenderType.solid()));
        }
    }

    @Override
    protected SuperByteBuffer getRotatedModel(KineticBlockEntity be, BlockState state) {
        return CachedBuffers.partialFacingVertical(AllPartialModels.SHAFTLESS_COGWHEEL, state,
            Direction.fromAxisAndDirection(state.getValue(RotatedPillarKineticBlock.AXIS), AxisDirection.POSITIVE));
    }
}
