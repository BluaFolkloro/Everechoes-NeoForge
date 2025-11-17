package net.bluafolkloro.overdeterminism.everechoes.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class MailBoxBlockEntity extends BlockEntity {
    private final SimpleContainer items = new SimpleContainer(27);

    public MailBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAIL_BOX.get(), pos, state);
    }
}
