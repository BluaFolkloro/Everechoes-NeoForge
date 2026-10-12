package net.bluafolkloro.overdeterminism.everechoes.postal;

import java.util.UUID;

/**
 * 封装不可变业务数据。
 */
public record NodeJoinRequest(/** 保存 {@code nodeId} 对应的状态或依赖。 */ UUID nodeId, /** 保存 {@code districtId} 对应的状态或依赖。 */ UUID districtId, /** 保存 {@code context} 对应的状态或依赖。 */ PostalActionContext context, /** 保存 {@code explicit} 对应的状态或依赖。 */ boolean explicit) {
}
