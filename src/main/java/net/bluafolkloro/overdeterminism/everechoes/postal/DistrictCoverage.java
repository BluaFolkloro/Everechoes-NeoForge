package net.bluafolkloro.overdeterminism.everechoes.postal;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 封装不可变业务数据。
 */
public record DistrictCoverage(/** 保存 {@code districtId} 对应的状态或依赖。 */ UUID districtId, /** 保存 {@code chunks} 对应的状态或依赖。 */ Set<PostalChunk> chunks, /** 保存 {@code revision} 对应的状态或依赖。 */ int revision) {
    /**
     * 定义 {@code MAX_CHUNKS} 常量。
     */
    public static final int MAX_CHUNKS = 4096;

    /**
     * 创建并初始化当前对象。
     */
    public DistrictCoverage {
        Objects.requireNonNull(districtId, "districtId");
        chunks = Set.copyOf(chunks);
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be at least 1");
        }
        if (!isValidShape(chunks)) {
            throw new IllegalArgumentException("district coverage must be non-empty, single-dimension, connected, and within its size limit");
        }
    }

    /**
     * 执行 {@code dimension} 对应的业务操作。
     */
    public ResourceLocation dimension() {
        return chunks.iterator().next().dimension();
    }

    /**
     * 判断 {@code contains} 对应的业务操作。
     */
    public boolean contains(PostalChunk chunk) {
        return chunks.contains(chunk);
    }

    /**
     * 执行 {@code withChunks} 对应的业务操作。
     */
    public DistrictCoverage withChunks(Set<PostalChunk> chunks) {
        return new DistrictCoverage(districtId, chunks, revision + 1);
    }

    /**
     * 判断 {@code isValidShape} 对应的业务操作。
     */
    public static boolean isValidShape(Set<PostalChunk> chunks) {
        if (chunks == null || chunks.isEmpty() || chunks.size() > MAX_CHUNKS) {
            return false;
        }
        ResourceLocation dimension = chunks.iterator().next().dimension();
        if (chunks.stream().anyMatch(chunk -> !chunk.dimension().equals(dimension))) {
            return false;
        }

        Set<PostalChunk> visited = new HashSet<>();
        ArrayDeque<PostalChunk> open = new ArrayDeque<>();
        open.add(chunks.iterator().next());
        while (!open.isEmpty()) {
            PostalChunk current = open.removeFirst();
            if (!visited.add(current)) {
                continue;
            }
            addIfPresent(chunks, visited, open, new PostalChunk(dimension, current.x() + 1, current.z()));
            addIfPresent(chunks, visited, open, new PostalChunk(dimension, current.x() - 1, current.z()));
            addIfPresent(chunks, visited, open, new PostalChunk(dimension, current.x(), current.z() + 1));
            addIfPresent(chunks, visited, open, new PostalChunk(dimension, current.x(), current.z() - 1));
        }
        return visited.size() == chunks.size();
    }

    /**
     * 添加或加入 {@code addIfPresent} 对应的业务操作。
     */
    private static void addIfPresent(
            Set<PostalChunk> chunks,
            Set<PostalChunk> visited,
            ArrayDeque<PostalChunk> open,
            PostalChunk candidate
    ) {
        if (chunks.contains(candidate) && !visited.contains(candidate)) {
            open.addLast(candidate);
        }
    }
}
