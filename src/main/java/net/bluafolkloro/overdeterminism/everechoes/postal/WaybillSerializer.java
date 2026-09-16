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

public final class WaybillSerializer {
    public static final Codec<Waybill> CODEC = SerializedWaybill.CODEC.comapFlatMap(
            WaybillSerializer::decode,
            WaybillSerializer::encode
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, Waybill> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    private WaybillSerializer() {
    }

    private static DataResult<Waybill> decode(SerializedWaybill serialized) {
        try {
            Waybill.CustodyState state = Waybill.CustodyState.valueOf(serialized.state().toUpperCase(Locale.ROOT));
            return DataResult.success(new Waybill(serialized.shipmentId(), state, serialized.carrierId()));
        } catch (RuntimeException exception) {
            return DataResult.error(() -> "Invalid waybill: " + exception.getMessage());
        }
    }

    private static SerializedWaybill encode(Waybill waybill) {
        return new SerializedWaybill(
                waybill.shipmentId(),
                waybill.state().name().toLowerCase(Locale.ROOT),
                waybill.carrierId()
        );
    }

    private record SerializedWaybill(UUID shipmentId, String state, Optional<UUID> carrierId) {
        private static final Codec<SerializedWaybill> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.STRING_CODEC.fieldOf("shipmentId").forGetter(SerializedWaybill::shipmentId),
                Codec.STRING.fieldOf("state").forGetter(SerializedWaybill::state),
                UUIDUtil.STRING_CODEC.optionalFieldOf("carrierId").forGetter(SerializedWaybill::carrierId)
        ).apply(instance, SerializedWaybill::new));
    }
}
