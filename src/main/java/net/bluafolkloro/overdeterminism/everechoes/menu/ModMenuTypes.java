package net.bluafolkloro.overdeterminism.everechoes.menu;

import net.bluafolkloro.overdeterminism.everechoes.Everechoes;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 封装该模块的相关职责。
 */
public class ModMenuTypes {

    /**
     * 定义 {@code MENUS} 常量。
     */
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Everechoes.MODID);

    /**
     * 定义 {@code POST_BOX_MENU} 常量。
     */
    public static final DeferredHolder<MenuType<?>, MenuType<PostBoxMenu>> POST_BOX_MENU =
            MENUS.register("post_box_menu",
                    () -> IMenuTypeExtension.create(PostBoxMenu::new)
            );

    /**
     * 定义 {@code LETTER_MENU} 常量。
     */
    public static final DeferredHolder<MenuType<?>, MenuType<LetterMenu>> LETTER_MENU =
            MENUS.register("letter_menu",
                    () -> IMenuTypeExtension.create(LetterMenu::new)
            );

    /**
     * 定义 {@code POST_BOX_CONFIG_MENU} 常量。
     */
    public static final DeferredHolder<MenuType<?>, MenuType<PostBoxConfigMenu>> POST_BOX_CONFIG_MENU =
            MENUS.register("post_box_config_menu",
                    () -> IMenuTypeExtension.create(PostBoxConfigMenu::new)
            );

    /**
     * 定义 {@code POSTAL_ATLAS_MENU} 常量。
     */
    public static final DeferredHolder<MenuType<?>, MenuType<PostalAtlasMenu>> POSTAL_ATLAS_MENU =
            MENUS.register("postal_atlas_menu",
                    () -> IMenuTypeExtension.create(PostalAtlasMenu::new)
            );

    /**
     * 注册 {@code register} 对应的业务操作。
     */
    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
