package net.bluafolkloro.overdeterminism.everechoes.block.entity;

import net.bluafolkloro.overdeterminism.everechoes.menu.PostBoxMenu;
import net.bluafolkloro.overdeterminism.everechoes.postal.DomainMembership;
import net.bluafolkloro.overdeterminism.everechoes.postal.MembershipState;
import net.bluafolkloro.overdeterminism.everechoes.postal.NodeRole;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalActionContext;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalCodes;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalDistrict;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalDomain;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalNetwork;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostBoxNode;
import net.bluafolkloro.overdeterminism.everechoes.postal.Waybills;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import java.util.UUID;

public class PostBoxBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_COUNT = 5;
    private static final int LEGACY_SLOT_COUNT = 27;

    private final SimpleContainer items = new SimpleContainer(SLOT_COUNT) {
        @Override
        public boolean canPlaceItem(int index, ItemStack stack) {
            return Waybills.isDepositable(stack) && PostBoxBlockEntity.this.acceptsMail();
        }

        @Override
        public void setItem(int index, ItemStack stack) {
            if (Waybills.isDepositable(stack) && PostBoxBlockEntity.this.acceptsMail()) {
                Waybills.markAwaitingCarrier(stack);
                super.setItem(index, stack);
                return;
            }
            if (stack.isEmpty()) {
                super.setItem(index, stack);
            }
        }

        @Override
        public void setChanged() {
            super.setChanged();
            PostBoxBlockEntity.this.setChanged();
        }
    };

    @Nullable
    private UUID nodeId;
    @Nullable
    private UUID districtId;
    @Nullable
    private NodeRole nodeRole;
    @Nullable
    private String domainCode;
    @Nullable
    private String districtCode;
    @Nullable
    private MembershipState membershipState;
    private final NonNullList<ItemStack> overflow = NonNullList.create();

    public PostBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POST_BOX.get(), pos, state);
    }

    public UUID nodeId() {
        if (nodeId == null) {
            nodeId = UUID.randomUUID();
        }
        return nodeId;
    }

    @Nullable
    public UUID districtId() {
        return districtId;
    }

    @Nullable
    public NodeRole nodeRole() {
        return nodeRole;
    }

    @Nullable
    public String domainCode() {
        return domainCode;
    }

    @Nullable
    public String districtCode() {
        return districtCode;
    }

    @Nullable
    public MembershipState membershipState() {
        return membershipState;
    }

    public PostalActionContext actionContext() {
        ResourceLocation dimension = level == null ? ResourceLocation.fromNamespaceAndPath("minecraft", "overworld") : level.dimension().location();
        return new PostalActionContext(districtId, nodeId(), dimension, getBlockPos());
    }

    public boolean hasActiveMembership() {
        if (level instanceof ServerLevel serverLevel && districtId != null) {
            return PostalNetwork.get(serverLevel).isActiveDeliveryDistrict(districtId);
        }
        return membershipState == MembershipState.ACTIVE && domainCode != null && districtCode != null;
    }

    public boolean acceptsMail() {
        return hasActiveMembership();
    }

    @Override
    public Component getDisplayName() {
        if (domainCode != null && districtCode != null) {
            return Component.translatable(
                    "block.everechoes.post_box.district",
                    PostalCodes.formatOutward(domainCode, districtCode)
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
            syncFromNetwork(serverLevel);
            dropOverflow(serverLevel);
        }
    }

    public void syncFromNetwork(ServerLevel level) {
        PostalNetwork network = PostalNetwork.get(level);
        PostBoxNode node = network.registerNode(nodeId(), level.dimension().location(), getBlockPos());
        districtId = node.districtId();
        nodeRole = node.role();
        if (districtId == null) {
            domainCode = null;
            districtCode = null;
            membershipState = null;
            setChanged();
            return;
        }
        DomainMembership membership = network.membership(districtId).orElse(null);
        PostalDomain domain = membership == null ? null : network.domain(membership.domainId()).orElse(null);
        applyMembership(membership, domain);
    }

    public void applyMembership(@Nullable DomainMembership membership, @Nullable PostalDomain domain) {
        if (membership == null || membership.state() == MembershipState.DETACHED) {
            domainCode = null;
            districtCode = null;
            membershipState = membership == null ? null : MembershipState.DETACHED;
        } else {
            domainCode = domain == null ? domainCode : domain.domainCode();
            districtCode = membership.districtCode();
            membershipState = membership.state();
        }
        setChanged();
    }

    public void unregister(ServerLevel level) {
        PostalNetwork.get(level).removeNode(nodeId());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items.getItems(), registries);
        tag.putUUID("nodeId", nodeId());
        if (districtId != null) {
            tag.putUUID("districtId", districtId);
        }
        if (nodeRole != null) {
            tag.putString("nodeRole", nodeRole.name());
        }
        if (domainCode != null) {
            tag.putString("domainCode", domainCode);
        }
        if (districtCode != null) {
            tag.putString("districtCode", districtCode);
        }
        if (membershipState != null) {
            tag.putString("membershipState", membershipState.name());
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
                items.getItems().set(dest++, stack);
            } else {
                overflow.add(stack);
            }
        }

        if (tag.hasUUID("nodeId")) {
            nodeId = tag.getUUID("nodeId");
        } else if (tag.hasUUID("districtUuid")) {
            nodeId = tag.getUUID("districtUuid");
        }
        if (tag.hasUUID("districtId") && tag.contains("nodeRole")) {
            districtId = tag.getUUID("districtId");
        }
        if (tag.contains("nodeRole") && !tag.getString("nodeRole").isEmpty()) {
            nodeRole = NodeRole.valueOf(tag.getString("nodeRole"));
        }
        domainCode = PostalCodes.canonicalDomain(tag.getString("domainCode")).orElse(null);
        districtCode = PostalCodes.canonicalDistrict(tag.getString("districtCode")).orElse(null);
        if (tag.contains("membershipState") && !tag.getString("membershipState").isEmpty()) {
            membershipState = MembershipState.valueOf(tag.getString("membershipState"));
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
