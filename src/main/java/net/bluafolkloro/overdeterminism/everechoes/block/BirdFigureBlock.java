package net.bluafolkloro.overdeterminism.everechoes.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class BirdFigureBlock extends Block {
    public static final MapCodec<BirdFigureBlock> CODEC = simpleCodec(BirdFigureBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final VoxelShape northShape;
    private final VoxelShape southShape;
    private final VoxelShape westShape;
    private final VoxelShape eastShape;

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    public BirdFigureBlock(Properties properties) {
        this(properties, Block.box(0, 0, 0, 16, 16, 16));
    }

    public BirdFigureBlock(Properties properties, VoxelShape northShape) {
        super(properties);
        this.northShape = northShape;
        this.southShape = rotateShape(northShape, Direction.SOUTH);
        this.westShape = rotateShape(northShape, Direction.WEST);
        this.eastShape = rotateShape(northShape, Direction.EAST);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShapeForFacing(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShapeForFacing(state.getValue(FACING));
    }

    private VoxelShape getShapeForFacing(Direction facing) {
        return switch (facing) {
            case SOUTH -> southShape;
            case WEST -> westShape;
            case EAST -> eastShape;
            default -> northShape;
        };
    }

    private static VoxelShape rotateShape(VoxelShape shape, Direction facing) {
        double minX = shape.min(Direction.Axis.X) * 16.0;
        double minY = shape.min(Direction.Axis.Y) * 16.0;
        double minZ = shape.min(Direction.Axis.Z) * 16.0;
        double maxX = shape.max(Direction.Axis.X) * 16.0;
        double maxY = shape.max(Direction.Axis.Y) * 16.0;
        double maxZ = shape.max(Direction.Axis.Z) * 16.0;

        return switch (facing) {
            case SOUTH -> Block.box(16.0 - maxX, minY, 16.0 - maxZ, 16.0 - minX, maxY, 16.0 - minZ);
            case WEST -> Block.box(minZ, minY, 16.0 - maxX, maxZ, maxY, 16.0 - minX);
            case EAST -> Block.box(16.0 - maxZ, minY, minX, 16.0 - minZ, maxY, maxX);
            default -> shape;
        };
    }
}
