package net.bluafolkloro.overdeterminism.everechoes.block.entity;

import net.bluafolkloro.overdeterminism.everechoes.postal.MembershipState;
import net.bluafolkloro.overdeterminism.everechoes.postal.NodeRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 封装相关行为的自动化回归测试。
 */
class PostBoxBlockEntityTest {
    /**
     * 保存或编码 {@code savedEnumsRejectUnknownValuesWithoutThrowing} 对应的业务操作。
     */
    @Test
    void savedEnumsRejectUnknownValuesWithoutThrowing() {
        assertEquals(NodeRole.HUB, PostBoxBlockEntity.enumValue(NodeRole.class, "HUB"));
        assertEquals(MembershipState.ACTIVE, PostBoxBlockEntity.enumValue(MembershipState.class, "ACTIVE"));
        assertNull(PostBoxBlockEntity.enumValue(NodeRole.class, "BROKEN"));
        assertNull(PostBoxBlockEntity.enumValue(MembershipState.class, ""));
    }
}
