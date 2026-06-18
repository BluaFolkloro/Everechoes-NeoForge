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

// Serialization bridge for the sealed Address hierarchy.
// Address 是 sealed interface，运行时可能是 MailBoxAddress 或 PlayerAddress；Data Component
// 不能直接知道应该恢复成哪个实现类，所以这里先转换成带 type 字段的中间记录。
//
// The persistent CODEC is used by DataComponentType.Builder#persistent for save data.
// 持久化 CODEC 供 DataComponentType.Builder#persistent 使用，负责把地址写入物品组件的存档数据。
//
// The STREAM_CODEC is used by DataComponentType.Builder#networkSynchronized for network sync.
// 网络 STREAM_CODEC 供 DataComponentType.Builder#networkSynchronized 使用，负责服务端和客户端之间同步组件值。
public final class AddressSerializer {
    private static final String MAILBOX_TYPE = "mailbox";
    private static final String PLAYER_TYPE = "player";

    // Converts between the public Address type and the serializable intermediate record.
    // 在公开的 Address 类型与可序列化的中间记录之间转换。
    //
    // comapFlatMap lets decoding fail with DataResult.error instead of throwing for malformed saved data.
    // comapFlatMap 允许反序列化在存档数据非法时返回 DataResult.error，而不是直接抛异常。
    public static final Codec<Address> CODEC = SerializedAddress.CODEC.comapFlatMap(
            AddressSerializer::decode,
            AddressSerializer::encode
    );

    // Uses Minecraft's registry-aware wrapper so the same structure can be sent through Data Component sync.
    // 使用 Minecraft 带注册表上下文的包装器，使同一套结构可以用于 Data Component 网络同步。
    public static final StreamCodec<RegistryFriendlyByteBuf, Address> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    private AddressSerializer() {
    }

    // Restores the concrete Address implementation according to the type discriminator.
    // 根据 type 类型标识恢复具体的 Address 实现类。
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

    // Writes only the fields needed by the concrete Address implementation.
    // 只写入当前具体 Address 实现需要的字段。
    private static SerializedAddress encode(Address address) {
        if (address instanceof MailBoxAddress mailBoxAddress) {
            return new SerializedAddress(MAILBOX_TYPE, Optional.of(mailBoxAddress.postalCode()), Optional.empty());
        }

        if (address instanceof PlayerAddress playerAddress) {
            return new SerializedAddress(PLAYER_TYPE, Optional.empty(), Optional.of(playerAddress.playerId()));
        }

        throw new IllegalArgumentException("Unsupported address type: " + address.getClass().getName());
    }

    // Flat serialized shape shared by persistent and network codecs.
    // 持久化和网络编码共用的扁平序列化结构。
    //
    // type is required; postalCode and playerId are optional because each address type only needs one payload field.
    // type 是必填字段；postalCode 和 playerId 是可选字段，因为每种地址只需要其中一个负载字段。
    private record SerializedAddress(String type, Optional<String> postalCode, Optional<UUID> playerId) {
        private static final Codec<SerializedAddress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("type").forGetter(SerializedAddress::type),
                Codec.STRING.optionalFieldOf("postalCode").forGetter(SerializedAddress::postalCode),
                UUIDUtil.STRING_CODEC.optionalFieldOf("playerId").forGetter(SerializedAddress::playerId)
        ).apply(instance, SerializedAddress::new));
    }
}
