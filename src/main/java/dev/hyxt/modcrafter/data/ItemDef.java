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

    /** 盔甲材质(决定护甲数值): LEATHER / CHAIN / IRON / GOLD / DIAMOND / NETHERITE / TURTLE */
    public String armorMaterial = "IRON";

    /**
     * 盔甲穿戴外观:
     *  VANILLA - 所选材质的原版外观
     *  TINT    - 皮革布局 + 任意自定义颜色染色
     *  CUSTOM  - 自定义护甲层贴图(packs/<包>/armor/<物品id>_layer_1.png / _layer_2.png)
     */
    public String armorTexMode = "VANILLA";
    /** TINT 模式的颜色 #RRGGBB */
    public String armorColor = "#FF5555";

    /** 体素 3D 模型名(空 = 使用 2D 贴图) */
    public String model = "";

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
