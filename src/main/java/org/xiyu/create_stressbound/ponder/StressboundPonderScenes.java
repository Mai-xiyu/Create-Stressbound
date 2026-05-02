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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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

        helper.forComponents(
                CreateStressbound.id("stress_transmitter"),
                CreateStressbound.id("stress_receiver"),
                CreateStressbound.id("kinetic_binder")
            )
            .addStoryBoard("stressbound/tumbler", StressboundPonderScenes::tumbler,
                StressboundPonderTags.STRESS_LINKS, AllCreatePonderTags.CONTRAPTION_ACTOR,
                AllCreatePonderTags.KINETIC_RELAYS);
    }

    public static void linking(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("stressbound_linking", "Binding a remote stress link");
        scene.configureBasePlate(0, 0, 8);
        scene.scaleSceneView(0.82F);
        addCheckerboardBase(scene, util, 8);
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
            .text("When both endpoints are loaded, redstone strength can throttle the remote speed and budget.")
            .pointAt(util.vector().blockSurface(output, Direction.UP))
            .placeNearTarget();
        scene.idle(80);

        scene.addKeyframe();
        scene.overlay().showOutline(PonderPalette.RED, "redstone_stop", util.select().position(receiver), 50);
        scene.overlay().showText(60)
            .text("A full redstone signal stops an endpoint; weaker signals scale it down smoothly.")
            .pointAt(util.vector().blockSurface(receiver, Direction.UP))
            .placeNearTarget();
        scene.idle(70);
    }

    public static void moving(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("stressbound_moving", "Links on moving structures");
        scene.configureBasePlate(0, 0, 8);
        scene.scaleSceneView(0.82F);
        addCheckerboardBase(scene, util, 8);
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

    public static void tumbler(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("stressbound_tumbler", "Compass tumbler template");
        scene.configureBasePlate(0, 0, 13);
        scene.scaleSceneView(0.5F);
        addCheckerboardBase(scene, util, 13);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos motor = util.grid().at(1, 2, 1);
        BlockPos transmitter = util.grid().at(1, 2, 2);
        BlockPos sensor = util.grid().at(6, 4, 6);
        BlockPos bodyCenter = util.grid().at(6, 4, 6);
        BlockPos westLinkTransmitter = util.grid().at(5, 4, 6);
        BlockPos eastLinkReceiver = util.grid().at(9, 4, 6);
        BlockPos eastReceiver = util.grid().at(10, 4, 6);
        BlockPos eastLamp = util.grid().at(9, 5, 6);
        BlockPos westLinkReceiver = util.grid().at(3, 4, 6);
        BlockPos westReceiver = util.grid().at(2, 4, 6);
        BlockPos westLamp = util.grid().at(3, 5, 6);
        BlockPos centralReceiver = util.grid().at(6, 5, 6);
        BlockPos gyroscopicBearing = util.grid().at(6, 6, 6);

        Selection fixedInput = util.select().fromTo(0, 1, 0, 4, 2, 2);
        Selection fixedKinetics = util.select().fromTo(0, 2, 1, 4, 2, 2);
        Selection foundation = util.select().fromTo(1, 3, 5, 11, 3, 7)
            .add(util.select().fromTo(5, 3, 1, 7, 3, 11))
            .add(util.select().fromTo(4, 3, 4, 8, 3, 8));
        Selection centerStack = util.select().fromTo(6, 1, 6, 6, 6, 6);
        Selection sailCross = util.select().fromTo(3, 7, 6, 9, 7, 6)
            .add(util.select().fromTo(6, 7, 3, 6, 7, 9));
        Selection linkTransmitters = util.select().position(5, 4, 6)
            .add(util.select().position(7, 4, 6))
            .add(util.select().position(6, 4, 5))
            .add(util.select().position(6, 4, 7));
        Selection linkReceivers = util.select().position(9, 4, 6)
            .add(util.select().position(3, 4, 6))
            .add(util.select().position(6, 4, 9))
            .add(util.select().position(6, 4, 3));
        Selection receivers = util.select().fromTo(10, 4, 6, 11, 4, 6)
            .add(util.select().fromTo(1, 4, 6, 2, 4, 6))
            .add(util.select().fromTo(6, 4, 1, 6, 4, 2))
            .add(util.select().fromTo(6, 4, 10, 6, 4, 11))
            .add(util.select().position(centralReceiver))
            .add(util.select().position(gyroscopicBearing));
        Selection lamps = util.select().position(eastLamp)
            .add(util.select().position(westLamp))
            .add(util.select().position(6, 5, 3))
            .add(util.select().position(6, 5, 9));
        Selection tumblerBody = util.select().position(sensor)
            .add(foundation)
            .add(centerStack)
            .add(sailCross)
            .add(linkTransmitters)
            .add(linkReceivers)
            .add(receivers)
            .add(lamps);
        Selection tumblerDrive = receivers.copy();
        Selection westStopSignal = util.select().position(westLinkReceiver)
            .add(util.select().position(westLamp));

        scene.world().showSection(fixedInput, Direction.DOWN);
        scene.world().setKineticSpeed(fixedKinetics, 64);
        scene.overlay().showOutlineWithText(util.select().position(transmitter), 65)
            .text("A small transmitter bank provides the remote stress source for the whole chassis.")
            .pointAt(util.vector().blockSurface(transmitter, Direction.UP))
            .colored(PonderPalette.INPUT);
        scene.idle(75);

        var body = scene.world().showIndependentSection(tumblerBody, Direction.DOWN);
        scene.world().setKineticSpeed(tumblerDrive, 64);
        scene.idle(15);
        scene.overlay().showOutlineWithText(foundation, 65)
            .text("The andesite cross is the moving chassis, so the four thrusters read as one body.")
            .pointAt(util.vector().centerOf(bodyCenter).add(0, 0.25, 0))
            .colored(PonderPalette.BLUE);
        scene.idle(75);
        scene.overlay().showOutlineWithText(util.select().fromTo(centralReceiver, gyroscopicBearing), 65)
            .text("The center stack adds a vertical receiver and gyroscopic propeller bearing as the tumbler core.")
            .pointAt(util.vector().blockSurface(gyroscopicBearing, Direction.UP))
            .colored(PonderPalette.GREEN);
        scene.idle(75);
        scene.overlay().showBigLine(PonderPalette.BLUE,
            util.vector().centerOf(transmitter).add(0, 0.55, 0),
            util.vector().centerOf(sensor).add(0, 0.55, 0),
            65);
        scene.overlay().showText(70)
            .text("Bind that transmitter bank to all four receivers; the redstone links only carry tilt signals.")
            .pointAt(util.vector().centerOf(sensor).add(0, 0.7, 0))
            .placeNearTarget();
        scene.idle(80);

        scene.addKeyframe();
        scene.overlay().showOutline(PonderPalette.BLUE, "four_receivers", receivers, 55);
        scene.overlay().showOutlineWithText(util.select().position(sensor), 80)
            .text("The sensor outputs analog redstone on each face proportional to that side's tilt.")
            .pointAt(util.vector().blockSurface(sensor, Direction.UP))
            .colored(PonderPalette.GREEN);
        scene.idle(85);

        scene.overlay().showOutline(PonderPalette.GREEN, "link_transmitters", linkTransmitters, 65);
        scene.overlay().showText(75)
            .text("Each sensor face feeds a redstone-link transmitter with its own frequency pair.")
            .pointAt(util.vector().blockSurface(westLinkTransmitter, Direction.UP))
            .colored(PonderPalette.GREEN)
            .placeNearTarget();
        scene.idle(85);

        scene.overlay().showOutline(PonderPalette.BLUE, "link_receivers", linkReceivers, 65);
        scene.overlay().showText(75)
            .text("Signals are crossed wirelessly: the east receiver listens to the west sensor face.")
            .pointAt(util.vector().blockSurface(eastLinkReceiver, Direction.UP))
            .colored(PonderPalette.BLUE)
            .placeNearTarget();
        scene.idle(85);

        scene.overlay().showOutline(PonderPalette.RED, "lamps", lamps, 55);
        scene.overlay().showText(60)
            .text("The lamps sit beside the receiver links and show each side's stop signal.")
            .pointAt(util.vector().blockSurface(eastLamp, Direction.UP))
            .colored(PonderPalette.RED)
            .placeNearTarget();
        scene.idle(70);

        scene.world().configureCenterOfRotation(body, util.vector().centerOf(bodyCenter));
        scene.world().rotateSection(body, 0, 0, -18, 35);
        scene.overlay().showText(70)
            .text("When the east side dips, the west-side sensor output is low, so the east receiver stays fast.")
            .pointAt(util.vector().blockSurface(eastReceiver, Direction.UP))
            .colored(PonderPalette.GREEN)
            .placeNearTarget();
        scene.idle(80);

        scene.world().toggleRedstonePower(westStopSignal);
        scene.overlay().showText(65)
            .text("The high side receives the strong opposite signal and throttles down, creating a righting torque.")
            .pointAt(util.vector().centerOf(westReceiver).add(0, 0.6, 0))
            .colored(PonderPalette.GREEN)
            .placeNearTarget();
        scene.idle(75);

        scene.world().rotateSection(body, 0, 0, 18, 35);
        scene.world().toggleRedstonePower(westStopSignal);
        scene.overlay().showText(65)
            .text("Place the whole template with /place template create_stressbound:stressbound/tumbler.")
            .independent(20)
            .colored(PonderPalette.BLUE);
        scene.idle(75);
    }

    private static void addCheckerboardBase(CreateSceneBuilder scene, SceneBuildingUtil util, int size) {
        BlockState white = Blocks.WHITE_CONCRETE.defaultBlockState();
        BlockState lightGray = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                scene.world().setBlock(util.grid().at(x, 0, z), (x + z) % 2 == 0 ? white : lightGray, false);
            }
        }
    }
}
