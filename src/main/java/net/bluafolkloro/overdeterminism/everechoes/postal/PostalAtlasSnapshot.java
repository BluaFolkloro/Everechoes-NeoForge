package net.bluafolkloro.overdeterminism.everechoes.postal;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 封装不可变业务数据。
 */
public record PostalAtlasSnapshot(
        /**
         * 保存 {@code districtId} 对应的状态或依赖。
         */
        UUID districtId,
        /**
         * 保存 {@code dimension} 对应的状态或依赖。
         */
        ResourceLocation dimension,
        /**
         * 保存 {@code revision} 对应的状态或依赖。
         */
        int revision,
        /**
         * 保存 {@code domainCode} 对应的状态或依赖。
         */
        String domainCode,
        /**
         * 保存 {@code districtCode} 对应的状态或依赖。
         */
        String districtCode,
        /**
         * 保存 {@code originX} 对应的状态或依赖。
         */
        int originX,
        /**
         * 保存 {@code originZ} 对应的状态或依赖。
         */
        int originZ,
        /**
         * 保存 {@code width} 对应的状态或依赖。
         */
        int width,
        /**
         * 保存 {@code height} 对应的状态或依赖。
         */
        int height,
        /**
         * 保存 {@code ownedPacked} 对应的状态或依赖。
         */
        Set<Long> ownedPacked,
        /**
         * 保存 {@code foreignPacked} 对应的状态或依赖。
         */
        Set<Long> foreignPacked,
        /**
         * 保存 {@code hubPacked} 对应的状态或依赖。
         */
        Set<Long> hubPacked,
        /**
         * 保存 {@code collectionPacked} 对应的状态或依赖。
         */
        Set<Long> collectionPacked,
        /**
         * 保存 {@code savedCoveragePacked} 对应的状态或依赖。
         */
        Set<Long> savedCoveragePacked,
        /**
         * 保存 {@code nodePacked} 对应的状态或依赖。
         */
        Set<Long> nodePacked,
        /**
         * 保存 {@code savedCoverageSize} 对应的状态或依赖。
         */
        int savedCoverageSize,
        /**
         * 保存 {@code maxChunks} 对应的状态或依赖。
         */
        int maxChunks
) {
    /**
     * 创建并初始化当前对象。
     */
    public PostalAtlasSnapshot {
        Objects.requireNonNull(districtId, "districtId");
        Objects.requireNonNull(dimension, "dimension");
        domainCode = domainCode == null ? "" : domainCode;
        districtCode = districtCode == null ? "" : districtCode;
        ownedPacked = Set.copyOf(ownedPacked);
        foreignPacked = Set.copyOf(foreignPacked);
        hubPacked = Set.copyOf(hubPacked);
        collectionPacked = Set.copyOf(collectionPacked);
        savedCoveragePacked = Set.copyOf(savedCoveragePacked);
        nodePacked = Set.copyOf(nodePacked);
    }
}
