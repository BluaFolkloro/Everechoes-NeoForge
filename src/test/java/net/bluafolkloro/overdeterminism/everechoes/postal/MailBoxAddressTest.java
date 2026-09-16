package net.bluafolkloro.overdeterminism.everechoes.postal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailBoxAddressTest {
    @Test
    void parseCanonicalizesLowercaseAndStripsHexZeros() {
        MailBoxAddress address = MailBoxAddress.parse(" abc1-00a ").orElseThrow();
        assertEquals("ABC", address.domainId());
        assertEquals("1", address.districtId());
        assertEquals("A", address.deliveryId());
        assertEquals("ABC1-A", address.postalCode());
    }

    @Test
    void parseAcceptsMaxWidth() {
        MailBoxAddress address = MailBoxAddress.parse("xyz99-fff").orElseThrow();
        assertEquals("XYZ99-FFF", address.format());
    }

    @Test
    void parseRejectsLeadingZeroDistrictAndZeroDelivery() {
        assertTrue(MailBoxAddress.parse("A01-1").isEmpty());
        assertTrue(MailBoxAddress.parse("A1-0").isEmpty());
        assertTrue(MailBoxAddress.parse("A1-000").isEmpty());
        assertTrue(MailBoxAddress.parse("overworld/d0001/home").isEmpty());
        assertTrue(MailBoxAddress.parse("A1").isEmpty());
    }

    @Test
    void constructorRejectsInvalidParts() {
        assertThrows(IllegalArgumentException.class, () -> new MailBoxAddress("ABCD", "1", "1"));
        assertThrows(IllegalArgumentException.class, () -> new MailBoxAddress("A", "0", "1"));
        assertThrows(IllegalArgumentException.class, () -> new MailBoxAddress("A", "1", "0"));
    }
}
