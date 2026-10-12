package net.bluafolkloro.overdeterminism.everechoes.postal;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

// Transport custody for a sealed letter. LetterData stays on the item; this only tracks who holds it.
// 封蜡信件的承运责任。正文仍在 LetterData 里；这里只记录当前由谁保管。
/**
 * 封装不可变业务数据。
 */
public record Waybill(/** 保存 {@code shipmentId} 对应的状态或依赖。 */ UUID shipmentId, /** 保存 {@code state} 对应的状态或依赖。 */ CustodyState state, /** 保存 {@code carrierId} 对应的状态或依赖。 */ Optional<UUID> carrierId) {
    /**
     * 创建并初始化当前对象。
     */
    public Waybill {
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(state, "state");
        carrierId = carrierId == null ? Optional.empty() : carrierId;
        if (state == CustodyState.IN_TRANSIT && carrierId.isEmpty()) {
            throw new IllegalArgumentException("In-transit waybill must have a carrier");
        }
    }

    /**
     * 执行 {@code awaitingCarrier} 对应的业务操作。
     */
    public static Waybill awaitingCarrier() {
        return new Waybill(UUID.randomUUID(), CustodyState.AWAITING_CARRIER, Optional.empty());
    }

    /**
     * 执行 {@code depositedAtPostBox} 对应的业务操作。
     */
    public Waybill depositedAtPostBox() {
        return new Waybill(shipmentId, CustodyState.AWAITING_CARRIER, Optional.empty());
    }

    /**
     * 执行 {@code takenBy} 对应的业务操作。
     */
    public Waybill takenBy(UUID playerId) {
        return new Waybill(shipmentId, CustodyState.IN_TRANSIT, Optional.of(playerId));
    }

    /**
     * 封装一组受限且稳定的业务状态。
     */
    public enum CustodyState {
        /**
         * 定义 {@code AWAITING_CARRIER} 常量。
         */
        AWAITING_CARRIER,
        /**
         * 定义 {@code IN_TRANSIT} 常量。
         */
        IN_TRANSIT
    }
}
