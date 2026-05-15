package org.xiyu.create_stressbound.client;

import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.xiyu.create_stressbound.CreateStressbound;

@EventBusSubscriber(modid = CreateStressbound.MODID, value = Dist.CLIENT)
public final class StressboundClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.EnumValue<LineVisibility> LINE_VISIBILITY = BUILDER
        .comment("Controls when stress link lines are rendered. | 控制应力链路线条在客户端何时显示。")
        .defineEnum("visual.lineVisibility", LineVisibility.BINDER_OR_GOGGLES);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static LineVisibility lineVisibility = LineVisibility.BINDER_OR_GOGGLES;

    private StressboundClientConfig() {
    }

    @SubscribeEvent
    static void onLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        lineVisibility = LINE_VISIBILITY.get();
    }

    public static boolean shouldRenderLinks(Player player, boolean holdingBinder) {
        boolean wearingGoggles = player != null && GogglesItem.isWearingGoggles(player);
        return switch (lineVisibility) {
            case BINDER_OR_GOGGLES -> holdingBinder || wearingGoggles;
            case BINDER_ONLY -> holdingBinder;
            case GOGGLES_ONLY -> wearingGoggles;
            case ALWAYS -> true;
            case NEVER -> false;
        };
    }

    public enum LineVisibility {
        BINDER_OR_GOGGLES,
        BINDER_ONLY,
        GOGGLES_ONLY,
        ALWAYS,
        NEVER
    }
}
