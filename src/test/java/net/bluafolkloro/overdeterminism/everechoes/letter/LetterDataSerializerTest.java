package net.bluafolkloro.overdeterminism.everechoes.letter;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.bluafolkloro.overdeterminism.everechoes.postal.MailBoxAddress;
import net.bluafolkloro.overdeterminism.everechoes.postal.PlayerAddress;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LetterDataSerializerTest {
    private static final MailBoxAddress RETURN_ADDRESS = MailBoxAddress.display("AB", "1", "1AA");
    private static final PlayerAddress PLAYER_ADDRESS = new PlayerAddress(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"));

    @Test
    void draftWithoutRecipientRoundTrips() {
        LetterData original = LetterData.createDraft(RETURN_ADDRESS)
                .withTitle("Draft title")
                .withBody("Draft body");

        assertEquals(original, roundTrip(original));
    }

    @Test
    void sealedLetterRoundTrips() {
        LetterData original = LetterData.createDraft(RETURN_ADDRESS)
                .withTitle("Sealed title")
                .withBody("Sealed body")
                .withSignatureSender("Heron")
                .withLetterRecipient("Traveler")
                .withRecipientAddress(PLAYER_ADDRESS)
                .seal();

        assertEquals(original, roundTrip(original));
    }

    @Test
    void openedLetterRoundTrips() {
        LetterData original = LetterData.createDraft(RETURN_ADDRESS)
                .withRecipientAddress(MailBoxAddress.display("CD", "2", "7QF"))
                .seal()
                .open();

        assertEquals(original, roundTrip(original));
    }

    @Test
    void unknownStateIsRejected() {
        JsonObject json = validDraftJson();
        json.addProperty("state", "archived");

        assertTrue(LetterDataSerializer.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
    }

    @Test
    void sealedLetterMissingRecipientIsRejected() {
        JsonObject json = validDraftJson();
        json.addProperty("state", "sealed");
        json.remove("recipientAddress");

        assertTrue(LetterDataSerializer.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
    }

    @Test
    void nullLetterIdIsRejected() {
        JsonObject json = validDraftJson();
        json.remove("letterId");

        assertTrue(LetterDataSerializer.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
    }

    @Test
    void malformedRecipientAddressIsRejected() {
        JsonObject json = validDraftJson();
        JsonObject recipient = new JsonObject();
        recipient.addProperty("type", "mailbox");
        recipient.addProperty("postalCode", "A1 1CI");
        json.add("recipientAddress", recipient);
        json.addProperty("state", "sealed");

        assertTrue(LetterDataSerializer.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
    }

    private static LetterData roundTrip(LetterData original) {
        var encoded = LetterDataSerializer.CODEC.encodeStart(JsonOps.INSTANCE, original);
        assertTrue(encoded.result().isPresent(), encoded.error().map(Object::toString).orElse("encode failed"));
        var decoded = LetterDataSerializer.CODEC.parse(JsonOps.INSTANCE, encoded.result().orElseThrow());
        assertTrue(decoded.result().isPresent(), decoded.error().map(Object::toString).orElse("decode failed"));
        return decoded.result().orElseThrow();
    }

    private static JsonObject validDraftJson() {
        JsonObject returnAddress = new JsonObject();
        returnAddress.addProperty("type", "mailbox");
        returnAddress.addProperty("domainCode", "AB");
        returnAddress.addProperty("districtCode", "1");
        returnAddress.addProperty("deliveryCode", "1AA");

        JsonObject json = new JsonObject();
        json.addProperty("letterId", UUID.randomUUID().toString());
        json.addProperty("state", "draft");
        json.add("returnAddress", returnAddress);
        json.addProperty("title", "Title");
        json.addProperty("body", "Body");
        return json;
    }
}
