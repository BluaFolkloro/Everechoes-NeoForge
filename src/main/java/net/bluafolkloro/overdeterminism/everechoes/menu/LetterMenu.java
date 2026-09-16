package net.bluafolkloro.overdeterminism.everechoes.menu;

import net.bluafolkloro.overdeterminism.everechoes.item.LetterItem;
import net.bluafolkloro.overdeterminism.everechoes.letter.LetterContents;
import net.bluafolkloro.overdeterminism.everechoes.letter.LetterData;
import net.bluafolkloro.overdeterminism.everechoes.letter.LetterDataSerializer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class LetterMenu extends AbstractContainerMenu {
    private final InteractionHand hand;
    private final UUID letterId;
    private final LetterData openingData;

    public LetterMenu(int containerId, Inventory playerInv, InteractionHand hand, LetterData openingData) {
        super(ModMenuTypes.LETTER_MENU.get(), containerId);
        this.hand = hand;
        this.openingData = openingData;
        this.letterId = openingData.letterId();
    }

    public LetterMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(
                containerId,
                playerInv,
                extraData.readEnum(InteractionHand.class),
                decodeOpeningData(extraData.readNbt())
        );
    }

    public static void writeOpeningData(FriendlyByteBuf buffer, InteractionHand hand, LetterData data) {
        buffer.writeEnum(hand);
        Tag tag = LetterDataSerializer.CODEC.encodeStart(NbtOps.INSTANCE, data)
                .result()
                .orElseThrow(() -> new IllegalStateException("Failed to encode letter data"));
        buffer.writeNbt(tag instanceof CompoundTag compoundTag ? compoundTag : null);
    }

    private static LetterData decodeOpeningData(CompoundTag tag) {
        if (tag == null) {
            throw new IllegalStateException("Missing letter data");
        }

        return LetterDataSerializer.CODEC.parse(NbtOps.INSTANCE, tag)
                .result()
                .orElseThrow(() -> new IllegalStateException("Failed to decode letter data"));
    }

    public InteractionHand hand() {
        return hand;
    }

    public UUID letterId() {
        return letterId;
    }

    public LetterData letterData(Player player) {
        LetterData current = LetterContents.get(player.getItemInHand(hand));
        if (current != null && current.letterId().equals(letterId)) {
            return current;
        }

        return openingData;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof LetterItem)) {
            return false;
        }

        LetterData data = LetterContents.get(stack);
        // Allow a short window where the item component has not synced to the client yet.
        // 允许客户端在物品组件尚未同步到时短暂保持菜单打开。
        return data == null || data.letterId().equals(letterId);
    }
}
