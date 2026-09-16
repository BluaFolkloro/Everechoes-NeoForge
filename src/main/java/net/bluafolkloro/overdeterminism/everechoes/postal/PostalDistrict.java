package net.bluafolkloro.overdeterminism.everechoes.postal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record PostalDistrict(
        UUID districtId,
        List<UUID> nodeIds,
        int revision
) {
    public PostalDistrict {
        Objects.requireNonNull(districtId, "districtId");
        nodeIds = List.copyOf(nodeIds);
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be at least 1");
        }
    }

    public PostalDistrict withNodes(List<UUID> nodeIds) {
        return new PostalDistrict(districtId, nodeIds, revision + 1);
    }

    public PostalDistrict adding(UUID nodeId) {
        if (nodeIds.contains(nodeId)) {
            return this;
        }
        List<UUID> next = new ArrayList<>(nodeIds);
        next.add(nodeId);
        return withNodes(next);
    }

    public PostalDistrict removing(UUID nodeId) {
        if (!nodeIds.contains(nodeId)) {
            return this;
        }
        List<UUID> next = new ArrayList<>(nodeIds);
        next.remove(nodeId);
        return withNodes(next);
    }

    public boolean isEmpty() {
        return nodeIds.isEmpty();
    }
}
