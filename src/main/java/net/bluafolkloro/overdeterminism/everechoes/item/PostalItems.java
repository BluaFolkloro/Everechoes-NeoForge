package net.bluafolkloro.overdeterminism.everechoes.item;

import net.bluafolkloro.overdeterminism.everechoes.Everechoes;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 封装该模块的相关职责。
 */
public class PostalItems {
    /**
     * 定义 {@code ITEMS} 常量。
     */
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Everechoes.MODID);

    /**
     * 定义 {@code POSTAL_ATLAS} 常量。
     */
    public static final DeferredItem<PostalAtlasItem> POSTAL_ATLAS =
            ITEMS.register("postal_atlas", () -> new PostalAtlasItem(new Item.Properties().stacksTo(1)));

    /**
     * 注册 {@code register} 对应的业务操作。
     */
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
