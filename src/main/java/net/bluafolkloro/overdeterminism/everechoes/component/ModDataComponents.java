package net.bluafolkloro.overdeterminism.everechoes.component;

import net.bluafolkloro.overdeterminism.everechoes.Everechoes;
import net.bluafolkloro.overdeterminism.everechoes.letter.LetterData;
import net.bluafolkloro.overdeterminism.everechoes.letter.LetterDataSerializer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Everechoes.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LetterData>> LETTER =
            COMPONENTS.registerComponentType(
                    "letter",
                    builder -> builder
                            .persistent(LetterDataSerializer.CODEC)
                            .networkSynchronized(LetterDataSerializer.STREAM_CODEC)
                            .cacheEncoding()
            );

    public static void register(IEventBus eventBus) {
        COMPONENTS.register(eventBus);
    }
}
