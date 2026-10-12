package net.bluafolkloro.overdeterminism.everechoes.postal;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

import java.util.Objects;

/**
 * 封装不可变业务数据。
 */
public record PostalChunk(/** 保存 {@code dimension} 对应的状态或依赖。 */ ResourceLocation dimension, /** 保存 {@code x} 对应的状态或依赖。 */ int x, /** 保存 {@code z} 对应的状态或依赖。 */ int z) {
    /**
     * 创建并初始化当前对象。
     */
    public PostalChunk {
        Objects.requireNonNull(dimension, "dimension");
    }

    /**
     * 执行 {@code at} 对应的业务操作。
     */
    public static PostalChunk at(ResourceLocation dimension, BlockPos position) {
        ChunkPos chunk = new ChunkPos(position);
        return new PostalChunk(dimension, chunk.x, chunk.z);
    }

    /**
     * 执行 {@code unpack} 对应的业务操作。
     */
    public static PostalChunk unpack(ResourceLocation dimension, long packed) {
        return new PostalChunk(dimension, ChunkPos.getX(packed), ChunkPos.getZ(packed));
    }

    /**
     * 执行 {@code packed} 对应的业务操作。
     */
    public long packed() {
        return ChunkPos.asLong(x, z);
    }
}
