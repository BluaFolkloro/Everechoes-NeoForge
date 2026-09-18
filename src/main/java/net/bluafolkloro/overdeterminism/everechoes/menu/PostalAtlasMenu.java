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

public class PostalAtlasMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final UUID nodeId;
    private final UUID districtId;
    private PostalAtlasSnapshot snapshot;
    private int coverageEpoch;
    @Nullable
    private String lastReasonKey;

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

    public BlockPos pos() {
        return pos;
    }

    public UUID nodeId() {
        return nodeId;
    }

    public UUID districtId() {
        return districtId;
    }

    public PostalAtlasSnapshot snapshot() {
        return snapshot;
    }

    public int coverageEpoch() {
        return coverageEpoch;
    }

    @Nullable
    public String lastReasonKey() {
        return lastReasonKey;
    }

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

    public void applyResult(boolean allowed, String reasonKey, int revision, Set<Long> savedCoverage) {
        lastReasonKey = reasonKey == null || reasonKey.isEmpty() ? null : reasonKey;
        if (!allowed) {
            return;
        }
        coverageEpoch++;
        Set<Long> saved = Set.copyOf(savedCoverage);
        snapshot = withSaved(snapshot, saved, saved.size(), revision);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

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
                base.exploredPacked(),
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
