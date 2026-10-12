package net.bluafolkloro.overdeterminism.everechoes.component;

import net.bluafolkloro.overdeterminism.everechoes.Everechoes;
import net.bluafolkloro.overdeterminism.everechoes.letter.LetterData;
import net.bluafolkloro.overdeterminism.everechoes.letter.LetterDataSerializer;
import net.bluafolkloro.overdeterminism.everechoes.postal.Waybill;
import net.bluafolkloro.overdeterminism.everechoes.postal.WaybillSerializer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 封装该模块的相关职责。
 */
public class ModDataComponents {
    /**
     * 定义 {@code COMPONENTS} 常量。
     */
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Everechoes.MODID);

    /**
     * 定义 {@code LETTER} 常量。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LetterData>> LETTER =
            COMPONENTS.registerComponentType(
                    "letter",
                    /**
                     * 保存 {@code builder} 对应的状态或依赖。
                     */
                    builder -> builder
                            .persistent(LetterDataSerializer.CODEC)
                            .networkSynchronized(LetterDataSerializer.STREAM_CODEC)
                            .cacheEncoding()
            );

    /**
     * 定义 {@code WAYBILL} 常量。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Waybill>> WAYBILL =
            COMPONENTS.registerComponentType(
                    "waybill",
                    /**
                     * 保存 {@code builder} 对应的状态或依赖。
                     */
                    builder -> builder
                            .persistent(WaybillSerializer.CODEC)
                            .networkSynchronized(WaybillSerializer.STREAM_CODEC)
                            .cacheEncoding()
            );

    /**
     * 注册 {@code register} 对应的业务操作。
     */
    public static void register(IEventBus eventBus) {
        COMPONENTS.register(eventBus);
    }
}
