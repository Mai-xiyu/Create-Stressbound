package org.xiyu.create_stressbound.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.xiyu.create_stressbound.CreateStressbound;
import org.xiyu.create_stressbound.content.kinetics.StressReceiverBlock;
import org.xiyu.create_stressbound.content.kinetics.StressTransmitterBlock;

public final class StressboundBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreateStressbound.MODID);

    public static final DeferredBlock<StressTransmitterBlock> STRESS_TRANSMITTER = BLOCKS.register("stress_transmitter",
        () -> new StressTransmitterBlock(BlockBehaviour.Properties.of()
            .strength(3.5F)
            .mapColor(MapColor.TERRACOTTA_YELLOW)
            .noOcclusion()
            .requiresCorrectToolForDrops()));

    public static final DeferredBlock<StressReceiverBlock> STRESS_RECEIVER = BLOCKS.register("stress_receiver",
        () -> new StressReceiverBlock(BlockBehaviour.Properties.of()
            .strength(3.5F)
            .mapColor(MapColor.COLOR_ORANGE)
            .noOcclusion()
            .requiresCorrectToolForDrops()));

    private StressboundBlocks() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
