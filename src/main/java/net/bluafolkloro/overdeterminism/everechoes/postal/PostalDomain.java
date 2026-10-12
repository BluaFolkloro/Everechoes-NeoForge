package net.bluafolkloro.overdeterminism.everechoes.postal;

import java.util.Objects;
import java.util.UUID;

/**
 * 封装不可变业务数据。
 */
public record PostalDomain(
        /**
         * 保存 {@code domainId} 对应的状态或依赖。
         */
        UUID domainId,
        /**
         * 保存 {@code domainCode} 对应的状态或依赖。
         */
        String domainCode,
        /**
         * 保存 {@code lifecycle} 对应的状态或依赖。
         */
        DomainLifecycle lifecycle,
        /**
         * 保存 {@code nextDistrict} 对应的状态或依赖。
         */
        int nextDistrict,
        /**
         * 保存 {@code creationPolicyId} 对应的状态或依赖。
         */
        String creationPolicyId,
        /**
         * 保存 {@code membershipPolicyId} 对应的状态或依赖。
         */
        String membershipPolicyId,
        /**
         * 保存 {@code exitPolicyId} 对应的状态或依赖。
         */
        String exitPolicyId
) {
    /**
     * 创建并初始化当前对象。
     */
    public PostalDomain {
        Objects.requireNonNull(domainId, "domainId");
        domainCode = PostalCodes.canonicalDomain(Objects.requireNonNull(domainCode, "domainCode"))
                .orElseThrow(() -> new IllegalArgumentException("invalid domainCode"));
        Objects.requireNonNull(lifecycle, "lifecycle");
        Objects.requireNonNull(creationPolicyId, "creationPolicyId");
        Objects.requireNonNull(membershipPolicyId, "membershipPolicyId");
        Objects.requireNonNull(exitPolicyId, "exitPolicyId");
        if (nextDistrict < 1) {
            throw new IllegalArgumentException("nextDistrict must be at least 1");
        }
    }

    /**
     * 判断 {@code hasOwner} 对应的业务操作。
     */
    public boolean hasOwner() {
        return false;
    }

    /**
     * 执行 {@code withLifecycle} 对应的业务操作。
     */
    public PostalDomain withLifecycle(DomainLifecycle lifecycle) {
        return new PostalDomain(domainId, domainCode, lifecycle, nextDistrict, creationPolicyId, membershipPolicyId, exitPolicyId);
    }

    /**
     * 执行 {@code withNextDistrict} 对应的业务操作。
     */
    public PostalDomain withNextDistrict(int nextDistrict) {
        return new PostalDomain(domainId, domainCode, lifecycle, nextDistrict, creationPolicyId, membershipPolicyId, exitPolicyId);
    }
}
