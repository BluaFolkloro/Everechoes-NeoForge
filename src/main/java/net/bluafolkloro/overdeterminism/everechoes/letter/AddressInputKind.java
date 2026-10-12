package net.bluafolkloro.overdeterminism.everechoes.letter;

// Player-facing recipient input mode used by the letter editor and its network payload.
// 写信界面与对应网络包共用的收件地址输入方式。
/**
 * 封装一组受限且稳定的业务状态。
 */
public enum AddressInputKind {
    /**
     * 定义 {@code NONE} 常量。
     */
    NONE,
    /**
     * 定义 {@code MAILBOX} 常量。
     */
    MAILBOX,
    /**
     * 定义 {@code PLAYER} 常量。
     */
    PLAYER
}
