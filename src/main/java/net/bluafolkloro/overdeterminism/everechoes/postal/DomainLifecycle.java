package net.bluafolkloro.overdeterminism.everechoes.postal;

/**
 * 封装一组受限且稳定的业务状态。
 */
public enum DomainLifecycle {
    /**
     * 定义 {@code ACTIVE} 常量。
     */
    ACTIVE,
    /**
     * 定义 {@code DORMANT} 常量。
     */
    DORMANT,
    /**
     * 定义 {@code HISTORICAL} 常量。
     */
    HISTORICAL
}
