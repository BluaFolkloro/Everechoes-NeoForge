package net.bluafolkloro.overdeterminism.everechoes.block;

import net.bluafolkloro.overdeterminism.everechoes.Everechoes;
import net.bluafolkloro.overdeterminism.everechoes.item.BirdFigureBlockItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class BirdFigureBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(Everechoes.MODID);

    public static final DeferredBlock<BirdFigureBlock> NIGHT_HERON_FIGURE =
            registerBlock("night_heron_figure", () -> new BirdFigureBlock(
                    BlockBehaviour.Properties.of()
                            .strength(1.0f)
                            .sound(SoundType.WOOL)
                            .noOcclusion(),
                    Block.box(5.5, 0, 0.7, 10.5, 9, 12.1)
            ));

    public static final DeferredBlock<BirdFigureBlock> NIGHT_HERON_COCKROACH_FIGURE =
            registerBlock("night_heron_cockroach_figure", () -> new BirdFigureBlock(
                    BlockBehaviour.Properties.of()
                            .strength(1.0f)
                            .sound(SoundType.WOOL)
                            .noOcclusion(),
                    Block.box(6, 0, 2, 11, 4.8, 13.4)
            ));

    public static final DeferredBlock<BirdFigureBlock> NIGHT_HERON_THOUGHTFUL_FIGURE =
            registerBlock("night_heron_thoughtful_figure", () -> new BirdFigureBlock(
                    BlockBehaviour.Properties.of()
                            .strength(1.0f)
                            .sound(SoundType.WOOL)
                            .noOcclusion(),
                    Block.box(5.5, 0, 1, 10.5, 11.3, 12.4)
            ));

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        BirdFigureBlockItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
