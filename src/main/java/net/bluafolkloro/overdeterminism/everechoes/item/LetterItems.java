package net.bluafolkloro.overdeterminism.everechoes.item;

import net.bluafolkloro.overdeterminism.everechoes.Everechoes;
import net.bluafolkloro.overdeterminism.everechoes.letter.LetterState;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class LetterItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Everechoes.MODID);

    public static final DeferredItem<LetterItem> SEALED_LETTER =
            ITEMS.register("sealed_letter", () -> new LetterItem(new Item.Properties().stacksTo(1), LetterState.SEALED));

    public static final DeferredItem<LetterItem> LETTER =
            ITEMS.register("letter", () -> new LetterItem(new Item.Properties().stacksTo(1), LetterState.DRAFT));

    public static final DeferredItem<LetterItem> OPENED_LETTER =
            ITEMS.register("opened_letter", () -> new LetterItem(new Item.Properties().stacksTo(1), LetterState.OPENED));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
