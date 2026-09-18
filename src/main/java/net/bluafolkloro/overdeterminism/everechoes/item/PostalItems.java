package net.bluafolkloro.overdeterminism.everechoes.item;

import net.bluafolkloro.overdeterminism.everechoes.Everechoes;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class PostalItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Everechoes.MODID);

    public static final DeferredItem<PostalAtlasItem> POSTAL_ATLAS =
            ITEMS.register("postal_atlas", () -> new PostalAtlasItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
