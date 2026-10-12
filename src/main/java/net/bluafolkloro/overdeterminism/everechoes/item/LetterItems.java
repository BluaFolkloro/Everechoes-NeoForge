package net.bluafolkloro.overdeterminism.everechoes.item;

import net.bluafolkloro.overdeterminism.everechoes.Everechoes;
import net.bluafolkloro.overdeterminism.everechoes.letter.LetterState;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 封装该模块的相关职责。
 */
public class LetterItems {
    /**
     * 定义 {@code ITEMS} 常量。
     */
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Everechoes.MODID);

    /**
     * 定义 {@code SEALED_LETTER} 常量。
     */
    public static final DeferredItem<LetterItem> SEALED_LETTER =
            ITEMS.register("sealed_letter", () -> new LetterItem(new Item.Properties().stacksTo(1), LetterState.SEALED));

    /**
     * 定义 {@code LETTER} 常量。
     */
    public static final DeferredItem<LetterItem> LETTER =
            ITEMS.register("letter", () -> new LetterItem(new Item.Properties().stacksTo(1), LetterState.DRAFT));

    /**
     * 定义 {@code OPENED_LETTER} 常量。
     */
    public static final DeferredItem<LetterItem> OPENED_LETTER =
            ITEMS.register("opened_letter", () -> new LetterItem(new Item.Properties().stacksTo(1), LetterState.OPENED));

    /**
     * 注册 {@code register} 对应的业务操作。
     */
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
