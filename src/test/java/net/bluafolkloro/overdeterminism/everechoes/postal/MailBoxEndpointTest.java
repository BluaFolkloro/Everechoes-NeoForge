package net.bluafolkloro.overdeterminism.everechoes.postal;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 封装相关行为的自动化回归测试。
 */
class MailBoxEndpointTest {
    /**
     * 定义 {@code OVERWORLD} 常量。
     */
    private static final ResourceLocation OVERWORLD = ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");

    /**
     * 执行 {@code allocateSkipsInvalidAndDoesNotMutateUsedSet} 对应的业务操作。
     */
    @Test
    void allocateSkipsInvalidAndDoesNotMutateUsedSet() {
        Set<String> used = new HashSet<>();
        used.add("0AA");
        used.add("0ab");
        used.add(null);
        used.add("!!!");
        Set<String> snapshot = new HashSet<>(used);

        // INWARD_LETTERS skips C, so index 2 is 0AD rather than 0AC.
        assertEquals("0AD", DeliveryCodeAllocator.allocate(used).orElseThrow());
        assertEquals(snapshot, used);
        assertTrue(used.contains(null));
        assertTrue(used.contains("0ab"));
        assertTrue(used.contains("!!!"));
    }

    /**
     * 执行 {@code allocateReturnsEmptyWhenAllCodesUsed} 对应的业务操作。
     */
    @Test
    void allocateReturnsEmptyWhenAllCodesUsed() {
        Set<String> used = new HashSet<>();
        int capacity = PostalCodes.deliveryCapacity();
        for (int index = 0; index < capacity; index++) {
            used.add(PostalCodes.deliveryAt(index).orElseThrow());
        }
        assertEquals(4000, used.size());
        assertTrue(DeliveryCodeAllocator.allocate(used).isEmpty());
    }

    /**
     * 执行 {@code constructorTreatsHalfBindAsUnboundAndCanonicalizesDelivery} 对应的业务操作。
     */
    @Test
    void constructorTreatsHalfBindAsUnboundAndCanonicalizesDelivery() {
        UUID mailboxId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        UUID districtId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        BlockPos position = new BlockPos(0, 64, 0);

        MailBoxEndpoint districtOnly = new MailBoxEndpoint(
                mailboxId, districtId, null, OVERWORLD, position, MailBoxEndpointState.ONLINE);
        assertFalse(districtOnly.isBound());
        assertNull(districtOnly.districtId());
        assertNull(districtOnly.deliveryCode());

        MailBoxEndpoint codeOnly = new MailBoxEndpoint(
                mailboxId, null, "0AA", OVERWORLD, position, MailBoxEndpointState.ONLINE);
        assertFalse(codeOnly.isBound());
        assertNull(codeOnly.districtId());
        assertNull(codeOnly.deliveryCode());

        MailBoxEndpoint bound = new MailBoxEndpoint(
                mailboxId, districtId, "0aa", OVERWORLD, position, MailBoxEndpointState.OFFLINE);
        assertTrue(bound.isBound());
        assertEquals("0AA", bound.deliveryCode());
        assertEquals(MailBoxEndpointState.OFFLINE, bound.state());

        assertThrows(IllegalArgumentException.class, () -> new MailBoxEndpoint(
                mailboxId, districtId, "0AC", OVERWORLD, position, MailBoxEndpointState.ONLINE));
        assertThrows(IllegalArgumentException.class, () -> new MailBoxEndpoint(
                mailboxId, districtId, "!!!", OVERWORLD, position, MailBoxEndpointState.ONLINE));
    }
}
