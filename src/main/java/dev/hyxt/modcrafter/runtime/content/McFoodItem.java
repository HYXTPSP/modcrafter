package dev.hyxt.modcrafter.runtime.content;

import dev.hyxt.modcrafter.data.ItemDef;
import dev.hyxt.modcrafter.event.EventRuntime;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

/** 食物物品: 额外派发"吃下"事件 */
public class McFoodItem extends McItem {
    public McFoodItem(ItemDef def, String packId, Settings settings) {
        super(def, packId, settings);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        ItemStack result = super.finishUsing(stack, world, user);
        if (!world.isClient && user instanceof ServerPlayerEntity player) {
            EventRuntime.onItemEaten(this, player);
        }
        return result;
    }
}
