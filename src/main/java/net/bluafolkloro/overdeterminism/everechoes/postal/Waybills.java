package net.bluafolkloro.overdeterminism.everechoes.postal;

import net.bluafolkloro.overdeterminism.everechoes.component.ModDataComponents;
import net.bluafolkloro.overdeterminism.everechoes.item.LetterItems;
import net.bluafolkloro.overdeterminism.everechoes.letter.LetterContents;
import net.bluafolkloro.overdeterminism.everechoes.letter.LetterData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * 封装该模块的相关职责。
 */
public final class Waybills {
    /**
     * 创建并初始化当前对象。
     */
    private Waybills() {
    }

    /**
     * 判断 {@code isDepositable} 对应的业务操作。
     */
    public static boolean isDepositable(ItemStack stack) {
        if (!stack.is(LetterItems.SEALED_LETTER.get())) {
            return false;
        }

        LetterData data = LetterContents.get(stack);
        return data != null && data.isSealed() && data.recipientAddress().isPresent();
    }

    /**
     * 获取或解析 {@code get} 对应的业务操作。
     */
    @Nullable
    public static Waybill get(ItemStack stack) {
        return stack.get(ModDataComponents.WAYBILL.get());
    }

    /**
     * 执行 {@code markAwaitingCarrier} 对应的业务操作。
     */
    public static void markAwaitingCarrier(ItemStack stack) {
        if (!isDepositable(stack)) {
            return;
        }

        Waybill existing = get(stack);
        stack.set(ModDataComponents.WAYBILL.get(), existing == null ? Waybill.awaitingCarrier() : existing.depositedAtPostBox());
    }

    /**
     * 执行 {@code markTakenBy} 对应的业务操作。
     */
    public static void markTakenBy(ItemStack stack, Player player) {
        if (!isDepositable(stack)) {
            return;
        }

        Waybill existing = get(stack);
        Waybill next = existing == null ? Waybill.awaitingCarrier() : existing;
        stack.set(ModDataComponents.WAYBILL.get(), next.takenBy(player.getUUID()));
    }
}
