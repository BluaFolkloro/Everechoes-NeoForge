package net.bluafolkloro.overdeterminism.everechoes.postal;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

import java.util.Objects;

public record PostalChunk(ResourceLocation dimension, int x, int z) {
    public PostalChunk {
        Objects.requireNonNull(dimension, "dimension");
    }

    public static PostalChunk at(ResourceLocation dimension, BlockPos position) {
        ChunkPos chunk = new ChunkPos(position);
        return new PostalChunk(dimension, chunk.x, chunk.z);
    }

    public static PostalChunk unpack(ResourceLocation dimension, long packed) {
        return new PostalChunk(dimension, ChunkPos.getX(packed), ChunkPos.getZ(packed));
    }

    public long packed() {
        return ChunkPos.asLong(x, z);
    }
}
