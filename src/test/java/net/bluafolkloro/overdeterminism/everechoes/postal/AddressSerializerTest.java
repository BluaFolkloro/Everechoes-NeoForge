package net.bluafolkloro.overdeterminism.everechoes.postal;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddressSerializerTest {
    @Test
    void mailboxAddressRoundTripsDisplayAndIdentity() {
        UUID domainId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        UUID districtId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        MailBoxAddress address = MailBoxAddress.display("ev", "12", "7qf").resolved(domainId, districtId, null);
        Address decoded = roundTrip(address);

        MailBoxAddress mailbox = (MailBoxAddress) decoded;
        assertEquals(domainId, mailbox.domainId().orElseThrow());
        assertEquals(districtId, mailbox.districtId().orElseThrow());
        assertEquals("EV12 7QF", mailbox.postalCode());
    }

    @Test
    void mailboxPostalCodeFallbackParsesLegacyAndUk() {
        JsonObject legacy = new JsonObject();
        legacy.addProperty("type", "mailbox");
        legacy.addProperty("postalCode", "ab1-ff");
        MailBoxAddress legacyAddress = (MailBoxAddress) AddressSerializer.CODEC.parse(JsonOps.INSTANCE, legacy).result().orElseThrow();
        assertEquals("AB", legacyAddress.domainCode());
        assertEquals("1", legacyAddress.districtCode());

        JsonObject uk = new JsonObject();
        uk.addProperty("type", "mailbox");
        uk.addProperty("postalCode", "ev12 7qf");
        assertEquals("EV12 7QF", ((MailBoxAddress) AddressSerializer.CODEC.parse(JsonOps.INSTANCE, uk).result().orElseThrow()).postalCode());
    }

    @Test
    void playerAddressRoundTrips() {
        PlayerAddress address = new PlayerAddress(UUID.fromString("12345678-1234-1234-1234-123456789abc"));
        assertEquals(address, roundTrip(address));
    }

    @Test
    void unknownTypeIsRejected() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "carrier");
        assertTrue(AddressSerializer.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
    }

    @Test
    void mailboxWithoutPartsIsRejected() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "mailbox");
        assertTrue(AddressSerializer.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
    }

    @Test
    void playerWithoutPlayerIdIsRejected() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "player");
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
