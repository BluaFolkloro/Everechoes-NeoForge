package net.bluafolkloro.overdeterminism.everechoes.block;

import com.mojang.serialization.MapCodec;
import net.bluafolkloro.overdeterminism.everechoes.block.entity.MailBoxBlockEntity;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 封装方块行为、形态与交互规则。
 */
public class MailBoxBlock extends BaseEntityBlock {
    /**
     * 定义 {@code CODEC} 常量。
     */
    public static final MapCodec<MailBoxBlock> CODEC = simpleCodec(MailBoxBlock::new);
    /**
     * 定义 {@code FACING} 常量。
     */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /**
     * 定义 {@code STYLE} 常量。
     */
    public static final EnumProperty<MailBoxStyle> STYLE = EnumProperty.create("style", MailBoxStyle.class);
    /**
     * 定义 {@code HALF} 常量。
     */
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    /**
     * 定义 {@code CHAIN_AXIS} 常量。
     */
    public static final EnumProperty<Direction.Axis> CHAIN_AXIS = EnumProperty.create(
            "chain_axis", Direction.Axis.class, Direction.Axis::isHorizontal);

    /**
     * 定义 {@code POST_LOWER_NORTH} 常量。
     */
    private static final VoxelShape POST_LOWER_NORTH = Block.box(6, 0, 6, 10, 16, 10);
    /**
     * 定义 {@code POST_UPPER_COLLISION_NORTH} 常量。
     */
    private static final VoxelShape POST_UPPER_COLLISION_NORTH = Block.box(2, 0, 2, 14, 8, 14);
    /**
     * 定义 {@code POST_UPPER_OUTLINE_NORTH} 常量。
     */
    private static final VoxelShape POST_UPPER_OUTLINE_NORTH = Shapes.or(
            Block.box(2, 0, 2, 14, 8, 14), Block.box(0, 5, 1, 16, 12, 15));
    /**
     * 定义 {@code DESK_COLLISION_NORTH} 常量。
     */
    private static final VoxelShape DESK_COLLISION_NORTH = Block.box(2, 0, 2, 14, 8, 14);
    /**
     * 定义 {@code DESK_OUTLINE_NORTH} 常量。
     */
    private static final VoxelShape DESK_OUTLINE_NORTH = Shapes.or(
            Block.box(2, 0, 2, 14, 8, 14), Block.box(0, 5, 1, 16, 12, 15));
    /**
     * 定义 {@code WALL_COLLISION_NORTH} 常量。
     */
    private static final VoxelShape WALL_COLLISION_NORTH = Block.box(2, 5, 4, 14, 13, 16);
    /**
     * 定义 {@code WALL_OUTLINE_NORTH} 常量。
     */
    private static final VoxelShape WALL_OUTLINE_NORTH = Shapes.or(
            Block.box(2, 5, 4, 14, 13, 16), Block.box(0, 10, 3, 16, 16, 16));
    /**
     * 定义 {@code HANGING_COLLISION_NORTH} 常量。
     */
    private static final VoxelShape HANGING_COLLISION_NORTH = Block.box(2, 5, 2, 14, 13, 14);
    /**
     * 定义 {@code HANGING_OUTLINE_NORTH} 常量。
     */
    private static final VoxelShape HANGING_OUTLINE_NORTH = Shapes.or(
            Block.box(2, 5, 2, 14, 13, 14), Block.box(0, 10, 1, 16, 17, 15));
    /**
     * 定义 {@code HANGING_RAISED_COLLISION_NORTH} 常量。
     */
    private static final VoxelShape HANGING_RAISED_COLLISION_NORTH = Block.box(2, 12.5, 2, 14, 20.5, 14);
    /**
     * 定义 {@code HANGING_RAISED_OUTLINE_NORTH} 常量。
     */
    private static final VoxelShape HANGING_RAISED_OUTLINE_NORTH = Shapes.or(
            Block.box(2, 12.5, 2, 14, 20.5, 14), Block.box(0, 17.5, 1, 16, 23.5, 15));

    /**
     * 定义 {@code OUTLINES} 常量。
     */
    private static final VoxelShape[][] OUTLINES = new VoxelShape[MailBoxStyle.values().length][4];
    /**
     * 定义 {@code COLLISIONS} 常量。
     */
    private static final VoxelShape[][] COLLISIONS = new VoxelShape[MailBoxStyle.values().length][4];
    /**
     * 定义 {@code POST_UPPER_OUTLINES} 常量。
     */
    private static final VoxelShape[] POST_UPPER_OUTLINES = new VoxelShape[4];
    /**
     * 定义 {@code POST_UPPER_COLLISIONS} 常量。
     */
    private static final VoxelShape[] POST_UPPER_COLLISIONS = new VoxelShape[4];

    static {
        fillFacingShapes(MailBoxStyle.POST, POST_LOWER_NORTH, POST_LOWER_NORTH);
        fillFacingShapes(MailBoxStyle.DESK, DESK_OUTLINE_NORTH, DESK_COLLISION_NORTH);
        fillFacingShapes(MailBoxStyle.WALL, WALL_OUTLINE_NORTH, WALL_COLLISION_NORTH);
        fillFacingShapes(MailBoxStyle.HANGING, HANGING_OUTLINE_NORTH, HANGING_COLLISION_NORTH);
        fillFacingShapes(MailBoxStyle.HANGING_RAISED, HANGING_RAISED_OUTLINE_NORTH, HANGING_RAISED_COLLISION_NORTH);
        fillFacingShapes(MailBoxStyle.HANGING_CHAINED, HANGING_RAISED_OUTLINE_NORTH, HANGING_RAISED_COLLISION_NORTH);
        for (/** 保存 {@code facing} 对应的状态或依赖。 */ Direction facing : Direction.Plane.HORIZONTAL) {
            /**
             * 保存 {@code index} 对应的状态或依赖。
             */
            int index = facing.get2DDataValue();
            POST_UPPER_OUTLINES[index] = rotateShape(POST_UPPER_OUTLINE_NORTH, facing);
            POST_UPPER_COLLISIONS[index] = rotateShape(POST_UPPER_COLLISION_NORTH, facing);
        }
    }

    /**
     * 创建并初始化当前对象。
     */
    public MailBoxBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(STYLE, MailBoxStyle.POST)
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(CHAIN_AXIS, Direction.Axis.X));
    }

    /**
     * 执行 {@code codec} 对应的业务操作。
     */
    @Override
    public MapCodec<? extends MailBoxBlock> codec() {
        return CODEC;
    }

    /**
     * 创建 {@code newBlockEntity} 对应的业务操作。
     */
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? null : new MailBoxBlockEntity(pos, state);
    }

    /**
     * 获取或解析 {@code getRenderShape} 对应的业务操作。
     */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * 创建 {@code createBlockStateDefinition} 对应的业务操作。
     */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STYLE, HALF, CHAIN_AXIS);
    }

    /**
     * 获取或解析 {@code getStateForPlacement} 对应的业务操作。
     */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        BlockState state;

        if (clickedFace == Direction.DOWN) {
            BlockState support = level.getBlockState(pos.above());
            if (!isRaisedHangingSupport(support) && !isVerticalChain(support)) {
                return null;
            }
            state = defaultBlockState()
                    .setValue(STYLE, isVerticalChain(support) ? MailBoxStyle.HANGING : MailBoxStyle.HANGING_RAISED)
                    .setValue(FACING, context.getHorizontalDirection().getOpposite())
                    .setValue(CHAIN_AXIS, raisedSupportAxis(support));
        } else if (clickedFace.getAxis().isHorizontal()) {
            state = defaultBlockState()
                    .setValue(STYLE, MailBoxStyle.WALL)
                    .setValue(FACING, clickedFace);
        } else {
            MailBoxStyle style = context.isSecondaryUseActive() ? MailBoxStyle.DESK : MailBoxStyle.POST;
            state = defaultBlockState()
                    .setValue(STYLE, style)
                    .setValue(FACING, context.getHorizontalDirection().getOpposite());
            if (style == MailBoxStyle.POST
                    && (pos.getY() >= level.getMaxBuildHeight() - 1
                    || !level.getBlockState(pos.above()).canBeReplaced(context))) {
                return null;
            }
        }

        return survivesAtPlacement(state, level, pos) ? state : null;
    }

    /**
     * 设置或写入 {@code setPlacedBy} 对应的业务操作。
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (state.getValue(STYLE) == MailBoxStyle.POST && state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
        }
    }

    /**
     * 执行 {@code survivesAtPlacement} 对应的业务操作。
     */
    private boolean survivesAtPlacement(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(STYLE) == MailBoxStyle.POST) {
            BlockPos below = pos.below();
            return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
        }
        return canSurvive(state, level, pos);
    }

    /**
     * 判断 {@code canSurvive} 对应的业务操作。
     */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        MailBoxStyle style = state.getValue(STYLE);
        DoubleBlockHalf half = state.getValue(HALF);
        if (half == DoubleBlockHalf.UPPER) {
            BlockState below = level.getBlockState(pos.below());
            return style == MailBoxStyle.POST && below.is(this)
                    && below.getValue(STYLE) == MailBoxStyle.POST
                    && below.getValue(HALF) == DoubleBlockHalf.LOWER;
        }
        if (style == MailBoxStyle.POST) {
            return true;
        }
        if (style == MailBoxStyle.WALL) {
            Direction facing = state.getValue(FACING);
            BlockPos supportPos = pos.relative(facing.getOpposite());
            return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, facing);
        }
        if (style == MailBoxStyle.HANGING) {
            return isVerticalChain(level.getBlockState(pos.above()));
        }
        if (style == MailBoxStyle.HANGING_RAISED || style == MailBoxStyle.HANGING_CHAINED) {
            return isRaisedHangingSupport(level.getBlockState(pos.above()));
        }
        if (style == MailBoxStyle.DESK) {
            return true;
        }
        return false;
    }

    /**
     * 更新 {@code updateShape} 对应的业务操作。
     */
    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        if (state.getValue(STYLE) == MailBoxStyle.POST) {
            DoubleBlockHalf half = state.getValue(HALF);
            if (half == DoubleBlockHalf.LOWER && facing == Direction.UP
                    && (!facingState.is(this)
                    || facingState.getValue(STYLE) != MailBoxStyle.POST
                    || facingState.getValue(HALF) != DoubleBlockHalf.UPPER)) {
                return Blocks.AIR.defaultBlockState();
            }
            if (half == DoubleBlockHalf.UPPER && facing == Direction.DOWN
                    && (!facingState.is(this)
                    || facingState.getValue(STYLE) != MailBoxStyle.POST
                    || facingState.getValue(HALF) != DoubleBlockHalf.LOWER)) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return state.canSurvive(level, currentPos)
                ? super.updateShape(state, facing, facingState, level, currentPos, facingPos)
                : Blocks.AIR.defaultBlockState();
    }

    /**
     * 执行 {@code playerWillDestroy} 对应的业务操作。
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (state.getValue(STYLE) != MailBoxStyle.POST || state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            return super.playerWillDestroy(level, pos, state, player);
        }

        BlockPos lowerPos = pos.below();
        BlockState lower = level.getBlockState(lowerPos);
        if (lower.is(this) && lower.getValue(STYLE) == MailBoxStyle.POST
                && lower.getValue(HALF) == DoubleBlockHalf.LOWER) {
            if (!level.isClientSide && !player.isCreative()) {
                Block.dropResources(lower, level, lowerPos, level.getBlockEntity(lowerPos), player, player.getMainHandItem());
            }
            level.removeBlock(lowerPos, false);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * 下半格被替换或拆除时，在方块实体卸下前将端点标为离线。
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // BE 只存在于 LOWER；POST 上半拆除已由 playerWillDestroy 先拆 LOWER。
        if (!state.is(newState.getBlock()) && state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MailBoxBlockEntity mailBox && level instanceof ServerLevel serverLevel) {
                UUID id = mailBox.mailboxId();
                if (id != null) {
                    PostalNetwork.get(serverLevel).markEndpointOffline(id);
                }
            }
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * 执行 {@code useWithoutItem} 对应的业务操作。
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        // No menu, inventory, key check, or postal-code claim flow exists in the first milestone.
        return InteractionResult.PASS;
    }

    /**
     * 执行 {@code useItemOn} 对应的业务操作。
     */
    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult
    ) {
        if (stack.is(Items.CHAIN)
                && state.getValue(HALF) == DoubleBlockHalf.LOWER
                && state.getValue(STYLE) == MailBoxStyle.HANGING_RAISED
                && hitResult.getDirection().getAxis().isHorizontal()) {
            if (!level.isClientSide) {
                level.setBlock(pos, state
                        .setValue(STYLE, MailBoxStyle.HANGING_CHAINED)
                        .setValue(CHAIN_AXIS, hitResult.getDirection().getAxis()), 3);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /**
     * 获取或解析 {@code getDrops} 对应的业务操作。
     */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER
                && state.getValue(STYLE) == MailBoxStyle.HANGING_CHAINED) {
            drops.add(new ItemStack(Items.CHAIN));
        }
        return drops;
    }

    /**
     * 执行 {@code rotate} 对应的业务操作。
     */
    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        Direction chainDirection = state.getValue(CHAIN_AXIS) == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        return state
                .setValue(FACING, rotation.rotate(state.getValue(FACING)))
                .setValue(CHAIN_AXIS, rotation.rotate(chainDirection).getAxis());
    }

    /**
     * 执行 {@code mirror} 对应的业务操作。
     */
    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /**
     * 获取或解析 {@code getShape} 对应的业务操作。
     */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape(OUTLINES, state);
    }

    /**
     * 获取或解析 {@code getCollisionShape} 对应的业务操作。
     */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape(COLLISIONS, state);
    }

    /**
     * 获取或解析 {@code getOcclusionShape} 对应的业务操作。
     */
    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    /**
     * 获取或解析 {@code getVisualShape} 对应的业务操作。
     */
    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /**
     * 判断 {@code isPathfindable} 对应的业务操作。
     */
    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    /**
     * 执行 {@code shape} 对应的业务操作。
     */
    private static VoxelShape shape(VoxelShape[][] shapes, BlockState state) {
        int facing = state.getValue(FACING).get2DDataValue();
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            if (state.getValue(STYLE) != MailBoxStyle.POST) {
                return Shapes.empty();
            }
            return shapes == OUTLINES ? POST_UPPER_OUTLINES[facing] : POST_UPPER_COLLISIONS[facing];
        }
        return shapes[state.getValue(STYLE).ordinal()][facing];
    }

    /**
     * 执行 {@code fillFacingShapes} 对应的业务操作。
     */
    private static void fillFacingShapes(MailBoxStyle style, VoxelShape outlineNorth, VoxelShape collisionNorth) {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            int index = facing.get2DDataValue();
            OUTLINES[style.ordinal()][index] = rotateShape(outlineNorth, facing);
            COLLISIONS[style.ordinal()][index] = rotateShape(collisionNorth, facing);
        }
    }

    /**
     * 判断 {@code isVerticalChain} 对应的业务操作。
     */
    private static boolean isVerticalChain(BlockState state) {
        return state.is(Blocks.CHAIN) && state.hasProperty(BlockStateProperties.AXIS)
                && state.getValue(BlockStateProperties.AXIS) == Direction.Axis.Y;
    }

    /**
     * 判断 {@code isHorizontalChain} 对应的业务操作。
     */
    private static boolean isHorizontalChain(BlockState state) {
        return state.is(Blocks.CHAIN) && state.hasProperty(BlockStateProperties.AXIS)
                && state.getValue(BlockStateProperties.AXIS) != Direction.Axis.Y;
    }

    /**
     * 判断 {@code isRaisedHangingSupport} 对应的业务操作。
     */
    private static boolean isRaisedHangingSupport(BlockState state) {
        return isHorizontalChain(state)
                || isHorizontalEndRod(state)
                || state.is(ContainerBlocks.MAIL_BOX.get())
                && state.getValue(HALF) == DoubleBlockHalf.LOWER
                && state.getValue(STYLE) == MailBoxStyle.HANGING_CHAINED;
    }

    /**
     * 判断 {@code isHorizontalEndRod} 对应的业务操作。
     */
    private static boolean isHorizontalEndRod(BlockState state) {
        return state.is(Blocks.END_ROD) && state.hasProperty(BlockStateProperties.FACING)
                && state.getValue(BlockStateProperties.FACING).getAxis().isHorizontal();
    }

    /**
     * 执行 {@code raisedSupportAxis} 对应的业务操作。
     */
    private static Direction.Axis raisedSupportAxis(BlockState state) {
        if (isHorizontalChain(state)) {
            return state.getValue(BlockStateProperties.AXIS);
        }
        if (isHorizontalEndRod(state)) {
            return state.getValue(BlockStateProperties.FACING).getAxis();
        }
        if (state.is(ContainerBlocks.MAIL_BOX.get()) && state.hasProperty(CHAIN_AXIS)) {
            return state.getValue(CHAIN_AXIS);
        }
        return Direction.Axis.X;
    }

    /**
     * 执行 {@code rotateShape} 对应的业务操作。
     */
    private static VoxelShape rotateShape(VoxelShape shape, Direction facing) {
        if (facing == Direction.NORTH) {
            return shape;
        }

        VoxelShape[] rotated = {Shapes.empty()};
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            VoxelShape box = switch (facing) {
                case SOUTH -> Shapes.box(1.0 - maxX, minY, 1.0 - maxZ, 1.0 - minX, maxY, 1.0 - minZ);
                case WEST -> Shapes.box(minZ, minY, 1.0 - maxX, maxZ, maxY, 1.0 - minX);
                case EAST -> Shapes.box(1.0 - maxZ, minY, minX, 1.0 - minZ, maxY, maxX);
                default -> Shapes.box(minX, minY, minZ, maxX, maxY, maxZ);
            };
            rotated[0] = Shapes.or(rotated[0], box);
        });
        return rotated[0];
    }
}
