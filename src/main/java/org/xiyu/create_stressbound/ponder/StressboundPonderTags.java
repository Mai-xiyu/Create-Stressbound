package org.xiyu.create_stressbound.ponder;

import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import org.xiyu.create_stressbound.CreateStressbound;
import org.xiyu.create_stressbound.registry.StressboundItems;

public final class StressboundPonderTags {
    public static final ResourceLocation STRESS_LINKS = CreateStressbound.id("stress_links");

    private StressboundPonderTags() {
    }

    public static void register(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(STRESS_LINKS)
            .title("Remote Stress Links")
            .description("Components used to bridge Create rotational force between static and moving structures")
            .item(StressboundItems.KINETIC_BINDER.get())
            .addToIndex()
            .register();

        helper.addToTag(STRESS_LINKS)
            .add(CreateStressbound.id("stress_transmitter"))
            .add(CreateStressbound.id("stress_receiver"))
            .add(CreateStressbound.id("kinetic_binder"));

        helper.addToTag(AllCreatePonderTags.KINETIC_RELAYS)
            .add(CreateStressbound.id("stress_transmitter"))
            .add(CreateStressbound.id("stress_receiver"));

        helper.addToTag(AllCreatePonderTags.CONTRAPTION_ACTOR)
            .add(CreateStressbound.id("stress_transmitter"))
            .add(CreateStressbound.id("stress_receiver"));

        helper.addToTag(AllCreatePonderTags.TRAIN_RELATED)
            .add(CreateStressbound.id("stress_transmitter"))
            .add(CreateStressbound.id("stress_receiver"));
    }
}
