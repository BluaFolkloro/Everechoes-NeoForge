package net.bluafolkloro.overdeterminism.everechoes.menu;

import net.bluafolkloro.overdeterminism.everechoes.block.entity.PostBoxBlockEntity;
import net.bluafolkloro.overdeterminism.everechoes.postal.Waybills;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class PostBoxMenu extends AbstractContainerMenu {
    private final PostBoxBlockEntity postBox;
    private final Container container;

    public PostBoxMenu(int windowId, Inventory playerInv, PostBoxBlockEntity postbox) {
        super(ModMenuTypes.POST_BOX_MENU.get(), windowId);
        this.postBox = postbox;
        this.container = postBox.getItems();

        for (int slot = 0; slot < PostBoxBlockEntity.SLOT_COUNT; slot++) {
            this.addSlot(new SealedLetterSlot(container, slot, 44 + slot * 18, 20));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 51 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 109));
        }
    }

    public PostBoxMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(containerId, playerInv, getPostBox(playerInv, extraData));
    }

    private static PostBoxBlockEntity getPostBox(Inventory playerInv, FriendlyByteBuf extraData) {
        BlockPos pos = extraData.readBlockPos();
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        if (be instanceof PostBoxBlockEntity postBox) {
            return postBox;
        }

        throw new IllegalStateException("Expected postbox block entity at " + pos + ", got " + be);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack original = slot.getItem();
        newStack = original.copy();
        int containerSlots = PostBoxBlockEntity.SLOT_COUNT;

        if (index < containerSlots) {
            Waybills.markTakenBy(original, player);
            if (!this.moveItemStackTo(original, containerSlots, this.slots.size(), true)) {
                Waybills.markAwaitingCarrier(original);
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(original, 0, containerSlots, false)) {
            return ItemStack.EMPTY;
        }

        if (original.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return newStack;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.postBox.isRemoved()) {
            return false;
        }

        if (player.level() != this.postBox.getLevel()) {
            return false;
        }

        double dx = player.getX() - (this.postBox.getBlockPos().getX() + 0.5);
        double dy = player.getY() - (this.postBox.getBlockPos().getY() + 0.5);
        double dz = player.getZ() - (this.postBox.getBlockPos().getZ() + 0.5);
        return dx * dx + dy * dy + dz * dz <= 64.0;
    }
}
