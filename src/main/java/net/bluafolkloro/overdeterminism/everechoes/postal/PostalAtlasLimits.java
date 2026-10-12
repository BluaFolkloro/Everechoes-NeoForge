package net.bluafolkloro.overdeterminism.everechoes.postal;

import net.minecraft.world.level.Level;

/**
 * 封装业务边界与容量限制。
 */
public final class PostalAtlasLimits {
    /**
     * 定义 {@code DEFAULT_WINDOW_WIDTH} 常量。
     */
    public static final int DEFAULT_WINDOW_WIDTH = 25;
    /**
     * 定义 {@code DEFAULT_WINDOW_HEIGHT} 常量。
     */
    public static final int DEFAULT_WINDOW_HEIGHT = 19;
    /**
     * 定义 {@code MAX_WINDOW_SIDE} 常量。
     */
    public static final int MAX_WINDOW_SIDE = 25;
    /**
     * 定义 {@code MAX_WINDOW_CELLS} 常量。
     */
    public static final int MAX_WINDOW_CELLS = DEFAULT_WINDOW_WIDTH * DEFAULT_WINDOW_HEIGHT;
    /**
     * 定义 {@code MAX_SUBMIT_CHUNKS} 常量。
     */
    public static final int MAX_SUBMIT_CHUNKS = DistrictCoverage.MAX_CHUNKS;
    /**
     * 定义 {@code MIN_CHUNK} 常量。
     */
    public static final int MIN_CHUNK = Math.floorDiv(-Level.MAX_LEVEL_SIZE, 16);
    /**
     * 定义 {@code MAX_CHUNK} 常量。
     */
    public static final int MAX_CHUNK = Math.floorDiv(Level.MAX_LEVEL_SIZE - 1, 16);

    /**
     * 创建并初始化当前对象。
     */
    private PostalAtlasLimits() {}

    /**
     * 判断 {@code isLegalChunk} 对应的业务操作。
     */
    public static boolean isLegalChunk(int x, int z) {
        return x >= MIN_CHUNK && x <= MAX_CHUNK && z >= MIN_CHUNK && z <= MAX_CHUNK;
    }
}
