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

/**
 * 封装服务端容器菜单及数据同步。
 */
public class LetterMenu extends AbstractContainerMenu {
    /**
     * 保存 {@code hand} 对应的状态或依赖。
     */
    private final InteractionHand hand;
    /**
     * 保存 {@code letterId} 对应的状态或依赖。
     */
    private final UUID letterId;
    /**
     * 保存 {@code openingData} 对应的状态或依赖。
     */
    private final LetterData openingData;

    /**
     * 创建并初始化当前对象。
     */
    public LetterMenu(int containerId, Inventory playerInv, InteractionHand hand, LetterData openingData) {
        super(ModMenuTypes.LETTER_MENU.get(), containerId);
        this.hand = hand;
        this.openingData = openingData;
        this.letterId = openingData.letterId();
    }

    /**
     * 创建并初始化当前对象。
     */
    public LetterMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(
                containerId,
                playerInv,
                extraData.readEnum(InteractionHand.class),
                decodeOpeningData(extraData.readNbt())
        );
    }

    /**
     * 保存或编码 {@code writeOpeningData} 对应的业务操作。
     */
    public static void writeOpeningData(FriendlyByteBuf buffer, InteractionHand hand, LetterData data) {
        buffer.writeEnum(hand);
        Tag tag = LetterDataSerializer.CODEC.encodeStart(NbtOps.INSTANCE, data)
                .result()
                .orElseThrow(() -> new IllegalStateException("Failed to encode letter data"));
        buffer.writeNbt(tag instanceof CompoundTag compoundTag ? compoundTag : null);
    }

    /**
     * 读取并恢复 {@code decodeOpeningData} 对应的业务操作。
     */
    private static LetterData decodeOpeningData(CompoundTag tag) {
        if (tag == null) {
            throw new IllegalStateException("Missing letter data");
        }

        return LetterDataSerializer.CODEC.parse(NbtOps.INSTANCE, tag)
                .result()
                .orElseThrow(() -> new IllegalStateException("Failed to decode letter data"));
    }

    /**
     * 执行 {@code hand} 对应的业务操作。
     */
    public InteractionHand hand() {
        return hand;
    }

    /**
     * 执行 {@code letterId} 对应的业务操作。
     */
    public UUID letterId() {
        return letterId;
    }

    /**
     * 执行 {@code letterData} 对应的业务操作。
     */
    public LetterData letterData(Player player) {
        LetterData current = LetterContents.get(player.getItemInHand(hand));
        if (current != null && current.letterId().equals(letterId)) {
            return current;
        }

        return openingData;
    }

    /**
     * 执行 {@code quickMoveStack} 对应的业务操作。
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    /**
     * 执行 {@code stillValid} 对应的业务操作。
     */
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
