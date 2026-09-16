package net.bluafolkloro.overdeterminism.everechoes.letter;

import net.bluafolkloro.overdeterminism.everechoes.item.LetterItem;
import net.bluafolkloro.overdeterminism.everechoes.menu.LetterMenu;
import net.bluafolkloro.overdeterminism.everechoes.network.LetterActionPayload;
import net.bluafolkloro.overdeterminism.everechoes.postal.Address;
import net.bluafolkloro.overdeterminism.everechoes.postal.MailBoxAddress;
import net.bluafolkloro.overdeterminism.everechoes.postal.PlayerAddress;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public final class LetterActions {
    private LetterActions() {
    }

    public static void handle(LetterActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        apply(player, payload);
    }

    private static void apply(ServerPlayer player, LetterActionPayload payload) {
        ItemStack stack = player.getItemInHand(payload.hand());
        if (!(stack.getItem() instanceof LetterItem)) {
            return;
        }

        LetterData data = LetterContents.get(stack);
        if (data == null || !data.letterId().equals(payload.letterId())) {
            return;
        }

        // The close-container packet often arrives before SAVE, so do not require the letter menu
        // to still be open. If it is open, it must still be the same letter.
        // 关闭容器的包经常比 SAVE 先到，因此不要求写信菜单仍开着；若仍开着，则必须还是同一封信。
        if (player.containerMenu instanceof LetterMenu menu && !menu.letterId().equals(data.letterId())) {
            return;
        }

        switch (payload.action()) {
            case SAVE -> save(player, payload, data);
            case SEAL -> seal(player, payload, data);
            case OPEN -> open(player, payload.hand(), data);
        }
    }

    private static void save(ServerPlayer player, LetterActionPayload payload, LetterData data) {
        DraftEdit edit = applyDraftEdits(player, payload, data);
        if (edit == null) {
            return;
        }

        LetterContents.writeToHand(player, payload.hand(), edit.data());
        if (edit.unknownPlayer()) {
            player.displayClientMessage(Component.translatable("message.everechoes.letter.unknown_player"), true);
        }
    }

    private static void seal(ServerPlayer player, LetterActionPayload payload, LetterData data) {
        DraftEdit edit = applyDraftEdits(player, payload, data);
        if (edit == null) {
            return;
        }

        LetterContents.writeToHand(player, payload.hand(), edit.data());
        if (edit.unknownPlayer()) {
            player.displayClientMessage(Component.translatable("message.everechoes.letter.unknown_player"), true);
            return;
        }

        AtomicReference<String> failureKey = new AtomicReference<>();
        Optional<LetterData> sealed = edit.data().trySeal(failureKey::set);
        if (sealed.isEmpty()) {
            String key = failureKey.get();
            if (key != null) {
                player.displayClientMessage(Component.translatable(key), true);
            }
            return;
        }

        LetterContents.writeToHand(player, payload.hand(), sealed.get());
    }

    private static void open(ServerPlayer player, InteractionHand hand, LetterData data) {
        data.tryOpen().ifPresent(opened -> LetterContents.writeToHand(player, hand, opened));
    }

    private static DraftEdit applyDraftEdits(ServerPlayer player, LetterActionPayload payload, LetterData data) {
        if (!data.isDraft()) {
            return null;
        }

        LetterData edited = data
                .withTitle(LetterLimits.sanitizeSingleLine(payload.title(), LetterLimits.TITLE))
                .withBody(LetterLimits.sanitizeMultiline(payload.body(), LetterLimits.BODY))
                .withSignatureSender(LetterLimits.sanitizeSingleLine(payload.signatureSender(), LetterLimits.SIGNATURE))
                .withLetterRecipient(LetterLimits.sanitizeSingleLine(payload.letterRecipient(), LetterLimits.LETTER_RECIPIENT));

        return applyRecipient(player, payload, edited);
    }

    private static DraftEdit applyRecipient(ServerPlayer player, LetterActionPayload payload, LetterData data) {
        String addressValue = LetterLimits.sanitizeSingleLine(payload.addressValue(), LetterLimits.ADDRESS_VALUE);
        return switch (payload.addressKind()) {
            case NONE -> new DraftEdit(data.withoutRecipientAddress(), false);
            case MAILBOX -> {
                if (addressValue.isBlank()) {
                    yield new DraftEdit(data.withoutRecipientAddress(), false);
                }
                var parsed = MailBoxAddress.parse(addressValue);
                if (parsed.isEmpty()) {
                    player.displayClientMessage(Component.translatable("message.everechoes.letter.invalid_postal_code"), true);
                    yield new DraftEdit(data, false);
                }
                MailBoxAddress resolved = player.getServer() == null
                        ? parsed.get()
                        : PostalNetwork.get(player.getServer().overworld()).resolveAddress(parsed.get());
                yield new DraftEdit(data.withRecipientAddress(resolved), false);
            }
            case PLAYER -> {
                if (addressValue.isBlank()) {
                    yield new DraftEdit(data.withoutRecipientAddress(), false);
                }

                Optional<Address> resolved = resolvePlayerAddress(player.getServer(), addressValue);
                if (resolved.isEmpty()) {
                    yield new DraftEdit(data, true);
                }

                yield new DraftEdit(data.withRecipientAddress(resolved.get()), false);
            }
        };
    }

    private static Optional<Address> resolvePlayerAddress(MinecraftServer server, String rawName) {
        try {
            return Optional.of(new PlayerAddress(UUID.fromString(rawName)));
        } catch (IllegalArgumentException ignored) {
            // Fall through to name lookup.
            // 不是 UUID 时再按玩家名查找。
        }

        if (server == null) {
            return Optional.empty();
        }

        ServerPlayer onlinePlayer = server.getPlayerList().getPlayerByName(rawName);
        if (onlinePlayer != null) {
            return Optional.of(new PlayerAddress(onlinePlayer.getUUID()));
        }

        return server.getProfileCache()
                .get(rawName)
                .map(profile -> new PlayerAddress(profile.getId()));
    }

    private record DraftEdit(LetterData data, boolean unknownPlayer) {
    }
}
