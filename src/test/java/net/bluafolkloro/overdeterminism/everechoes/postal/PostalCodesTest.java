package net.bluafolkloro.overdeterminism.everechoes.postal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 封装相关行为的自动化回归测试。
 */
class PostalCodesTest {
    /**
     * 执行 {@code deliveryAtFollowsDigitThenInwardLetters} 对应的业务操作。
     */
    @Test
    void deliveryAtFollowsDigitThenInwardLetters() {
        assertEquals("ABDEFGHJLNPQRSTUWXYZ", PostalCodes.INWARD_LETTERS);
        assertEquals('Z', PostalCodes.INWARD_LETTERS.charAt(PostalCodes.INWARD_LETTERS.length() - 1));
        assertEquals(20, PostalCodes.INWARD_LETTERS.length());

        assertEquals("0AA", PostalCodes.deliveryAt(0).orElseThrow());
        assertEquals("0AB", PostalCodes.deliveryAt(1).orElseThrow());
        assertEquals("0AZ", PostalCodes.deliveryAt(19).orElseThrow());
        assertEquals("0BA", PostalCodes.deliveryAt(20).orElseThrow());
        assertEquals("1AA", PostalCodes.deliveryAt(400).orElseThrow());
        assertEquals("9ZZ", PostalCodes.deliveryAt(3999).orElseThrow());
        assertTrue(PostalCodes.deliveryAt(-1).isEmpty());
        assertTrue(PostalCodes.deliveryAt(4000).isEmpty());
    }

    /**
     * 执行 {@code deliveryCapacityIsFourThousand} 对应的业务操作。
     */
    @Test
    void deliveryCapacityIsFourThousand() {
        assertEquals(4000, PostalCodes.deliveryCapacity());
        assertEquals(
                10 * PostalCodes.INWARD_LETTERS.length() * PostalCodes.INWARD_LETTERS.length(),
                PostalCodes.deliveryCapacity()
        );
    }
}
