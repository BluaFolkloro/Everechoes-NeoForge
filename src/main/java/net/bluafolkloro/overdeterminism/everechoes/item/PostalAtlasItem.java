package net.bluafolkloro.overdeterminism.everechoes.item;

import net.bluafolkloro.overdeterminism.everechoes.block.PostBoxBlock;
import net.bluafolkloro.overdeterminism.everechoes.block.entity.PostBoxBlockEntity;
import net.bluafolkloro.overdeterminism.everechoes.menu.PostalAtlasMenu;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalAtlasLimits;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalAtlasSnapshot;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalChunk;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.List;
import java.util.UUID;

/**
 * 封装物品行为与玩家交互。
 */
public class PostalAtlasItem extends Item {
    /**
     * 创建并初始化当前对象。
     */
    public PostalAtlasItem(Properties properties) {
        super(properties);
    }

    /**
     * 执行 {@code useOn} 对应的业务操作。
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockState state = level.getBlockState(clicked);
        if (!(state.getBlock() instanceof PostBoxBlock)) {
            return InteractionResult.PASS;
        }

        BlockPos pos = state.getValue(PostBoxBlock.HALF) == DoubleBlockHalf.UPPER ? clicked.below() : clicked;
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Player player = context.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }
        if (!(serverLevel.getBlockEntity(pos) instanceof PostBoxBlockEntity postBox)) {
            return InteractionResult.FAIL;
        }

        postBox.syncFromNetwork(serverLevel);
        UUID districtId = postBox.districtId();
        if (districtId == null) {
            serverPlayer.displayClientMessage(Component.translatable("message.everechoes.atlas.need_district"), true);
            return InteractionResult.FAIL;
        }

        PostalNetwork network = PostalNetwork.get(serverLevel);
        PostalChunk chunk = PostalChunk.at(serverLevel.dimension().location(), pos);
        int originX = chunk.x() - PostalAtlasLimits.DEFAULT_WINDOW_WIDTH / 2;
        int originZ = chunk.z() - PostalAtlasLimits.DEFAULT_WINDOW_HEIGHT / 2;
        PostalAtlasSnapshot snapshot = network.atlasWindow(
                districtId,
                serverLevel.dimension().location(),
                originX,
                originZ,
                PostalAtlasLimits.DEFAULT_WINDOW_WIDTH,
                PostalAtlasLimits.DEFAULT_WINDOW_HEIGHT,
                true
        ).orElse(null);
        if (snapshot == null) {
            serverPlayer.displayClientMessage(Component.translatable("message.everechoes.coverage.unknown_district"), true);
            return InteractionResult.FAIL;
        }

        UUID nodeId = postBox.nodeId();
        serverPlayer.openMenu(
                new SimpleMenuProvider(
                        (containerId, inventory, menuPlayer) -> new PostalAtlasMenu(
                                containerId,
                                inventory,
                                pos,
                                nodeId,
                                districtId,
                                snapshot
                        ),
                        Component.translatable("gui.everechoes.atlas.title")
                ),
                buffer -> PostalAtlasMenu.writeOpeningData(buffer, pos, nodeId, districtId, snapshot)
        );
        return InteractionResult.CONSUME;
    }

    /**
     * 执行 {@code appendHoverText} 对应的业务操作。
     */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.everechoes.postal_atlas.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
