package net.bluafolkloro.overdeterminism.everechoes.menu;

import net.bluafolkloro.overdeterminism.everechoes.block.entity.MailBoxBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MailBoxMenu extends AbstractContainerMenu {
    private final MailBoxBlockEntity mailBox;
    private final Container container;

    public MailBoxMenu(int windowId, Inventory playerInv, MailBoxBlockEntity mailbox) {
        super(ModMenuTypes.MAIL_BOX_MENU.get(), windowId);
        this.mailBox = mailbox;
        this.container = mailBox.getItems();

        int slot = 0;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(container, slot++, 8 + col * 18, 18 + row * 18));
            }
        }

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9,
                        8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    public MailBoxMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(
                containerId,
                playerInv,
                (MailBoxBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos())
        );
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return null;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
