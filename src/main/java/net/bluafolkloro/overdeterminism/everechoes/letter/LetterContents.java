package net.bluafolkloro.overdeterminism.everechoes.letter;

import net.bluafolkloro.overdeterminism.everechoes.component.ModDataComponents;
import net.bluafolkloro.overdeterminism.everechoes.item.LetterItem;
import net.bluafolkloro.overdeterminism.everechoes.item.LetterItems;
import net.bluafolkloro.overdeterminism.everechoes.postal.Address;
import net.bluafolkloro.overdeterminism.everechoes.postal.MailBoxAddress;
import net.bluafolkloro.overdeterminism.everechoes.postal.PlayerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 封装该模块的相关职责。
 */
public final class LetterContents {
    /**
     * 创建并初始化当前对象。
     */
    private LetterContents() {
    }

    /**
     * 获取或解析 {@code get} 对应的业务操作。
     */
    @Nullable
    public static LetterData get(ItemStack stack) {
        return stack.get(ModDataComponents.LETTER.get());
    }

    /**
     * 设置或写入 {@code set} 对应的业务操作。
     */
    public static void set(ItemStack stack, LetterData data) {
        stack.set(ModDataComponents.LETTER.get(), data);
    }

    /**
     * 执行 {@code ensureInitialized} 对应的业务操作。
     */
    @Nullable
    public static LetterData ensureInitialized(ItemStack stack, Player player) {
        LetterData existing = get(stack);
        if (existing != null) {
            return existing;
        }

        if (!(stack.getItem() instanceof LetterItem letterItem) || letterItem.expectedState() != LetterState.DRAFT) {
            return null;
        }

        LetterData created = LetterData.createDraft(new PlayerAddress(player.getUUID()));
        set(stack, created);
        return created;
    }

    /**
     * 执行 {@code itemFor} 对应的业务操作。
     */
    public static Item itemFor(LetterState state) {
        return switch (state) {
            case DRAFT -> LetterItems.LETTER.get();
            case SEALED -> LetterItems.SEALED_LETTER.get();
            case OPENED -> LetterItems.OPENED_LETTER.get();
        };
    }

    /**
     * 保存或编码 {@code writeToHand} 对应的业务操作。
     */
    public static void writeToHand(Player player, InteractionHand hand, LetterData data) {
        ItemStack current = player.getItemInHand(hand);
        Item expected = itemFor(data.state());
        if (current.is(expected)) {
            set(current, data);
            return;
        }

        ItemStack updated = current.transmuteCopy(expected);
        set(updated, data);
        if (data.isOpened()) {
            updated.remove(ModDataComponents.WAYBILL.get());
        }
        player.setItemInHand(hand, updated);
    }

    /**
     * 格式化 {@code formatAddress} 对应的业务操作。
     */
    public static Component formatAddress(Address address, @Nullable Level level) {
        if (address instanceof MailBoxAddress mailBoxAddress) {
            return Component.literal(mailBoxAddress.postalCode());
        }

        if (address instanceof PlayerAddress playerAddress) {
            String playerName = playerName(playerAddress.playerId(), level);
            return Component.literal(playerName != null ? playerName : shortUuid(playerAddress.playerId()));
        }

        return Component.empty();
    }

    /**
     * 添加或加入 {@code addressInputValue} 对应的业务操作。
     */
    public static String addressInputValue(Address address, @Nullable Level level) {
        if (address instanceof MailBoxAddress mailBoxAddress) {
            return mailBoxAddress.postalCode();
        }

        if (address instanceof PlayerAddress playerAddress) {
            String playerName = playerName(playerAddress.playerId(), level);
            return playerName != null ? playerName : playerAddress.playerId().toString();
        }

        return "";
    }

    /**
     * 添加或加入 {@code addressInputKind} 对应的业务操作。
     */
    public static AddressInputKind addressInputKind(@Nullable Address address) {
        if (address instanceof MailBoxAddress) {
            return AddressInputKind.MAILBOX;
        }
        if (address instanceof PlayerAddress) {
            return AddressInputKind.PLAYER;
        }
        return AddressInputKind.NONE;
    }

    /**
     * 执行 {@code playerName} 对应的业务操作。
     */
    @Nullable
    private static String playerName(UUID playerId, @Nullable Level level) {
        if (level == null) {
            return null;
        }

        Player player = level.getPlayerByUUID(playerId);
        return player == null ? null : player.getGameProfile().getName();
    }

    /**
     * 执行 {@code shortUuid} 对应的业务操作。
     */
    private static String shortUuid(UUID uuid) {
        return uuid.toString().substring(0, 8);
    }
}
