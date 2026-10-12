package net.bluafolkloro.overdeterminism.everechoes.postal;

/**
 * 封装邮政业务的权限与准入策略。
 */
public final class OpenDomainPolicies {
    /**
     * 定义 {@code CREATION} 常量。
     */
    public static final DomainCreationPolicy CREATION = new OpenCreationPolicy();
    /**
     * 定义 {@code MEMBERSHIP} 常量。
     */
    public static final DomainMembershipPolicy MEMBERSHIP = new OpenMembershipPolicy();
    /**
     * 定义 {@code EXIT} 常量。
     */
    public static final DomainExitPolicy EXIT = new OpenExitPolicy();

    /**
     * 创建并初始化当前对象。
     */
    private OpenDomainPolicies() {
    }

    /**
     * 封装邮政业务的权限与准入策略。
     */
    private static final class OpenCreationPolicy implements DomainCreationPolicy {
        /**
         * 执行 {@code policyId} 对应的业务操作。
         */
        @Override
        public String policyId() {
            return PostalPolicies.OPEN;
        }

        /**
         * 判断 {@code canCreate} 对应的业务操作。
         */
        @Override
        public PolicyDecision canCreate(PostalNetwork network, DomainCreationRequest request) {
            if (PostalCodes.canonicalDomain(request.domainCode()).isEmpty()) {
                return PolicyDecision.deny("message.everechoes.post_box.invalid_domain");
            }
            if (network.findDomainByCode(request.domainCode()).isPresent()) {
                return PolicyDecision.deny("message.everechoes.post_box.domain_exists");
            }
            return PolicyDecision.allow();
        }
    }

    /**
     * 封装邮政业务的权限与准入策略。
     */
    private static final class OpenMembershipPolicy implements DomainMembershipPolicy {
        /**
         * 执行 {@code policyId} 对应的业务操作。
         */
        @Override
        public String policyId() {
            return PostalPolicies.OPEN;
        }

        /**
         * 判断 {@code canJoin} 对应的业务操作。
         */
        @Override
        public PolicyDecision canJoin(PostalNetwork network, DomainJoinRequest request) {
            return network.domain(request.domainId())
                    .filter(domain -> domain.lifecycle() != DomainLifecycle.HISTORICAL)
                    .map(domain -> PolicyDecision.allow())
                    .orElseGet(() -> PolicyDecision.deny("message.everechoes.post_box.unknown_domain"));
        }

        /**
         * 执行 {@code autoActivate} 对应的业务操作。
         */
        @Override
        public boolean autoActivate() {
            return true;
        }
    }

    /**
     * 封装邮政业务的权限与准入策略。
     */
    private static final class OpenExitPolicy implements DomainExitPolicy {
        /**
         * 执行 {@code policyId} 对应的业务操作。
         */
        @Override
        public String policyId() {
            return PostalPolicies.OPEN;
        }

        /**
         * 判断 {@code canRequestExit} 对应的业务操作。
         */
        @Override
        public PolicyDecision canRequestExit(PostalNetwork network, DomainMembership membership) {
            if (membership.state() != MembershipState.ACTIVE) {
                return PolicyDecision.deny("message.everechoes.post_box.leave_invalid");
            }
            return PolicyDecision.allow();
        }

        /**
         * 判断 {@code canCompleteExit} 对应的业务操作。
         */
        @Override
        public PolicyDecision canCompleteExit(PostalNetwork network, DomainMembership membership) {
            if (membership.state() != MembershipState.LEAVING) {
                return PolicyDecision.deny("message.everechoes.post_box.leave_invalid");
            }
            return PolicyDecision.allow();
        }
    }
}
