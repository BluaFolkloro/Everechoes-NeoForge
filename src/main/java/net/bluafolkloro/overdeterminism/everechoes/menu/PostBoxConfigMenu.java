package net.bluafolkloro.overdeterminism.everechoes.menu;

import net.bluafolkloro.overdeterminism.everechoes.block.entity.PostBoxBlockEntity;
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
import java.util.List;
import java.util.UUID;

public class PostBoxConfigMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    @Nullable
    private final UUID districtId;
    @Nullable
    private final NodeRole nodeRole;
    @Nullable
    private final String domainCode;
    @Nullable
    private final String districtCode;
    @Nullable
    private final MembershipState membershipState;
    private final List<String> domainCodes;
    private final List<NearbyDistrict> nearbyDistricts;

    public PostBoxConfigMenu(int containerId, Inventory playerInv, BlockPos pos, PostBoxBlockEntity postBox) {
        this(containerId, playerInv, snapshot(pos, postBox));
    }

    public PostBoxConfigMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(containerId, playerInv, readSnapshot(extraData));
    }

    private PostBoxConfigMenu(int containerId, Inventory playerInv, Snapshot snapshot) {
        super(ModMenuTypes.POST_BOX_CONFIG_MENU.get(), containerId);
        this.pos = snapshot.pos;
        this.districtId = snapshot.districtId;
        this.nodeRole = snapshot.nodeRole;
        this.domainCode = snapshot.domainCode;
        this.districtCode = snapshot.districtCode;
        this.membershipState = snapshot.membershipState;
        this.domainCodes = snapshot.domainCodes;
        this.nearbyDistricts = snapshot.nearbyDistricts;
    }

    public static void writeOpeningData(FriendlyByteBuf buffer, BlockPos pos, PostBoxBlockEntity postBox, PostalNetwork network) {
        Snapshot snapshot = snapshot(pos, postBox, network);
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
        buffer.writeVarInt(snapshot.nearbyDistricts.size());
        for (NearbyDistrict nearby : snapshot.nearbyDistricts) {
            buffer.writeUUID(nearby.districtId());
            buffer.writeUtf(nearby.label(), 16);
        }
    }

    public BlockPos pos() {
        return pos;
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

    public List<String> domainCodes() {
        return domainCodes;
    }

    public List<NearbyDistrict> nearbyDistricts() {
        return nearbyDistricts;
    }

    public boolean inDomain() {
        return membershipState == MembershipState.ACTIVE;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

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

    private static Snapshot snapshot(BlockPos pos, PostBoxBlockEntity postBox) {
        PostalNetwork network = postBox.getLevel() instanceof ServerLevel serverLevel
                ? PostalNetwork.get(serverLevel)
                : new PostalNetwork();
        return snapshot(pos, postBox, network);
    }

    private static Snapshot snapshot(BlockPos pos, PostBoxBlockEntity postBox, PostalNetwork network) {
        List<NearbyDistrict> nearby = new ArrayList<>();
        if (postBox.districtId() == null && postBox.getLevel() != null) {
            for (PostalDistrict district : network.nearbyJoinableDistricts(postBox.getLevel().dimension().location(), pos)) {
                DomainMembership membership = network.membership(district.districtId()).orElse(null);
                PostalDomain domain = membership == null ? null : network.domain(membership.domainId()).orElse(null);
                String label = domain != null && membership != null
                        ? PostalCodes.formatOutward(domain.domainCode(), membership.districtCode())
                        : district.districtId().toString().substring(0, 8);
                nearby.add(new NearbyDistrict(district.districtId(), label));
            }
        }
        return new Snapshot(
                pos,
                postBox.districtId(),
                postBox.nodeRole(),
                postBox.domainCode(),
                postBox.districtCode(),
                postBox.membershipState(),
                network.liveDomainCodes(),
                nearby
        );
    }

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
        int nearbyCount = extraData.readVarInt();
        List<NearbyDistrict> nearby = new ArrayList<>();
        for (int index = 0; index < nearbyCount; index++) {
            nearby.add(new NearbyDistrict(extraData.readUUID(), extraData.readUtf(16)));
        }
        return new Snapshot(
                pos,
                districtId,
                role.isEmpty() ? null : NodeRole.valueOf(role),
                domainCode,
                districtCode,
                state.isEmpty() ? null : MembershipState.valueOf(state),
                domainCodes,
                nearby
        );
    }

    @Nullable
    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    public record NearbyDistrict(UUID districtId, String label) {
    }

    private record Snapshot(
            BlockPos pos,
            @Nullable UUID districtId,
            @Nullable NodeRole nodeRole,
            @Nullable String domainCode,
            @Nullable String districtCode,
            @Nullable MembershipState membershipState,
            List<String> domainCodes,
            List<NearbyDistrict> nearbyDistricts
    ) {
    }
}
