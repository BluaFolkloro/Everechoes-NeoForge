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

    private static DataResult<Address> decode(SerializedAddress serializedAddress) {
        try {
            return switch (serializedAddress.type()) {
                case MAILBOX_TYPE -> decodeMailbox(serializedAddress);
                case PLAYER_TYPE -> serializedAddress.playerId()
                        .<DataResult<Address>>map(playerId -> DataResult.success(new PlayerAddress(playerId)))
                        .orElseGet(() -> DataResult.error(() -> "Player address is missing playerId"));
                default -> DataResult.error(() -> "Unknown address type: " + serializedAddress.type());
            };
        } catch (RuntimeException exception) {
            return DataResult.error(() -> "Invalid address: " + exception.getMessage());
        }
    }

    private static DataResult<Address> decodeMailbox(SerializedAddress serializedAddress) {
        if (serializedAddress.domainId().isPresent()
                && serializedAddress.districtId().isPresent()
                && serializedAddress.deliveryId().isPresent()) {
            return DataResult.success(new MailBoxAddress(
                    serializedAddress.domainId().orElseThrow(),
                    serializedAddress.districtId().orElseThrow(),
                    serializedAddress.deliveryId().orElseThrow()
            ));
        }

        return serializedAddress.postalCode()
                .flatMap(MailBoxAddress::parse)
                .<DataResult<Address>>map(DataResult::success)
                .orElseGet(() -> DataResult.error(() -> "Mailbox address is missing domainId/districtId/deliveryId"));
    }

    private static SerializedAddress encode(Address address) {
        Objects.requireNonNull(address, "address cannot be null");
        if (address instanceof MailBoxAddress mailBoxAddress) {
            return new SerializedAddress(
                    MAILBOX_TYPE,
                    Optional.of(mailBoxAddress.domainId()),
                    Optional.of(mailBoxAddress.districtId()),
                    Optional.of(mailBoxAddress.deliveryId()),
                    Optional.of(mailBoxAddress.postalCode()),
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
                    Optional.of(playerAddress.playerId())
            );
        }

        throw new IllegalArgumentException("Unsupported address type: " + address.getClass().getName());
    }

    private record SerializedAddress(
            String type,
            Optional<String> domainId,
            Optional<String> districtId,
            Optional<String> deliveryId,
            Optional<String> postalCode,
            Optional<UUID> playerId
    ) {
        private static final Codec<SerializedAddress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("type").forGetter(SerializedAddress::type),
                Codec.STRING.optionalFieldOf("domainId").forGetter(SerializedAddress::domainId),
                Codec.STRING.optionalFieldOf("districtId").forGetter(SerializedAddress::districtId),
                Codec.STRING.optionalFieldOf("deliveryId").forGetter(SerializedAddress::deliveryId),
                Codec.STRING.optionalFieldOf("postalCode").forGetter(SerializedAddress::postalCode),
                UUIDUtil.STRING_CODEC.optionalFieldOf("playerId").forGetter(SerializedAddress::playerId)
        ).apply(instance, SerializedAddress::new));
    }
}
