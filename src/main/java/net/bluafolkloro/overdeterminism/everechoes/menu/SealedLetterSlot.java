package net.bluafolkloro.overdeterminism.everechoes.menu;

import net.bluafolkloro.overdeterminism.everechoes.postal.Waybills;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 封装该模块的相关职责。
 */
public class SealedLetterSlot extends Slot {
    /**
     * 创建并初始化当前对象。
     */
    public SealedLetterSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    /**
     * 执行 {@code mayPlace} 对应的业务操作。
     */
    @Override
    public boolean mayPlace(ItemStack stack) {
        return Waybills.isDepositable(stack);
    }

    /**
     * 处理 {@code onTake} 对应的业务操作。
     */
    @Override
    public void onTake(Player player, ItemStack stack) {
        Waybills.markTakenBy(stack, player);
        super.onTake(player, stack);
    }
}
