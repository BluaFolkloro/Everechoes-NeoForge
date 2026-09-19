package net.bluafolkloro.overdeterminism.everechoes.network;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.bluafolkloro.overdeterminism.everechoes.postal.DistrictCoverage;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalAtlasLimits;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalAtlasSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostalAtlasLimitsCodecTest {
    @Test
    void readPackedSetRejectsOversizedCountBeforeAllocating() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(PostalAtlasLimits.MAX_SUBMIT_CHUNKS + 1);
        DecoderException thrown = assertThrows(
                DecoderException.class,
                () -> PostalAtlasSyncPayload.readPackedSet(buffer, PostalAtlasLimits.MAX_SUBMIT_CHUNKS)
        );
        assertTrue(thrown.getMessage().contains(String.valueOf(PostalAtlasLimits.MAX_SUBMIT_CHUNKS)));
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    void readPackedSetRejectsNegativeCount() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(-1);
        assertThrows(
                DecoderException.class,
                () -> PostalAtlasSyncPayload.readPackedSet(buffer, PostalAtlasLimits.MAX_SUBMIT_CHUNKS)
        );
    }

    @Test
    void writePackedSetTruncatesToCap() {
        Set<Long> values = new LinkedHashSet<>();
        for (long index = 0; index < 8; index++) {
            values.add(index);
        }
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        PostalAtlasSyncPayload.writePackedSet(buffer, values, 3);
        assertEquals(3, buffer.readVarInt());
        assertEquals(0L, buffer.readLong());
        assertEquals(1L, buffer.readLong());
        assertEquals(2L, buffer.readLong());
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    void readPackedSetAcceptsExactCap() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(2);
        buffer.writeLong(10L);
        buffer.writeLong(11L);
        Set<Long> values = PostalAtlasSyncPayload.readPackedSet(buffer, PostalAtlasLimits.MAX_WINDOW_CELLS);
        assertEquals(Set.of(10L, 11L), values);
    }

    @Test
    void writeSnapshotReadSnapshotRoundTripDoesNotWriteExploredSet() {
        ResourceLocation overworld = ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
        UUID districtId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        PostalAtlasSnapshot original = new PostalAtlasSnapshot(
                districtId,
                overworld,
                4,
                "EV",
                "1",
                0,
                0,
                PostalAtlasLimits.DEFAULT_WINDOW_WIDTH,
                PostalAtlasLimits.DEFAULT_WINDOW_HEIGHT,
                Set.of(0L, 1L),
                Set.of(1L),
                Set.of(0L),
                Set.of(2L),
                Set.of(0L, 1L, 2L),
                Set.of(0L, 2L),
                3,
                DistrictCoverage.MAX_CHUNKS
        );

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        PostalAtlasSyncPayload.writeSnapshot(buffer, original, true);
        PostalAtlasSnapshot decoded = PostalAtlasSyncPayload.readSnapshot(buffer);
        assertEquals(0, buffer.readableBytes());
        assertEquals(original, decoded);
        assertEquals(original.ownedPacked(), decoded.ownedPacked());
        assertEquals(original.foreignPacked(), decoded.foreignPacked());
        assertEquals(original.hubPacked(), decoded.hubPacked());
        assertEquals(original.collectionPacked(), decoded.collectionPacked());
        assertEquals(original.savedCoveragePacked(), decoded.savedCoveragePacked());
        assertEquals(original.nodePacked(), decoded.nodePacked());
        assertEquals(original.savedCoverageSize(), decoded.savedCoverageSize());
        assertEquals(original.maxChunks(), decoded.maxChunks());
    }
}
