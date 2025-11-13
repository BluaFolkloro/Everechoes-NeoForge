package net.bluafolkloro.overdeterminism.everechoes.item;

import net.bluafolkloro.overdeterminism.everechoes.Everechoes;
import net.bluafolkloro.overdeterminism.everechoes.block.ContainerBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Everechoes.MODID);

    public static final Supplier<CreativeModeTab> EVERECHOES_TAB =
            CREATIVE_MODE_TAB.register("everechoes_tab",
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(LetterItems.SEALED_LETTER.get()))
                            .title(Component.translatable("creativetab.everechoes.everechoes_items"))
                            .displayItems((itemDisplayParameters, output) -> {
                                output.accept(LetterItems.SEALED_LETTER);
                                output.accept(LetterItems.LETTER);
                                output.accept(LetterItems.OPENED_LETTER);
                                output.accept(ContainerBlocks.MAIL_BOX);
                            })
                            .build()
            );

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
