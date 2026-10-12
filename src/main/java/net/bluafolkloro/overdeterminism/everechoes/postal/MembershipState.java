package net.bluafolkloro.overdeterminism.everechoes.postal;

/**
 * 封装一组受限且稳定的业务状态。
 */
public enum MembershipState {
    /**
     * 定义 {@code PENDING} 常量。
     */
    PENDING,
    /**
     * 定义 {@code ACTIVE} 常量。
     */
    ACTIVE,
    /**
     * 定义 {@code LEAVING} 常量。
     */
    LEAVING,
    /**
     * 定义 {@code DETACHED} 常量。
     */
    DETACHED
}
