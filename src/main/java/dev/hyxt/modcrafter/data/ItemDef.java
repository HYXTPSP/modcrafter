package dev.hyxt.modcrafter.data;

import java.util.ArrayList;
import java.util.List;

/** 自定义物品定义 */
public class ItemDef {
    public String id = "";
    public String name = "";
    public List<String> tooltip = new ArrayList<>();

    /**
     * 贴图引用:
     *  "preset:xxx"  -> 模组自带预设贴图 assets/modcrafter/textures/preset/xxx.png
     *  "custom:xxx"  -> 内容包目录下 textures/xxx.png (玩家画的或导入的)
     */
    public String texture = "preset:gem_red";

    /** ITEM / FOOD / SWORD / PICKAXE / AXE / SHOVEL / HOE / HELMET / CHESTPLATE / LEGGINGS / BOOTS */
    public String type = "ITEM";

    /** 盔甲材质(决定护甲值与穿戴外观): LEATHER / CHAIN / IRON / GOLD / DIAMOND / NETHERITE / TURTLE */
    public String armorMaterial = "IRON";

    public int maxCount = 64;
    public int maxDamage = 0;
    /** COMMON / UNCOMMON / RARE / EPIC */
    public String rarity = "COMMON";
    public boolean fireproof = false;
    public boolean glint = false;

    public FoodDef food = null;
    public ToolDef tool = null;

    public static class FoodDef {
        public int nutrition = 4;
        public float saturation = 0.3f;
        public boolean alwaysEdible = false;
    }

    public static class ToolDef {
        /** WOOD / STONE / IRON / GOLD / DIAMOND / NETHERITE */
        public String material = "IRON";
        public float attackDamage = 3.0f;
        public float attackSpeed = -2.4f;
    }

    public boolean isFood() { return "FOOD".equals(type); }
    public boolean isTool() {
        return "SWORD".equals(type) || "PICKAXE".equals(type)
            || "AXE".equals(type) || "SHOVEL".equals(type) || "HOE".equals(type);
    }
    public boolean isArmor() {
        return "HELMET".equals(type) || "CHESTPLATE".equals(type)
            || "LEGGINGS".equals(type) || "BOOTS".equals(type);
    }
}
