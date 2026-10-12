package net.bluafolkloro.overdeterminism.everechoes.menu;

import net.bluafolkloro.overdeterminism.everechoes.block.entity.PostBoxBlockEntity;
import net.bluafolkloro.overdeterminism.everechoes.postal.DomainLifecycle;
import net.bluafolkloro.overdeterminism.everechoes.postal.DomainMembership;
import net.bluafolkloro.overdeterminism.everechoes.postal.MembershipState;
import net.bluafolkloro.overdeterminism.everechoes.postal.NodeRole;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalCodes;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalDistrict;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalDomain;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 封装服务端容器菜单及数据同步。
 */
public class PostBoxConfigMenu extends AbstractContainerMenu {
    /**
     * 保存 {@code pos} 对应的状态或依赖。
     */
    private final BlockPos pos;
    /**
     * 保存 {@code districtId} 对应的状态或依赖。
     */
    @Nullable
    private final UUID districtId;
    /**
     * 保存 {@code nodeRole} 对应的状态或依赖。
     */
    @Nullable
    private final NodeRole nodeRole;
    /**
     * 保存 {@code domainCode} 对应的状态或依赖。
     */
    @Nullable
    private final String domainCode;
    /**
     * 保存 {@code districtCode} 对应的状态或依赖。
     */
    @Nullable
    private final String districtCode;
    /**
     * 保存 {@code membershipState} 对应的状态或依赖。
     */
    @Nullable
    private final MembershipState membershipState;
    /**
     * 保存 {@code domainCodes} 对应的状态或依赖。
     */
    private final List<String> domainCodes;
    /**
     * 保存 {@code coveringDistricts} 对应的状态或依赖。
     */
    private final List<CoveringDistrict> coveringDistricts;

    /**
     * 创建并初始化当前对象。
     */
    public PostBoxConfigMenu(int containerId, Inventory playerInv, BlockPos pos, PostBoxBlockEntity postBox) {
        this(containerId, playerInv, snapshot(pos, postBox, playerInv.player.getUUID()));
    }

    /**
     * 创建并初始化当前对象。
     */
    public PostBoxConfigMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(containerId, playerInv, readSnapshot(extraData));
    }

    /**
     * 创建并初始化当前对象。
     */
    private PostBoxConfigMenu(int containerId, Inventory playerInv, Snapshot snapshot) {
        super(ModMenuTypes.POST_BOX_CONFIG_MENU.get(), containerId);
        this.pos = snapshot.pos;
        this.districtId = snapshot.districtId;
        this.nodeRole = snapshot.nodeRole;
        this.domainCode = snapshot.domainCode;
        this.districtCode = snapshot.districtCode;
        this.membershipState = snapshot.membershipState;
        this.domainCodes = snapshot.domainCodes;
        this.coveringDistricts = snapshot.coveringDistricts;
    }

    /**
     * 保存或编码 {@code writeOpeningData} 对应的业务操作。
     */
    public static void writeOpeningData(FriendlyByteBuf buffer, BlockPos pos, PostBoxBlockEntity postBox, PostalNetwork network, UUID actorId) {
        Snapshot snapshot = snapshot(pos, postBox, network, actorId);
        buffer.writeBlockPos(snapshot.pos);
        buffer.writeBoolean(snapshot.districtId != null);
        if (snapshot.districtId != null) {
            buffer.writeUUID(snapshot.districtId);
        }
        buffer.writeUtf(snapshot.nodeRole == null ? "" : snapshot.nodeRole.name(), 16);
        buffer.writeUtf(snapshot.domainCode == null ? "" : snapshot.domainCode, 8);
        buffer.writeUtf(snapshot.districtCode == null ? "" : snapshot.districtCode, 8);
        buffer.writeUtf(snapshot.membershipState == null ? "" : snapshot.membershipState.name(), 16);
        buffer.writeVarInt(snapshot.domainCodes.size());
        for (String code : snapshot.domainCodes) {
            buffer.writeUtf(code, 8);
        }
        buffer.writeVarInt(snapshot.coveringDistricts.size());
        for (CoveringDistrict district : snapshot.coveringDistricts) {
            buffer.writeUUID(district.districtId());
            buffer.writeUtf(district.label(), 16);
            buffer.writeUtf(district.domainCode() == null ? "" : district.domainCode(), 8);
            buffer.writeUtf(district.districtCode() == null ? "" : district.districtCode(), 8);
            buffer.writeBoolean(district.joinable());
            buffer.writeUtf(district.reasonKey() == null ? "" : district.reasonKey(), 64);
        }
    }

    /**
     * 执行 {@code pos} 对应的业务操作。
     */
    public BlockPos pos() {
        return pos;
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
     * 执行 {@code domainCodes} 对应的业务操作。
     */
    public List<String> domainCodes() {
        return domainCodes;
    }

    /**
     * 执行 {@code coveringDistricts} 对应的业务操作。
     */
    public List<CoveringDistrict> coveringDistricts() {
        return coveringDistricts;
    }

    /**
     * 添加或加入 {@code joinableDistricts} 对应的业务操作。
     */
    public List<CoveringDistrict> joinableDistricts() {
        return coveringDistricts.stream().filter(CoveringDistrict::joinable).toList();
    }

    /**
     * 执行 {@code blockedDistricts} 对应的业务操作。
     */
    public List<CoveringDistrict> blockedDistricts() {
        return coveringDistricts.stream().filter(district -> !district.joinable()).toList();
    }

    /**
     * 执行 {@code inDomain} 对应的业务操作。
     */
    public boolean inDomain() {
        return membershipState == MembershipState.ACTIVE;
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
        if (!(player.level().getBlockEntity(pos) instanceof PostBoxBlockEntity)) {
            return false;
        }
        double dx = player.getX() - (pos.getX() + 0.5);
        double dy = player.getY() - (pos.getY() + 0.5);
        double dz = player.getZ() - (pos.getZ() + 0.5);
        return dx * dx + dy * dy + dz * dz <= 64.0;
    }

    /**
     * 执行 {@code snapshot} 对应的业务操作。
     */
    private static Snapshot snapshot(BlockPos pos, PostBoxBlockEntity postBox, UUID actorId) {
        PostalNetwork network = postBox.getLevel() instanceof ServerLevel serverLevel
                ? PostalNetwork.get(serverLevel)
                : new PostalNetwork();
        return snapshot(pos, postBox, network, actorId);
    }

    /**
     * 执行 {@code snapshot} 对应的业务操作。
     */
    private static Snapshot snapshot(BlockPos pos, PostBoxBlockEntity postBox, PostalNetwork network, UUID actorId) {
        List<CoveringDistrict> covering = new ArrayList<>();
        if (postBox.districtId() == null && postBox.getLevel() != null) {
            for (PostalNetwork.CoverageMatch match : network.coverageMatches(postBox.nodeId(), postBox.actionContext(actorId))) {
                PostalDistrict district = match.district();
                DomainMembership membership = network.membership(district.districtId()).orElse(null);
                PostalDomain domain = membership == null ? null : network.domain(membership.domainId()).orElse(null);
                boolean showOutward = membership != null
                        && membership.isDeliveryEndpoint()
                        && domain != null
                        && domain.lifecycle() != DomainLifecycle.HISTORICAL;
                String domainCode = showOutward ? domain.domainCode() : null;
                String districtCode = showOutward ? membership.districtCode() : null;
                String label = domainCode != null && districtCode != null
                        ? PostalCodes.formatOutward(domainCode, districtCode)
                        : district.districtId().toString().substring(0, 8);
                covering.add(new CoveringDistrict(
                        district.districtId(),
                        label,
                        domainCode,
                        districtCode,
                        match.joinable(),
                        match.reasonKey()
                ));
            }
            covering.sort(Comparator
                    .comparing((CoveringDistrict district) -> district.domainCode() == null ? "" : district.domainCode())
                    .thenComparing(district -> district.districtCode() == null ? "" : district.districtCode())
                    .thenComparing(CoveringDistrict::label)
                    .thenComparing(district -> district.districtId().toString()));
        }
        return new Snapshot(
                pos,
                postBox.districtId(),
                postBox.nodeRole(),
                postBox.domainCode(),
                postBox.districtCode(),
                postBox.membershipState(),
                network.liveDomainCodes(),
                covering
        );
    }

    /**
     * 获取或解析 {@code readSnapshot} 对应的业务操作。
     */
    private static Snapshot readSnapshot(FriendlyByteBuf extraData) {
        BlockPos pos = extraData.readBlockPos();
        UUID districtId = extraData.readBoolean() ? extraData.readUUID() : null;
        String role = extraData.readUtf(16);
        String domainCode = emptyToNull(extraData.readUtf(8));
        String districtCode = emptyToNull(extraData.readUtf(8));
        String state = extraData.readUtf(16);
        int domainCount = extraData.readVarInt();
        List<String> domainCodes = new ArrayList<>();
        for (int index = 0; index < domainCount; index++) {
            domainCodes.add(extraData.readUtf(8));
        }
        int coverageCount = extraData.readVarInt();
        List<CoveringDistrict> covering = new ArrayList<>();
        for (int index = 0; index < coverageCount; index++) {
            covering.add(new CoveringDistrict(
                    extraData.readUUID(),
                    extraData.readUtf(16),
                    emptyToNull(extraData.readUtf(8)),
                    emptyToNull(extraData.readUtf(8)),
                    extraData.readBoolean(),
                    emptyToNull(extraData.readUtf(64))
            ));
        }
        return new Snapshot(
                pos,
                districtId,
                role.isEmpty() ? null : NodeRole.valueOf(role),
                domainCode,
                districtCode,
                state.isEmpty() ? null : MembershipState.valueOf(state),
                domainCodes,
                covering
        );
    }

    /**
     * 执行 {@code emptyToNull} 对应的业务操作。
     */
    @Nullable
    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    /**
     * 封装不可变业务数据。
     */
    public record CoveringDistrict(
            /**
             * 保存 {@code districtId} 对应的状态或依赖。
             */
            @Nullable UUID districtId,
            /**
             * 保存 {@code label} 对应的状态或依赖。
             */
            String label,
            /**
             * 保存 {@code domainCode} 对应的状态或依赖。
             */
            @Nullable String domainCode,
            /**
             * 保存 {@code districtCode} 对应的状态或依赖。
             */
            @Nullable String districtCode,
            /**
             * 保存 {@code joinable} 对应的状态或依赖。
             */
            boolean joinable,
            /**
             * 保存 {@code reasonKey} 对应的状态或依赖。
             */
            @Nullable String reasonKey
    ) {
    }

    /**
     * 封装不可变业务数据。
     */
    private record Snapshot(
            /**
             * 保存 {@code pos} 对应的状态或依赖。
             */
            BlockPos pos,
            /**
             * 保存 {@code districtId} 对应的状态或依赖。
             */
            @Nullable UUID districtId,
            /**
             * 保存 {@code nodeRole} 对应的状态或依赖。
             */
            @Nullable NodeRole nodeRole,
            /**
             * 保存 {@code domainCode} 对应的状态或依赖。
             */
            @Nullable String domainCode,
            /**
             * 保存 {@code districtCode} 对应的状态或依赖。
             */
            @Nullable String districtCode,
            /**
             * 保存 {@code membershipState} 对应的状态或依赖。
             */
            @Nullable MembershipState membershipState,
            /**
             * 保存 {@code domainCodes} 对应的状态或依赖。
             */
            List<String> domainCodes,
            /**
             * 保存 {@code coveringDistricts} 对应的状态或依赖。
             */
            List<CoveringDistrict> coveringDistricts
    ) {
    }
}
