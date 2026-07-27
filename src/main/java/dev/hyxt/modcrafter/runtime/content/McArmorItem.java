package dev.hyxt.modcrafter.runtime.content;

import dev.hyxt.modcrafter.data.ItemDef;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

import java.util.List;

/** 自定义盔甲(使用原版盔甲材质的数值与穿戴外观,物品图标与名称自定义) */
public class McArmorItem extends ArmorItem {
    public final ItemDef def;
    public final String packId;

    public McArmorItem(ItemDef def, String packId, RegistryEntry<ArmorMaterial> material,
                       ArmorItem.Type type, Settings settings) {
        super(material, type, settings);
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
