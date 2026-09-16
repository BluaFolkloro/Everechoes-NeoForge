package net.bluafolkloro.overdeterminism.everechoes.network;

import net.bluafolkloro.overdeterminism.everechoes.Everechoes;
import net.bluafolkloro.overdeterminism.everechoes.letter.AddressInputKind;
import net.bluafolkloro.overdeterminism.everechoes.letter.LetterLimits;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

import java.util.UUID;

public record LetterActionPayload(
        Action action,
        InteractionHand hand,
        UUID letterId,
        String title,
        String body,
        String signatureSender,
        String letterRecipient,
        AddressInputKind addressKind,
        String addressValue
) implements CustomPacketPayload {
    public LetterActionPayload {
        title = LetterLimits.sanitizeSingleLine(title, LetterLimits.TITLE);
        body = LetterLimits.sanitizeMultiline(body, LetterLimits.BODY);
        signatureSender = LetterLimits.sanitizeSingleLine(signatureSender, LetterLimits.SIGNATURE);
        letterRecipient = LetterLimits.sanitizeSingleLine(letterRecipient, LetterLimits.LETTER_RECIPIENT);
        addressValue = LetterLimits.sanitizeSingleLine(addressValue, LetterLimits.ADDRESS_VALUE);
    }

    public static final CustomPacketPayload.Type<LetterActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Everechoes.MODID, "letter_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LetterActionPayload> STREAM_CODEC =
            StreamCodec.of(LetterActionPayload::write, LetterActionPayload::read);

    public enum Action {
        SAVE,
        SEAL,
        OPEN
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, LetterActionPayload payload) {
        buffer.writeEnum(payload.action);
        buffer.writeEnum(payload.hand);
        buffer.writeUUID(payload.letterId);
        buffer.writeUtf(payload.title, LetterLimits.TITLE);
        buffer.writeUtf(payload.body, LetterLimits.BODY);
        buffer.writeUtf(payload.signatureSender, LetterLimits.SIGNATURE);
        buffer.writeUtf(payload.letterRecipient, LetterLimits.LETTER_RECIPIENT);
        buffer.writeEnum(payload.addressKind);
        buffer.writeUtf(payload.addressValue, LetterLimits.ADDRESS_VALUE);
    }

    private static LetterActionPayload read(RegistryFriendlyByteBuf buffer) {
        return new LetterActionPayload(
                buffer.readEnum(Action.class),
                buffer.readEnum(InteractionHand.class),
                buffer.readUUID(),
                buffer.readUtf(LetterLimits.TITLE),
                buffer.readUtf(LetterLimits.BODY),
                buffer.readUtf(LetterLimits.SIGNATURE),
                buffer.readUtf(LetterLimits.LETTER_RECIPIENT),
                buffer.readEnum(AddressInputKind.class),
                buffer.readUtf(LetterLimits.ADDRESS_VALUE)
        );
    }
}
