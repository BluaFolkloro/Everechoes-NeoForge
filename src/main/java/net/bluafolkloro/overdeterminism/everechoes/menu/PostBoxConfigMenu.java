package net.bluafolkloro.overdeterminism.everechoes.menu;

import net.bluafolkloro.overdeterminism.everechoes.block.entity.PostBoxBlockEntity;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class PostBoxConfigMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    @Nullable
    private final String domainId;
    @Nullable
    private final String districtId;
    private final List<String> domainIds;

    public PostBoxConfigMenu(
            int containerId,
            Inventory playerInv,
            BlockPos pos,
            @Nullable String domainId,
            @Nullable String districtId,
            List<String> domainIds
    ) {
        super(ModMenuTypes.POST_BOX_CONFIG_MENU.get(), containerId);
        this.pos = pos;
        this.domainId = domainId;
        this.districtId = districtId;
        this.domainIds = List.copyOf(domainIds);
    }

    public PostBoxConfigMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(
                containerId,
                playerInv,
                extraData.readBlockPos(),
                readOptionalUtf(extraData),
                readOptionalUtf(extraData),
                readDomains(extraData)
        );
    }

    public static void writeOpeningData(FriendlyByteBuf buffer, BlockPos pos, PostBoxBlockEntity postBox, PostalNetwork network) {
        buffer.writeBlockPos(pos);
        writeOptionalUtf(buffer, postBox.domainId());
        writeOptionalUtf(buffer, postBox.districtId());
        List<String> domains = network.domainIds();
        buffer.writeVarInt(domains.size());
        for (String domain : domains) {
            buffer.writeUtf(domain, 3);
        }
    }

    public BlockPos pos() {
        return pos;
    }

    @Nullable
    public String domainId() {
        return domainId;
    }

    @Nullable
    public String districtId() {
        return districtId;
    }

    public List<String> domainIds() {
        return domainIds;
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

    private static void writeOptionalUtf(FriendlyByteBuf buffer, @Nullable String value) {
        buffer.writeUtf(value == null ? "" : value, 8);
    }

    @Nullable
    private static String readOptionalUtf(FriendlyByteBuf buffer) {
        String value = buffer.readUtf(8);
        return value.isEmpty() ? null : value;
    }

    private static List<String> readDomains(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<String> domains = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            domains.add(buffer.readUtf(3));
        }
        return domains;
    }
}
