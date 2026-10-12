package net.bluafolkloro.overdeterminism.everechoes.letter;

// Hard limits for letter fields. Client widgets clamp to these values; the server
// clamps and validates again so oversized or malformed packets cannot bypass the UI.
// 信件字段硬限制。客户端控件会按这些值截断；服务端会再次截断并校验，避免绕过界面提交超长或畸形数据。
/**
 * 封装业务边界与容量限制。
 */
public final class LetterLimits {
    /**
     * 定义 {@code TITLE} 常量。
     */
    public static final int TITLE = 64;
    /**
     * 定义 {@code BODY} 常量。
     */
    public static final int BODY = 4000;
    /**
     * 定义 {@code SIGNATURE} 常量。
     */
    public static final int SIGNATURE = 32;
    /**
     * 定义 {@code LETTER_RECIPIENT} 常量。
     */
    public static final int LETTER_RECIPIENT = 32;
    // Long enough for a player UUID string (36) or a mailbox code.
    // 需要能放下玩家 UUID 字符串（36 字符）或邮箱邮编。
    /**
     * 定义 {@code ADDRESS_VALUE} 常量。
     */
    public static final int ADDRESS_VALUE = 64;

    /**
     * 创建并初始化当前对象。
     */
    private LetterLimits() {
    }

    /**
     * 执行 {@code sanitizeSingleLine} 对应的业务操作。
     */
    public static String sanitizeSingleLine(String value, int maxLength) {
        return clamp(stripSectionSigns(value).replace('\n', ' ').replace('\r', ' '), maxLength);
    }

    /**
     * 执行 {@code sanitizeMultiline} 对应的业务操作。
     */
    public static String sanitizeMultiline(String value, int maxLength) {
        return clamp(stripSectionSigns(value).replace('\r', '\n'), maxLength);
    }

    /**
     * 执行 {@code clamp} 对应的业务操作。
     */
    public static String clamp(String value, int maxLength) {
        String text = value == null ? "" : value;
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    /**
     * 执行 {@code stripSectionSigns} 对应的业务操作。
     */
    private static String stripSectionSigns(String value) {
        return value == null ? "" : value.replace("§", "");
    }
}
