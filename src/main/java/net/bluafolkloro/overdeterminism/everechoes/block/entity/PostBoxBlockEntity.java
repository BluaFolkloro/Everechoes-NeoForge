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
import java.util.Objects;
import java.util.UUID;

/**
 * 封装方块实体的持久状态与运行逻辑。
 */
public class PostBoxBlockEntity extends BlockEntity implements MenuProvider {
    /**
     * 定义 {@code SLOT_COUNT} 常量。
     */
    public static final int SLOT_COUNT = 5;
    /**
     * 定义 {@code LEGACY_SLOT_COUNT} 常量。
     */
    private static final int LEGACY_SLOT_COUNT = 27;

    /**
     * 保存 {@code items} 对应的状态或依赖。
     */
    private final SimpleContainer items = new SimpleContainer(SLOT_COUNT) /** 封装该模块的相关职责。 */ {
        /**
         * 判断 {@code canPlaceItem} 对应的业务操作。
         */
        @Override
        public boolean canPlaceItem(int index, ItemStack stack) {
            return Waybills.isDepositable(stack) && PostBoxBlockEntity.this.acceptsMail();
        }

        /**
         * 设置或写入 {@code setItem} 对应的业务操作。
         */
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

        /**
         * 设置或写入 {@code setChanged} 对应的业务操作。
         */
        @Override
        public void setChanged() {
            super.setChanged();
            PostBoxBlockEntity.this.setChanged();
        }
    };

    /**
     * 保存 {@code nodeId} 对应的状态或依赖。
     */
    @Nullable
    private UUID nodeId;
    /**
     * 保存 {@code districtId} 对应的状态或依赖。
     */
    @Nullable
    private UUID districtId;
    /**
     * 保存 {@code nodeRole} 对应的状态或依赖。
     */
    @Nullable
    private NodeRole nodeRole;
    /**
     * 保存 {@code domainCode} 对应的状态或依赖。
     */
    @Nullable
    private String domainCode;
    /**
     * 保存 {@code districtCode} 对应的状态或依赖。
     */
    @Nullable
    private String districtCode;
    /**
     * 保存 {@code membershipState} 对应的状态或依赖。
     */
    @Nullable
    private MembershipState membershipState;
    /**
     * 保存 {@code overflow} 对应的状态或依赖。
     */
    private final NonNullList<ItemStack> overflow = NonNullList.create();

    /**
     * 创建并初始化当前对象。
     */
    public PostBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POST_BOX.get(), pos, state);
    }

    /**
     * 执行 {@code nodeId} 对应的业务操作。
     */
    public UUID nodeId() {
        if (nodeId == null) {
            nodeId = UUID.randomUUID();
        }
        return nodeId;
    }

    /**
     * 执行 {@code districtId} 对应的业务操作。
     */
    @Nullable
    public UUID districtId() {
        return districtId;
    }

    /**
     * 执行 {@code nodeRole} 对应的业务操作。
     */
    @Nullable
    public NodeRole nodeRole() {
        return nodeRole;
    }

    /**
     * 执行 {@code domainCode} 对应的业务操作。
     */
    @Nullable
    public String domainCode() {
        return domainCode;
    }

    /**
     * 执行 {@code districtCode} 对应的业务操作。
     */
    @Nullable
    public String districtCode() {
        return districtCode;
    }

    /**
     * 执行 {@code membershipState} 对应的业务操作。
     */
    @Nullable
    public MembershipState membershipState() {
        return membershipState;
    }

    /**
     * 执行 {@code actionContext} 对应的业务操作。
     */
    public PostalActionContext actionContext(@Nullable UUID actorId) {
        ResourceLocation dimension = level == null ? ResourceLocation.fromNamespaceAndPath("minecraft", "overworld") : level.dimension().location();
        return new PostalActionContext(districtId, nodeId(), actorId, dimension, getBlockPos());
    }

    /**
     * 判断 {@code hasActiveMembership} 对应的业务操作。
     */
    public boolean hasActiveMembership() {
        if (level instanceof ServerLevel serverLevel && districtId != null) {
            return PostalNetwork.get(serverLevel).isActiveDeliveryDistrict(districtId);
        }
        return membershipState == MembershipState.ACTIVE && domainCode != null && districtCode != null;
    }

    /**
     * 执行 {@code acceptsMail} 对应的业务操作。
     */
    public boolean acceptsMail() {
        return hasActiveMembership();
    }

    /**
     * 获取或解析 {@code getDisplayName} 对应的业务操作。
     */
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

    /**
     * 创建 {@code createMenu} 对应的业务操作。
     */
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInv, Player player) {
        return new PostBoxMenu(windowId, playerInv, this);
    }

    /**
     * 处理 {@code onLoad} 对应的业务操作。
     */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            syncFromNetwork(serverLevel);
            dropOverflow(serverLevel);
        }
    }

    /**
     * 同步 {@code syncFromNetwork} 对应的业务操作。
     */
    public void syncFromNetwork(ServerLevel level) {
        PostalNetwork network = PostalNetwork.get(level);
        UUID requestedId = nodeId();
        PostBoxNode node = network.registerNode(requestedId, level.dimension().location(), getBlockPos());
        boolean nodeChanged = !Objects.equals(nodeId, node.nodeId())
                || !Objects.equals(districtId, node.districtId())
                || nodeRole != node.role();
        nodeId = node.nodeId();
        districtId = node.districtId();
        nodeRole = node.role();
        if (districtId == null) {
            boolean membershipChanged = domainCode != null || districtCode != null || membershipState != null;
            domainCode = null;
            districtCode = null;
            membershipState = null;
            if (nodeChanged || membershipChanged) {
                setChanged();
            }
            return;
        }
        DomainMembership membership = network.membership(districtId).orElse(null);
        PostalDomain domain = membership == null ? null : network.domain(membership.domainId()).orElse(null);
        applyMembership(membership, domain);
        if (nodeChanged) {
            setChanged();
        }
    }

    /**
     * 应用 {@code applyMembership} 对应的业务操作。
     */
    public void applyMembership(@Nullable DomainMembership membership, @Nullable PostalDomain domain) {
        String nextDomainCode;
        String nextDistrictCode;
        MembershipState nextState;
        if (membership == null || membership.state() == MembershipState.DETACHED) {
            nextDomainCode = null;
            nextDistrictCode = null;
            nextState = membership == null ? null : MembershipState.DETACHED;
        } else {
            nextDomainCode = domain == null ? domainCode : domain.domainCode();
            nextDistrictCode = membership.districtCode();
            nextState = membership.state();
        }
        if (Objects.equals(domainCode, nextDomainCode)
                && Objects.equals(districtCode, nextDistrictCode)
                && membershipState == nextState) {
            return;
        }
        domainCode = nextDomainCode;
        districtCode = nextDistrictCode;
        membershipState = nextState;
        setChanged();
    }

    /**
     * 执行 {@code unregister} 对应的业务操作。
     */
    public void unregister(ServerLevel level) {
        PostalNetwork.get(level).removeNode(nodeId());
    }

    /**
     * 保存或编码 {@code saveAdditional} 对应的业务操作。
     */
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

    /**
     * 读取并恢复 {@code loadAdditional} 对应的业务操作。
     */
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
            nodeRole = enumValue(NodeRole.class, tag.getString("nodeRole"));
        }
        domainCode = PostalCodes.canonicalDomain(tag.getString("domainCode")).orElse(null);
        districtCode = PostalCodes.canonicalDistrict(tag.getString("districtCode")).orElse(null);
        if (tag.contains("membershipState") && !tag.getString("membershipState").isEmpty()) {
            membershipState = enumValue(MembershipState.class, tag.getString("membershipState"));
        }
    }

    /**
     * 执行 {@code enumValue} 对应的业务操作。
     */
    @Nullable
    static <E extends Enum<E>> E enumValue(Class<E> type, String raw) {
        try {
            return Enum.valueOf(type, raw);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return null;
        }
    }

    /**
     * 获取或解析 {@code getItems} 对应的业务操作。
     */
    public SimpleContainer getItems() {
        return items;
    }

    /**
     * 执行 {@code dropOverflow} 对应的业务操作。
     */
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
