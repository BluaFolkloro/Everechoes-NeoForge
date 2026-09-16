package net.bluafolkloro.overdeterminism.everechoes.letter;

import net.bluafolkloro.overdeterminism.everechoes.postal.MailBoxAddress;
import net.bluafolkloro.overdeterminism.everechoes.postal.PlayerAddress;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LetterDataTest {
    private static final MailBoxAddress RETURN_ADDRESS = new MailBoxAddress("return-1");
    private static final MailBoxAddress RECIPIENT_ADDRESS = new MailBoxAddress("box-1");

    @Test
    void draftCanBeEditedThenSealedThenOpened() {
        LetterData draft = LetterData.createDraft(RETURN_ADDRESS)
                .withTitle("Hello")
                .withBody("Body")
                .withSignatureSender("Sender")
                .withLetterRecipient("Friend")
                .withRecipientAddress(RECIPIENT_ADDRESS);

        LetterData sealed = draft.seal();
        LetterData opened = sealed.open();

        assertEquals(draft.letterId(), sealed.letterId());
        assertEquals(draft.letterId(), opened.letterId());
        assertEquals(LetterState.SEALED, sealed.state());
        assertEquals(LetterState.OPENED, opened.state());
        assertEquals("Hello", opened.title());
        assertEquals("Body", opened.body());
        assertEquals("Sender", opened.signatureSender().orElseThrow());
        assertEquals("Friend", opened.letterRecipient().orElseThrow());
        assertEquals(RECIPIENT_ADDRESS, opened.requireRecipientAddress());
    }

    @Test
    void draftWithoutRecipientCannotBeSealed() {
        LetterData draft = LetterData.createDraft(RETURN_ADDRESS);
        AtomicReference<String> failureKey = new AtomicReference<>();

        assertFalse(draft.canSeal());
        assertTrue(draft.trySeal(failureKey::set).isEmpty());
        assertEquals("message.everechoes.letter.missing_recipient_address", failureKey.get());
        assertThrows(IllegalStateException.class, draft::seal);
    }

    @Test
    void sealedAndOpenedLettersCannotBeEdited() {
        LetterData sealed = completeDraft().seal();
        LetterData opened = sealed.open();

        assertThrows(IllegalStateException.class, () -> sealed.withTitle("nope"));
        assertThrows(IllegalStateException.class, () -> sealed.withBody("nope"));
        assertThrows(IllegalStateException.class, () -> sealed.withRecipientAddress(new MailBoxAddress("other")));
        assertThrows(IllegalStateException.class, () -> sealed.withReturnAddress(new PlayerAddress(UUID.randomUUID())));
        assertThrows(IllegalStateException.class, () -> opened.withTitle("nope"));
        assertThrows(IllegalStateException.class, () -> opened.withSignatureSender("nope"));
        assertThrows(IllegalStateException.class, sealed::seal);
        assertThrows(IllegalStateException.class, opened::open);
        assertTrue(sealed.tryOpen().isPresent());
        assertTrue(opened.tryOpen().isEmpty());
    }

    @Test
    void sealedAndOpenedLettersRequireRecipientAddress() {
        UUID letterId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> LetterData.reconstruct(
                letterId,
                LetterState.SEALED,
                RETURN_ADDRESS,
                null,
                "title",
                "body",
                null,
                null
        ));
        assertThrows(IllegalArgumentException.class, () -> LetterData.reconstruct(
                letterId,
                LetterState.OPENED,
                RETURN_ADDRESS,
                null,
                "title",
                "body",
                null,
                null
        ));
        assertTrue(LetterData.tryReconstruct(
                letterId,
                LetterState.SEALED,
                RETURN_ADDRESS,
                null,
                "title",
                "body",
                null,
                null
        ).isEmpty());
    }

    @Test
    void addressLimitFitsPlayerUuid() {
        String uuid = UUID.randomUUID().toString();
        assertEquals(36, uuid.length());
        assertEquals(uuid, LetterLimits.clamp(uuid, LetterLimits.ADDRESS_VALUE));
    }

    @Test
    void blankOptionalTextIsNormalizedToAbsent() {
        LetterData draft = LetterData.createDraft(RETURN_ADDRESS)
                .withSignatureSender("   ")
                .withLetterRecipient("");

        assertTrue(draft.signatureSender().isEmpty());
        assertTrue(draft.letterRecipient().isEmpty());
    }

    private static LetterData completeDraft() {
        return LetterData.createDraft(RETURN_ADDRESS).withRecipientAddress(RECIPIENT_ADDRESS);
    }
}
