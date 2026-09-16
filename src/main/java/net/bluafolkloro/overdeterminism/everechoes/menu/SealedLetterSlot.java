package net.bluafolkloro.overdeterminism.everechoes.menu;

import net.bluafolkloro.overdeterminism.everechoes.postal.Waybills;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SealedLetterSlot extends Slot {
    public SealedLetterSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return Waybills.isDepositable(stack);
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        Waybills.markTakenBy(stack, player);
        super.onTake(player, stack);
    }
}
