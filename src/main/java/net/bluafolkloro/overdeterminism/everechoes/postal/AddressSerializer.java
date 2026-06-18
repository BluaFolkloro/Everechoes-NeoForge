package net.bluafolkloro.overdeterminism.everechoes.postal;

import net.minecraft.nbt.CompoundTag;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class AddressSerializer {
    private static final String TYPE_KEY = "type";
    private static final String POSTAL_CODE_KEY = "postalCode";
    private static final String PLAYER_ID_KEY = "playerId";

    private static final String MAILBOX_TYPE = "mailbox";
    private static final String PLAYER_TYPE = "player";

    private AddressSerializer() {
    }

    // Serializes a postal address into NBT with an explicit type discriminator.
    // 将邮政地址序列化为 NBT，并写入明确的类型标识。
    public static CompoundTag serialize(Address address) {
        Objects.requireNonNull(address, "address cannot be null");

        CompoundTag tag = new CompoundTag();
        if (address instanceof LetterBoxAddress letterBoxAddress) {
            tag.putString(TYPE_KEY, MAILBOX_TYPE);
            tag.putString(POSTAL_CODE_KEY, letterBoxAddress.postalCode());
            return tag;
        }

        if (address instanceof PlayerAddress playerAddress) {
            tag.putString(TYPE_KEY, PLAYER_TYPE);
            tag.putString(PLAYER_ID_KEY, playerAddress.playerId().toString());
            return tag;
        }

        throw new IllegalArgumentException("Unsupported address type: " + address.getClass().getName());
    }

    // Deserializes a postal address from NBT and rejects malformed data.
    // 从 NBT 反序列化邮政地址，并拒绝格式错误的数据。
    public static Address deserialize(CompoundTag tag) {
        Objects.requireNonNull(tag, "tag cannot be null");

        return switch (tag.getString(TYPE_KEY)) {
            case MAILBOX_TYPE -> new LetterBoxAddress(tag.getString(POSTAL_CODE_KEY));
            case PLAYER_TYPE -> new PlayerAddress(UUID.fromString(tag.getString(PLAYER_ID_KEY)));
            default -> throw new IllegalArgumentException("Unknown address type: " + tag.getString(TYPE_KEY));
        };
    }

    // Attempts to deserialize a postal address without throwing on malformed data.
    // 尝试从 NBT 反序列化邮政地址；数据格式错误时不抛出异常，而是返回空结果。
    public static Optional<Address> tryDeserialize(CompoundTag tag) {
        try {
            return Optional.of(deserialize(tag));
        } catch (NullPointerException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
