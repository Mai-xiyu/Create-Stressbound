package org.xiyu.create_stressbound.compat;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.OptionalInt;

public final class SimulatedTorsionSpringSupport {
    private static final float MIN_ACTIVE_SPEED = 0.01F;
    private static final String TORSION_OUTPUT_CLASS =
        "dev.simulated_team.simulated.content.blocks.torsion_spring.TorsionSpringBlockEntity$Output";

    private SimulatedTorsionSpringSupport() {
    }

    public static OptionalInt getInputSideStressBudget(KineticBlockEntity blockEntity, int creativeStressBudget) {
        KineticBlockEntity parent = findTorsionSpringParent(blockEntity);
        if (parent == null || !parent.hasNetwork()) {
            return OptionalInt.empty();
        }

        KineticNetwork parentNetwork = parent.getOrCreateNetwork();
        if (hasCreativeSource(parentNetwork)) {
            return OptionalInt.of(creativeStressBudget);
        }

        float availableStress = Math.max(parentNetwork.calculateCapacity() - parentNetwork.calculateStress(), 0.0F);
        return OptionalInt.of(Math.max(Math.round(availableStress), 0));
    }

    public static boolean hasCreativeInputSource(KineticBlockEntity blockEntity) {
        KineticBlockEntity parent = findTorsionSpringParent(blockEntity);
        return parent != null && parent.hasNetwork() && hasCreativeSource(parent.getOrCreateNetwork());
    }

    public static boolean isActiveReturnOutput(KineticBlockEntity blockEntity) {
        return hasActiveTorsionSpringOutput(blockEntity);
    }

    public static boolean hasActiveTorsionSpringOutput(KineticBlockEntity blockEntity) {
        KineticBlockEntity output = findTorsionSpringOutput(blockEntity);
        if (output == null || output.isOverStressed()) {
            return false;
        }

        KineticBlockEntity parent = getTorsionSpringParent(output);
        if (parent == null || parent.isOverStressed()) {
            return false;
        }

        return isActiveSpeed(getGeneratedSpeed(output))
            || isActiveSpeed(output.getSpeed())
            || isActiveSpeed(output.getTheoreticalSpeed())
            || isActiveSpeed(getFloatField(output, "queuedSpeed"));
    }

    private static KineticBlockEntity findTorsionSpringParent(KineticBlockEntity blockEntity) {
        KineticBlockEntity output = findTorsionSpringOutput(blockEntity);
        return output == null ? null : getTorsionSpringParent(output);
    }

    private static KineticBlockEntity findTorsionSpringOutput(KineticBlockEntity blockEntity) {
        if (!isSimulatedCompatLoaded() || blockEntity == null || !blockEntity.hasNetwork()) {
            return null;
        }

        for (KineticBlockEntity source : blockEntity.getOrCreateNetwork().sources.keySet()) {
            if (isTorsionSpringOutput(source)) {
                return source;
            }
        }
        return null;
    }

    private static KineticBlockEntity getTorsionSpringParent(KineticBlockEntity source) {
        if (!isTorsionSpringOutput(source)) {
            return null;
        }
        try {
            Method method = source.getClass().getMethod("getParentBlockEntity");
            Object parent = method.invoke(source);
            return parent instanceof KineticBlockEntity parentKinetic ? parentKinetic : null;
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | RuntimeException
                 | LinkageError ignored) {
            return null;
        }
    }

    private static boolean isTorsionSpringOutput(KineticBlockEntity source) {
        String className = source.getClass().getName();
        return TORSION_OUTPUT_CLASS.equals(className)
            || className.endsWith(".content.blocks.torsion_spring.TorsionSpringBlockEntity$Output");
    }

    private static boolean hasCreativeSource(KineticNetwork network) {
        return network.sources.keySet().stream().anyMatch(CreativeMotorBlockEntity.class::isInstance);
    }

    private static float getGeneratedSpeed(KineticBlockEntity blockEntity) {
        return blockEntity instanceof GeneratingKineticBlockEntity generator
            ? generator.getGeneratedSpeed()
            : blockEntity.getTheoreticalSpeed();
    }

    private static float getFloatField(Object target, String fieldName) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.getFloat(target);
        } catch (IllegalAccessException | NoSuchFieldException | RuntimeException | LinkageError ignored) {
            return 0.0F;
        }
    }

    private static boolean isActiveSpeed(float speed) {
        return Float.isFinite(speed) && Math.abs(speed) >= MIN_ACTIVE_SPEED;
    }

    private static boolean isSimulatedCompatLoaded() {
        return MovingStructureSupport.isSimulatedLoaded() || MovingStructureSupport.isAeronauticsLoaded();
    }
}
