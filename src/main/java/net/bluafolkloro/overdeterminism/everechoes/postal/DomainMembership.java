package net.bluafolkloro.overdeterminism.everechoes.postal;

import java.util.Objects;
import java.util.UUID;

/**
 * 封装不可变业务数据。
 */
public record DomainMembership(
        /**
         * 保存 {@code districtId} 对应的状态或依赖。
         */
        UUID districtId,
        /**
         * 保存 {@code domainId} 对应的状态或依赖。
         */
        UUID domainId,
        /**
         * 保存 {@code state} 对应的状态或依赖。
         */
        MembershipState state,
        /**
         * 保存 {@code revision} 对应的状态或依赖。
         */
        int revision,
        /**
         * 保存 {@code joinedAt} 对应的状态或依赖。
         */
        long joinedAt,
        /**
         * 保存 {@code districtCode} 对应的状态或依赖。
         */
        String districtCode
) {
    /**
     * 创建并初始化当前对象。
     */
    public DomainMembership {
        Objects.requireNonNull(districtId, "districtId");
        Objects.requireNonNull(domainId, "domainId");
        Objects.requireNonNull(state, "state");
        districtCode = PostalCodes.canonicalDistrict(Objects.requireNonNull(districtCode, "districtCode"))
                .orElseThrow(() -> new IllegalArgumentException("invalid districtCode"));
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be at least 1");
        }
    }

    /**
     * 判断 {@code isDeliveryEndpoint} 对应的业务操作。
     */
    public boolean isDeliveryEndpoint() {
        return state == MembershipState.ACTIVE;
    }

    /**
     * 执行 {@code withState} 对应的业务操作。
     */
    public DomainMembership withState(MembershipState state) {
        return new DomainMembership(districtId, domainId, state, revision + 1, joinedAt, districtCode);
    }
}
