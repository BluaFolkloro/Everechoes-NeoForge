package net.bluafolkloro.overdeterminism.everechoes.postal;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Canonical AAAXX-YYY postal codes: domain letters, district 1-99, delivery 1-FFF hex.
// 规范邮编 AAAXX-YYY：邮域字母、邮区 1-99、递送点 1-FFF 的十六进制。
public final class PostalCodes {
    public static final int DOMAIN_MAX = 3;
    public static final int DISTRICT_MAX = 99;
    public static final int DELIVERY_MAX = 0xFFF;

    private static final Pattern ADDRESS = Pattern.compile("^([A-Za-z]{1,3})([1-9][0-9]?)-([0-9A-Fa-f]{1,3})$");
    private static final Pattern DOMAIN = Pattern.compile("^[A-Za-z]{1,3}$");
    private static final Pattern DISTRICT = Pattern.compile("^[1-9][0-9]?$");
    private static final Pattern DELIVERY = Pattern.compile("^[0-9A-Fa-f]{1,3}$");

    private PostalCodes() {
    }

    public static Optional<String> canonicalDomain(String raw) {
        if (raw == null || !DOMAIN.matcher(raw.strip()).matches()) {
            return Optional.empty();
        }
        return Optional.of(raw.strip().toUpperCase(Locale.ROOT));
    }

    public static Optional<String> canonicalDistrict(String raw) {
        if (raw == null || !DISTRICT.matcher(raw.strip()).matches()) {
            return Optional.empty();
        }
        int value = Integer.parseInt(raw.strip());
        if (value < 1 || value > DISTRICT_MAX) {
            return Optional.empty();
        }
        return Optional.of(Integer.toString(value));
    }

    public static Optional<String> canonicalDelivery(String raw) {
        if (raw == null || !DELIVERY.matcher(raw.strip()).matches()) {
            return Optional.empty();
        }
        int value = Integer.parseInt(raw.strip(), 16);
        if (value < 1 || value > DELIVERY_MAX) {
            return Optional.empty();
        }
        return Optional.of(Integer.toHexString(value).toUpperCase(Locale.ROOT));
    }

    public static Optional<MailBoxAddress> parseAddress(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = ADDRESS.matcher(raw.strip());
        if (!matcher.matches()) {
            return Optional.empty();
        }

        Optional<String> domain = canonicalDomain(matcher.group(1));
        Optional<String> district = canonicalDistrict(matcher.group(2));
        Optional<String> delivery = canonicalDelivery(matcher.group(3));
        if (domain.isEmpty() || district.isEmpty() || delivery.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new MailBoxAddress(domain.get(), district.get(), delivery.get()));
    }

    public static String format(String domainId, String districtId, String deliveryId) {
        return domainId + districtId + "-" + deliveryId;
    }

    public static String formatDistrict(String domainId, String districtId) {
        return domainId + districtId;
    }
}
