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

public record PostalAtlasSyncPayload(
        Action action,
        @Nullable PostalAtlasSnapshot window,
        boolean allowed,
        String reasonKey,
        int revision,
        Set<Long> savedCoveragePacked
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PostalAtlasSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Everechoes.MODID, "postal_atlas_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PostalAtlasSyncPayload> STREAM_CODEC =
            StreamCodec.of(PostalAtlasSyncPayload::write, PostalAtlasSyncPayload::read);

    private static final int REASON_MAX = 64;
    private static final int CODE_MAX = 8;

    public enum Action {
        WINDOW,
        RESULT
    }

    public static PostalAtlasSyncPayload window(PostalAtlasSnapshot snapshot) {
        return new PostalAtlasSyncPayload(Action.WINDOW, snapshot, true, "", snapshot.revision(), Set.of());
    }

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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

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

    public static void requirePackedCount(int count, int maxSize) {
        if (count < 0 || count > maxSize) {
            throw new DecoderException("packed set count " + count + " exceeds " + maxSize);
        }
    }

    public static Set<Long> readPackedSet(FriendlyByteBuf buffer, int maxSize) {
        int count = buffer.readVarInt();
        requirePackedCount(count, maxSize);
        Set<Long> values = new LinkedHashSet<>(count);
        for (int index = 0; index < count; index++) {
            values.add(buffer.readLong());
        }
        return Set.copyOf(values);
    }

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
        writePackedSet(buffer, snapshot.exploredPacked(), PostalAtlasLimits.MAX_WINDOW_CELLS);
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
                || width > PostalAtlasLimits.WINDOW_SIZE
                || height > PostalAtlasLimits.WINDOW_SIZE
                || width * height > PostalAtlasLimits.MAX_WINDOW_CELLS) {
            throw new DecoderException("atlas window " + width + "x" + height + " exceeds limits");
        }
        Set<Long> exploredPacked = readPackedSet(buffer, PostalAtlasLimits.MAX_WINDOW_CELLS);
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
                exploredPacked,
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

    private static String clamp(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
