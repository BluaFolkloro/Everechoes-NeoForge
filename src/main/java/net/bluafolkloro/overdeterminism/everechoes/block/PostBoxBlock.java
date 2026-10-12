package net.bluafolkloro.overdeterminism.everechoes.block;

import com.mojang.serialization.MapCodec;
import net.bluafolkloro.overdeterminism.everechoes.block.entity.PostBoxBlockEntity;
import net.bluafolkloro.overdeterminism.everechoes.menu.PostBoxConfigMenu;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * 封装方块行为、形态与交互规则。
 */
public class PostBoxBlock extends BaseEntityBlock {
    /**
     * 定义 {@code CODEC} 常量。
     */
    public static final MapCodec<PostBoxBlock> CODEC = simpleCodec(PostBoxBlock::new);
    /**
     * 定义 {@code FACING} 常量。
     */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /**
     * 定义 {@code HALF} 常量。
     */
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    /**
     * 定义 {@code LOWER_SHAPE} 常量。
     */
    private static final VoxelShape LOWER_SHAPE = Block.box(2, 0, 2, 14, 16, 14);
    /**
     * 定义 {@code UPPER_SHAPE} 常量。
     */
    private static final VoxelShape UPPER_SHAPE = Block.box(2, 0, 2, 14, 10, 14);

    /**
     * 执行 {@code codec} 对应的业务操作。
     */
    @Override
    public MapCodec<? extends PostBoxBlock> codec() {
        return CODEC;
    }

    /**
     * 创建 {@code newBlockEntity} 对应的业务操作。
     */
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // Only the lower half owns the postbox inventory.
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            return null;
        }

        return new PostBoxBlockEntity(pos, state);
    }

    /**
     * 创建并初始化当前对象。
     */
    public PostBoxBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(HALF, DoubleBlockHalf.LOWER)
        );
    }

    /**
     * 执行 {@code useWithoutItem} 对应的业务操作。
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // Upper-half clicks open the lower-half block entity.
        BlockPos menuPos = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        BlockEntity be = level.getBlockEntity(menuPos);
        if (be instanceof PostBoxBlockEntity postBox && player instanceof ServerPlayer serverPlayer) {
            if (player.isShiftKeyDown() || !postBox.hasActiveMembership()) {
                openConfig(serverPlayer, (ServerLevel) level, menuPos, postBox);
            } else {
                serverPlayer.openMenu(postBox, buf -> buf.writeBlockPos(menuPos));
            }
        }

        return InteractionResult.CONSUME;
    }

    /**
     * 打开 {@code openConfig} 对应的业务操作。
     */
    public static void openConfig(ServerPlayer player, ServerLevel level, BlockPos pos, PostBoxBlockEntity postBox) {
        PostalNetwork network = PostalNetwork.get(level);
        postBox.syncFromNetwork(level);
        player.openMenu(
                new SimpleMenuProvider(
                        (containerId, inventory, menuPlayer) -> new PostBoxConfigMenu(
                                containerId,
                                inventory,
                                pos,
                                postBox
                        ),
                        Component.translatable("gui.everechoes.post_box.config")
                ),
                buf -> PostBoxConfigMenu.writeOpeningData(buf, pos, postBox, network, player.getUUID())
        );
    }

    /**
     * 创建 {@code createBlockStateDefinition} 对应的业务操作。
     */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING);
    }

    /**
     * 获取或解析 {@code getStateForPlacement} 对应的业务操作。
     */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos blockpos = context.getClickedPos();
        Level level = context.getLevel();
        if (blockpos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(blockpos.above()).canBeReplaced(context)) {
            return this.defaultBlockState()
                    .setValue(FACING, context.getHorizontalDirection().getOpposite())
                    .setValue(HALF, DoubleBlockHalf.LOWER);
        } else {
            return null;
        }
    }

    /**
     * 设置或写入 {@code setPlacedBy} 对应的业务操作。
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }

    /**
     * 判断 {@code canSurvive} 对应的业务操作。
     */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            return true;
        } else {
            BlockPos below = pos.below();
            BlockState belowState = level.getBlockState(below);
            return belowState.is(this) && belowState.getValue(HALF) == DoubleBlockHalf.LOWER;
        }
    }

    /**
     * 更新 {@code updateShape} 对应的业务操作。
     */
    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        DoubleBlockHalf half = state.getValue(HALF);

        if (facing == Direction.UP && half == DoubleBlockHalf.LOWER) {
            if (!facingState.is(this) || facingState.getValue(HALF) != DoubleBlockHalf.UPPER) {
                return Blocks.AIR.defaultBlockState();
            }
        }

        if (facing == Direction.DOWN && half == DoubleBlockHalf.UPPER) {
            if (!facingState.is(this) || facingState.getValue(HALF) != DoubleBlockHalf.LOWER) {
                return Blocks.AIR.defaultBlockState();
            }
        }

        return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    /**
     * 执行 {@code playerWillDestroy} 对应的业务操作。
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (half == DoubleBlockHalf.LOWER) {
            return super.playerWillDestroy(level, pos, state, player);
        }

        BlockPos otherPos = pos.below();
        BlockState otherState = level.getBlockState(otherPos);

        if (otherState.is(this) && otherState.getValue(HALF) == DoubleBlockHalf.LOWER) {
            if (!level.isClientSide && !player.isCreative()) {
                Block.dropResources(otherState, level, otherPos, level.getBlockEntity(otherPos), player, player.getMainHandItem());
            }

            level.removeBlock(otherPos, false);
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * 处理 {@code onRemove} 对应的业务操作。
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // Contents are stored only on the lower half, so drop them from there once.
        if (!state.is(newState.getBlock()) && state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PostBoxBlockEntity postBox) {
                if (level instanceof ServerLevel serverLevel) {
                    postBox.unregister(serverLevel);
                }
                Containers.dropContents(level, pos, postBox.getItems());
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * 获取或解析 {@code getShape} 对应的业务操作。
     */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER_SHAPE : UPPER_SHAPE;
    }

    /**
     * 获取或解析 {@code getRenderShape} 对应的业务操作。
     */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
