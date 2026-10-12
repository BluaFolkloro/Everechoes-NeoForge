package net.bluafolkloro.overdeterminism.everechoes.item;

import net.bluafolkloro.overdeterminism.everechoes.Everechoes;
import net.bluafolkloro.overdeterminism.everechoes.block.BirdFigureBlocks;
import net.bluafolkloro.overdeterminism.everechoes.block.ContainerBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 封装该模块的相关职责。
 */
public class ModCreativeModeTabs {
    /**
     * 定义 {@code CREATIVE_MODE_TAB} 常量。
     */
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Everechoes.MODID);

    /**
     * 定义 {@code EVERECHOES_TAB} 常量。
     */
    public static final Supplier<CreativeModeTab> EVERECHOES_TAB =
            CREATIVE_MODE_TAB.register("everechoes_tab",
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(LetterItems.SEALED_LETTER.get()))
                            .title(Component.translatable("creativetab.everechoes.everechoes_items"))
                            .displayItems((/** 保存 {@code itemDisplayParameters} 对应的状态或依赖。 */ itemDisplayParameters, /** 保存 {@code output} 对应的状态或依赖。 */ output) -> {
                                output.accept(LetterItems.SEALED_LETTER);
                                output.accept(LetterItems.LETTER);
                                output.accept(LetterItems.OPENED_LETTER);
                                output.accept(PostalItems.POSTAL_ATLAS);
                                output.accept(ContainerBlocks.POST_BOX);
                                output.accept(ContainerBlocks.MAIL_BOX);
                                output.accept(BirdFigureBlocks.NIGHT_HERON_FIGURE);
                                output.accept(BirdFigureBlocks.NIGHT_HERON_COCKROACH_FIGURE);
                                output.accept(BirdFigureBlocks.NIGHT_HERON_THOUGHTFUL_FIGURE);
                            })
                            .build()
            );

    /**
     * 注册 {@code register} 对应的业务操作。
     */
    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
