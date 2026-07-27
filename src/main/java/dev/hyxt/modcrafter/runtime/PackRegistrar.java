package dev.hyxt.modcrafter.runtime;

import dev.hyxt.modcrafter.ModCrafter;
import dev.hyxt.modcrafter.data.BlockDef;
import dev.hyxt.modcrafter.data.ContentPack;
import dev.hyxt.modcrafter.data.ItemDef;
import dev.hyxt.modcrafter.data.PackManager;
import dev.hyxt.modcrafter.data.VoxelModel;
import dev.hyxt.modcrafter.runtime.content.McArmorItem;
import dev.hyxt.modcrafter.runtime.content.McBlock;
import dev.hyxt.modcrafter.runtime.content.McFacingBlock;
import dev.hyxt.modcrafter.runtime.content.McFoodItem;
import dev.hyxt.modcrafter.runtime.content.McHorizontalBlock;
import dev.hyxt.modcrafter.runtime.content.ShapeUtil;
import dev.hyxt.modcrafter.runtime.content.McItem;
import dev.hyxt.modcrafter.runtime.content.McMiningToolItem;
import dev.hyxt.modcrafter.runtime.content.McSwordItem;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.ToolMaterials;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.List;

/** 把内容包定义注册进 Minecraft 注册表 */
public final class PackRegistrar {

    private PackRegistrar() {
    }

    /** 启动时注册全部内容包 */
    public static void registerAll() {
        for (ContentPack pack : PackManager.all()) {
            registerPack(pack);
        }
    }

    /**
     * 注册一个内容包中所有尚未注册的元素。
     * 返回新注册的元素数量。已注册过的元素跳过(修改属性需重启)。
     */
    public static int registerPack(ContentPack pack) {
        int added = 0;
        for (ItemDef def : pack.items) {
            Identifier id = Identifier.of(pack.id, def.id);
            if (Registries.ITEM.containsId(id)) {
                RuntimeRegistry.ITEMS.putIfAbsent(id, Registries.ITEM.get(id));
                continue;
            }
            try {
                Item item = buildItem(def, pack.id);
                Registry.register(Registries.ITEM, id, item);
                RuntimeRegistry.ITEMS.put(id, item);
                added++;
            } catch (Exception e) {
                ModCrafter.LOGGER.error("注册物品失败: " + id, e);
            }
        }
        for (BlockDef def : pack.blocks) {
            Identifier id = Identifier.of(pack.id, def.id);
            if (Registries.BLOCK.containsId(id)) {
                RuntimeRegistry.BLOCKS.putIfAbsent(id, Registries.BLOCK.get(id));
                RuntimeRegistry.ITEMS.putIfAbsent(id, Registries.ITEM.get(id));
                continue;
            }
            try {
                Block block = buildBlock(def, pack.id);
                Registry.register(Registries.BLOCK, id, block);
                BlockItem blockItem = new BlockItem(block, new Item.Settings());
                Registry.register(Registries.ITEM, id, blockItem);
                blockItem.appendBlocks(Item.BLOCK_ITEMS, blockItem);
                RuntimeRegistry.BLOCKS.put(id, block);
                RuntimeRegistry.ITEMS.put(id, blockItem);
                added++;
            } catch (Exception e) {
                ModCrafter.LOGGER.error("注册方块失败: " + id, e);
            }
        }
        return added;
    }

    public static Item buildItem(ItemDef def, String packId) {
        Item.Settings settings = new Item.Settings();
        if (def.maxDamage > 0) {
            settings.maxDamage(def.maxDamage);
        } else {
            settings.maxCount(Math.max(1, Math.min(99, def.maxCount)));
        }
        settings.rarity(parseRarity(def.rarity));
        if (def.fireproof) settings.fireproof();

        switch (def.type) {
            case "FOOD" -> {
                ItemDef.FoodDef f = def.food != null ? def.food : new ItemDef.FoodDef();
                FoodComponent.Builder food = new FoodComponent.Builder()
                    .nutrition(Math.max(0, f.nutrition))
                    .saturationModifier(Math.max(0f, f.saturation));
                if (f.alwaysEdible) food.alwaysEdible();
                settings.food(food.build());
                return new McFoodItem(def, packId, settings);
            }
            case "SWORD" -> {
                ItemDef.ToolDef t = tool(def);
                ToolMaterial mat = parseMaterial(t.material);
                settings.attributeModifiers(SwordItem.createAttributeModifiers(mat, (int) t.attackDamage, t.attackSpeed));
                return new McSwordItem(def, packId, mat, settings);
            }
            case "PICKAXE" -> {
                ItemDef.ToolDef t = tool(def);
                ToolMaterial mat = parseMaterial(t.material);
                settings.attributeModifiers(PickaxeItem.createAttributeModifiers(mat, t.attackDamage, t.attackSpeed));
                return new McMiningToolItem.Pickaxe(def, packId, mat, settings);
            }
            case "AXE" -> {
                ItemDef.ToolDef t = tool(def);
                ToolMaterial mat = parseMaterial(t.material);
                settings.attributeModifiers(AxeItem.createAttributeModifiers(mat, t.attackDamage, t.attackSpeed));
                return new McMiningToolItem.Axe(def, packId, mat, settings);
            }
            case "SHOVEL" -> {
                ItemDef.ToolDef t = tool(def);
                ToolMaterial mat = parseMaterial(t.material);
                settings.attributeModifiers(ShovelItem.createAttributeModifiers(mat, t.attackDamage, t.attackSpeed));
                return new McMiningToolItem.Shovel(def, packId, mat, settings);
            }
            case "HOE" -> {
                ItemDef.ToolDef t = tool(def);
                ToolMaterial mat = parseMaterial(t.material);
                settings.attributeModifiers(HoeItem.createAttributeModifiers(mat, t.attackDamage, t.attackSpeed));
                return new McMiningToolItem.Hoe(def, packId, mat, settings);
            }
            case "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS" -> {
                RegistryEntry<ArmorMaterial> mat = armorMaterialFor(def, packId);
                ArmorItem.Type type = switch (def.type) {
                    case "HELMET" -> ArmorItem.Type.HELMET;
                    case "CHESTPLATE" -> ArmorItem.Type.CHESTPLATE;
                    case "LEGGINGS" -> ArmorItem.Type.LEGGINGS;
                    default -> ArmorItem.Type.BOOTS;
                };
                if (def.maxDamage <= 0) {
                    settings.maxDamage(type.getMaxDamage(15)); // 铁甲耐久系数
                }
                if ("TINT".equals(def.armorTexMode)) {
                    settings.component(DataComponentTypes.DYED_COLOR,
                        new DyedColorComponent(parseColor(def.armorColor, 0xFF5555), false));
                }
                return new McArmorItem(def, packId, mat, type, settings);
            }
            default -> {
                return new McItem(def, packId, settings);
            }
        }
    }

    private static ItemDef.ToolDef tool(ItemDef def) {
        return def.tool != null ? def.tool : new ItemDef.ToolDef();
    }

    public static Block buildBlock(BlockDef def, String packId) {
        AbstractBlock.Settings settings = AbstractBlock.Settings.create()
            .strength(Math.max(0f, def.hardness), Math.max(0f, def.resistance))
            .sounds(parseSound(def.sound));
        if (def.luminance > 0) {
            int lum = Math.min(15, def.luminance);
            settings.luminance(state -> lum);
        }
        if (def.requiresTool) settings.requiresTool();
        if (def.transparent) settings.nonOpaque();
        if (def.slipperiness > 0 && def.slipperiness != 0.6f) {
            settings.slipperiness(Math.min(0.999f, def.slipperiness));
        }

        // 体素模型: 非满格时必须 nonOpaque,并计算轮廓
        double[] bounds = modelBounds(def, packId);
        if (bounds != null && !ShapeUtil.isFullCube(bounds)) {
            settings.nonOpaque();
        }

        McBlock block = switch (def.facingMode == null ? "NONE" : def.facingMode) {
            case "HORIZONTAL" -> new McHorizontalBlock(def, packId, settings);
            case "ALL" -> new McFacingBlock(def, packId, settings);
            default -> new McBlock(def, packId, settings);
        };
        block.updateShapeBounds(bounds);
        return block;
    }

    /** MODEL 模式方块的体素包围盒;非 MODEL 或模型缺失返回 null */
    public static double[] modelBounds(BlockDef def, String packId) {
        if (!"MODEL".equals(def.textureMode) || def.model == null || def.model.isEmpty()) return null;
        VoxelModel model = PackManager.loadVoxelModel(packId, def.model);
        if (model == null || model.isEmpty()) return null;
        return VoxelMesher.bounds(model);
    }

    /** 热应用后刷新已注册方块的碰撞/轮廓箱(模型可能被重新编辑过) */
    public static void refreshShapes() {
        for (ContentPack pack : PackManager.all()) {
            for (BlockDef def : pack.blocks) {
                Block block = RuntimeRegistry.BLOCKS.get(Identifier.of(pack.id, def.id));
                if (block instanceof McBlock mc) {
                    mc.updateShapeBounds(modelBounds(def, pack.id));
                }
            }
        }
    }

    /** 盔甲材质: VANILLA 直接用原版;TINT/CUSTOM 注册专属材质(数值继承所选基础材质) */
    public static RegistryEntry<ArmorMaterial> armorMaterialFor(ItemDef def, String packId) {
        RegistryEntry<ArmorMaterial> base = parseArmorMaterial(def.armorMaterial);
        String mode = def.armorTexMode == null ? "VANILLA" : def.armorTexMode;
        if ("VANILLA".equals(mode)) return base;

        Identifier id = Identifier.of(packId, def.id + "_mat");
        ArmorMaterial existing = Registries.ARMOR_MATERIAL.get(id);
        if (existing != null) {
            return Registries.ARMOR_MATERIAL.getEntry(existing);
        }

        ArmorMaterial bm = base.value();
        List<ArmorMaterial.Layer> layers;
        if ("TINT".equals(mode)) {
            // 皮革布局(可染色) + 白色覆盖层,与原版皮革甲结构一致
            layers = List.of(
                new ArmorMaterial.Layer(Identifier.of("minecraft", "leather"), "", true),
                new ArmorMaterial.Layer(Identifier.of("minecraft", "leather"), "_overlay", false));
        } else {
            layers = List.of(new ArmorMaterial.Layer(Identifier.of(packId, def.id), "", false));
        }
        ArmorMaterial material = new ArmorMaterial(
            bm.defense(), bm.enchantability(), bm.equipSound(), bm.repairIngredient(),
            layers, bm.toughness(), bm.knockbackResistance());
        return Registry.registerReference(Registries.ARMOR_MATERIAL, id, material);
    }

    public static int parseColor(String hex, int fallback) {
        try {
            return (int) Long.parseLong(hex.replace("#", "").trim(), 16) & 0xFFFFFF;
        } catch (Exception e) {
            return fallback;
        }
    }

    public static RegistryEntry<ArmorMaterial> parseArmorMaterial(String s) {
        if (s == null) return ArmorMaterials.IRON;
        return switch (s) {
            case "LEATHER" -> ArmorMaterials.LEATHER;
            case "CHAIN" -> ArmorMaterials.CHAIN;
            case "GOLD" -> ArmorMaterials.GOLD;
            case "DIAMOND" -> ArmorMaterials.DIAMOND;
            case "NETHERITE" -> ArmorMaterials.NETHERITE;
            case "TURTLE" -> ArmorMaterials.TURTLE;
            default -> ArmorMaterials.IRON;
        };
    }

    public static Rarity parseRarity(String s) {
        try {
            return Rarity.valueOf(s);
        } catch (Exception e) {
            return Rarity.COMMON;
        }
    }

    public static ToolMaterials parseMaterial(String s) {
        try {
            return ToolMaterials.valueOf(s);
        } catch (Exception e) {
            return ToolMaterials.IRON;
        }
    }

    public static BlockSoundGroup parseSound(String s) {
        if (s == null) return BlockSoundGroup.STONE;
        return switch (s) {
            case "WOOD" -> BlockSoundGroup.WOOD;
            case "METAL" -> BlockSoundGroup.METAL;
            case "GLASS" -> BlockSoundGroup.GLASS;
            case "GRASS" -> BlockSoundGroup.GRASS;
            case "SAND" -> BlockSoundGroup.SAND;
            case "WOOL" -> BlockSoundGroup.WOOL;
            default -> BlockSoundGroup.STONE;
        };
    }
}
