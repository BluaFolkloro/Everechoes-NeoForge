package net.bluafolkloro.overdeterminism.everechoes.postal;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 封装相关行为的自动化回归测试。
 */
class AddressSerializerTest {
    /**
     * 执行 {@code mailboxAddressRoundTripsDisplayAndIdentity} 对应的业务操作。
     */
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

    /**
     * 执行 {@code mailboxPostalCodeFallbackRejectsLegacyAndParsesUk} 对应的业务操作。
     */
    @Test
    void mailboxPostalCodeFallbackRejectsLegacyAndParsesUk() {
        JsonObject legacy = new JsonObject();
        legacy.addProperty("type", "mailbox");
        legacy.addProperty("postalCode", "ab1-ff");
        assertTrue(AddressSerializer.CODEC.parse(JsonOps.INSTANCE, legacy).error().isPresent());

        JsonObject uk = new JsonObject();
        uk.addProperty("type", "mailbox");
        uk.addProperty("postalCode", "ev12 7qf");
        assertEquals("EV12 7QF", ((MailBoxAddress) AddressSerializer.CODEC.parse(JsonOps.INSTANCE, uk).result().orElseThrow()).postalCode());
    }

    /**
     * 执行 {@code playerAddressRoundTrips} 对应的业务操作。
     */
    @Test
    void playerAddressRoundTrips() {
        PlayerAddress address = new PlayerAddress(UUID.fromString("12345678-1234-1234-1234-123456789abc"));
        assertEquals(address, roundTrip(address));
    }

    /**
     * 执行 {@code unknownTypeIsRejected} 对应的业务操作。
     */
    @Test
    void unknownTypeIsRejected() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "carrier");
        assertTrue(AddressSerializer.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
    }

    /**
     * 执行 {@code mailboxWithoutPartsIsRejected} 对应的业务操作。
     */
    @Test
    void mailboxWithoutPartsIsRejected() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "mailbox");
        assertTrue(AddressSerializer.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
    }

    /**
     * 执行 {@code playerWithoutPlayerIdIsRejected} 对应的业务操作。
     */
    @Test
    void playerWithoutPlayerIdIsRejected() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "player");
        assertTrue(AddressSerializer.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
    }

    /**
     * 执行 {@code roundTrip} 对应的业务操作。
     */
    private static Address roundTrip(Address address) {
        var encoded = AddressSerializer.CODEC.encodeStart(JsonOps.INSTANCE, address);
        assertTrue(encoded.result().isPresent(), encoded.error().map(Object::toString).orElse("encode failed"));
        var decoded = AddressSerializer.CODEC.parse(JsonOps.INSTANCE, encoded.result().orElseThrow());
        assertTrue(decoded.result().isPresent(), decoded.error().map(Object::toString).orElse("decode failed"));
        return decoded.result().orElseThrow();
    }
}
