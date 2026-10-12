package net.bluafolkloro.overdeterminism.everechoes.network;

import io.netty.handler.codec.DecoderException;
import net.bluafolkloro.overdeterminism.everechoes.Everechoes;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalAtlasLimits;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalAtlasSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 封装客户端与服务端之间的网络载荷。
 */
public record PostalAtlasSyncPayload(
        /**
         * 保存 {@code action} 对应的状态或依赖。
         */
        Action action,
        /**
         * 保存 {@code window} 对应的状态或依赖。
         */
        @Nullable PostalAtlasSnapshot window,
        /**
         * 保存 {@code allowed} 对应的状态或依赖。
         */
        boolean allowed,
        /**
         * 保存 {@code reasonKey} 对应的状态或依赖。
         */
        String reasonKey,
        /**
         * 保存 {@code revision} 对应的状态或依赖。
         */
        int revision,
        /**
         * 保存 {@code savedCoveragePacked} 对应的状态或依赖。
         */
        Set<Long> savedCoveragePacked
) implements CustomPacketPayload {
    /**
     * 定义 {@code TYPE} 常量。
     */
    public static final CustomPacketPayload.Type<PostalAtlasSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Everechoes.MODID, "postal_atlas_sync"));

    /**
     * 定义 {@code STREAM_CODEC} 常量。
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, PostalAtlasSyncPayload> STREAM_CODEC =
            StreamCodec.of(PostalAtlasSyncPayload::write, PostalAtlasSyncPayload::read);

    /**
     * 定义 {@code REASON_MAX} 常量。
     */
    private static final int REASON_MAX = 64;
    /**
     * 定义 {@code CODE_MAX} 常量。
     */
    private static final int CODE_MAX = 8;

    /**
     * 封装一组受限且稳定的业务状态。
     */
    public enum Action {
        /**
         * 定义 {@code WINDOW} 常量。
         */
        WINDOW,
        /**
         * 定义 {@code RESULT} 常量。
         */
        RESULT
    }

    /**
     * 执行 {@code window} 对应的业务操作。
     */
    public static PostalAtlasSyncPayload window(PostalAtlasSnapshot snapshot) {
        return new PostalAtlasSyncPayload(Action.WINDOW, snapshot, true, "", snapshot.revision(), Set.of());
    }

    /**
     * 执行 {@code result} 对应的业务操作。
     */
    public static PostalAtlasSyncPayload result(boolean allowed, String reasonKey, int revision, Set<Long> savedCoveragePacked) {
        return new PostalAtlasSyncPayload(
                Action.RESULT,
                null,
                allowed,
                reasonKey == null ? "" : reasonKey,
                revision,
                allowed ? Set.copyOf(savedCoveragePacked) : Set.of()
        );
    }

    /**
     * 执行 {@code type} 对应的业务操作。
     */
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 保存或编码 {@code writePackedSet} 对应的业务操作。
     */
    public static void writePackedSet(FriendlyByteBuf buffer, Set<Long> values, int maxSize) {
        int size = Math.min(values.size(), maxSize);
        buffer.writeVarInt(size);
        int written = 0;
        for (long packed : values) {
            if (written >= maxSize) {
                break;
            }
            buffer.writeLong(packed);
            written++;
        }
    }

    /**
     * 执行 {@code requirePackedCount} 对应的业务操作。
     */
    public static void requirePackedCount(int count, int maxSize) {
        if (count < 0 || count > maxSize) {
            throw new DecoderException("packed set count " + count + " exceeds " + maxSize);
        }
    }

    /**
     * 获取或解析 {@code readPackedSet} 对应的业务操作。
     */
    public static Set<Long> readPackedSet(FriendlyByteBuf buffer, int maxSize) {
        int count = buffer.readVarInt();
        requirePackedCount(count, maxSize);
        Set<Long> values = new LinkedHashSet<>(count);
        for (int index = 0; index < count; index++) {
            values.add(buffer.readLong());
        }
        return Set.copyOf(values);
    }

    /**
     * 保存或编码 {@code writeSnapshot} 对应的业务操作。
     */
    public static void writeSnapshot(FriendlyByteBuf buffer, PostalAtlasSnapshot snapshot, boolean includeSavedCoverage) {
        buffer.writeUUID(snapshot.districtId());
        buffer.writeResourceLocation(snapshot.dimension());
        buffer.writeInt(snapshot.revision());
        buffer.writeUtf(clamp(snapshot.domainCode(), CODE_MAX), CODE_MAX);
        buffer.writeUtf(clamp(snapshot.districtCode(), CODE_MAX), CODE_MAX);
        buffer.writeInt(snapshot.originX());
        buffer.writeInt(snapshot.originZ());
        buffer.writeVarInt(snapshot.width());
        buffer.writeVarInt(snapshot.height());
        writePackedSet(buffer, snapshot.ownedPacked(), PostalAtlasLimits.MAX_WINDOW_CELLS);
        writePackedSet(buffer, snapshot.foreignPacked(), PostalAtlasLimits.MAX_WINDOW_CELLS);
        writePackedSet(buffer, snapshot.hubPacked(), PostalAtlasLimits.MAX_WINDOW_CELLS);
        writePackedSet(buffer, snapshot.collectionPacked(), PostalAtlasLimits.MAX_WINDOW_CELLS);
        writePackedSet(
                buffer,
                includeSavedCoverage ? snapshot.savedCoveragePacked() : Set.of(),
                PostalAtlasLimits.MAX_SUBMIT_CHUNKS
        );
        writePackedSet(buffer, snapshot.nodePacked(), PostalAtlasLimits.MAX_SUBMIT_CHUNKS);
        buffer.writeVarInt(snapshot.savedCoverageSize());
        buffer.writeVarInt(snapshot.maxChunks());
    }

    /**
     * 获取或解析 {@code readSnapshot} 对应的业务操作。
     */
    public static PostalAtlasSnapshot readSnapshot(FriendlyByteBuf buffer) {
        UUID districtId = buffer.readUUID();
        ResourceLocation dimension = buffer.readResourceLocation();
        int revision = buffer.readInt();
        String domainCode = buffer.readUtf(CODE_MAX);
        String districtCode = buffer.readUtf(CODE_MAX);
        int originX = buffer.readInt();
        int originZ = buffer.readInt();
        int width = buffer.readVarInt();
        int height = buffer.readVarInt();
        if (width < 1
                || height < 1
                || width > PostalAtlasLimits.MAX_WINDOW_SIDE
                || height > PostalAtlasLimits.MAX_WINDOW_SIDE
                || width * height > PostalAtlasLimits.MAX_WINDOW_CELLS) {
            throw new DecoderException("atlas window " + width + "x" + height + " exceeds limits");
        }
        Set<Long> ownedPacked = readPackedSet(buffer, PostalAtlasLimits.MAX_WINDOW_CELLS);
        Set<Long> foreignPacked = readPackedSet(buffer, PostalAtlasLimits.MAX_WINDOW_CELLS);
        Set<Long> hubPacked = readPackedSet(buffer, PostalAtlasLimits.MAX_WINDOW_CELLS);
        Set<Long> collectionPacked = readPackedSet(buffer, PostalAtlasLimits.MAX_WINDOW_CELLS);
        Set<Long> savedCoveragePacked = readPackedSet(buffer, PostalAtlasLimits.MAX_SUBMIT_CHUNKS);
        Set<Long> nodePacked = readPackedSet(buffer, PostalAtlasLimits.MAX_SUBMIT_CHUNKS);
        int savedCoverageSize = buffer.readVarInt();
        int maxChunks = buffer.readVarInt();
        return new PostalAtlasSnapshot(
                districtId,
                dimension,
                revision,
                domainCode,
                districtCode,
                originX,
                originZ,
                width,
                height,
                ownedPacked,
                foreignPacked,
                hubPacked,
                collectionPacked,
                savedCoveragePacked,
                nodePacked,
                savedCoverageSize,
                maxChunks
        );
    }

    /**
     * 保存或编码 {@code write} 对应的业务操作。
     */
    private static void write(RegistryFriendlyByteBuf buffer, PostalAtlasSyncPayload payload) {
        buffer.writeEnum(payload.action);
        switch (payload.action) {
            case WINDOW -> writeSnapshot(buffer, payload.window, false);
            case RESULT -> {
                buffer.writeBoolean(payload.allowed);
                buffer.writeUtf(clamp(payload.reasonKey, REASON_MAX), REASON_MAX);
                buffer.writeInt(payload.revision);
                writePackedSet(
                        buffer,
                        payload.allowed ? payload.savedCoveragePacked : Set.of(),
                        PostalAtlasLimits.MAX_SUBMIT_CHUNKS
                );
            }
        }
    }

    /**
     * 获取或解析 {@code read} 对应的业务操作。
     */
    private static PostalAtlasSyncPayload read(RegistryFriendlyByteBuf buffer) {
        Action action = buffer.readEnum(Action.class);
        return switch (action) {
            case WINDOW -> window(readSnapshot(buffer));
            case RESULT -> result(
                    buffer.readBoolean(),
                    buffer.readUtf(REASON_MAX),
                    buffer.readInt(),
                    readPackedSet(buffer, PostalAtlasLimits.MAX_SUBMIT_CHUNKS)
            );
        };
    }

    /**
     * 执行 {@code clamp} 对应的业务操作。
     */
    private static String clamp(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
