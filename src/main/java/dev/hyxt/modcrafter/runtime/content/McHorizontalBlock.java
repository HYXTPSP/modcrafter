package dev.hyxt.modcrafter.runtime.content;

import dev.hyxt.modcrafter.data.BlockDef;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

import java.util.EnumMap;
import java.util.Map;

/** 水平四向朝向方块(放置时背对玩家,像熔炉) */
public class McHorizontalBlock extends McBlock {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    private volatile Map<Direction, VoxelShape> facingShapes = null;

    public McHorizontalBlock(BlockDef def, String packId, Settings settings) {
        super(def, packId, settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<net.minecraft.block.Block, BlockState> builder) {
        builder.add(Properties.HORIZONTAL_FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public void updateShapeBounds(double[] bounds) {
        if (bounds == null || ShapeUtil.isFullCube(bounds)) {
            this.facingShapes = null;
            this.outlineShape = null;
            return;
        }
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        for (Direction d : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            shapes.put(d, ShapeUtil.shapeFor(bounds, d));
        }
        this.facingShapes = shapes;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Map<Direction, VoxelShape> shapes = this.facingShapes;
        if (shapes != null) {
            VoxelShape shape = shapes.get(state.get(FACING));
            if (shape != null) return shape;
        }
        return super.getOutlineShape(state, world, pos, context);
    }
}
