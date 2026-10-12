package net.bluafolkloro.overdeterminism.everechoes.postal;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 封装相关行为的自动化回归测试。
 */
class WaybillSerializerTest {
    /**
     * 执行 {@code awaitingAndInTransitRoundTrip} 对应的业务操作。
     */
    @Test
    void awaitingAndInTransitRoundTrip() {
        Waybill awaiting = Waybill.awaitingCarrier();
        assertEquals(awaiting, roundTrip(awaiting));

        UUID carrier = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        Waybill inTransit = awaiting.takenBy(carrier);
        assertEquals(inTransit, roundTrip(inTransit));
        assertEquals(awaiting.shipmentId(), inTransit.depositedAtPostBox().shipmentId());
        assertEquals(Waybill.CustodyState.AWAITING_CARRIER, inTransit.depositedAtPostBox().state());
    }

    /**
     * 执行 {@code inTransitWithoutCarrierIsRejected} 对应的业务操作。
     */
    @Test
    void inTransitWithoutCarrierIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Waybill(
                UUID.randomUUID(),
                Waybill.CustodyState.IN_TRANSIT,
                java.util.Optional.empty()
        ));
    }

    /**
     * 执行 {@code unknownStateIsRejected} 对应的业务操作。
     */
    @Test
    void unknownStateIsRejected() {
        JsonObject json = new JsonObject();
        json.addProperty("shipmentId", UUID.randomUUID().toString());
        json.addProperty("state", "teleported");

        assertTrue(WaybillSerializer.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
    }

    /**
     * 执行 {@code roundTrip} 对应的业务操作。
     */
    private static Waybill roundTrip(Waybill waybill) {
        var encoded = WaybillSerializer.CODEC.encodeStart(JsonOps.INSTANCE, waybill);
        assertTrue(encoded.result().isPresent(), encoded.error().map(Object::toString).orElse("encode failed"));
        var decoded = WaybillSerializer.CODEC.parse(JsonOps.INSTANCE, encoded.result().orElseThrow());
        assertTrue(decoded.result().isPresent(), decoded.error().map(Object::toString).orElse("decode failed"));
        return decoded.result().orElseThrow();
    }
}
