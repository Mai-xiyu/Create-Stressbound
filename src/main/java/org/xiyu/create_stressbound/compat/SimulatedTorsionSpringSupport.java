package org.xiyu.create_stressbound.compat;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.OptionalInt;

public final class SimulatedTorsionSpringSupport {
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

    private static KineticBlockEntity findTorsionSpringParent(KineticBlockEntity blockEntity) {
        if (!isSimulatedCompatLoaded() || blockEntity == null || !blockEntity.hasNetwork()) {
            return null;
        }

        for (KineticBlockEntity source : blockEntity.getOrCreateNetwork().sources.keySet()) {
            KineticBlockEntity parent = getTorsionSpringParent(source);
            if (parent != null) {
                return parent;
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

    private static boolean isSimulatedCompatLoaded() {
        return MovingStructureSupport.isSimulatedLoaded() || MovingStructureSupport.isAeronauticsLoaded();
    }
}
