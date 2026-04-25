package org.xiyu.create_stressbound.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.xiyu.create_stressbound.CreateStressbound;
import org.xiyu.create_stressbound.content.item.KineticBinderItem;

public final class StressboundItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateStressbound.MODID);

    public static final DeferredItem<BlockItem> STRESS_TRANSMITTER_ITEM =
        ITEMS.registerSimpleBlockItem("stress_transmitter", StressboundBlocks.STRESS_TRANSMITTER);

    public static final DeferredItem<BlockItem> STRESS_RECEIVER_ITEM =
        ITEMS.registerSimpleBlockItem("stress_receiver", StressboundBlocks.STRESS_RECEIVER);

    public static final DeferredItem<Item> KINETIC_BINDER = ITEMS.register("kinetic_binder",
        () -> new KineticBinderItem(new Item.Properties().stacksTo(1)));

    private StressboundItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
