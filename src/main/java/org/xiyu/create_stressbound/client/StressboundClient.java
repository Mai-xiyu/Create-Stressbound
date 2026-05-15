package org.xiyu.create_stressbound.client;

import com.simibubi.create.content.kinetics.base.ShaftRenderer;
import com.simibubi.create.content.kinetics.base.ShaftVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.xiyu.create_stressbound.client.gui.ReceiverScreen;
import org.xiyu.create_stressbound.client.gui.TransmitterScreen;
import org.xiyu.create_stressbound.client.visual.StressLinkParticleRenderer;
import org.xiyu.create_stressbound.ponder.StressboundPonderPlugin;
import org.xiyu.create_stressbound.registry.StressboundBlockEntities;
import org.xiyu.create_stressbound.registry.StressboundMenuTypes;

public final class StressboundClient {
    private StressboundClient() {
    }

    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            PonderIndex.addPlugin(new StressboundPonderPlugin());
            registerVisualizers();
        });

        // Register particle renderer on the game event bus
        NeoForge.EVENT_BUS.addListener(StressLinkParticleRenderer::onRenderLevelStage);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(StressboundBlockEntities.STRESS_TRANSMITTER.get(), ShaftRenderer::new);
        event.registerBlockEntityRenderer(StressboundBlockEntities.STRESS_RECEIVER.get(), ShaftRenderer::new);
    }

    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(StressboundMenuTypes.RECEIVER.get(), ReceiverScreen::new);
        event.register(StressboundMenuTypes.TRANSMITTER.get(), TransmitterScreen::new);
    }

    private static void registerVisualizers() {
        SimpleBlockEntityVisualizer.builder(StressboundBlockEntities.STRESS_TRANSMITTER.get())
            .factory(ShaftVisual::new)
            .apply();
        SimpleBlockEntityVisualizer.builder(StressboundBlockEntities.STRESS_RECEIVER.get())
            .factory(ShaftVisual::new)
            .apply();
    }
}
