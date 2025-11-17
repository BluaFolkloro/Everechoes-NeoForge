package net.bluafolkloro.overdeterminism.everechoes.block;

import com.mojang.serialization.MapCodec;
import net.bluafolkloro.overdeterminism.everechoes.block.entity.MailBoxBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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

public class MailBoxBlock extends BaseEntityBlock {
    public static final MapCodec<MailBoxBlock> CODEC = simpleCodec(MailBoxBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    private static final VoxelShape LOWER_SHAPE = Block.box(2, 0, 2, 14, 16, 14);
    private static final VoxelShape UPPER_SHAPE = Block.box(2, 0, 2, 14, 10, 14);

    @Override
    public MapCodec<? extends MailBoxBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MailBoxBlockEntity(pos, state);
    }

    public MailBoxBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(HALF, DoubleBlockHalf.LOWER)
        );
    }

    //测试用
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        // 客户端只负责动画，直接返回 SUCCESS
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MailBoxBlockEntity mailBox) {
            // 先做一个调试用输出，确认交互触发 & BE 存在
            player.sendSystemMessage(Component.literal("你右键了邮筒（将来这里会打开GUI）"));
        }

        return InteractionResult.CONSUME;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING);
    }

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

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }

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

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        DoubleBlockHalf half = state.getValue(HALF);
        BlockPos otherPos = (half == DoubleBlockHalf.UPPER) ? pos.below() : pos.above();
        BlockState otherState = level.getBlockState(otherPos);

        if (otherState.is(this) && otherState.getValue(HALF) != half) {
            level.removeBlock(otherPos, false);
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER_SHAPE : UPPER_SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}