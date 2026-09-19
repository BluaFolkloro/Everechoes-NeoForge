package net.bluafolkloro.overdeterminism.everechoes.network;

import net.bluafolkloro.overdeterminism.everechoes.Everechoes;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalAtlasLimits;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Set;

public record PostalAtlasRequestPayload(
        Action action,
        int originX,
        int originZ,
        int width,
        int height,
        int expectedRevision,
        @Nullable ResourceLocation dimension,
        Set<Long> packedChunks
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PostalAtlasRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Everechoes.MODID, "postal_atlas_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PostalAtlasRequestPayload> STREAM_CODEC =
            StreamCodec.of(PostalAtlasRequestPayload::write, PostalAtlasRequestPayload::read);

    public enum Action {
        PAN,
        SUBMIT
    }

    public static PostalAtlasRequestPayload pan(int originX, int originZ, int width, int height) {
        return new PostalAtlasRequestPayload(Action.PAN, originX, originZ, width, height, 0, null, Set.of());
    }

    public static PostalAtlasRequestPayload submit(int expectedRevision, ResourceLocation dimension, Set<Long> packedChunks) {
        return new PostalAtlasRequestPayload(Action.SUBMIT, 0, 0, 0, 0, expectedRevision, dimension, Set.copyOf(packedChunks));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, PostalAtlasRequestPayload payload) {
        buffer.writeEnum(payload.action);
        switch (payload.action) {
            case PAN -> {
                buffer.writeInt(payload.originX);
                buffer.writeInt(payload.originZ);
                buffer.writeVarInt(payload.width);
                buffer.writeVarInt(payload.height);
            }
            case SUBMIT -> {
                buffer.writeInt(payload.expectedRevision);
                buffer.writeResourceLocation(payload.dimension);
                PostalAtlasSyncPayload.writePackedSet(buffer, payload.packedChunks, PostalAtlasLimits.MAX_SUBMIT_CHUNKS);
            }
        }
    }

    private static PostalAtlasRequestPayload read(RegistryFriendlyByteBuf buffer) {
        Action action = buffer.readEnum(Action.class);
        return switch (action) {
            case PAN -> pan(buffer.readInt(), buffer.readInt(), buffer.readVarInt(), buffer.readVarInt());
            case SUBMIT -> submit(
                    buffer.readInt(),
                    buffer.readResourceLocation(),
                    PostalAtlasSyncPayload.readPackedSet(buffer, PostalAtlasLimits.MAX_SUBMIT_CHUNKS)
            );
        };
    }
}
