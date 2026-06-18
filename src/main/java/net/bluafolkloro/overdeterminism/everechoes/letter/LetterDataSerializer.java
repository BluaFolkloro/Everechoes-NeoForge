package net.bluafolkloro.overdeterminism.everechoes.letter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.bluafolkloro.overdeterminism.everechoes.postal.Address;
import net.bluafolkloro.overdeterminism.everechoes.postal.AddressSerializer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

// Serialization bridge for mutable LetterData snapshots.
// LetterData 是可变对象；序列化时这里会读取当前字段形成快照，反序列化时再通过工厂方法重建对象。
//
// The persistent CODEC is intended for DataComponentType.Builder#persistent.
// 持久化 CODEC 供 DataComponentType.Builder#persistent 使用，负责把信件数据写入物品组件存档。
//
// The STREAM_CODEC is intended for DataComponentType.Builder#networkSynchronized.
// 网络 STREAM_CODEC 供 DataComponentType.Builder#networkSynchronized 使用，负责同步服务端和客户端的信件组件值。
public final class LetterDataSerializer {
    private static final Codec<LetterState> STATE_CODEC = Codec.STRING.comapFlatMap(
            LetterDataSerializer::decodeState,
            state -> state.name().toLowerCase(Locale.ROOT)
    );

    // Converts between the mutable LetterData object and an immutable serialized record.
    // 在可变的 LetterData 对象与不可变的序列化记录之间转换。
    public static final Codec<LetterData> CODEC = SerializedLetterData.CODEC.comapFlatMap(
            LetterDataSerializer::decode,
            LetterDataSerializer::encode
    );

    // Uses the same persistent structure for network synchronization through Minecraft's registry-aware wrapper.
    // 通过 Minecraft 带注册表上下文的包装器，让网络同步复用同一套持久化结构。
    public static final StreamCodec<RegistryFriendlyByteBuf, LetterData> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    private LetterDataSerializer() {
    }

    private static DataResult<LetterState> decodeState(String value) {
        try {
            return DataResult.success(LetterState.valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(() -> "Unknown letter state: " + value);
        }
    }

    // Reconstructs LetterData through its validation-aware factory.
    // 通过带校验的工厂方法重建 LetterData。
    private static DataResult<LetterData> decode(SerializedLetterData serializedLetterData) {
        return LetterData.tryReconstruct(
                serializedLetterData.letterId(),
                serializedLetterData.state(),
                serializedLetterData.returnAddress(),
                serializedLetterData.recipientAddress().orElse(null),
                serializedLetterData.title(),
                serializedLetterData.body(),
                serializedLetterData.signatureSender().orElse(null),
                serializedLetterData.letterRecipient().orElse(null)
        ).<DataResult<LetterData>>map(DataResult::success)
                .orElseGet(() -> DataResult.error(() -> "Invalid letter data"));
    }

    // Writes a snapshot of the current LetterData fields.
    // 写入 LetterData 当前字段的快照。
    private static SerializedLetterData encode(LetterData letterData) {
        return new SerializedLetterData(
                letterData.letterId(),
                letterData.state(),
                letterData.returnAddress(),
                letterData.recipientAddress(),
                letterData.title(),
                letterData.body(),
                letterData.signatureSender(),
                letterData.letterRecipient()
        );
    }

    // Flat serialized shape shared by persistent and network codecs.
    // 持久化和网络编码共用的扁平序列化结构。
    private record SerializedLetterData(
            UUID letterId,
            LetterState state,
            Address returnAddress,
            Optional<Address> recipientAddress,
            String title,
            String body,
            Optional<String> signatureSender,
            Optional<String> letterRecipient
    ) {
        private static final Codec<SerializedLetterData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.STRING_CODEC.fieldOf("letterId").forGetter(SerializedLetterData::letterId),
                STATE_CODEC.fieldOf("state").forGetter(SerializedLetterData::state),
                AddressSerializer.CODEC.fieldOf("returnAddress").forGetter(SerializedLetterData::returnAddress),
                AddressSerializer.CODEC.optionalFieldOf("recipientAddress").forGetter(SerializedLetterData::recipientAddress),
                Codec.STRING.fieldOf("title").forGetter(SerializedLetterData::title),
                Codec.STRING.fieldOf("body").forGetter(SerializedLetterData::body),
                Codec.STRING.optionalFieldOf("signatureSender").forGetter(SerializedLetterData::signatureSender),
                Codec.STRING.optionalFieldOf("letterRecipient").forGetter(SerializedLetterData::letterRecipient)
        ).apply(instance, SerializedLetterData::new));
    }
}
