package dev.hyxt.modcrafter.runtime;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

/** 记录本模组运行期注册过的所有内容(跨所有内容包) */
public final class RuntimeRegistry {
    /** 完整物品 id -> Item (包含方块物品) */
    public static final Map<Identifier, Item> ITEMS = new LinkedHashMap<>();
    /** 完整方块 id -> Block */
    public static final Map<Identifier, Block> BLOCKS = new LinkedHashMap<>();

    private RuntimeRegistry() {
    }

    public static ItemStack groupIcon() {
        for (Item item : ITEMS.values()) {
            return new ItemStack(item);
        }
        return new ItemStack(Items.CRAFTING_TABLE);
    }

    public static boolean hasItem(Identifier id) {
        return ITEMS.containsKey(id);
    }

    public static boolean hasBlock(Identifier id) {
        return BLOCKS.containsKey(id);
    }
}
