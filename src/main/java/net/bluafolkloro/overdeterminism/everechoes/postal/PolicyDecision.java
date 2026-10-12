package net.bluafolkloro.overdeterminism.everechoes.postal;

import javax.annotation.Nullable;

/**
 * 封装不可变业务数据。
 */
public record PolicyDecision(/** 保存 {@code allowed} 对应的状态或依赖。 */ boolean allowed, /** 保存 {@code reasonKey} 对应的状态或依赖。 */ @Nullable String reasonKey) {
    /**
     * 执行 {@code allow} 对应的业务操作。
     */
    public static PolicyDecision allow() {
        return new PolicyDecision(true, null);
    }

    /**
     * 执行 {@code deny} 对应的业务操作。
     */
    public static PolicyDecision deny(String reasonKey) {
        return new PolicyDecision(false, reasonKey);
    }
}
