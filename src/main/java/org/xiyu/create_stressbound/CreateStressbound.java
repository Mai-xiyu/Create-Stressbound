package org.xiyu.create_stressbound;

import com.mojang.logging.LogUtils;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;
import org.xiyu.create_stressbound.client.StressboundClient;
import org.xiyu.create_stressbound.command.StressboundCommands;
import org.xiyu.create_stressbound.compat.MovingStructureSupport;
import org.xiyu.create_stressbound.content.link.EndpointRole;
import org.xiyu.create_stressbound.content.link.MovingEndpointMovementBehaviour;
import org.xiyu.create_stressbound.content.link.StressLinkService;
import org.xiyu.create_stressbound.registry.StressboundBlockEntities;
import org.xiyu.create_stressbound.registry.StressboundBlocks;
import org.xiyu.create_stressbound.registry.StressboundItems;

@Mod(CreateStressbound.MODID)
public final class CreateStressbound {
    public static final String MODID = "create_stressbound";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateStressbound(IEventBus modEventBus, ModContainer modContainer) {
        StressboundBlocks.register(modEventBus);
        StressboundItems.register(modEventBus);
        StressboundBlockEntities.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreativeTabEntries);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(StressboundClient::clientSetup);
        }
        modContainer.registerConfig(ModConfig.Type.COMMON, StressboundConfig.SPEC);

        NeoForge.EVENT_BUS.addListener(StressLinkService::onServerTick);
        NeoForge.EVENT_BUS.addListener(StressboundCommands::register);

        LOGGER.info("Create Stressbound initialized. {}", MovingStructureSupport.describeRuntime());
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.parse(MODID + ":" + path);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            MovementBehaviour.REGISTRY.register(
                StressboundBlocks.STRESS_TRANSMITTER.get(),
                new MovingEndpointMovementBehaviour(EndpointRole.TRANSMITTER)
            );
            MovementBehaviour.REGISTRY.register(
                StressboundBlocks.STRESS_RECEIVER.get(),
                new MovingEndpointMovementBehaviour(EndpointRole.RECEIVER)
            );
        });
    }

    private void addCreativeTabEntries(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(StressboundItems.STRESS_TRANSMITTER_ITEM);
            event.accept(StressboundItems.STRESS_RECEIVER_ITEM);
        }

        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(StressboundItems.KINETIC_BINDER);
        }
    }
}
