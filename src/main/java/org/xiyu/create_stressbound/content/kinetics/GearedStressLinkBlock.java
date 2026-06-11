package org.xiyu.create_stressbound.content.kinetics;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Encased-cogwheel-style base for the geared stress link blocks. Meshes with cogwheels on the
 * four faces perpendicular to its axis and exposes wrench-toggleable shaft stubs on both axis
 * ends, mirroring Create's {@link EncasedCogwheelBlock} interaction model.
 */
public abstract class GearedStressLinkBlock extends RotatedPillarKineticBlock implements ICogWheel {
    public static final BooleanProperty TOP_SHAFT = EncasedCogwheelBlock.TOP_SHAFT;
    public static final BooleanProperty BOTTOM_SHAFT = EncasedCogwheelBlock.BOTTOM_SHAFT;

    protected GearedStressLinkBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
            .setValue(TOP_SHAFT, false)
            .setValue(BOTTOM_SHAFT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(TOP_SHAFT, BOTTOM_SHAFT));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AXIS, getAxisForPlacement(context));
    }

    /**
     * Mirrors Create's CogWheelBlock placement: sneaking uses the clicked face axis, placing
     * against a small cog aligns with it, otherwise neighbor shafts are preferred and the
     * clicked face axis is the fallback. The parent class would fall back to the player's
     * looking direction instead, which made horizontal placement unreliable.
     */
    protected Axis getAxisForPlacement(BlockPlaceContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            return context.getClickedFace().getAxis();
        }

        BlockPos placedOnPos = context.getClickedPos().relative(context.getClickedFace().getOpposite());
        BlockState placedAgainst = context.getLevel().getBlockState(placedOnPos);
        if (ICogWheel.isSmallCog(placedAgainst)) {
            return ((IRotate) placedAgainst.getBlock()).getRotationAxis(placedAgainst);
        }

        Axis preferredAxis = getPreferredAxis(context);
        return preferredAxis != null ? preferredAxis : context.getClickedFace().getAxis();
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (context.getClickedFace().getAxis() != state.getValue(AXIS)) {
            return super.onWrenched(state, context);
        }

        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos();
        KineticBlockEntity.switchToBlockState(level, pos, state.cycle(
            context.getClickedFace().getAxisDirection() == AxisDirection.POSITIVE ? TOP_SHAFT : BOTTOM_SHAFT));
        IWrenchable.playRotateSound(level, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
        originalState = swapShaftsForRotation(originalState, Rotation.CLOCKWISE_90, targetedFace.getAxis());
        return originalState.setValue(AXIS,
            VoxelShaper.axisAsFace(originalState.getValue(AXIS))
                .getClockWise(targetedFace.getAxis())
                .getAxis());
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(AXIS)
            && state.getValue(face.getAxisDirection() == AxisDirection.POSITIVE ? TOP_SHAFT : BOTTOM_SHAFT);
    }

    @Override
    protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
        if (oldState.getBlock() instanceof GearedStressLinkBlock
            && newState.getBlock() instanceof GearedStressLinkBlock) {
            if (newState.getValue(TOP_SHAFT) != oldState.getValue(TOP_SHAFT)) {
                return false;
            }
            if (newState.getValue(BOTTOM_SHAFT) != oldState.getValue(BOTTOM_SHAFT)) {
                return false;
            }
        }
        return super.areStatesKineticallyEquivalent(oldState, newState);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader worldIn, BlockPos pos) {
        return CogWheelBlock.isValidCogwheelPosition(false, worldIn, pos, state.getValue(AXIS));
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public boolean isSmallCog() {
        return true;
    }

    @Override
    public boolean hideStressImpact() {
        return true;
    }

    protected BlockState swapShafts(BlockState state) {
        boolean bottom = state.getValue(BOTTOM_SHAFT);
        boolean top = state.getValue(TOP_SHAFT);
        return state.setValue(BOTTOM_SHAFT, top).setValue(TOP_SHAFT, bottom);
    }

    protected BlockState swapShaftsForRotation(BlockState state, Rotation rotation, Axis rotationAxis) {
        if (rotation == Rotation.NONE) {
            return state;
        }

        Axis axis = state.getValue(AXIS);
        if (axis == rotationAxis) {
            return state;
        }

        if (rotation == Rotation.CLOCKWISE_180) {
            return swapShafts(state);
        }

        boolean clockwise = rotation == Rotation.CLOCKWISE_90;
        if (rotationAxis == Axis.X) {
            if (axis == Axis.Z && !clockwise || axis == Axis.Y && clockwise) {
                return swapShafts(state);
            }
        } else if (rotationAxis == Axis.Y) {
            if (axis == Axis.X && !clockwise || axis == Axis.Z && clockwise) {
                return swapShafts(state);
            }
        } else if (rotationAxis == Axis.Z) {
            if (axis == Axis.Y && !clockwise || axis == Axis.X && clockwise) {
                return swapShafts(state);
            }
        }
        return state;
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        Axis axis = state.getValue(AXIS);
        if (axis == Axis.X && mirror == Mirror.FRONT_BACK
            || axis == Axis.Z && mirror == Mirror.LEFT_RIGHT) {
            return swapShafts(state);
        }
        return state;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        state = swapShaftsForRotation(state, rotation, Axis.Y);
        return super.rotate(state, rotation);
    }
}
