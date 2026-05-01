package org.xiyu.create_stressbound.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.xiyu.create_stressbound.CreateStressbound;

public final class StressboundCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateStressbound.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register("main",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.create_stressbound.main"))
            .icon(() -> new ItemStack(StressboundItems.KINETIC_BINDER.get()))
            .displayItems((parameters, output) -> {
                output.accept(StressboundItems.STRESS_TRANSMITTER_ITEM.get());
                output.accept(StressboundItems.STRESS_RECEIVER_ITEM.get());
                output.accept(StressboundItems.KINETIC_BINDER.get());
            })
            .build());

    private StressboundCreativeTabs() {
    }

    public static void register(IEventBus bus) {
        CREATIVE_MODE_TABS.register(bus);
    }
}