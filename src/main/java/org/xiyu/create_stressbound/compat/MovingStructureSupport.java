package org.xiyu.create_stressbound.compat;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.neoforged.fml.ModList;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.xiyu.create_stressbound.content.link.AnchorKind;

public final class MovingStructureSupport {
    public static final String AERONAUTICS_MODID = "aeronautics";
    public static final String LEGACY_AERONAUTICS_MODID = "create_aeronautics";
    public static final String SIMULATED_MODID = "simulated";
    public static final String SABLE_MODID = "sable";
    public static final String VALKYRIEN_SKIES_MODID = "valkyrienskies";

    private MovingStructureSupport() {
    }

    public static boolean isAeronauticsLoaded() {
        return ModList.get().isLoaded(AERONAUTICS_MODID)
            || ModList.get().isLoaded(LEGACY_AERONAUTICS_MODID)
            || ModList.get().isLoaded(SIMULATED_MODID);
    }

    public static boolean isSimulatedLoaded() {
        return ModList.get().isLoaded(SIMULATED_MODID);
    }

    public static boolean isSableLoaded() {
        return ModList.get().isLoaded(SABLE_MODID);
    }

    public static boolean isValkyrienSkiesLoaded() {
        return ModList.get().isLoaded(VALKYRIEN_SKIES_MODID);
    }

    public static AnchorKind classify(AbstractContraptionEntity entity) {
        if (entity instanceof CarriageContraptionEntity) {
            return AnchorKind.CREATE_TRAIN;
        }
        if (isAeronauticsLoaded() && isAeronauticsEntity(entity)) {
            return AnchorKind.AERONAUTICS_CRAFT;
        }
        return AnchorKind.CREATE_CONTRAPTION;
    }

    public static String describeEntityHandle(AbstractContraptionEntity entity) {
        if (entity instanceof CarriageContraptionEntity carriage && carriage.trainId != null) {
            return carriage.trainId.toString();
        }
        return entity.getClass().getName();
    }

    public static Vec3 projectBlockCenter(Level level, BlockPos pos) {
        if (isSableLoaded() || isSimulatedLoaded() || isAeronauticsLoaded()) {
            return SableSubLevelSupport.projectBlockCenter(level, pos);
        }
        return Vec3.atCenterOf(pos);
    }

    public static boolean isInSubLevel(Level level, BlockPos pos) {
        if (isSableLoaded() || isSimulatedLoaded() || isAeronauticsLoaded()) {
            return SableSubLevelSupport.isInSubLevel(level, pos);
        }
        return false;
    }

    private static boolean isAeronauticsEntity(AbstractContraptionEntity entity) {
        String name = entity.getClass().getName().toLowerCase(Locale.ROOT);
        return name.contains("aeronaut") || name.contains("simulated");
    }

    public static String describeRuntime() {
        return "Moving-structure bridges detected: aeronautics=" + isAeronauticsLoaded()
            + ", simulated=" + isSimulatedLoaded()
            + ", sable=" + isSableLoaded()
            + ", valkyrienskies=" + isValkyrienSkiesLoaded()
            + ", implemented_anchors=static_block,create_contraption,create_train,aeronautics_heuristic,sable_sublevel_visuals";
    }
}
