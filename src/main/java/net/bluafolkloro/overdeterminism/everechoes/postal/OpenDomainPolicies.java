package net.bluafolkloro.overdeterminism.everechoes.postal;

public final class OpenDomainPolicies {
    public static final DomainCreationPolicy CREATION = OpenDomainPolicies::canCreate;
    public static final DomainMembershipPolicy MEMBERSHIP = new OpenMembershipPolicy();
    public static final DomainExitPolicy EXIT = new OpenExitPolicy();

    private OpenDomainPolicies() {
    }

    private static PolicyDecision canCreate(PostalNetwork network, DomainCreationRequest request) {
        if (PostalCodes.canonicalDomain(request.domainCode()).isEmpty()) {
            return PolicyDecision.deny("message.everechoes.post_box.invalid_domain");
        }
        if (network.findLiveDomainByCode(request.domainCode()).isPresent()) {
            return PolicyDecision.deny("message.everechoes.post_box.domain_exists");
        }
        return PolicyDecision.allow();
    }

    private static final class OpenMembershipPolicy implements DomainMembershipPolicy {
        @Override
        public PolicyDecision canJoin(PostalNetwork network, DomainJoinRequest request) {
            return network.domain(request.domainId())
                    .filter(domain -> domain.lifecycle() != DomainLifecycle.HISTORICAL)
                    .map(domain -> PolicyDecision.allow())
                    .orElseGet(() -> PolicyDecision.deny("message.everechoes.post_box.unknown_domain"));
        }

        @Override
        public boolean autoActivate() {
            return true;
        }
    }

    private static final class OpenExitPolicy implements DomainExitPolicy {
        @Override
        public PolicyDecision canRequestExit(PostalNetwork network, DomainMembership membership) {
            if (membership.state() != MembershipState.ACTIVE) {
                return PolicyDecision.deny("message.everechoes.post_box.leave_invalid");
            }
            return PolicyDecision.allow();
        }

        @Override
        public PolicyDecision canCompleteExit(PostalNetwork network, DomainMembership membership) {
            if (membership.state() != MembershipState.LEAVING) {
                return PolicyDecision.deny("message.everechoes.post_box.leave_invalid");
            }
            return PolicyDecision.allow();
        }
    }
}
