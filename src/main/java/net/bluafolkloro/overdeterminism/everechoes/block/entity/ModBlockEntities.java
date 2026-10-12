package net.bluafolkloro.overdeterminism.everechoes.block.entity;

import net.bluafolkloro.overdeterminism.everechoes.Everechoes;
import net.bluafolkloro.overdeterminism.everechoes.block.ContainerBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * 封装该模块的相关职责。
 */
public class ModBlockEntities {

    /**
     * 定义 {@code BLOCK_ENTITIES} 常量。
     */
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Everechoes.MODID);

    /**
     * 定义 {@code POST_BOX} 常量。
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PostBoxBlockEntity>> POST_BOX =
            BLOCK_ENTITIES.register("post_box",
                    () -> BlockEntityType.Builder
                            .of(PostBoxBlockEntity::new, ContainerBlocks.POST_BOX.get())
                            .build(null)
            );

    /**
     * 定义 {@code MAIL_BOX} 常量。
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MailBoxBlockEntity>> MAIL_BOX =
            BLOCK_ENTITIES.register("mail_box",
                    () -> BlockEntityType.Builder
                            .of(MailBoxBlockEntity::new, ContainerBlocks.MAIL_BOX.get())
                            .build(null)
            );

    /**
     * 注册 {@code register} 对应的业务操作。
     */
    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
