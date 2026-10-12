package net.bluafolkloro.overdeterminism.everechoes.block;

import net.bluafolkloro.overdeterminism.everechoes.Everechoes;
import net.bluafolkloro.overdeterminism.everechoes.item.ContainerBlockItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 封装该模块的相关职责。
 */
public class ContainerBlocks {
    /**
     * 定义 {@code BLOCKS} 常量。
     */
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(Everechoes.MODID);

    /**
     * 定义 {@code POST_BOX} 常量。
     */
    public static final DeferredBlock<PostBoxBlock> POST_BOX =
            registerBlock("post_box", () -> new PostBoxBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()
            ));

    /**
     * 定义 {@code MAIL_BOX} 常量。
     */
    public static final DeferredBlock<MailBoxBlock> MAIL_BOX =
            registerBlock("mail_box", () -> new MailBoxBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()
            ));

    /**
     * 注册 {@code registerBlock} 对应的业务操作。
     */
    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    /**
     * 注册 {@code registerBlockItem} 对应的业务操作。
     */
    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ContainerBlockItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    /**
     * 注册 {@code register} 对应的业务操作。
     */
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
