package net.bluafolkloro.overdeterminism.everechoes.block.entity;

import net.bluafolkloro.overdeterminism.everechoes.Everechoes;
import net.bluafolkloro.overdeterminism.everechoes.block.ContainerBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Everechoes.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MailBoxBlockEntity>> MAIL_BOX =
            BLOCK_ENTITIES.register("mail_box",
                    () -> BlockEntityType.Builder
                            .of(MailBoxBlockEntity::new, ContainerBlocks.MAIL_BOX.get())
                            .build(null)
            );

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}