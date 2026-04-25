package org.xiyu.create_stressbound.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.xiyu.create_stressbound.CreateStressbound;
import org.xiyu.create_stressbound.content.kinetics.StressReceiverBlockEntity;
import org.xiyu.create_stressbound.content.kinetics.StressTransmitterBlockEntity;

public final class StressboundBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateStressbound.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StressTransmitterBlockEntity>> STRESS_TRANSMITTER =
        BLOCK_ENTITY_TYPES.register("stress_transmitter",
            () -> BlockEntityType.Builder.of(StressTransmitterBlockEntity::new, StressboundBlocks.STRESS_TRANSMITTER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StressReceiverBlockEntity>> STRESS_RECEIVER =
        BLOCK_ENTITY_TYPES.register("stress_receiver",
            () -> BlockEntityType.Builder.of(StressReceiverBlockEntity::new, StressboundBlocks.STRESS_RECEIVER.get()).build(null));

    private StressboundBlockEntities() {
    }

    public static void register(IEventBus bus) {
        BLOCK_ENTITY_TYPES.register(bus);
    }
}
