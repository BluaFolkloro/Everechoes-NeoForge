package net.bluafolkloro.overdeterminism.everechoes.postal;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record PostalAtlasSnapshot(
        UUID districtId,
        ResourceLocation dimension,
        int revision,
        String domainCode,
        String districtCode,
        int originX,
        int originZ,
        int width,
        int height,
        Set<Long> ownedPacked,
        Set<Long> foreignPacked,
        Set<Long> hubPacked,
        Set<Long> collectionPacked,
        Set<Long> savedCoveragePacked,
        Set<Long> nodePacked,
        int savedCoverageSize,
        int maxChunks
) {
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
