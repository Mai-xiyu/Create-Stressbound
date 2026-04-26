package org.xiyu.create_stressbound.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.xiyu.create_stressbound.CreateStressbound;
import org.xiyu.create_stressbound.registry.StressboundItems;

public final class StressboundPonderScenes {
    private StressboundPonderScenes() {
    }

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(
                CreateStressbound.id("stress_transmitter"),
                CreateStressbound.id("stress_receiver"),
                CreateStressbound.id("kinetic_binder")
            )
            .addStoryBoard("stressbound/linking", StressboundPonderScenes::linking,
                StressboundPonderTags.STRESS_LINKS, AllCreatePonderTags.KINETIC_RELAYS);

        helper.forComponents(
                CreateStressbound.id("stress_transmitter"),
                CreateStressbound.id("stress_receiver")
            )
            .addStoryBoard("stressbound/moving", StressboundPonderScenes::moving,
                StressboundPonderTags.STRESS_LINKS, AllCreatePonderTags.CONTRAPTION_ACTOR);
    }

    public static void linking(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("stressbound_linking", "Binding a remote stress link");
        scene.configureBasePlate(0, 0, 8);
        scene.scaleSceneView(0.82F);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos motor = util.grid().at(0, 1, 2);
        BlockPos transmitter = util.grid().at(2, 1, 2);
        BlockPos receiver = util.grid().at(5, 1, 2);
        BlockPos output = util.grid().at(6, 1, 2);
        Selection inputNetwork = util.select().fromTo(motor, transmitter);
        Selection receiverNetwork = util.select().fromTo(receiver, output);
        ItemStack binder = StressboundItems.KINETIC_BINDER.get().getDefaultInstance();

        scene.world().showSection(inputNetwork, Direction.DOWN);
        scene.world().setKineticSpeed(inputNetwork, 32);
        scene.idle(15);

        scene.overlay().showOutlineWithText(util.select().position(transmitter), 70)
            .text("The Stress Transmitter reads speed and spare SU from its local Create network.")
            .pointAt(util.vector().blockSurface(transmitter, Direction.UP))
            .colored(PonderPalette.INPUT);
        scene.idle(80);

        scene.world().showSection(receiverNetwork, Direction.DOWN);
        scene.idle(10);

        scene.overlay().showControls(util.vector().blockSurface(transmitter, Direction.NORTH), Pointing.DOWN, 35)
            .rightClick()
            .withItem(binder);
        scene.overlay().showText(45)
            .text("Right-click the transmitter with the Kinetic Binder to store it.")
            .pointAt(util.vector().blockSurface(transmitter, Direction.NORTH))
            .placeNearTarget();
        scene.idle(55);

        scene.overlay().showControls(util.vector().blockSurface(receiver, Direction.NORTH), Pointing.DOWN, 35)
            .rightClick()
            .withItem(binder);
        scene.overlay().showText(55)
            .text("Then right-click a receiver. The link reserves a configurable SU budget.")
            .pointAt(util.vector().blockSurface(receiver, Direction.NORTH))
            .placeNearTarget();
        scene.idle(65);

        Vec3 from = util.vector().centerOf(transmitter).add(0, 0.45, 0);
        Vec3 to = util.vector().centerOf(receiver).add(0, 0.45, 0);
        scene.overlay().showBigLine(PonderPalette.BLUE, from, to, 70);
        scene.world().setKineticSpeed(receiverNetwork, 32);
        scene.overlay().showText(70)
            .text("When both endpoints are loaded and not disabled by redstone, the receiver outputs the remote speed.")
            .pointAt(util.vector().blockSurface(output, Direction.UP))
            .placeNearTarget();
        scene.idle(80);

        scene.addKeyframe();
        scene.overlay().showOutline(PonderPalette.RED, "redstone_stop", util.select().position(receiver), 50);
        scene.overlay().showText(60)
            .text("Redstone can pause either side, and overload protection keeps the server-friendly budget predictable.")
            .pointAt(util.vector().blockSurface(receiver, Direction.UP))
            .placeNearTarget();
        scene.idle(70);
    }

    public static void moving(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("stressbound_moving", "Links on moving structures");
        scene.configureBasePlate(0, 0, 8);
        scene.scaleSceneView(0.82F);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos transmitter = util.grid().at(2, 1, 2);
        BlockPos receiver = util.grid().at(5, 1, 2);
        Selection platform = util.select().fromTo(1, 1, 1, 6, 1, 3);

        var movingSection = scene.world().showIndependentSection(platform, Direction.DOWN);
        scene.world().setKineticSpeed(platform, 24);
        scene.idle(20);

        scene.overlay().showOutlineWithText(util.select().position(transmitter), 55)
            .text("Stressbound endpoints can be carried by Create Contraptions, trains, and compatible Aeronautics-style bodies.")
            .pointAt(util.vector().blockSurface(transmitter, Direction.UP))
            .colored(PonderPalette.BLUE);
        scene.idle(65);

        scene.world().moveSection(movingSection, util.vector().of(0, 0.75, 0), 30);
        scene.idle(35);
        scene.world().moveSection(movingSection, util.vector().of(1.5, 0, 0), 45);

        scene.overlay().showBigLine(PonderPalette.BLUE,
            util.vector().centerOf(transmitter).add(0, 0.6, 0),
            util.vector().centerOf(receiver).add(1.5, 0.6, 0),
            65);
        scene.overlay().showText(70)
            .text("While moving, the link follows runtime endpoint ids instead of assuming fixed block positions.")
            .pointAt(util.vector().centerOf(receiver).add(1.5, 0.8, 0))
            .placeNearTarget();
        scene.idle(80);

        scene.addKeyframe();
        scene.overlay().showText(70)
            .text("For best performance, keep both chunks loaded and avoid remote loops between receivers and transmitters.")
            .independent(20)
            .colored(PonderPalette.GREEN);
        scene.idle(80);
    }
}
