package net.bluafolkloro.overdeterminism.everechoes.postal;

import net.bluafolkloro.overdeterminism.everechoes.component.ModDataComponents;
import net.bluafolkloro.overdeterminism.everechoes.item.LetterItems;
import net.bluafolkloro.overdeterminism.everechoes.letter.LetterContents;
import net.bluafolkloro.overdeterminism.everechoes.letter.LetterData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public final class Waybills {
    private Waybills() {
    }

    public static boolean isDepositable(ItemStack stack) {
        if (!stack.is(LetterItems.SEALED_LETTER.get())) {
            return false;
        }

        LetterData data = LetterContents.get(stack);
        return data != null && data.isSealed() && data.recipientAddress().isPresent();
    }

    @Nullable
    public static Waybill get(ItemStack stack) {
        return stack.get(ModDataComponents.WAYBILL.get());
    }

    public static void markAwaitingCarrier(ItemStack stack) {
        if (!isDepositable(stack)) {
            return;
        }

        Waybill existing = get(stack);
        stack.set(ModDataComponents.WAYBILL.get(), existing == null ? Waybill.awaitingCarrier() : existing.depositedAtPostBox());
    }

    public static void markTakenBy(ItemStack stack, Player player) {
        if (!isDepositable(stack)) {
            return;
        }

        Waybill existing = get(stack);
        Waybill next = existing == null ? Waybill.awaitingCarrier() : existing;
        stack.set(ModDataComponents.WAYBILL.get(), next.takenBy(player.getUUID()));
    }
}
