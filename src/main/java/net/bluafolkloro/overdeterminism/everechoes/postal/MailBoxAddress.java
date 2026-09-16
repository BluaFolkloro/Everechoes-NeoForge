package net.bluafolkloro.overdeterminism.everechoes.postal;

import java.util.Objects;
import java.util.Optional;

public record MailBoxAddress(String domainId, String districtId, String deliveryId) implements Address {
    public MailBoxAddress {
        domainId = PostalCodes.canonicalDomain(Objects.requireNonNull(domainId, "domainId"))
                .orElseThrow(() -> new IllegalArgumentException("invalid domainId"));
        districtId = PostalCodes.canonicalDistrict(Objects.requireNonNull(districtId, "districtId"))
                .orElseThrow(() -> new IllegalArgumentException("invalid districtId"));
        deliveryId = PostalCodes.canonicalDelivery(Objects.requireNonNull(deliveryId, "deliveryId"))
                .orElseThrow(() -> new IllegalArgumentException("invalid deliveryId"));
    }

    public static Optional<MailBoxAddress> parse(String raw) {
        return PostalCodes.parseAddress(raw);
    }

    public String postalCode() {
        return PostalCodes.format(domainId, districtId, deliveryId);
    }

    public String format() {
        return postalCode();
    }
}
