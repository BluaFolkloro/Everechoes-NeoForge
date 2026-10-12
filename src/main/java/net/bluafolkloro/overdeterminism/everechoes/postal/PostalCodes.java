package net.bluafolkloro.overdeterminism.everechoes.postal;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Provisional UK-style outward/inward codes. Not a final address identity.
// 暂定英式外码/内码。不是最终的内部地址身份。
/**
 * 封装该模块的相关职责。
 */
public final class PostalCodes {
    /**
     * 定义 {@code DOMAIN_MAX} 常量。
     */
    public static final int DOMAIN_MAX = 2;
    /**
     * 定义 {@code DISTRICT_MAX} 常量。
     */
    public static final int DISTRICT_MAX = 99;
    /**
     * 定义 {@code INWARD_LETTERS} 常量。
     */
    public static final String INWARD_LETTERS = "ABDEFGHJLNPQRSTUWXYZ";

    /**
     * 定义 {@code UK_COMPACT} 常量。
     */
    private static final Pattern UK_COMPACT = Pattern.compile(
            "^([A-Za-z]{1,2})([1-9][0-9]?[A-Za-z]?)([0-9])([A-Za-z]{2})$"
    );
    /**
     * 定义 {@code DOMAIN} 常量。
     */
    private static final Pattern DOMAIN = Pattern.compile("^[A-Za-z]{1,2}$");
    /**
     * 定义 {@code DISTRICT} 常量。
     */
    private static final Pattern DISTRICT = Pattern.compile("^[1-9][0-9]?[A-Za-z]?$");
    /**
     * 定义 {@code AUTO_DISTRICT} 常量。
     */
    private static final Pattern AUTO_DISTRICT = Pattern.compile("^[1-9][0-9]?$");

    /**
     * 创建并初始化当前对象。
     */
    private PostalCodes() {
    }

    /**
     * 判断 {@code canonicalDomain} 对应的业务操作。
     */
    public static Optional<String> canonicalDomain(String raw) {
        if (raw == null || !DOMAIN.matcher(raw.strip()).matches()) {
            return Optional.empty();
        }
        return Optional.of(raw.strip().toUpperCase(Locale.ROOT));
    }

    /**
     * 判断 {@code canonicalDistrict} 对应的业务操作。
     */
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

    /**
     * 判断 {@code canonicalDelivery} 对应的业务操作。
     */
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

    /**
     * 执行 {@code parse} 对应的业务操作。
     */
    public static Optional<ParsedPostalCode> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }

        String compact = raw.strip().toUpperCase(Locale.ROOT).replace("-", "").replace(" ", "");
        Matcher uk = UK_COMPACT.matcher(compact);
        if (uk.matches()) {
            return codes(uk.group(1), uk.group(2), uk.group(3) + uk.group(4));
        }

        return Optional.empty();
    }

    /**
     * 执行 {@code parseAddress} 对应的业务操作。
     */
    public static Optional<MailBoxAddress> parseAddress(String raw) {
        return parse(raw).map(parsed -> MailBoxAddress.display(
                parsed.domainCode(),
                parsed.districtCode(),
                parsed.deliveryCode()
        ));
    }

    /**
     * 格式化 {@code format} 对应的业务操作。
     */
    public static String format(String domainCode, String districtCode, String deliveryCode) {
        return domainCode + districtCode + " " + deliveryCode;
    }

    /**
     * 格式化 {@code formatOutward} 对应的业务操作。
     */
    public static String formatOutward(String domainCode, String districtCode) {
        return domainCode + districtCode;
    }

    /**
     * 执行 {@code nextNumericDistrict} 对应的业务操作。
     */
    public static String nextNumericDistrict(int nextDistrict) {
        if (nextDistrict < 1 || nextDistrict > DISTRICT_MAX) {
            throw new IllegalArgumentException("district allocator exhausted");
        }
        return Integer.toString(nextDistrict);
    }

    /**
     * 按序号给出区内投递码：数字 0-9，后接 {@link #INWARD_LETTERS} 的两个字母。
     * 序号 0 为 {@code 0AA}，20 为 {@code 0BA}，400 为 {@code 1AA}；越界则空。
     */
    public static Optional<String> deliveryAt(int index) {
        int radix = INWARD_LETTERS.length();
        int capacity = deliveryCapacity();
        if (index < 0 || index >= capacity) {
            return Optional.empty();
        }
        int digit = index / (radix * radix);
        int remainder = index % (radix * radix);
        int first = remainder / radix;
        int second = remainder % radix;
        return Optional.of("" + digit + INWARD_LETTERS.charAt(first) + INWARD_LETTERS.charAt(second));
    }

    /**
     * 区内可分配投递码容量：10 × 20 × 20 = 4000。
     */
    public static int deliveryCapacity() {
        int radix = INWARD_LETTERS.length();
        return 10 * radix * radix;
    }

    /**
     * 执行 {@code codes} 对应的业务操作。
     */
    private static Optional<ParsedPostalCode> codes(String domain, String district, String delivery) {
        Optional<String> domainCode = canonicalDomain(domain);
        Optional<String> districtCode = canonicalDistrict(district);
        Optional<String> deliveryCode = canonicalDelivery(delivery);
        if (domainCode.isEmpty() || districtCode.isEmpty() || deliveryCode.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ParsedPostalCode(domainCode.get(), districtCode.get(), deliveryCode.get()));
    }

    /**
     * 封装不可变业务数据。
     */
    public record ParsedPostalCode(/** 保存 {@code domainCode} 对应的状态或依赖。 */ String domainCode, /** 保存 {@code districtCode} 对应的状态或依赖。 */ String districtCode, /** 保存 {@code deliveryCode} 对应的状态或依赖。 */ String deliveryCode) {
    }
}
