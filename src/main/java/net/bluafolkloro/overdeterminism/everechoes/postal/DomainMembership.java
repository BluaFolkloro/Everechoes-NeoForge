package net.bluafolkloro.overdeterminism.everechoes.postal;

import java.util.Objects;
import java.util.UUID;

public record DomainMembership(
        UUID districtId,
        UUID domainId,
        MembershipState state,
        int revision,
        long joinedAt,
        String districtCode
) {
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

    public boolean isDeliveryEndpoint() {
        return state == MembershipState.ACTIVE;
    }

    public DomainMembership withState(MembershipState state) {
        return new DomainMembership(districtId, domainId, state, revision + 1, joinedAt, districtCode);
    }
}
