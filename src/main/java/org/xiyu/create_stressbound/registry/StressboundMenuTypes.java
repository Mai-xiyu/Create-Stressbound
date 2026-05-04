package org.xiyu.create_stressbound.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.xiyu.create_stressbound.CreateStressbound;
import org.xiyu.create_stressbound.client.gui.ReceiverMenu;

public final class StressboundMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
        DeferredRegister.create(Registries.MENU, CreateStressbound.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ReceiverMenu>> RECEIVER =
        MENU_TYPES.register("receiver", () -> IMenuTypeExtension.create(
            (windowId, playerInv, extraData) -> new ReceiverMenu(windowId, playerInv, extraData)
        ));

    private StressboundMenuTypes() {
    }

    public static void register(IEventBus bus) {
        MENU_TYPES.register(bus);
    }
}
