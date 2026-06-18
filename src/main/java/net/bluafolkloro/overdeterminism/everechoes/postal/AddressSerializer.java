package net.bluafolkloro.overdeterminism.everechoes.postal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;
import java.util.UUID;

public final class AddressSerializer {
    private static final String MAILBOX_TYPE = "mailbox";
    private static final String PLAYER_TYPE = "player";

    // Persistent address codec used by DataComponentType.Builder#persistent.
    // 用于 DataComponentType.Builder#persistent 的地址持久化 Codec。
    public static final Codec<Address> CODEC = SerializedAddress.CODEC.comapFlatMap(
            AddressSerializer::decode,
            AddressSerializer::encode
    );

    // Network address codec used by DataComponentType.Builder#networkSynchronized.
    // 用于 DataComponentType.Builder#networkSynchronized 的地址网络同步 StreamCodec。
    public static final StreamCodec<RegistryFriendlyByteBuf, Address> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    private AddressSerializer() {
    }

    private static DataResult<Address> decode(SerializedAddress serializedAddress) {
        return switch (serializedAddress.type()) {
            case MAILBOX_TYPE -> serializedAddress.postalCode()
                    .<DataResult<Address>>map(postalCode -> DataResult.success(new MailBoxAddress(postalCode)))
                    .orElseGet(() -> DataResult.error(() -> "Mailbox address is missing postalCode"));
            case PLAYER_TYPE -> serializedAddress.playerId()
                    .<DataResult<Address>>map(playerId -> DataResult.success(new PlayerAddress(playerId)))
                    .orElseGet(() -> DataResult.error(() -> "Player address is missing playerId"));
            default -> DataResult.error(() -> "Unknown address type: " + serializedAddress.type());
        };
    }

    private static SerializedAddress encode(Address address) {
        if (address instanceof MailBoxAddress mailBoxAddress) {
            return new SerializedAddress(MAILBOX_TYPE, Optional.of(mailBoxAddress.postalCode()), Optional.empty());
        }

        if (address instanceof PlayerAddress playerAddress) {
            return new SerializedAddress(PLAYER_TYPE, Optional.empty(), Optional.of(playerAddress.playerId()));
        }

        throw new IllegalArgumentException("Unsupported address type: " + address.getClass().getName());
    }

    private record SerializedAddress(String type, Optional<String> postalCode, Optional<UUID> playerId) {
        private static final Codec<SerializedAddress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("type").forGetter(SerializedAddress::type),
                Codec.STRING.optionalFieldOf("postalCode").forGetter(SerializedAddress::postalCode),
                UUIDUtil.STRING_CODEC.optionalFieldOf("playerId").forGetter(SerializedAddress::playerId)
        ).apply(instance, SerializedAddress::new));
    }
}
