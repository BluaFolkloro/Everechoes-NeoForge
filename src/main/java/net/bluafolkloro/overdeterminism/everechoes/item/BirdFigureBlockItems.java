package net.bluafolkloro.overdeterminism.everechoes.item;

import net.bluafolkloro.overdeterminism.everechoes.Everechoes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BirdFigureBlockItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Everechoes.MODID);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
