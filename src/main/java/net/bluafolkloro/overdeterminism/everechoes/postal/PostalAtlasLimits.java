package net.bluafolkloro.overdeterminism.everechoes.postal;

import net.minecraft.world.level.Level;

public final class PostalAtlasLimits {
    public static final int WINDOW_SIZE = 21;
    public static final int MAX_WINDOW_CELLS = 441;
    public static final int MAX_SUBMIT_CHUNKS = DistrictCoverage.MAX_CHUNKS;
    public static final int MIN_CHUNK = Math.floorDiv(-Level.MAX_LEVEL_SIZE, 16);
    public static final int MAX_CHUNK = Math.floorDiv(Level.MAX_LEVEL_SIZE - 1, 16);

    private PostalAtlasLimits() {}

    public static boolean isLegalChunk(int x, int z) {
        return x >= MIN_CHUNK && x <= MAX_CHUNK && z >= MIN_CHUNK && z <= MAX_CHUNK;
    }
}
