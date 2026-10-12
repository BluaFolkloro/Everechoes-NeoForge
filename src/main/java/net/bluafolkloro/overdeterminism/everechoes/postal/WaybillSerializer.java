package net.bluafolkloro.overdeterminism.everechoes.postal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * 封装数据的序列化与反序列化。
 */
public final class WaybillSerializer {
    /**
     * 定义 {@code CODEC} 常量。
     */
    public static final Codec<Waybill> CODEC = SerializedWaybill.CODEC.comapFlatMap(
            WaybillSerializer::decode,
            WaybillSerializer::encode
    );

    /**
     * 定义 {@code STREAM_CODEC} 常量。
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, Waybill> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    /**
     * 创建并初始化当前对象。
     */
    private WaybillSerializer() {
    }

    /**
     * 读取并恢复 {@code decode} 对应的业务操作。
     */
    private static DataResult<Waybill> decode(SerializedWaybill serialized) {
        try {
            Waybill.CustodyState state = Waybill.CustodyState.valueOf(serialized.state().toUpperCase(Locale.ROOT));
            return DataResult.success(new Waybill(serialized.shipmentId(), state, serialized.carrierId()));
        } catch (RuntimeException exception) {
            return DataResult.error(() -> "Invalid waybill: " + exception.getMessage());
        }
    }

    /**
     * 保存或编码 {@code encode} 对应的业务操作。
     */
    private static SerializedWaybill encode(Waybill waybill) {
        return new SerializedWaybill(
                waybill.shipmentId(),
                waybill.state().name().toLowerCase(Locale.ROOT),
                waybill.carrierId()
        );
    }

    /**
     * 封装不可变业务数据。
     */
    private record SerializedWaybill(/** 保存 {@code shipmentId} 对应的状态或依赖。 */ UUID shipmentId, /** 保存 {@code state} 对应的状态或依赖。 */ String state, /** 保存 {@code carrierId} 对应的状态或依赖。 */ Optional<UUID> carrierId) {
        /**
         * 定义 {@code CODEC} 常量。
         */
        private static final Codec<SerializedWaybill> CODEC = RecordCodecBuilder.create(/** 保存 {@code instance} 对应的状态或依赖。 */ instance -> instance.group(
                UUIDUtil.STRING_CODEC.fieldOf("shipmentId").forGetter(SerializedWaybill::shipmentId),
                Codec.STRING.fieldOf("state").forGetter(SerializedWaybill::state),
                UUIDUtil.STRING_CODEC.optionalFieldOf("carrierId").forGetter(SerializedWaybill::carrierId)
        ).apply(instance, SerializedWaybill::new));
    }
}
