package net.bluafolkloro.overdeterminism.everechoes.network;

import net.bluafolkloro.overdeterminism.everechoes.Everechoes;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalCodes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PostBoxConfigPayload(Action action, BlockPos pos, String domainCode) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PostBoxConfigPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Everechoes.MODID, "post_box_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PostBoxConfigPayload> STREAM_CODEC =
            StreamCodec.of(PostBoxConfigPayload::write, PostBoxConfigPayload::read);

    public enum Action {
        CREATE,
        SELECT
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, PostBoxConfigPayload payload) {
        buffer.writeEnum(payload.action);
        buffer.writeBlockPos(payload.pos);
        buffer.writeUtf(payload.domainCode, PostalCodes.DOMAIN_MAX);
    }

    private static PostBoxConfigPayload read(RegistryFriendlyByteBuf buffer) {
        return new PostBoxConfigPayload(
                buffer.readEnum(Action.class),
                buffer.readBlockPos(),
                buffer.readUtf(PostalCodes.DOMAIN_MAX)
        );
    }
}
