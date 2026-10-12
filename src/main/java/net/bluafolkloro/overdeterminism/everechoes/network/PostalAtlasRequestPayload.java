package net.bluafolkloro.overdeterminism.everechoes.network;

import net.bluafolkloro.overdeterminism.everechoes.Everechoes;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalAtlasLimits;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * 封装客户端与服务端之间的网络载荷。
 */
public record PostalAtlasRequestPayload(
        /**
         * 保存 {@code action} 对应的状态或依赖。
         */
        Action action,
        /**
         * 保存 {@code originX} 对应的状态或依赖。
         */
        int originX,
        /**
         * 保存 {@code originZ} 对应的状态或依赖。
         */
        int originZ,
        /**
         * 保存 {@code width} 对应的状态或依赖。
         */
        int width,
        /**
         * 保存 {@code height} 对应的状态或依赖。
         */
        int height,
        /**
         * 保存 {@code expectedRevision} 对应的状态或依赖。
         */
        int expectedRevision,
        /**
         * 保存 {@code dimension} 对应的状态或依赖。
         */
        @Nullable ResourceLocation dimension,
        /**
         * 保存 {@code packedChunks} 对应的状态或依赖。
         */
        Set<Long> packedChunks
) implements CustomPacketPayload {
    /**
     * 定义 {@code TYPE} 常量。
     */
    public static final CustomPacketPayload.Type<PostalAtlasRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Everechoes.MODID, "postal_atlas_request"));

    /**
     * 定义 {@code STREAM_CODEC} 常量。
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, PostalAtlasRequestPayload> STREAM_CODEC =
            StreamCodec.of(PostalAtlasRequestPayload::write, PostalAtlasRequestPayload::read);

    /**
     * 封装一组受限且稳定的业务状态。
     */
    public enum Action {
        /**
         * 定义 {@code PAN} 常量。
         */
        PAN,
        /**
         * 定义 {@code SUBMIT} 常量。
         */
        SUBMIT
    }

    /**
     * 执行 {@code pan} 对应的业务操作。
     */
    public static PostalAtlasRequestPayload pan(int originX, int originZ, int width, int height) {
        return new PostalAtlasRequestPayload(Action.PAN, originX, originZ, width, height, 0, null, Set.of());
    }

    /**
     * 提交或发送 {@code submit} 对应的业务操作。
     */
    public static PostalAtlasRequestPayload submit(int expectedRevision, ResourceLocation dimension, Set<Long> packedChunks) {
        return new PostalAtlasRequestPayload(Action.SUBMIT, 0, 0, 0, 0, expectedRevision, dimension, Set.copyOf(packedChunks));
    }

    /**
     * 执行 {@code type} 对应的业务操作。
     */
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 保存或编码 {@code write} 对应的业务操作。
     */
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

    /**
     * 获取或解析 {@code read} 对应的业务操作。
     */
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
