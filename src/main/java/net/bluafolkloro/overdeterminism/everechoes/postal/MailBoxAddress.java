package net.bluafolkloro.overdeterminism.everechoes.postal;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

// Internal UUIDs are routing identity. Codes are a display snapshot and are not unique identity.
// 内部 UUID 才是路由身份。代码只是显示快照，不是唯一身份。
public record MailBoxAddress(
        Optional<UUID> domainId,
        Optional<UUID> districtId,
        Optional<UUID> mailboxId,
        String domainCode,
        String districtCode,
        String deliveryCode
) implements Address {
    public MailBoxAddress {
        domainId = domainId == null ? Optional.empty() : domainId;
        districtId = districtId == null ? Optional.empty() : districtId;
        mailboxId = mailboxId == null ? Optional.empty() : mailboxId;
        domainCode = PostalCodes.canonicalDomain(Objects.requireNonNull(domainCode, "domainCode"))
                .orElseThrow(() -> new IllegalArgumentException("invalid domainCode"));
        districtCode = PostalCodes.canonicalDistrict(Objects.requireNonNull(districtCode, "districtCode"))
                .orElseThrow(() -> new IllegalArgumentException("invalid districtCode"));
        deliveryCode = PostalCodes.canonicalDelivery(Objects.requireNonNull(deliveryCode, "deliveryCode"))
                .orElseThrow(() -> new IllegalArgumentException("invalid deliveryCode"));
    }

    public static MailBoxAddress display(String domainCode, String districtCode, String deliveryCode) {
        return new MailBoxAddress(Optional.empty(), Optional.empty(), Optional.empty(), domainCode, districtCode, deliveryCode);
    }

    public static Optional<MailBoxAddress> parse(String raw) {
        return PostalCodes.parseAddress(raw);
    }

    public MailBoxAddress resolved(UUID domainId, UUID districtId, UUID mailboxId) {
        return new MailBoxAddress(
                Optional.ofNullable(domainId),
                Optional.ofNullable(districtId),
                Optional.ofNullable(mailboxId),
                domainCode,
                districtCode,
                deliveryCode
        );
    }

    public String postalCode() {
        return PostalCodes.format(domainCode, districtCode, deliveryCode);
    }

    public String format() {
        return postalCode();
    }

    public String outwardCode() {
        return PostalCodes.formatOutward(domainCode, districtCode);
    }
}
