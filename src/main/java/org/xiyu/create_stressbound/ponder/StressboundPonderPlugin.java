package org.xiyu.create_stressbound.ponder;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import org.xiyu.create_stressbound.CreateStressbound;

public final class StressboundPonderPlugin implements PonderPlugin {
    @Override
    public String getModId() {
        return CreateStressbound.MODID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        StressboundPonderScenes.register(helper);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        StressboundPonderTags.register(helper);
    }
}
