package net.bluafolkloro.overdeterminism.everechoes.block.entity;

import net.bluafolkloro.overdeterminism.everechoes.menu.MailBoxMenu;
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

public class MailBoxBlockEntity extends BlockEntity implements MenuProvider {
    private final SimpleContainer items = new SimpleContainer(27);

    @Override
    public Component getDisplayName() {
        return Component.literal("Mail Box");
    }

    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInv, Player player) {
        return new MailBoxMenu(windowId, playerInv, this);
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

    public MailBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAIL_BOX.get(), pos, state);
    }
}
