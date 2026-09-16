package net.bluafolkloro.overdeterminism.everechoes.postal;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Provisional UK-style outward/inward codes. Not a final address identity.
// 暂定英式外码/内码。不是最终的内部地址身份。
public final class PostalCodes {
    public static final int DOMAIN_MAX = 2;
    public static final int DISTRICT_MAX = 99;
    public static final int NEARBY_NODE_RANGE = 128;
    public static final String INWARD_LETTERS = "ABDEFGHJLNPQRSTUWXYZ";

    private static final Pattern UK_COMPACT = Pattern.compile(
            "^([A-Za-z]{1,2})([1-9][0-9]?[A-Za-z]?)([0-9])([A-Za-z]{2})$"
    );
    private static final Pattern LEGACY_FULL = Pattern.compile("^([A-Za-z]{1,3})([1-9][0-9]?)-([0-9A-Fa-f]{1,3})$");
    private static final Pattern DOMAIN = Pattern.compile("^[A-Za-z]{1,2}$");
    private static final Pattern DISTRICT = Pattern.compile("^[1-9][0-9]?[A-Za-z]?$");
    private static final Pattern AUTO_DISTRICT = Pattern.compile("^[1-9][0-9]?$");

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
        String text = raw.strip().toUpperCase(Locale.ROOT);
        if (AUTO_DISTRICT.matcher(text).matches()) {
            int value = Integer.parseInt(text);
            if (value < 1 || value > DISTRICT_MAX) {
                return Optional.empty();
            }
            return Optional.of(Integer.toString(value));
        }
        return Optional.of(text);
    }

    public static Optional<String> canonicalDelivery(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String text = raw.strip().toUpperCase(Locale.ROOT);
        if (text.length() != 3 || !Character.isDigit(text.charAt(0))) {
            return Optional.empty();
        }
        if (INWARD_LETTERS.indexOf(text.charAt(1)) < 0 || INWARD_LETTERS.indexOf(text.charAt(2)) < 0) {
            return Optional.empty();
        }
        return Optional.of(text);
    }

    public static Optional<ParsedPostalCode> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }

        String compact = raw.strip().toUpperCase(Locale.ROOT).replace("-", "").replace(" ", "");
        Matcher uk = UK_COMPACT.matcher(compact);
        if (uk.matches()) {
            return codes(uk.group(1), uk.group(2), uk.group(3) + uk.group(4));
        }

        Matcher legacy = LEGACY_FULL.matcher(raw.strip());
        if (legacy.matches()) {
            String domainRaw = legacy.group(1);
            if (domainRaw.length() > DOMAIN_MAX) {
                domainRaw = domainRaw.substring(0, DOMAIN_MAX);
            }
            Optional<String> domain = canonicalDomain(domainRaw);
            Optional<String> district = canonicalDistrict(legacy.group(2));
            if (domain.isPresent() && district.isPresent()) {
                return Optional.of(new ParsedPostalCode(domain.get(), district.get(), "1AA"));
            }
        }
        return Optional.empty();
    }

    public static Optional<MailBoxAddress> parseAddress(String raw) {
        return parse(raw).map(parsed -> MailBoxAddress.display(
                parsed.domainCode(),
                parsed.districtCode(),
                parsed.deliveryCode()
        ));
    }

    public static String format(String domainCode, String districtCode, String deliveryCode) {
        return domainCode + districtCode + " " + deliveryCode;
    }

    public static String formatOutward(String domainCode, String districtCode) {
        return domainCode + districtCode;
    }

    public static String nextNumericDistrict(int nextDistrict) {
        if (nextDistrict < 1 || nextDistrict > DISTRICT_MAX) {
            throw new IllegalArgumentException("district allocator exhausted");
        }
        return Integer.toString(nextDistrict);
    }

    private static Optional<ParsedPostalCode> codes(String domain, String district, String delivery) {
        Optional<String> domainCode = canonicalDomain(domain);
        Optional<String> districtCode = canonicalDistrict(district);
        Optional<String> deliveryCode = canonicalDelivery(delivery);
        if (domainCode.isEmpty() || districtCode.isEmpty() || deliveryCode.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ParsedPostalCode(domainCode.get(), districtCode.get(), deliveryCode.get()));
    }

    public record ParsedPostalCode(String domainCode, String districtCode, String deliveryCode) {
    }
}
