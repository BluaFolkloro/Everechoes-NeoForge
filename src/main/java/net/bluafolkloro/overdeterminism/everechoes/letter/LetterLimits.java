package net.bluafolkloro.overdeterminism.everechoes.letter;

// Hard limits for letter fields. Client widgets clamp to these values; the server
// clamps and validates again so oversized or malformed packets cannot bypass the UI.
// 信件字段硬限制。客户端控件会按这些值截断；服务端会再次截断并校验，避免绕过界面提交超长或畸形数据。
public final class LetterLimits {
    public static final int TITLE = 64;
    public static final int BODY = 4000;
    public static final int SIGNATURE = 32;
    public static final int LETTER_RECIPIENT = 32;
    // Long enough for a player UUID string (36) or a mailbox code.
    // 需要能放下玩家 UUID 字符串（36 字符）或邮箱邮编。
    public static final int ADDRESS_VALUE = 64;

    private LetterLimits() {
    }

    public static String sanitizeSingleLine(String value, int maxLength) {
        return clamp(stripSectionSigns(value).replace('\n', ' ').replace('\r', ' '), maxLength);
    }

    public static String sanitizeMultiline(String value, int maxLength) {
        return clamp(stripSectionSigns(value).replace('\r', '\n'), maxLength);
    }

    public static String clamp(String value, int maxLength) {
        String text = value == null ? "" : value;
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private static String stripSectionSigns(String value) {
        return value == null ? "" : value.replace("§", "");
    }
}
