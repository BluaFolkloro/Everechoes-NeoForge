package net.bluafolkloro.overdeterminism.everechoes.postal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * 封装数据的序列化与反序列化。
 */
public final class AddressSerializer {
    /**
     * 定义 {@code MAILBOX_TYPE} 常量。
     */
    private static final String MAILBOX_TYPE = "mailbox";
    /**
     * 定义 {@code PLAYER_TYPE} 常量。
     */
    private static final String PLAYER_TYPE = "player";

    /**
     * 定义 {@code CODEC} 常量。
     */
    public static final Codec<Address> CODEC = SerializedAddress.CODEC.comapFlatMap(
            AddressSerializer::decode,
            AddressSerializer::encode
    );

    /**
     * 定义 {@code STREAM_CODEC} 常量。
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, Address> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    /**
     * 创建并初始化当前对象。
     */
    private AddressSerializer() {
    }

    /**
     * 读取并恢复 {@code decode} 对应的业务操作。
     */
    private static DataResult<Address> decode(SerializedAddress serialized) {
        try {
            return switch (serialized.type()) {
                case MAILBOX_TYPE -> decodeMailbox(serialized);
                case PLAYER_TYPE -> serialized.playerId()
                        .<DataResult<Address>>map(playerId -> DataResult.success(new PlayerAddress(playerId)))
                        .orElseGet(() -> DataResult.error(() -> "Player address is missing playerId"));
                default -> DataResult.error(() -> "Unknown address type: " + serialized.type());
            };
        } catch (RuntimeException exception) {
            return DataResult.error(() -> "Invalid address: " + exception.getMessage());
        }
    }

    /**
     * 读取并恢复 {@code decodeMailbox} 对应的业务操作。
     */
    private static DataResult<Address> decodeMailbox(SerializedAddress serialized) {
        Optional<MailBoxAddress> fromCodes = codes(serialized)
                .map(parsed -> new MailBoxAddress(
                        serialized.domainUuid(),
                        serialized.districtUuid(),
                        serialized.mailboxUuid(),
                        parsed.domainCode(),
                        parsed.districtCode(),
                        parsed.deliveryCode()
                ));
        if (fromCodes.isPresent()) {
            return DataResult.success(fromCodes.get());
        }
        return serialized.postalCode()
                .or(() -> serialized.legacyDomainId().flatMap(domain -> serialized.legacyDistrictId().flatMap(district ->
                        serialized.legacyDeliveryId().map(delivery -> domain + district + "-" + delivery))))
                .flatMap(MailBoxAddress::parse)
                .<DataResult<Address>>map(DataResult::success)
                .orElseGet(() -> DataResult.error(() -> "Mailbox address is missing display codes"));
    }

    /**
     * 执行 {@code codes} 对应的业务操作。
     */
    private static Optional<PostalCodes.ParsedPostalCode> codes(SerializedAddress serialized) {
        if (serialized.domainCode().isEmpty() || serialized.districtCode().isEmpty() || serialized.deliveryCode().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new PostalCodes.ParsedPostalCode(
                serialized.domainCode().orElseThrow(),
                serialized.districtCode().orElseThrow(),
                serialized.deliveryCode().orElseThrow()
        ));
    }

    /**
     * 保存或编码 {@code encode} 对应的业务操作。
     */
    private static SerializedAddress encode(Address address) {
        Objects.requireNonNull(address, "address cannot be null");
        if (address instanceof MailBoxAddress mailBoxAddress) {
            return new SerializedAddress(
                    MAILBOX_TYPE,
                    mailBoxAddress.domainId(),
                    mailBoxAddress.districtId(),
                    mailBoxAddress.mailboxId(),
                    Optional.of(mailBoxAddress.domainCode()),
                    Optional.of(mailBoxAddress.districtCode()),
                    Optional.of(mailBoxAddress.deliveryCode()),
                    Optional.of(mailBoxAddress.postalCode()),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
            );
        }
        if (address instanceof PlayerAddress playerAddress) {
            return new SerializedAddress(
                    PLAYER_TYPE,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(playerAddress.playerId())
            );
        }
        throw new IllegalArgumentException("Unsupported address type: " + address.getClass().getName());
    }

    /**
     * 封装邮政地址值对象。
     */
    private record SerializedAddress(
            /**
             * 保存 {@code type} 对应的状态或依赖。
             */
            String type,
            /**
             * 保存 {@code domainUuid} 对应的状态或依赖。
             */
            Optional<UUID> domainUuid,
            /**
             * 保存 {@code districtUuid} 对应的状态或依赖。
             */
            Optional<UUID> districtUuid,
            /**
             * 保存 {@code mailboxUuid} 对应的状态或依赖。
             */
            Optional<UUID> mailboxUuid,
            /**
             * 保存 {@code domainCode} 对应的状态或依赖。
             */
            Optional<String> domainCode,
            /**
             * 保存 {@code districtCode} 对应的状态或依赖。
             */
            Optional<String> districtCode,
            /**
             * 保存 {@code deliveryCode} 对应的状态或依赖。
             */
            Optional<String> deliveryCode,
            /**
             * 保存 {@code postalCode} 对应的状态或依赖。
             */
            Optional<String> postalCode,
            /**
             * 保存 {@code legacyDomainId} 对应的状态或依赖。
             */
            Optional<String> legacyDomainId,
            /**
             * 保存 {@code legacyDistrictId} 对应的状态或依赖。
             */
            Optional<String> legacyDistrictId,
            /**
             * 保存 {@code legacyDeliveryId} 对应的状态或依赖。
             */
            Optional<String> legacyDeliveryId,
            /**
             * 保存 {@code playerId} 对应的状态或依赖。
             */
            Optional<UUID> playerId
    ) {
        /**
         * 定义 {@code CODEC} 常量。
         */
        private static final Codec<SerializedAddress> CODEC = RecordCodecBuilder.create(/** 保存 {@code instance} 对应的状态或依赖。 */ instance -> instance.group(
                Codec.STRING.fieldOf("type").forGetter(SerializedAddress::type),
                UUIDUtil.STRING_CODEC.optionalFieldOf("domainUuid").forGetter(SerializedAddress::domainUuid),
                UUIDUtil.STRING_CODEC.optionalFieldOf("districtUuid").forGetter(SerializedAddress::districtUuid),
                UUIDUtil.STRING_CODEC.optionalFieldOf("mailboxUuid").forGetter(SerializedAddress::mailboxUuid),
                Codec.STRING.optionalFieldOf("domainCode").forGetter(SerializedAddress::domainCode),
                Codec.STRING.optionalFieldOf("districtCode").forGetter(SerializedAddress::districtCode),
                Codec.STRING.optionalFieldOf("deliveryCode").forGetter(SerializedAddress::deliveryCode),
                Codec.STRING.optionalFieldOf("postalCode").forGetter(SerializedAddress::postalCode),
                Codec.STRING.optionalFieldOf("domainId").forGetter(SerializedAddress::legacyDomainId),
                Codec.STRING.optionalFieldOf("districtId").forGetter(SerializedAddress::legacyDistrictId),
                Codec.STRING.optionalFieldOf("deliveryId").forGetter(SerializedAddress::legacyDeliveryId),
                UUIDUtil.STRING_CODEC.optionalFieldOf("playerId").forGetter(SerializedAddress::playerId)
        ).apply(instance, SerializedAddress::new));
    }
}
