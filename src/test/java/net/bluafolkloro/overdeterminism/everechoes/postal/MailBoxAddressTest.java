package net.bluafolkloro.overdeterminism.everechoes.postal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailBoxAddressTest {
    @Test
    void parseCanonicalizesUkStyleAndSpacing() {
        MailBoxAddress address = MailBoxAddress.parse(" ev12-7qf ").orElseThrow();
        assertEquals("EV", address.domainCode());
        assertEquals("12", address.districtCode());
        assertEquals("7QF", address.deliveryCode());
        assertEquals("EV12 7QF", address.postalCode());
        assertTrue(address.domainId().isEmpty());
    }

    @Test
    void parseAcceptsAllOutwardShapes() {
        assertEquals("A1 1AA", MailBoxAddress.parse("a1 1aa").orElseThrow().postalCode());
        assertEquals("A12 1AA", MailBoxAddress.parse("A12 1AA").orElseThrow().postalCode());
        assertEquals("A1A 1AA", MailBoxAddress.parse("A1A1AA").orElseThrow().postalCode());
        assertEquals("AA1 1AA", MailBoxAddress.parse("aa1 1aa").orElseThrow().postalCode());
        assertEquals("AA12 1AA", MailBoxAddress.parse("AA12 1AA").orElseThrow().postalCode());
        assertEquals("AA1A 1AA", MailBoxAddress.parse("AA1A 1AA").orElseThrow().postalCode());
    }

    @Test
    void parseRejectsInvalidInwardLetters() {
        assertTrue(MailBoxAddress.parse("EV12 7CI").isEmpty());
        assertTrue(MailBoxAddress.parse("EV12").isEmpty());
        assertTrue(MailBoxAddress.parse("ABC12 1AA").isEmpty());
    }

    @Test
    void constructorRejectsInvalidParts() {
        assertThrows(IllegalArgumentException.class, () -> MailBoxAddress.display("ABC", "1", "1AA"));
        assertThrows(IllegalArgumentException.class, () -> MailBoxAddress.display("A", "0", "1AA"));
        assertThrows(IllegalArgumentException.class, () -> MailBoxAddress.display("A", "1", "1CI"));
    }
}
