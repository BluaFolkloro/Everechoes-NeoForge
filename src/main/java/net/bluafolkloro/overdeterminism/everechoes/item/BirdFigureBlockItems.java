package net.bluafolkloro.overdeterminism.everechoes.item;

import net.bluafolkloro.overdeterminism.everechoes.Everechoes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 封装该模块的相关职责。
 */
public class BirdFigureBlockItems {
    /**
     * 定义 {@code ITEMS} 常量。
     */
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Everechoes.MODID);

    /**
     * 注册 {@code register} 对应的业务操作。
     */
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
