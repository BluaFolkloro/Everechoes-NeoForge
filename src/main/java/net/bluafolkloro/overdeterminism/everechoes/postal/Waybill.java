package net.bluafolkloro.overdeterminism.everechoes.postal;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

// Transport custody for a sealed letter. LetterData stays on the item; this only tracks who holds it.
// 封蜡信件的承运责任。正文仍在 LetterData 里；这里只记录当前由谁保管。
public record Waybill(UUID shipmentId, CustodyState state, Optional<UUID> carrierId) {
    public Waybill {
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(state, "state");
        carrierId = carrierId == null ? Optional.empty() : carrierId;
        if (state == CustodyState.IN_TRANSIT && carrierId.isEmpty()) {
            throw new IllegalArgumentException("In-transit waybill must have a carrier");
        }
    }

    public static Waybill awaitingCarrier() {
        return new Waybill(UUID.randomUUID(), CustodyState.AWAITING_CARRIER, Optional.empty());
    }

    public Waybill depositedAtPostBox() {
        return new Waybill(shipmentId, CustodyState.AWAITING_CARRIER, Optional.empty());
    }

    public Waybill takenBy(UUID playerId) {
        return new Waybill(shipmentId, CustodyState.IN_TRANSIT, Optional.of(playerId));
    }

    public enum CustodyState {
        AWAITING_CARRIER,
        IN_TRANSIT
    }
}
