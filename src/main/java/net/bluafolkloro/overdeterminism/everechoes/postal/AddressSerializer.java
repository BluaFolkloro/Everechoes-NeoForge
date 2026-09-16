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

public final class AddressSerializer {
    private static final String MAILBOX_TYPE = "mailbox";
    private static final String PLAYER_TYPE = "player";

    public static final Codec<Address> CODEC = SerializedAddress.CODEC.comapFlatMap(
            AddressSerializer::decode,
            AddressSerializer::encode
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, Address> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    private AddressSerializer() {
    }

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

    private record SerializedAddress(
            String type,
            Optional<UUID> domainUuid,
            Optional<UUID> districtUuid,
            Optional<UUID> mailboxUuid,
            Optional<String> domainCode,
            Optional<String> districtCode,
            Optional<String> deliveryCode,
            Optional<String> postalCode,
            Optional<String> legacyDomainId,
            Optional<String> legacyDistrictId,
            Optional<String> legacyDeliveryId,
            Optional<UUID> playerId
    ) {
        private static final Codec<SerializedAddress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
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
