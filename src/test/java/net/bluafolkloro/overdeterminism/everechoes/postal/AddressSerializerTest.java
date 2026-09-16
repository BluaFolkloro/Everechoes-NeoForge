package net.bluafolkloro.overdeterminism.everechoes.postal;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddressSerializerTest {
    @Test
    void mailboxAddressRoundTrips() {
        MailBoxAddress address = new MailBoxAddress("  district-7  ");
        Address decoded = roundTrip(address);

        assertEquals(new MailBoxAddress("district-7"), decoded);
    }

    @Test
    void playerAddressRoundTrips() {
        PlayerAddress address = new PlayerAddress(UUID.fromString("12345678-1234-1234-1234-123456789abc"));
        Address decoded = roundTrip(address);

        assertEquals(address, decoded);
    }

    @Test
    void unknownTypeIsRejected() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "carrier");

        assertTrue(AddressSerializer.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
    }

    @Test
    void mailboxWithoutPostalCodeIsRejected() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "mailbox");

        assertTrue(AddressSerializer.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
    }

    @Test
    void mailboxWithBlankPostalCodeIsRejected() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "mailbox");
        json.addProperty("postalCode", "   ");

        assertTrue(AddressSerializer.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
    }

    @Test
    void playerWithoutPlayerIdIsRejected() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "player");

        assertTrue(AddressSerializer.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
    }

    @Test
    void playerWithMalformedUuidIsRejected() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "player");
        json.addProperty("playerId", "not-a-uuid");

        assertTrue(AddressSerializer.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
    }

    private static Address roundTrip(Address address) {
        var encoded = AddressSerializer.CODEC.encodeStart(JsonOps.INSTANCE, address);
        assertTrue(encoded.result().isPresent(), encoded.error().map(Object::toString).orElse("encode failed"));
        var decoded = AddressSerializer.CODEC.parse(JsonOps.INSTANCE, encoded.result().orElseThrow());
        assertTrue(decoded.result().isPresent(), decoded.error().map(Object::toString).orElse("decode failed"));
        return decoded.result().orElseThrow();
    }
}
