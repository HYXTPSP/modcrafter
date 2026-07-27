package {{PACKAGE}};

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/** 自定义物品/方块类(ModCrafter 导出的小型运行时) */
public final class ContentClasses {

    static void appendTooltip(Defs.ItemD def, List<Text> tooltip) {
        if (def.tooltip == null) return;
        for (String line : def.tooltip) {
            if (line == null || line.isEmpty()) continue;
            tooltip.add(Text.literal(line.replace('&', '§')).formatted(Formatting.GRAY));
        }
    }

    public static class McItem extends Item {
        public final Defs.ItemD def;

        public McItem(Defs.ItemD def, Settings settings) {
            super(settings);
            this.def = def;
        }

        @Override
        public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
            super.appendTooltip(stack, context, tooltip, type);
            ContentClasses.appendTooltip(def, tooltip);
        }

        @Override
        public boolean hasGlint(ItemStack stack) {
            return def.glint || super.hasGlint(stack);
        }
    }

    public static class McFoodItem extends McItem {
        public McFoodItem(Defs.ItemD def, Settings settings) {
            super(def, settings);
        }

        @Override
        public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
            ItemStack result = super.finishUsing(stack, world, user);
            if (!world.isClient && user instanceof ServerPlayerEntity player) {
                EventRt.onItemEaten(this, player);
            }
            return result;
        }
    }

    public static class McSwordItem extends SwordItem {
        public final Defs.ItemD def;

        public McSwordItem(Defs.ItemD def, ToolMaterial material, Settings settings) {
            super(material, settings);
            this.def = def;
        }

        @Override
        public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
            super.appendTooltip(stack, context, tooltip, type);
            ContentClasses.appendTooltip(def, tooltip);
        }

        @Override
        public boolean hasGlint(ItemStack stack) {
            return def.glint || super.hasGlint(stack);
        }
    }

    public static class McPickaxeItem extends PickaxeItem {
        public final Defs.ItemD def;

        public McPickaxeItem(Defs.ItemD def, ToolMaterial material, Settings settings) {
            super(material, settings);
            this.def = def;
        }

        @Override
        public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
            super.appendTooltip(stack, context, tooltip, type);
            ContentClasses.appendTooltip(def, tooltip);
        }

        @Override
        public boolean hasGlint(ItemStack stack) {
            return def.glint || super.hasGlint(stack);
        }
    }

    public static class McAxeItem extends AxeItem {
        public final Defs.ItemD def;

        public McAxeItem(Defs.ItemD def, ToolMaterial material, Settings settings) {
            super(material, settings);
            this.def = def;
        }

        @Override
        public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
            super.appendTooltip(stack, context, tooltip, type);
            ContentClasses.appendTooltip(def, tooltip);
        }

        @Override
        public boolean hasGlint(ItemStack stack) {
            return def.glint || super.hasGlint(stack);
        }
    }

    public static class McShovelItem extends ShovelItem {
        public final Defs.ItemD def;

        public McShovelItem(Defs.ItemD def, ToolMaterial material, Settings settings) {
            super(material, settings);
            this.def = def;
        }

        @Override
        public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
            super.appendTooltip(stack, context, tooltip, type);
            ContentClasses.appendTooltip(def, tooltip);
        }

        @Override
        public boolean hasGlint(ItemStack stack) {
            return def.glint || super.hasGlint(stack);
        }
    }

    public static class McHoeItem extends HoeItem {
        public final Defs.ItemD def;

        public McHoeItem(Defs.ItemD def, ToolMaterial material, Settings settings) {
            super(material, settings);
            this.def = def;
        }

        @Override
        public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
            super.appendTooltip(stack, context, tooltip, type);
            ContentClasses.appendTooltip(def, tooltip);
        }

        @Override
        public boolean hasGlint(ItemStack stack) {
            return def.glint || super.hasGlint(stack);
        }
    }

    public static class McArmorItem extends ArmorItem {
        public final Defs.ItemD def;

        public McArmorItem(Defs.ItemD def, RegistryEntry<ArmorMaterial> material, ArmorItem.Type type, Settings settings) {
            super(material, type, settings);
            this.def = def;
        }

        @Override
        public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
            super.appendTooltip(stack, context, tooltip, type);
            ContentClasses.appendTooltip(def, tooltip);
        }

        @Override
        public boolean hasGlint(ItemStack stack) {
            return def.glint || super.hasGlint(stack);
        }
    }

    /** 体素包围盒旋转(与 blockstate 旋转一致) */
    public static net.minecraft.util.shape.VoxelShape shapeFor(double[] b, net.minecraft.util.math.Direction facing) {
        double[] p1 = transformPoint(b[0], b[1], b[2], facing);
        double[] p2 = transformPoint(b[3], b[4], b[5], facing);
        return Block.createCuboidShape(
            Math.min(p1[0], p2[0]), Math.min(p1[1], p2[1]), Math.min(p1[2], p2[2]),
            Math.max(p1[0], p2[0]), Math.max(p1[1], p2[1]), Math.max(p1[2], p2[2]));
    }

    private static double[] transformPoint(double x, double y, double z, net.minecraft.util.math.Direction facing) {
        return switch (facing) {
            case EAST -> new double[]{16 - z, y, x};
            case SOUTH -> new double[]{16 - x, y, 16 - z};
            case WEST -> new double[]{z, y, 16 - x};
            case UP -> new double[]{x, 16 - z, y};
            case DOWN -> new double[]{x, z, 16 - y};
            default -> new double[]{x, y, z};
        };
    }

    public static boolean isFullCube(double[] b) {
        return b[0] <= 0 && b[1] <= 0 && b[2] <= 0 && b[3] >= 16 && b[4] >= 16 && b[5] >= 16;
    }

    public static class McBlock extends Block {
        public final Defs.BlockD def;
        protected net.minecraft.util.shape.VoxelShape outlineShape = null;

        public McBlock(Defs.BlockD def, Settings settings) {
            super(settings);
            this.def = def;
        }

        public void updateShapeBounds(double[] bounds) {
            if (bounds == null || isFullCube(bounds)) {
                this.outlineShape = null;
            } else {
                this.outlineShape = shapeFor(bounds, net.minecraft.util.math.Direction.NORTH);
            }
        }

        @Override
        protected net.minecraft.util.shape.VoxelShape getOutlineShape(BlockState state, net.minecraft.world.BlockView world,
                BlockPos pos, net.minecraft.block.ShapeContext context) {
            net.minecraft.util.shape.VoxelShape shape = this.outlineShape;
            return shape != null ? shape : super.getOutlineShape(state, world, pos, context);
        }

        @Override
        public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
            super.onSteppedOn(world, pos, state, entity);
            if (!world.isClient && entity instanceof ServerPlayerEntity player) {
                EventRt.onBlockSteppedOn(this, player, pos);
            }
        }

        @Override
        public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
            super.onPlaced(world, pos, state, placer, itemStack);
            if (!world.isClient && placer instanceof ServerPlayerEntity player) {
                EventRt.onBlockPlaced(this, player, pos);
            }
        }
    }

    public static class McHorizontalBlock extends McBlock {
        public static final net.minecraft.state.property.DirectionProperty FACING =
            net.minecraft.state.property.Properties.HORIZONTAL_FACING;
        private java.util.Map<net.minecraft.util.math.Direction, net.minecraft.util.shape.VoxelShape> facingShapes = null;

        public McHorizontalBlock(Defs.BlockD def, Settings settings) {
            super(def, settings);
            setDefaultState(getStateManager().getDefaultState().with(FACING, net.minecraft.util.math.Direction.NORTH));
        }

        @Override
        protected void appendProperties(net.minecraft.state.StateManager.Builder<Block, BlockState> builder) {
            builder.add(net.minecraft.state.property.Properties.HORIZONTAL_FACING);
        }

        @Override
        public BlockState getPlacementState(net.minecraft.item.ItemPlacementContext ctx) {
            return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
        }

        @Override
        public void updateShapeBounds(double[] bounds) {
            if (bounds == null || isFullCube(bounds)) {
                this.facingShapes = null;
                return;
            }
            java.util.Map<net.minecraft.util.math.Direction, net.minecraft.util.shape.VoxelShape> shapes =
                new java.util.EnumMap<>(net.minecraft.util.math.Direction.class);
            for (net.minecraft.util.math.Direction d : new net.minecraft.util.math.Direction[]{
                net.minecraft.util.math.Direction.NORTH, net.minecraft.util.math.Direction.EAST,
                net.minecraft.util.math.Direction.SOUTH, net.minecraft.util.math.Direction.WEST}) {
                shapes.put(d, shapeFor(bounds, d));
            }
            this.facingShapes = shapes;
        }

        @Override
        protected net.minecraft.util.shape.VoxelShape getOutlineShape(BlockState state, net.minecraft.world.BlockView world,
                BlockPos pos, net.minecraft.block.ShapeContext context) {
            if (facingShapes != null) {
                net.minecraft.util.shape.VoxelShape shape = facingShapes.get(state.get(FACING));
                if (shape != null) return shape;
            }
            return super.getOutlineShape(state, world, pos, context);
        }
    }

    public static class McFacingBlock extends McBlock {
        public static final net.minecraft.state.property.DirectionProperty FACING =
            net.minecraft.state.property.Properties.FACING;
        private java.util.Map<net.minecraft.util.math.Direction, net.minecraft.util.shape.VoxelShape> facingShapes = null;

        public McFacingBlock(Defs.BlockD def, Settings settings) {
            super(def, settings);
            setDefaultState(getStateManager().getDefaultState().with(FACING, net.minecraft.util.math.Direction.NORTH));
        }

        @Override
        protected void appendProperties(net.minecraft.state.StateManager.Builder<Block, BlockState> builder) {
            builder.add(net.minecraft.state.property.Properties.FACING);
        }

        @Override
        public BlockState getPlacementState(net.minecraft.item.ItemPlacementContext ctx) {
            return getDefaultState().with(FACING, ctx.getPlayerLookDirection().getOpposite());
        }

        @Override
        public void updateShapeBounds(double[] bounds) {
            if (bounds == null || isFullCube(bounds)) {
                this.facingShapes = null;
                return;
            }
            java.util.Map<net.minecraft.util.math.Direction, net.minecraft.util.shape.VoxelShape> shapes =
                new java.util.EnumMap<>(net.minecraft.util.math.Direction.class);
            for (net.minecraft.util.math.Direction d : net.minecraft.util.math.Direction.values()) {
                shapes.put(d, shapeFor(bounds, d));
            }
            this.facingShapes = shapes;
        }

        @Override
        protected net.minecraft.util.shape.VoxelShape getOutlineShape(BlockState state, net.minecraft.world.BlockView world,
                BlockPos pos, net.minecraft.block.ShapeContext context) {
            if (facingShapes != null) {
                net.minecraft.util.shape.VoxelShape shape = facingShapes.get(state.get(FACING));
                if (shape != null) return shape;
            }
            return super.getOutlineShape(state, world, pos, context);
        }
    }

    private ContentClasses() {
    }
}
