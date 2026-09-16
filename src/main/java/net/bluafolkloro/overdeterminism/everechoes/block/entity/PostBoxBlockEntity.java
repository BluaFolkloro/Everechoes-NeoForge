package net.bluafolkloro.overdeterminism.everechoes.block.entity;

import net.bluafolkloro.overdeterminism.everechoes.menu.PostBoxMenu;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalCodes;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalNetwork;
import net.bluafolkloro.overdeterminism.everechoes.postal.Waybills;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class PostBoxBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_COUNT = 5;
    private static final int LEGACY_SLOT_COUNT = 27;

    private final SimpleContainer items = new SimpleContainer(SLOT_COUNT) {
        @Override
        public boolean canPlaceItem(int index, ItemStack stack) {
            return Waybills.isDepositable(stack);
        }

        @Override
        public void setItem(int index, ItemStack stack) {
            if (Waybills.isDepositable(stack)) {
                Waybills.markAwaitingCarrier(stack);
            }
            super.setItem(index, stack);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            PostBoxBlockEntity.this.setChanged();
        }
    };

    @Nullable
    private String domainId;
    @Nullable
    private String districtId;
    private final NonNullList<ItemStack> overflow = NonNullList.create();

    public PostBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POST_BOX.get(), pos, state);
    }

    public boolean hasDistrict() {
        return domainId != null && districtId != null;
    }

    @Nullable
    public String domainId() {
        return domainId;
    }

    @Nullable
    public String districtId() {
        return districtId;
    }

    @Override
    public Component getDisplayName() {
        if (hasDistrict()) {
            return Component.translatable(
                    "block.everechoes.post_box.district",
                    PostalCodes.formatDistrict(domainId, districtId)
            );
        }

        return Component.translatable("block.everechoes.post_box");
    }

    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInv, Player player) {
        return new PostBoxMenu(windowId, playerInv, this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            dropOverflow(serverLevel);
        }
    }

    public boolean assignToDomain(ServerLevel level, String domainId) {
        PostalNetwork.PostalIds current = hasDistrict() ? new PostalNetwork.PostalIds(this.domainId, this.districtId) : null;
        return PostalNetwork.get(level).assignDistrict(domainId, current).map(ids -> {
            this.domainId = ids.domainId();
            this.districtId = ids.districtId();
            setChanged();
            return true;
        }).orElse(false);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items.getItems(), registries);
        if (domainId != null) {
            tag.putString("domainId", domainId);
        }
        if (districtId != null) {
            tag.putString("districtId", districtId);
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        NonNullList<ItemStack> loaded = NonNullList.withSize(LEGACY_SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, loaded, registries);
        overflow.clear();
        int dest = 0;
        for (ItemStack stack : loaded) {
            if (stack.isEmpty()) {
                continue;
            }
            if (Waybills.isDepositable(stack) && dest < SLOT_COUNT) {
                items.setItem(dest++, stack);
            } else {
                overflow.add(stack);
            }
        }

        domainId = PostalCodes.canonicalDomain(tag.getString("domainId")).orElse(null);
        districtId = PostalCodes.canonicalDistrict(tag.getString("districtId")).orElse(null);
        if (domainId == null || districtId == null) {
            domainId = null;
            districtId = null;
        }
    }

    public SimpleContainer getItems() {
        return items;
    }

    private void dropOverflow(ServerLevel level) {
        if (overflow.isEmpty()) {
            return;
        }

        for (ItemStack stack : overflow) {
            Containers.dropItemStack(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), stack);
        }
        overflow.clear();
    }
}
