package net.bluafolkloro.overdeterminism.everechoes.postal;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

/**
 * 封装不可变业务数据。
 */
public record PostBoxNode(
        /**
         * 保存 {@code nodeId} 对应的状态或依赖。
         */
        UUID nodeId,
        /**
         * 保存 {@code districtId} 对应的状态或依赖。
         */
        @Nullable UUID districtId,
        /**
         * 保存 {@code role} 对应的状态或依赖。
         */
        NodeRole role,
        /**
         * 保存 {@code dimension} 对应的状态或依赖。
         */
        ResourceLocation dimension,
        /**
         * 保存 {@code position} 对应的状态或依赖。
         */
        BlockPos position,
        /**
         * 保存 {@code state} 对应的状态或依赖。
         */
        NodeState state
) {
    /**
     * 创建并初始化当前对象。
     */
    public PostBoxNode {
        Objects.requireNonNull(nodeId, "nodeId");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(state, "state");
    }

    /**
     * 执行 {@code withDistrict} 对应的业务操作。
     */
    public PostBoxNode withDistrict(UUID districtId, NodeRole role) {
        return new PostBoxNode(nodeId, districtId, role, dimension, position, state);
    }

    /**
     * 执行 {@code withoutDistrict} 对应的业务操作。
     */
    public PostBoxNode withoutDistrict() {
        return new PostBoxNode(nodeId, null, NodeRole.COLLECTION, dimension, position, state);
    }

    /**
     * 执行 {@code withRole} 对应的业务操作。
     */
    public PostBoxNode withRole(NodeRole role) {
        return new PostBoxNode(nodeId, districtId, role, dimension, position, state);
    }

    /**
     * 执行 {@code withPosition} 对应的业务操作。
     */
    public PostBoxNode withPosition(ResourceLocation dimension, BlockPos position) {
        return new PostBoxNode(nodeId, districtId, role, dimension, position, state);
    }
}
