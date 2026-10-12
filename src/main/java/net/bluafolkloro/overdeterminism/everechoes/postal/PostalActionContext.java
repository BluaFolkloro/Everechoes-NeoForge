package net.bluafolkloro.overdeterminism.everechoes.postal;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 封装不可变业务数据。
 */
public record PostalActionContext(
        /**
         * 保存 {@code sourceDistrictId} 对应的状态或依赖。
         */
        @Nullable UUID sourceDistrictId,
        /**
         * 保存 {@code sourceNodeId} 对应的状态或依赖。
         */
        @Nullable UUID sourceNodeId,
        /**
         * 保存 {@code actorId} 对应的状态或依赖。
         */
        @Nullable UUID actorId,
        /**
         * 保存 {@code dimension} 对应的状态或依赖。
         */
        @Nullable ResourceLocation dimension,
        /**
         * 保存 {@code position} 对应的状态或依赖。
         */
        @Nullable BlockPos position
) {
    /**
     * 执行 {@code empty} 对应的业务操作。
     */
    public static PostalActionContext empty() {
        return new PostalActionContext(null, null, null, null, null);
    }
}
