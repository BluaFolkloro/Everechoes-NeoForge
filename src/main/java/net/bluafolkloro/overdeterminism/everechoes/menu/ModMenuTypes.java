package net.bluafolkloro.overdeterminism.everechoes.menu;

import net.bluafolkloro.overdeterminism.everechoes.Everechoes;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Everechoes.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<MailBoxMenu>> MAIL_BOX_MENU =
            MENUS.register("mail_box_menu",
                    () -> IMenuTypeExtension.create(MailBoxMenu::new)
            );

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}