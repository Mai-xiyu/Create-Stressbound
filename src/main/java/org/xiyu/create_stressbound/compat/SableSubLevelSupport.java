package org.xiyu.create_stressbound.compat;

import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

final class SableSubLevelSupport {
    private SableSubLevelSupport() {
    }

    static Vec3 projectBlockCenter(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return pos == null ? Vec3.ZERO : Vec3.atCenterOf(pos);
        }
        try {
            return Sable.HELPER.projectOutOfSubLevel(level, Vec3.atCenterOf(pos));
        } catch (LinkageError | RuntimeException ignored) {
            return Vec3.atCenterOf(pos);
        }
    }

    static boolean isInSubLevel(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return false;
        }
        try {
            return Sable.HELPER.getContaining(level, Vec3.atCenterOf(pos)) != null;
        } catch (LinkageError | RuntimeException ignored) {
            return false;
        }
    }
}
