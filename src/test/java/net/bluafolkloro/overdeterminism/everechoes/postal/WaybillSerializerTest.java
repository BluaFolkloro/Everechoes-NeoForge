package net.bluafolkloro.overdeterminism.everechoes.postal;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaybillSerializerTest {
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

    @Test
    void inTransitWithoutCarrierIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Waybill(
                UUID.randomUUID(),
                Waybill.CustodyState.IN_TRANSIT,
                java.util.Optional.empty()
        ));
    }

    @Test
    void unknownStateIsRejected() {
        JsonObject json = new JsonObject();
        json.addProperty("shipmentId", UUID.randomUUID().toString());
        json.addProperty("state", "teleported");

        assertTrue(WaybillSerializer.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
    }

    private static Waybill roundTrip(Waybill waybill) {
        var encoded = WaybillSerializer.CODEC.encodeStart(JsonOps.INSTANCE, waybill);
        assertTrue(encoded.result().isPresent(), encoded.error().map(Object::toString).orElse("encode failed"));
        var decoded = WaybillSerializer.CODEC.parse(JsonOps.INSTANCE, encoded.result().orElseThrow());
        assertTrue(decoded.result().isPresent(), decoded.error().map(Object::toString).orElse("decode failed"));
        return decoded.result().orElseThrow();
    }
}
