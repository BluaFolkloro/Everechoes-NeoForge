package net.bluafolkloro.overdeterminism.everechoes.postal;

import java.util.Objects;
import java.util.UUID;

/**
 * 封装邮政地址值对象。
 */
public record PlayerAddress(/** 保存 {@code playerId} 对应的状态或依赖。 */ UUID playerId) implements Address {
    /**
     * 创建并初始化当前对象。
     */
    public PlayerAddress {
        Objects.requireNonNull(playerId, "playerId cannot be null");
    }
}
