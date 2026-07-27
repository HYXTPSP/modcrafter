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

    public static class McBlock extends Block {
        public final Defs.BlockD def;

        public McBlock(Defs.BlockD def, Settings settings) {
            super(settings);
            this.def = def;
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

    private ContentClasses() {
    }
}
