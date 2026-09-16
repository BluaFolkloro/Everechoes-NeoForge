package net.bluafolkloro.overdeterminism.everechoes.network;

import net.bluafolkloro.overdeterminism.everechoes.Everechoes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.UUID;

public record PostBoxConfigPayload(Action action, BlockPos pos, String domainCode, @Nullable UUID targetDistrictId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PostBoxConfigPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Everechoes.MODID, "post_box_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PostBoxConfigPayload> STREAM_CODEC =
            StreamCodec.of(PostBoxConfigPayload::write, PostBoxConfigPayload::read);

    public enum Action {
        CREATE_DISTRICT,
        JOIN_NEARBY,
        ESTABLISH_DOMAIN,
        JOIN_DOMAIN,
        LEAVE_DOMAIN,
        LEAVE_DISTRICT
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, PostBoxConfigPayload payload) {
        buffer.writeEnum(payload.action);
        buffer.writeBlockPos(payload.pos);
        buffer.writeUtf(payload.domainCode, 8);
        buffer.writeBoolean(payload.targetDistrictId != null);
        if (payload.targetDistrictId != null) {
            buffer.writeUUID(payload.targetDistrictId);
        }
    }

    private static PostBoxConfigPayload read(RegistryFriendlyByteBuf buffer) {
        Action action = buffer.readEnum(Action.class);
        BlockPos pos = buffer.readBlockPos();
        String domainCode = buffer.readUtf(8);
        UUID target = buffer.readBoolean() ? buffer.readUUID() : null;
        return new PostBoxConfigPayload(action, pos, domainCode, target);
    }
}
