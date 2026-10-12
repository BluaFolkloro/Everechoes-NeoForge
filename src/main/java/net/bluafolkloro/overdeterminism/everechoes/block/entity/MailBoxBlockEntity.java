package net.bluafolkloro.overdeterminism.everechoes.block.entity;

import net.bluafolkloro.overdeterminism.everechoes.postal.MailBoxEndpoint;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 封装方块实体的持久状态与运行逻辑。
 */
public class MailBoxBlockEntity extends BlockEntity {
    /**
     * 定义 {@code MAILBOX_ID_TAG} 常量。
     */
    static final String MAILBOX_ID_TAG = "mailboxId";

    /**
     * 保存 {@code mailboxId} 对应的状态或依赖。
     */
    @Nullable
    private UUID mailboxId;

    /**
     * 创建并初始化当前对象。
     */
    public MailBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAIL_BOX.get(), pos, state);
    }

    /**
     * 执行 {@code mailboxId} 对应的业务操作。
     */
    @Nullable
    public UUID mailboxId() {
        return mailboxId;
    }

    /**
     * 获取或解析 {@code readMailboxId} 对应的业务操作。
     */
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

    /**
     * 服务端加载时确保 mailboxId，并向邮政网络注册端点；不绑定投递区。
     */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            ensureMailboxIdOnServer();
            if (mailboxId != null) {
                MailBoxEndpoint registered = PostalNetwork.get(serverLevel).registerEndpoint(
                        mailboxId, serverLevel.dimension().location(), getBlockPos());
                if (!registered.mailboxId().equals(mailboxId)) {
                    mailboxId = registered.mailboxId();
                    setChanged();
                }
            }
        }
    }

    /**
     * 保存或编码 {@code saveAdditional} 对应的业务操作。
     */
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

    /**
     * 读取并恢复 {@code loadAdditional} 对应的业务操作。
     */
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        mailboxId = readMailboxId(tag);
    }

    /**
     * 执行 {@code ensureMailboxIdOnServer} 对应的业务操作。
     */
    private void ensureMailboxIdOnServer() {
        UUID assigned = assignMailboxIdOnServer(mailboxId, true);
        if (mailboxId == null && assigned != null) {
            mailboxId = assigned;
            setChanged();
        }
    }
}
