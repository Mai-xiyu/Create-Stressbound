package org.xiyu.create_stressbound.client;

import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.xiyu.create_stressbound.ponder.StressboundPonderPlugin;

public final class StressboundClient {
    private StressboundClient() {
    }

    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> PonderIndex.addPlugin(new StressboundPonderPlugin()));
    }
}
