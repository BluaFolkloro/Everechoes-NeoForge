package net.bluafolkloro.overdeterminism.everechoes.postal;

import net.minecraft.world.level.Level;

public final class PostalAtlasLimits {
    public static final int DEFAULT_WINDOW_WIDTH = 25;
    public static final int DEFAULT_WINDOW_HEIGHT = 19;
    public static final int MAX_WINDOW_SIDE = 25;
    public static final int MAX_WINDOW_CELLS = DEFAULT_WINDOW_WIDTH * DEFAULT_WINDOW_HEIGHT;
    public static final int MAX_SUBMIT_CHUNKS = DistrictCoverage.MAX_CHUNKS;
    public static final int MIN_CHUNK = Math.floorDiv(-Level.MAX_LEVEL_SIZE, 16);
    public static final int MAX_CHUNK = Math.floorDiv(Level.MAX_LEVEL_SIZE - 1, 16);

    private PostalAtlasLimits() {}

    public static boolean isLegalChunk(int x, int z) {
        return x >= MIN_CHUNK && x <= MAX_CHUNK && z >= MIN_CHUNK && z <= MAX_CHUNK;
    }
}
