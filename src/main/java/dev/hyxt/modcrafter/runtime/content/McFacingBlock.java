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

/** 六向朝向方块(放置时背对玩家视线,像投掷器) */
public class McFacingBlock extends McBlock {
    public static final DirectionProperty FACING = Properties.FACING;

    private volatile Map<Direction, VoxelShape> facingShapes = null;

    public McFacingBlock(BlockDef def, String packId, Settings settings) {
        super(def, packId, settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<net.minecraft.block.Block, BlockState> builder) {
        builder.add(Properties.FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getPlayerLookDirection().getOpposite());
    }

    @Override
    public void updateShapeBounds(double[] bounds) {
        if (bounds == null || ShapeUtil.isFullCube(bounds)) {
            this.facingShapes = null;
            this.outlineShape = null;
            return;
        }
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        for (Direction d : Direction.values()) {
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
