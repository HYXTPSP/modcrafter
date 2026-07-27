package dev.hyxt.modcrafter.runtime.content;

import dev.hyxt.modcrafter.data.ItemDef;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.List;

/** 普通自定义物品 */
public class McItem extends Item {
    public final ItemDef def;
    public final String packId;

    public McItem(ItemDef def, String packId, Settings settings) {
        super(settings);
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
