package net.bluafolkloro.overdeterminism.everechoes.network;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalAtlasLimits;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

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
}
