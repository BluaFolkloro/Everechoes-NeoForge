package net.bluafolkloro.overdeterminism.everechoes.block;

import com.mojang.serialization.MapCodec;
import net.bluafolkloro.overdeterminism.everechoes.block.entity.MailBoxBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

public class MailBoxBlock extends BaseEntityBlock {
    public static final MapCodec<MailBoxBlock> CODEC = simpleCodec(MailBoxBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<MailBoxStyle> STYLE = EnumProperty.create("style", MailBoxStyle.class);
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<Direction.Axis> CHAIN_AXIS = EnumProperty.create(
            "chain_axis", Direction.Axis.class, Direction.Axis::isHorizontal);

    private static final VoxelShape POST_LOWER_NORTH = Block.box(6, 0, 6, 10, 16, 10);
    private static final VoxelShape POST_UPPER_COLLISION_NORTH = Block.box(2, 0, 2, 14, 8, 14);
    private static final VoxelShape POST_UPPER_OUTLINE_NORTH = Shapes.or(
            Block.box(2, 0, 2, 14, 8, 14), Block.box(0, 5, 1, 16, 12, 15));
    private static final VoxelShape DESK_COLLISION_NORTH = Block.box(2, 0, 2, 14, 8, 14);
    private static final VoxelShape DESK_OUTLINE_NORTH = Shapes.or(
            Block.box(2, 0, 2, 14, 8, 14), Block.box(0, 5, 1, 16, 12, 15));
    private static final VoxelShape WALL_COLLISION_NORTH = Block.box(2, 5, 4, 14, 13, 16);
    private static final VoxelShape WALL_OUTLINE_NORTH = Shapes.or(
            Block.box(2, 5, 4, 14, 13, 16), Block.box(0, 10, 3, 16, 16, 16));
    private static final VoxelShape HANGING_COLLISION_NORTH = Block.box(2, 5, 2, 14, 13, 14);
    private static final VoxelShape HANGING_OUTLINE_NORTH = Shapes.or(
            Block.box(2, 5, 2, 14, 13, 14), Block.box(0, 10, 1, 16, 17, 15));
    private static final VoxelShape HANGING_RAISED_COLLISION_NORTH = Block.box(2, 12.5, 2, 14, 20.5, 14);
    private static final VoxelShape HANGING_RAISED_OUTLINE_NORTH = Shapes.or(
            Block.box(2, 12.5, 2, 14, 20.5, 14), Block.box(0, 17.5, 1, 16, 23.5, 15));

    private static final VoxelShape[][] OUTLINES = new VoxelShape[MailBoxStyle.values().length][4];
    private static final VoxelShape[][] COLLISIONS = new VoxelShape[MailBoxStyle.values().length][4];
    private static final VoxelShape[] POST_UPPER_OUTLINES = new VoxelShape[4];
    private static final VoxelShape[] POST_UPPER_COLLISIONS = new VoxelShape[4];

    static {
        fillFacingShapes(MailBoxStyle.POST, POST_LOWER_NORTH, POST_LOWER_NORTH);
        fillFacingShapes(MailBoxStyle.DESK, DESK_OUTLINE_NORTH, DESK_COLLISION_NORTH);
        fillFacingShapes(MailBoxStyle.WALL, WALL_OUTLINE_NORTH, WALL_COLLISION_NORTH);
        fillFacingShapes(MailBoxStyle.HANGING, HANGING_OUTLINE_NORTH, HANGING_COLLISION_NORTH);
        fillFacingShapes(MailBoxStyle.HANGING_RAISED, HANGING_RAISED_OUTLINE_NORTH, HANGING_RAISED_COLLISION_NORTH);
        fillFacingShapes(MailBoxStyle.HANGING_CHAINED, HANGING_RAISED_OUTLINE_NORTH, HANGING_RAISED_COLLISION_NORTH);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            int index = facing.get2DDataValue();
            POST_UPPER_OUTLINES[index] = rotateShape(POST_UPPER_OUTLINE_NORTH, facing);
            POST_UPPER_COLLISIONS[index] = rotateShape(POST_UPPER_COLLISION_NORTH, facing);
        }
    }

    public MailBoxBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(STYLE, MailBoxStyle.POST)
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(CHAIN_AXIS, Direction.Axis.X));
    }

    @Override
    public MapCodec<? extends MailBoxBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? null : new MailBoxBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STYLE, HALF, CHAIN_AXIS);
    }

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

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (state.getValue(STYLE) == MailBoxStyle.POST && state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
        }
    }

    private boolean survivesAtPlacement(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(STYLE) == MailBoxStyle.POST) {
            BlockPos below = pos.below();
            return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
        }
        return canSurvive(state, level, pos);
    }

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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        // No menu, inventory, key check, or postal-code claim flow exists in the first milestone.
        return InteractionResult.PASS;
    }

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

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER
                && state.getValue(STYLE) == MailBoxStyle.HANGING_CHAINED) {
            drops.add(new ItemStack(Items.CHAIN));
        }
        return drops;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        Direction chainDirection = state.getValue(CHAIN_AXIS) == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        return state
                .setValue(FACING, rotation.rotate(state.getValue(FACING)))
                .setValue(CHAIN_AXIS, rotation.rotate(chainDirection).getAxis());
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape(OUTLINES, state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape(COLLISIONS, state);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

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

    private static void fillFacingShapes(MailBoxStyle style, VoxelShape outlineNorth, VoxelShape collisionNorth) {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            int index = facing.get2DDataValue();
            OUTLINES[style.ordinal()][index] = rotateShape(outlineNorth, facing);
            COLLISIONS[style.ordinal()][index] = rotateShape(collisionNorth, facing);
        }
    }

    private static boolean isVerticalChain(BlockState state) {
        return state.is(Blocks.CHAIN) && state.hasProperty(BlockStateProperties.AXIS)
                && state.getValue(BlockStateProperties.AXIS) == Direction.Axis.Y;
    }

    private static boolean isHorizontalChain(BlockState state) {
        return state.is(Blocks.CHAIN) && state.hasProperty(BlockStateProperties.AXIS)
                && state.getValue(BlockStateProperties.AXIS) != Direction.Axis.Y;
    }

    private static boolean isRaisedHangingSupport(BlockState state) {
        return isHorizontalChain(state)
                || isHorizontalEndRod(state)
                || state.is(ContainerBlocks.MAIL_BOX.get())
                && state.getValue(HALF) == DoubleBlockHalf.LOWER
                && state.getValue(STYLE) == MailBoxStyle.HANGING_CHAINED;
    }

    private static boolean isHorizontalEndRod(BlockState state) {
        return state.is(Blocks.END_ROD) && state.hasProperty(BlockStateProperties.FACING)
                && state.getValue(BlockStateProperties.FACING).getAxis().isHorizontal();
    }

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
