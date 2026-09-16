package net.bluafolkloro.overdeterminism.everechoes.postal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostalNetworkTest {
    @Test
    void districtsIncrementPerDomain() {
        PostalNetwork network = new PostalNetwork();
        assertEquals("ABC", network.createDomain("abc").orElseThrow());
        assertEquals("XY", network.createDomain("xy").orElseThrow());

        PostalNetwork.PostalIds firstAbc = network.assignDistrict("ABC", null).orElseThrow();
        PostalNetwork.PostalIds secondAbc = network.assignDistrict("ABC", null).orElseThrow();
        PostalNetwork.PostalIds firstXy = network.assignDistrict("XY", null).orElseThrow();

        assertEquals("ABC1", firstAbc.format());
        assertEquals("ABC2", secondAbc.format());
        assertEquals("XY1", firstXy.format());
    }

    @Test
    void reassigningSameDomainKeepsDistrict() {
        PostalNetwork network = new PostalNetwork();
        network.createDomain("A");
        PostalNetwork.PostalIds first = network.assignDistrict("A", null).orElseThrow();
        PostalNetwork.PostalIds again = network.assignDistrict("A", first).orElseThrow();

        assertEquals(first, again);
        assertEquals("A2", network.assignDistrict("A", null).orElseThrow().format());
    }

    @Test
    void createRejectsDuplicateAndInvalidDomain() {
        PostalNetwork network = new PostalNetwork();
        assertTrue(network.createDomain("A1").isEmpty());
        assertTrue(network.createDomain("ABCD").isEmpty());
        assertEquals("AB", network.createDomain("ab").orElseThrow());
        assertTrue(network.createDomain("AB").isEmpty());
    }
}
