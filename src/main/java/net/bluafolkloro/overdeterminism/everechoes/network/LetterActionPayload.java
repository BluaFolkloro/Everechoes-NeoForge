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

/**
 * 封装客户端与服务端之间的网络载荷。
 */
public record LetterActionPayload(
        /**
         * 保存 {@code action} 对应的状态或依赖。
         */
        Action action,
        /**
         * 保存 {@code hand} 对应的状态或依赖。
         */
        InteractionHand hand,
        /**
         * 保存 {@code letterId} 对应的状态或依赖。
         */
        UUID letterId,
        /**
         * 保存 {@code title} 对应的状态或依赖。
         */
        String title,
        /**
         * 保存 {@code body} 对应的状态或依赖。
         */
        String body,
        /**
         * 保存 {@code signatureSender} 对应的状态或依赖。
         */
        String signatureSender,
        /**
         * 保存 {@code letterRecipient} 对应的状态或依赖。
         */
        String letterRecipient,
        /**
         * 保存 {@code addressKind} 对应的状态或依赖。
         */
        AddressInputKind addressKind,
        /**
         * 保存 {@code addressValue} 对应的状态或依赖。
         */
        String addressValue
) implements CustomPacketPayload {
    /**
     * 创建并初始化当前对象。
     */
    public LetterActionPayload {
        title = LetterLimits.sanitizeSingleLine(title, LetterLimits.TITLE);
        body = LetterLimits.sanitizeMultiline(body, LetterLimits.BODY);
        signatureSender = LetterLimits.sanitizeSingleLine(signatureSender, LetterLimits.SIGNATURE);
        letterRecipient = LetterLimits.sanitizeSingleLine(letterRecipient, LetterLimits.LETTER_RECIPIENT);
        addressValue = LetterLimits.sanitizeSingleLine(addressValue, LetterLimits.ADDRESS_VALUE);
    }

    /**
     * 定义 {@code TYPE} 常量。
     */
    public static final CustomPacketPayload.Type<LetterActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Everechoes.MODID, "letter_action"));

    /**
     * 定义 {@code STREAM_CODEC} 常量。
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, LetterActionPayload> STREAM_CODEC =
            StreamCodec.of(LetterActionPayload::write, LetterActionPayload::read);

    /**
     * 封装一组受限且稳定的业务状态。
     */
    public enum Action {
        /**
         * 定义 {@code SAVE} 常量。
         */
        SAVE,
        /**
         * 定义 {@code SEAL} 常量。
         */
        SEAL,
        /**
         * 定义 {@code OPEN} 常量。
         */
        OPEN
    }

    /**
     * 执行 {@code type} 对应的业务操作。
     */
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 保存或编码 {@code write} 对应的业务操作。
     */
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

    /**
     * 获取或解析 {@code read} 对应的业务操作。
     */
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
