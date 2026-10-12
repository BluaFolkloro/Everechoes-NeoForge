package net.bluafolkloro.overdeterminism.everechoes.postal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 封装不可变业务数据。
 */
public record PostalDistrict(
        /**
         * 保存 {@code districtId} 对应的状态或依赖。
         */
        UUID districtId,
        /**
         * 保存 {@code nodeIds} 对应的状态或依赖。
         */
        List<UUID> nodeIds,
        /**
         * 保存 {@code revision} 对应的状态或依赖。
         */
        int revision
) {
    /**
     * 创建并初始化当前对象。
     */
    public PostalDistrict {
        Objects.requireNonNull(districtId, "districtId");
        nodeIds = List.copyOf(nodeIds);
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be at least 1");
        }
    }

    /**
     * 执行 {@code withNodes} 对应的业务操作。
     */
    public PostalDistrict withNodes(List<UUID> nodeIds) {
        return new PostalDistrict(districtId, nodeIds, revision + 1);
    }

    /**
     * 添加或加入 {@code adding} 对应的业务操作。
     */
    public PostalDistrict adding(UUID nodeId) {
        if (nodeIds.contains(nodeId)) {
            return this;
        }
        List<UUID> next = new ArrayList<>(nodeIds);
        next.add(nodeId);
        return withNodes(next);
    }

    /**
     * 执行 {@code removing} 对应的业务操作。
     */
    public PostalDistrict removing(UUID nodeId) {
        if (!nodeIds.contains(nodeId)) {
            return this;
        }
        List<UUID> next = new ArrayList<>(nodeIds);
        next.remove(nodeId);
        return withNodes(next);
    }

    /**
     * 判断 {@code isEmpty} 对应的业务操作。
     */
    public boolean isEmpty() {
        return nodeIds.isEmpty();
    }
}
