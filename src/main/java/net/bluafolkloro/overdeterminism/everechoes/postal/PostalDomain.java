package net.bluafolkloro.overdeterminism.everechoes.postal;

import java.util.Objects;
import java.util.UUID;

public record PostalDomain(
        UUID domainId,
        String domainCode,
        DomainLifecycle lifecycle,
        int nextDistrict,
        String creationPolicyId,
        String membershipPolicyId,
        String exitPolicyId
) {
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

    public boolean hasOwner() {
        return false;
    }

    public PostalDomain withLifecycle(DomainLifecycle lifecycle) {
        return new PostalDomain(domainId, domainCode, lifecycle, nextDistrict, creationPolicyId, membershipPolicyId, exitPolicyId);
    }

    public PostalDomain withNextDistrict(int nextDistrict) {
        return new PostalDomain(domainId, domainCode, lifecycle, nextDistrict, creationPolicyId, membershipPolicyId, exitPolicyId);
    }
}
