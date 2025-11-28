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

    //服务端构造函数
    public MailBoxMenu(int windowId, Inventory playerInv, MailBoxBlockEntity mailbox) {
        super(ModMenuTypes.MAIL_BOX_MENU.get(), windowId);
        this.mailBox = mailbox;
        this.container = mailBox.getItems();

        //邮筒3*9
        int slot = 0;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(container, slot++, 8 + col * 18, 18 + row * 18));
            }
        }

        //背包3*9
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9,
                        8 + col * 18, 84 + row * 18));
            }
        }

        //快捷栏1*9
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    //客户端构造函数，此时只有extraData，必须要从中获取方块坐标，然后调用服务端构造函数
    public MailBoxMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(
                containerId,
                playerInv,
                (MailBoxBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos())
        );
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack original = slot.getItem();
            newStack = original.copy();

            int containerSlots = 27; // 邮筒格子数

            //情况 1：Shift+点击的是邮筒里的物品（0–26）
            if (index < containerSlots) {
                // → 移到玩家背包（27–62）
                if (!this.moveItemStackTo(original, containerSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            }

            //情况 2：Shift+点击的是玩家背包或快捷栏（27–62）
            else {
                // → 移到邮筒（0–26）
                if (!this.moveItemStackTo(original, 0, containerSlots, false)) {
                    return ItemStack.EMPTY;
                }
            }

            //如果移动后原物品空了 → 清空槽位
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
        // 1. 方块实体已被移除,不再允许交互
        if (this.mailBox.isRemoved()) {
            return false;
        }

        // 2. 玩家必须在同一个维度
        if (player.level() != this.mailBox.getLevel()) {
            return false;
        }

        // 3. 距离检查：用 64（= 8 格）作为上限
        double dx = player.getX() - (this.mailBox.getBlockPos().getX() + 0.5);
        double dy = player.getY() - (this.mailBox.getBlockPos().getY() + 0.5);
        double dz = player.getZ() - (this.mailBox.getBlockPos().getZ() + 0.5);
        double distSq = dx * dx + dy * dy + dz * dz;

        return distSq <= 64.0;
    }
}
