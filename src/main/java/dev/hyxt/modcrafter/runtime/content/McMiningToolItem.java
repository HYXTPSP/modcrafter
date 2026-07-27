package dev.hyxt.modcrafter.runtime.content;

import dev.hyxt.modcrafter.data.ItemDef;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.List;

/** 镐/斧/铲 三种挖掘工具的包装类 */
public final class McMiningToolItem {

    public static class Pickaxe extends PickaxeItem {
        public final ItemDef def;
        public final String packId;

        public Pickaxe(ItemDef def, String packId, ToolMaterial material, Settings settings) {
            super(material, settings);
            this.def = def;
            this.packId = packId;
        }

        @Override
        public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
            super.appendTooltip(stack, context, tooltip, type);
            TooltipHelper.append(def, tooltip);
        }

        @Override
        public boolean hasGlint(ItemStack stack) {
            return def.glint || super.hasGlint(stack);
        }
    }

    public static class Axe extends AxeItem {
        public final ItemDef def;
        public final String packId;

        public Axe(ItemDef def, String packId, ToolMaterial material, Settings settings) {
            super(material, settings);
            this.def = def;
            this.packId = packId;
        }

        @Override
        public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
            super.appendTooltip(stack, context, tooltip, type);
            TooltipHelper.append(def, tooltip);
        }

        @Override
        public boolean hasGlint(ItemStack stack) {
            return def.glint || super.hasGlint(stack);
        }
    }

    public static class Hoe extends net.minecraft.item.HoeItem {
        public final ItemDef def;
        public final String packId;

        public Hoe(ItemDef def, String packId, ToolMaterial material, Settings settings) {
            super(material, settings);
            this.def = def;
            this.packId = packId;
        }

        @Override
        public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
            super.appendTooltip(stack, context, tooltip, type);
            TooltipHelper.append(def, tooltip);
        }

        @Override
        public boolean hasGlint(ItemStack stack) {
            return def.glint || super.hasGlint(stack);
        }
    }

    public static class Shovel extends ShovelItem {
        public final ItemDef def;
        public final String packId;

        public Shovel(ItemDef def, String packId, ToolMaterial material, Settings settings) {
            super(material, settings);
            this.def = def;
            this.packId = packId;
        }

        @Override
        public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
            super.appendTooltip(stack, context, tooltip, type);
            TooltipHelper.append(def, tooltip);
        }

        @Override
        public boolean hasGlint(ItemStack stack) {
            return def.glint || super.hasGlint(stack);
        }
    }

    private McMiningToolItem() {
    }
}
