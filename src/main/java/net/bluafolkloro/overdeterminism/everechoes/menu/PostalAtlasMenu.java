package net.bluafolkloro.overdeterminism.everechoes.menu;

import net.bluafolkloro.overdeterminism.everechoes.block.entity.PostBoxBlockEntity;
import net.bluafolkloro.overdeterminism.everechoes.network.PostalAtlasSyncPayload;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalAtlasSnapshot;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 封装服务端容器菜单及数据同步。
 */
public class PostalAtlasMenu extends AbstractContainerMenu {
    /**
     * 保存 {@code pos} 对应的状态或依赖。
     */
    private final BlockPos pos;
    /**
     * 保存 {@code nodeId} 对应的状态或依赖。
     */
    private final UUID nodeId;
    /**
     * 保存 {@code districtId} 对应的状态或依赖。
     */
    private final UUID districtId;
    /**
     * 保存 {@code snapshot} 对应的状态或依赖。
     */
    private PostalAtlasSnapshot snapshot;
    /**
     * 保存 {@code coverageEpoch} 对应的状态或依赖。
     */
    private int coverageEpoch;
    /**
     * 保存 {@code lastReasonKey} 对应的状态或依赖。
     */
    @Nullable
    private String lastReasonKey;

    /**
     * 创建并初始化当前对象。
     */
    public PostalAtlasMenu(
            int containerId,
            Inventory playerInv,
            BlockPos pos,
            UUID nodeId,
            UUID districtId,
            PostalAtlasSnapshot snapshot
    ) {
        super(ModMenuTypes.POSTAL_ATLAS_MENU.get(), containerId);
        this.pos = pos;
        this.nodeId = nodeId;
        this.districtId = districtId;
        this.snapshot = snapshot;
    }

    /**
     * 创建并初始化当前对象。
     */
    public PostalAtlasMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(
                containerId,
                playerInv,
                extraData.readBlockPos(),
                extraData.readUUID(),
                extraData.readUUID(),
                PostalAtlasSyncPayload.readSnapshot(extraData)
        );
    }

    /**
     * 保存或编码 {@code writeOpeningData} 对应的业务操作。
     */
    public static void writeOpeningData(
            FriendlyByteBuf buffer,
            BlockPos pos,
            UUID nodeId,
            UUID districtId,
            PostalAtlasSnapshot snapshot
    ) {
        buffer.writeBlockPos(pos);
        buffer.writeUUID(nodeId);
        buffer.writeUUID(districtId);
        PostalAtlasSyncPayload.writeSnapshot(buffer, snapshot, true);
    }

    /**
     * 执行 {@code pos} 对应的业务操作。
     */
    public BlockPos pos() {
        return pos;
    }

    /**
     * 执行 {@code nodeId} 对应的业务操作。
     */
    public UUID nodeId() {
        return nodeId;
    }

    /**
     * 执行 {@code districtId} 对应的业务操作。
     */
    public UUID districtId() {
        return districtId;
    }

    /**
     * 执行 {@code snapshot} 对应的业务操作。
     */
    public PostalAtlasSnapshot snapshot() {
        return snapshot;
    }

    /**
     * 执行 {@code coverageEpoch} 对应的业务操作。
     */
    public int coverageEpoch() {
        return coverageEpoch;
    }

    /**
     * 执行 {@code lastReasonKey} 对应的业务操作。
     */
    @Nullable
    public String lastReasonKey() {
        return lastReasonKey;
    }

    /**
     * 执行 {@code replaceWindow} 对应的业务操作。
     */
    public void replaceWindow(PostalAtlasSnapshot incoming) {
        if (!districtId.equals(incoming.districtId())) {
            return;
        }
        Set<Long> saved = snapshot.savedCoveragePacked();
        int savedSize = snapshot.savedCoverageSize();
        if (!incoming.savedCoveragePacked().isEmpty()) {
            saved = incoming.savedCoveragePacked();
            savedSize = incoming.savedCoverageSize();
        } else if (incoming.savedCoverageSize() > 0) {
            savedSize = incoming.savedCoverageSize();
        }
        snapshot = withSaved(incoming, saved, savedSize, incoming.revision());
    }

    /**
     * 应用 {@code applyResult} 对应的业务操作。
     */
    public void applyResult(boolean allowed, String reasonKey, int revision, Set<Long> savedCoverage) {
        lastReasonKey = reasonKey == null || reasonKey.isEmpty() ? null : reasonKey;
        if (!allowed) {
            return;
        }
        coverageEpoch++;
        Set<Long> saved = Set.copyOf(savedCoverage);
        snapshot = withSaved(snapshot, saved, saved.size(), revision);
    }

    /**
     * 执行 {@code quickMoveStack} 对应的业务操作。
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    /**
     * 执行 {@code stillValid} 对应的业务操作。
     */
    @Override
    public boolean stillValid(Player player) {
        if (!(player.level().getBlockEntity(pos) instanceof PostBoxBlockEntity postBox) || postBox.isRemoved()) {
            return false;
        }
        double dx = player.getX() - (pos.getX() + 0.5);
        double dy = player.getY() - (pos.getY() + 0.5);
        double dz = player.getZ() - (pos.getZ() + 0.5);
        if (dx * dx + dy * dy + dz * dz > 64.0) {
            return false;
        }
        // Client BE may not have districtId synced; range + presence is enough there.
        if (player.level() instanceof ServerLevel serverLevel) {
            if (!districtId.equals(postBox.districtId()) || !nodeId.equals(postBox.nodeId())) {
                return false;
            }
            return PostalNetwork.get(serverLevel).node(nodeId)
                    .filter(node -> districtId.equals(node.districtId()))
                    .isPresent();
        }
        return true;
    }

    /**
     * 执行 {@code withSaved} 对应的业务操作。
     */
    private static PostalAtlasSnapshot withSaved(PostalAtlasSnapshot base, Set<Long> saved, int savedSize, int revision) {
        Set<Long> owned = new LinkedHashSet<>();
        int width = base.width();
        int height = base.height();
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < height; dz++) {
                long packed = ChunkPos.asLong(base.originX() + dx, base.originZ() + dz);
                if (saved.contains(packed)) {
                    owned.add(packed);
                }
            }
        }
        return new PostalAtlasSnapshot(
                base.districtId(),
                base.dimension(),
                revision,
                base.domainCode(),
                base.districtCode(),
                base.originX(),
                base.originZ(),
                width,
                height,
                Set.copyOf(owned),
                base.foreignPacked(),
                base.hubPacked(),
                base.collectionPacked(),
                saved,
                base.nodePacked(),
                savedSize,
                base.maxChunks()
        );
    }
}
