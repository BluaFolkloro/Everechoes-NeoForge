package net.bluafolkloro.overdeterminism.everechoes.menu;

import net.bluafolkloro.overdeterminism.everechoes.block.entity.MailBoxBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MailBoxMenu extends AbstractContainerMenu {
    private final MailBoxBlockEntity mailBox;
    private final Container container;

    // Server-side constructor.
    public MailBoxMenu(int windowId, Inventory playerInv, MailBoxBlockEntity mailbox) {
        super(ModMenuTypes.MAIL_BOX_MENU.get(), windowId);
        this.mailBox = mailbox;
        this.container = mailBox.getItems();

        // Placeholder mailbox GUI layout; the concrete mail system behavior is still to be implemented.
        // 占位邮箱 GUI 布局，具体邮件系统行为待实现。
        // Mailbox inventory: 3 rows x 9 columns.
        int slot = 0;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(container, slot++, 8 + col * 18, 18 + row * 18));
            }
        }

        // Player inventory.
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9,
                        8 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar.
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    // Client-side constructor. The server sends the lower-half block position.
    public MailBoxMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(
                containerId,
                playerInv,
                getMailBox(playerInv, extraData)
        );
    }

    private static MailBoxBlockEntity getMailBox(Inventory playerInv, FriendlyByteBuf extraData) {
        BlockPos pos = extraData.readBlockPos();
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        if (be instanceof MailBoxBlockEntity mailBox) {
            return mailBox;
        }

        throw new IllegalStateException("Expected mail box block entity at " + pos + ", got " + be);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack original = slot.getItem();
            newStack = original.copy();

            int containerSlots = 27;

            if (index < containerSlots) {
                if (!this.moveItemStackTo(original, containerSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(original, 0, containerSlots, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (original.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return newStack;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.mailBox.isRemoved()) {
            return false;
        }

        if (player.level() != this.mailBox.getLevel()) {
            return false;
        }

        double dx = player.getX() - (this.mailBox.getBlockPos().getX() + 0.5);
        double dy = player.getY() - (this.mailBox.getBlockPos().getY() + 0.5);
        double dz = player.getZ() - (this.mailBox.getBlockPos().getZ() + 0.5);
        double distSq = dx * dx + dy * dy + dz * dz;

        return distSq <= 64.0;
    }
}
