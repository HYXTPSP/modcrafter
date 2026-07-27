package dev.hyxt.modcrafter.runtime.content;

import dev.hyxt.modcrafter.data.ItemDef;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.List;

public class McSwordItem extends SwordItem {
    public final ItemDef def;
    public final String packId;

    public McSwordItem(ItemDef def, String packId, ToolMaterial material, Settings settings) {
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
