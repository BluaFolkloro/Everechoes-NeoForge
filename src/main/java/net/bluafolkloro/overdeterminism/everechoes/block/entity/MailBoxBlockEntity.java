package net.bluafolkloro.overdeterminism.everechoes.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

public class MailBoxBlockEntity extends BlockEntity {
    static final String MAILBOX_ID_TAG = "mailboxId";

    @Nullable
    private UUID mailboxId;

    public MailBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAIL_BOX.get(), pos, state);
    }

    @Nullable
    public UUID mailboxId() {
        return mailboxId;
    }

    @Nullable
    static UUID readMailboxId(CompoundTag tag) {
        return tag.hasUUID(MAILBOX_ID_TAG) ? tag.getUUID(MAILBOX_ID_TAG) : null;
    }

    /**
     * Identity policy used by onLoad/saveAdditional.
     * existing non-null UUID is returned unchanged.
     * null + serverSide → new random UUID.
     * null + !serverSide → null.
     */
    @Nullable
    static UUID assignMailboxIdOnServer(@Nullable UUID current, boolean serverSide) {
        if (current != null) {
            return current;
        }
        return serverSide ? UUID.randomUUID() : null;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel) {
            ensureMailboxIdOnServer();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (level instanceof ServerLevel) {
            ensureMailboxIdOnServer();
        }
        if (mailboxId != null) {
            tag.putUUID(MAILBOX_ID_TAG, mailboxId);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        mailboxId = readMailboxId(tag);
    }

    private void ensureMailboxIdOnServer() {
        UUID assigned = assignMailboxIdOnServer(mailboxId, true);
        if (mailboxId == null && assigned != null) {
            mailboxId = assigned;
            setChanged();
        }
    }
}
