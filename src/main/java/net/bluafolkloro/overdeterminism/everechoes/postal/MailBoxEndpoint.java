package net.bluafolkloro.overdeterminism.everechoes.postal;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

/**
 * 信箱端点：身份是 {@code mailboxId}，绑定后持有区内投递码。
 */
public record MailBoxEndpoint(
        /**
         * 信箱身份，注册后不再改写。
         */
        UUID mailboxId,
        /**
         * 绑定的投递区；未绑定则为空。
         */
        @Nullable UUID districtId,
        /**
         * 区内投递码；未绑定则为空。
         */
        @Nullable String deliveryCode,
        /**
         * 所在维度。
         */
        ResourceLocation dimension,
        /**
         * 方块坐标。
         */
        BlockPos position,
        /**
         * 在线或离线。
         */
        MailBoxEndpointState state
) {
    /**
     * 校验必填字段；半绑定视为未绑定，投递码写入前规范化。
     */
    public MailBoxEndpoint {
        Objects.requireNonNull(mailboxId, "mailboxId");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(state, "state");
        if (districtId == null || deliveryCode == null) {
            districtId = null;
            deliveryCode = null;
        } else {
            deliveryCode = PostalCodes.canonicalDelivery(deliveryCode)
                    .orElseThrow(() -> new IllegalArgumentException("invalid deliveryCode"));
        }
    }

    /**
     * 已绑定到投递区且持有投递码。
     */
    public boolean isBound() {
        return districtId != null && deliveryCode != null;
    }

    /**
     * 仅改在线状态，保留绑定。
     */
    public MailBoxEndpoint withState(MailBoxEndpointState state) {
        return new MailBoxEndpoint(mailboxId, districtId, deliveryCode, dimension, position, state);
    }

    /**
     * 绑定到投递区并设为在线。
     */
    public MailBoxEndpoint withBind(UUID districtId, String deliveryCode) {
        return new MailBoxEndpoint(mailboxId, districtId, deliveryCode, dimension, position, MailBoxEndpointState.ONLINE);
    }

    /**
     * 解除绑定，保留坐标与在线状态。
     */
    public MailBoxEndpoint withoutBind() {
        return new MailBoxEndpoint(mailboxId, null, null, dimension, position, state);
    }
}
