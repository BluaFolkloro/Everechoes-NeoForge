package net.bluafolkloro.overdeterminism.everechoes.postal;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

public record PostBoxNode(
        UUID nodeId,
        @Nullable UUID districtId,
        NodeRole role,
        ResourceLocation dimension,
        BlockPos position,
        NodeState state
) {
    public PostBoxNode {
        Objects.requireNonNull(nodeId, "nodeId");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(state, "state");
    }

    public PostBoxNode withDistrict(UUID districtId, NodeRole role) {
        return new PostBoxNode(nodeId, districtId, role, dimension, position, state);
    }

    public PostBoxNode withoutDistrict() {
        return new PostBoxNode(nodeId, null, NodeRole.COLLECTION, dimension, position, state);
    }

    public PostBoxNode withRole(NodeRole role) {
        return new PostBoxNode(nodeId, districtId, role, dimension, position, state);
    }

    public PostBoxNode withPosition(ResourceLocation dimension, BlockPos position) {
        return new PostBoxNode(nodeId, districtId, role, dimension, position, state);
    }
}
