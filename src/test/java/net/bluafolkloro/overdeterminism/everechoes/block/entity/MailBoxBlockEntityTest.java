package net.bluafolkloro.overdeterminism.everechoes.block.entity;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 封装相关行为的自动化回归测试。
 */
class MailBoxBlockEntityTest {
    /**
     * 获取或解析 {@code readMailboxIdRoundTripsExistingTagAndReturnsNullWhenMissing} 对应的业务操作。
     */
    @Test
    void readMailboxIdRoundTripsExistingTagAndReturnsNullWhenMissing() {
        assertEquals("mailboxId", MailBoxBlockEntity.MAILBOX_ID_TAG);

        UUID original = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        CompoundTag tagged = new CompoundTag();
        tagged.putUUID(MailBoxBlockEntity.MAILBOX_ID_TAG, original);
        assertEquals(original, MailBoxBlockEntity.readMailboxId(tagged));
        assertTrue(tagged.hasUUID(MailBoxBlockEntity.MAILBOX_ID_TAG));

        CompoundTag empty = new CompoundTag();
        assertNull(MailBoxBlockEntity.readMailboxId(empty));
        assertFalse(empty.hasUUID(MailBoxBlockEntity.MAILBOX_ID_TAG));

        CompoundTag otherKey = new CompoundTag();
        otherKey.putUUID("otherId", original);
        assertNull(MailBoxBlockEntity.readMailboxId(otherKey));
        assertFalse(otherKey.hasUUID(MailBoxBlockEntity.MAILBOX_ID_TAG));
    }

    /**
     * 执行 {@code assignMailboxIdOnServerKeepsExistingAndMintsOnlyOnServer} 对应的业务操作。
     */
    @Test
    void assignMailboxIdOnServerKeepsExistingAndMintsOnlyOnServer() {
        UUID existing = UUID.fromString("11111111-2222-3333-4444-555555555555");
        assertSame(existing, MailBoxBlockEntity.assignMailboxIdOnServer(existing, true));
        assertSame(existing, MailBoxBlockEntity.assignMailboxIdOnServer(existing, false));
        assertEquals(existing, MailBoxBlockEntity.assignMailboxIdOnServer(existing, true));

        assertNull(MailBoxBlockEntity.assignMailboxIdOnServer(null, false));
        assertNotNull(MailBoxBlockEntity.assignMailboxIdOnServer(null, true));
    }

    /**
     * 执行 {@code javaSourceAssignsMailboxIdOnServerWithoutGetterMint} 对应的业务操作。
     */
    @Test
    void javaSourceAssignsMailboxIdOnServerWithoutGetterMint() {
        String source = readEntitySource();
        assertFalse(source.contains("MenuProvider"), "MailBoxBlockEntity implements MenuProvider");
        assertFalse(source.contains("PostalNetwork"), "MailBoxBlockEntity imports PostalNetwork");

        String getter = methodBody(source, "UUID mailboxId()");
        assertFalse(getter.contains("randomUUID"), "mailboxId() still mints a UUID");

        String assign = methodBody(source, "UUID assignMailboxIdOnServer(");
        assertTrue(assign.contains("UUID.randomUUID()"), "assignMailboxIdOnServer does not mint");
        assertEquals(1, count(source, "UUID.randomUUID()"), "UUID.randomUUID() should only mint on the server assign path");

        assertTrue(source.contains("void onLoad()"), "onLoad is missing");
        String onLoad = methodBody(source, "void onLoad()");
        assertTrue(onLoad.contains("ServerLevel"), "onLoad does not mention ServerLevel");

        String save = methodBody(source, "void saveAdditional(");
        assertFalse(save.contains("mailboxId()"), "saveAdditional still force-creates via mailboxId()");
        assertTrue(save.contains("ServerLevel"), "saveAdditional does not assign on the server");
        assertTrue(
                save.contains("putUUID(MAILBOX_ID_TAG") || save.contains("putUUID(\"mailboxId\""),
                "saveAdditional does not write mailboxId"
        );
        assertTrue(
                save.contains("mailboxId != null") || save.contains("if (mailboxId != null)"),
                "saveAdditional writes mailboxId without a server-assigned value"
        );

        int assignAt = source.indexOf("assignMailboxIdOnServer(");
        int changedAt = source.indexOf("setChanged()");
        assertTrue(assignAt >= 0, "assignMailboxIdOnServer is never called");
        assertTrue(changedAt >= 0, "assign path does not call setChanged()");
    }

    /**
     * 执行 {@code methodBody} 对应的业务操作。
     */
    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, "missing " + signature);
        int brace = source.indexOf('{', start);
        assertTrue(brace >= 0, "missing body for " + signature);
        int depth = 0;
        for (int index = brace; index < source.length(); index++) {
            char ch = source.charAt(index);
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(brace, index + 1);
                }
            }
        }
        throw new AssertionError("unclosed method " + signature);
    }

    /**
     * 执行 {@code count} 对应的业务操作。
     */
    private static int count(String haystack, String needle) {
        int total = 0;
        int from = 0;
        while (true) {
            int at = haystack.indexOf(needle, from);
            if (at < 0) {
                return total;
            }
            total++;
            from = at + needle.length();
        }
    }

    /**
     * 获取或解析 {@code readEntitySource} 对应的业务操作。
     */
    private static String readEntitySource() {
        Path path = repoRoot()
                .resolve("src/main/java/net/bluafolkloro/overdeterminism/everechoes/block/entity/MailBoxBlockEntity.java");
        try {
            assertTrue(Files.isRegularFile(path), "missing " + path);
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError("cannot read " + path, e);
        }
    }

    /**
     * 执行 {@code repoRoot} 对应的业务操作。
     */
    private static Path repoRoot() {
        Path dir = Path.of("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.isDirectory(dir.resolve("src/main/resources/assets/everechoes"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        throw new AssertionError("cannot locate repo root from " + Path.of("").toAbsolutePath());
    }
}
