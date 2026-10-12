package net.bluafolkloro.overdeterminism.everechoes.postal;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

// Internal UUIDs are routing identity. Codes are a display snapshot and are not unique identity.
// 内部 UUID 才是路由身份。代码只是显示快照，不是唯一身份。
/**
 * 封装邮政地址值对象。
 */
public record MailBoxAddress(
        /**
         * 保存 {@code domainId} 对应的状态或依赖。
         */
        Optional<UUID> domainId,
        /**
         * 保存 {@code districtId} 对应的状态或依赖。
         */
        Optional<UUID> districtId,
        /**
         * 保存 {@code mailboxId} 对应的状态或依赖。
         */
        Optional<UUID> mailboxId,
        /**
         * 保存 {@code domainCode} 对应的状态或依赖。
         */
        String domainCode,
        /**
         * 保存 {@code districtCode} 对应的状态或依赖。
         */
        String districtCode,
        /**
         * 保存 {@code deliveryCode} 对应的状态或依赖。
         */
        String deliveryCode
) implements Address {
    /**
     * 创建并初始化当前对象。
     */
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

    /**
     * 执行 {@code display} 对应的业务操作。
     */
    public static MailBoxAddress display(String domainCode, String districtCode, String deliveryCode) {
        return new MailBoxAddress(Optional.empty(), Optional.empty(), Optional.empty(), domainCode, districtCode, deliveryCode);
    }

    /**
     * 执行 {@code parse} 对应的业务操作。
     */
    public static Optional<MailBoxAddress> parse(String raw) {
        return PostalCodes.parseAddress(raw);
    }

    /**
     * 获取或解析 {@code resolved} 对应的业务操作。
     */
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

    /**
     * 执行 {@code postalCode} 对应的业务操作。
     */
    public String postalCode() {
        return PostalCodes.format(domainCode, districtCode, deliveryCode);
    }

    /**
     * 格式化 {@code format} 对应的业务操作。
     */
    public String format() {
        return postalCode();
    }

    /**
     * 执行 {@code outwardCode} 对应的业务操作。
     */
    public String outwardCode() {
        return PostalCodes.formatOutward(domainCode, districtCode);
    }
}
