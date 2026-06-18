package net.bluafolkloro.overdeterminism.everechoes.block.entity;

import net.bluafolkloro.overdeterminism.everechoes.menu.PostBoxMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PostBoxBlockEntity extends BlockEntity implements MenuProvider {
    // Placeholder postbox storage; the concrete mail system behavior is still to be implemented.
    // 占位邮箱储存，具体邮件系统行为待实现。
    private final SimpleContainer items = new SimpleContainer(27) {
        @Override
        public void setChanged() {
            super.setChanged();
            // Mark the block entity dirty whenever the inventory changes.
            PostBoxBlockEntity.this.setChanged();
        }
    };

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.everechoes.post_box");
    }

    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInv, Player player) {
        return new PostBoxMenu(windowId, playerInv, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items.getItems(), registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, this.items.getItems(), registries);
    }

    public SimpleContainer getItems() {
        return items;
    }

    public PostBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POST_BOX.get(), pos, state);
    }
}
