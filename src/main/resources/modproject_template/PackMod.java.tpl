package {{PACKAGE}};

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.ToolMaterials;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.world.gen.GenerationStep;

import java.util.LinkedHashMap;
import java.util.Map;

/** {{PACK_NAME}} —— 由 ModCrafter 模组工坊导出的独立 Fabric 模组 */
public class PackMod implements ModInitializer {
    public static final String PACK_ID = "{{PACK_ID}}";
    public static final Map<Identifier, Item> ITEMS = new LinkedHashMap<>();
    public static final Map<Identifier, Block> BLOCKS = new LinkedHashMap<>();
    public static Defs.Pack PACK;

    @Override
    public void onInitialize() {
        Defs.Pack pack = Defs.load();
        PACK = pack;

        for (Defs.ItemD def : pack.items) {
            Identifier id = Identifier.of(PACK_ID, def.id);
            Item item = buildItem(def);
            Registry.register(Registries.ITEM, id, item);
            ITEMS.put(id, item);
        }

        Map<String, double[]> shapes = Defs.loadShapes();

        for (Defs.BlockD def : pack.blocks) {
            Identifier id = Identifier.of(PACK_ID, def.id);
            Block block = buildBlock(def, shapes.get(def.id));
            Registry.register(Registries.BLOCK, id, block);
            BlockItem blockItem = new BlockItem(block, new Item.Settings());
            Registry.register(Registries.ITEM, id, blockItem);
            blockItem.appendBlocks(Item.BLOCK_ITEMS, blockItem);
            BLOCKS.put(id, block);
            ITEMS.put(id, blockItem);
        }

        // 矿石世界生成(worldgen JSON 打包在本模组的 data/ 里,始终可用)
        for (Defs.BlockD def : pack.blocks) {
            if (def.oreGen != null && def.oreGen.enabled) {
                BiomeModifications.addFeature(
                    BiomeSelectors.foundInOverworld(),
                    GenerationStep.Feature.UNDERGROUND_ORES,
                    RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(PACK_ID, "ore_" + def.id)));
            }
        }

        ItemGroup group = FabricItemGroup.builder()
            .icon(() -> ITEMS.isEmpty() ? new ItemStack(Items.CHEST) : new ItemStack(ITEMS.values().iterator().next()))
            .displayName(Text.literal("{{PACK_NAME}}"))
            .entries((context, entries) -> {
                for (Item item : ITEMS.values()) {
                    entries.add(item);
                }
            })
            .build();
        Registry.register(Registries.ITEM_GROUP, Identifier.of(PACK_ID, "main"), group);

        EventRt.init(pack);
    }

    private static Item buildItem(Defs.ItemD def) {
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
                Defs.FoodD f = def.food != null ? def.food : new Defs.FoodD();
                FoodComponent.Builder food = new FoodComponent.Builder()
                    .nutrition(Math.max(0, f.nutrition))
                    .saturationModifier(Math.max(0f, f.saturation));
                if (f.alwaysEdible) food.alwaysEdible();
                settings.food(food.build());
                return new ContentClasses.McFoodItem(def, settings);
            }
            case "SWORD" -> {
                Defs.ToolD t = tool(def);
                ToolMaterial mat = parseMaterial(t.material);
                settings.attributeModifiers(SwordItem.createAttributeModifiers(mat, (int) t.attackDamage, t.attackSpeed));
                return new ContentClasses.McSwordItem(def, mat, settings);
            }
            case "PICKAXE" -> {
                Defs.ToolD t = tool(def);
                ToolMaterial mat = parseMaterial(t.material);
                settings.attributeModifiers(PickaxeItem.createAttributeModifiers(mat, t.attackDamage, t.attackSpeed));
                return new ContentClasses.McPickaxeItem(def, mat, settings);
            }
            case "AXE" -> {
                Defs.ToolD t = tool(def);
                ToolMaterial mat = parseMaterial(t.material);
                settings.attributeModifiers(AxeItem.createAttributeModifiers(mat, t.attackDamage, t.attackSpeed));
                return new ContentClasses.McAxeItem(def, mat, settings);
            }
            case "SHOVEL" -> {
                Defs.ToolD t = tool(def);
                ToolMaterial mat = parseMaterial(t.material);
                settings.attributeModifiers(ShovelItem.createAttributeModifiers(mat, t.attackDamage, t.attackSpeed));
                return new ContentClasses.McShovelItem(def, mat, settings);
            }
            case "HOE" -> {
                Defs.ToolD t = tool(def);
                ToolMaterial mat = parseMaterial(t.material);
                settings.attributeModifiers(HoeItem.createAttributeModifiers(mat, t.attackDamage, t.attackSpeed));
                return new ContentClasses.McHoeItem(def, mat, settings);
            }
            case "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS" -> {
                RegistryEntry<ArmorMaterial> mat = armorMaterialFor(def);
                ArmorItem.Type type = switch (def.type) {
                    case "HELMET" -> ArmorItem.Type.HELMET;
                    case "CHESTPLATE" -> ArmorItem.Type.CHESTPLATE;
                    case "LEGGINGS" -> ArmorItem.Type.LEGGINGS;
                    default -> ArmorItem.Type.BOOTS;
                };
                if (def.maxDamage <= 0) {
                    settings.maxDamage(type.getMaxDamage(15));
                }
                if ("TINT".equals(def.armorTexMode)) {
                    settings.component(net.minecraft.component.DataComponentTypes.DYED_COLOR,
                        new net.minecraft.component.type.DyedColorComponent(parseColor(def.armorColor, 0xFF5555), false));
                }
                return new ContentClasses.McArmorItem(def, mat, type, settings);
            }
            default -> {
                return new ContentClasses.McItem(def, settings);
            }
        }
    }

    private static Defs.ToolD tool(Defs.ItemD def) {
        return def.tool != null ? def.tool : new Defs.ToolD();
    }

    private static Block buildBlock(Defs.BlockD def, double[] bounds) {
        AbstractBlock.Settings settings = AbstractBlock.Settings.create()
            .strength(Math.max(0f, def.hardness), Math.max(0f, def.resistance))
            .sounds(parseSound(def.sound));
        if (def.luminance > 0) {
            int lum = Math.min(15, def.luminance);
            settings.luminance(state -> lum);
        }
        if (def.requiresTool) settings.requiresTool();
        if (def.transparent) settings.nonOpaque();
        if (bounds != null && !ContentClasses.isFullCube(bounds)) settings.nonOpaque();
        if (def.slipperiness > 0 && def.slipperiness != 0.6f) {
            settings.slipperiness(Math.min(0.999f, def.slipperiness));
        }
        ContentClasses.McBlock block = switch (def.facingMode == null ? "NONE" : def.facingMode) {
            case "HORIZONTAL" -> new ContentClasses.McHorizontalBlock(def, settings);
            case "ALL" -> new ContentClasses.McFacingBlock(def, settings);
            default -> new ContentClasses.McBlock(def, settings);
        };
        block.updateShapeBounds(bounds);
        return block;
    }

    /** VANILLA 用原版材质;TINT/CUSTOM 注册专属材质(数值继承基础材质) */
    private static RegistryEntry<ArmorMaterial> armorMaterialFor(Defs.ItemD def) {
        RegistryEntry<ArmorMaterial> base = parseArmorMaterial(def.armorMaterial);
        String mode = def.armorTexMode == null ? "VANILLA" : def.armorTexMode;
        if ("VANILLA".equals(mode)) return base;
        Identifier id = Identifier.of(PACK_ID, def.id + "_mat");
        ArmorMaterial existing = Registries.ARMOR_MATERIAL.get(id);
        if (existing != null) return Registries.ARMOR_MATERIAL.getEntry(existing);
        ArmorMaterial bm = base.value();
        java.util.List<ArmorMaterial.Layer> layers;
        if ("TINT".equals(mode)) {
            layers = java.util.List.of(
                new ArmorMaterial.Layer(Identifier.of("minecraft", "leather"), "", true),
                new ArmorMaterial.Layer(Identifier.of("minecraft", "leather"), "_overlay", false));
        } else {
            layers = java.util.List.of(new ArmorMaterial.Layer(Identifier.of(PACK_ID, def.id), "", false));
        }
        ArmorMaterial material = new ArmorMaterial(
            bm.defense(), bm.enchantability(), bm.equipSound(), bm.repairIngredient(),
            layers, bm.toughness(), bm.knockbackResistance());
        return Registry.registerReference(Registries.ARMOR_MATERIAL, id, material);
    }

    private static int parseColor(String hex, int fallback) {
        try {
            return (int) Long.parseLong(hex.replace("#", "").trim(), 16) & 0xFFFFFF;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static RegistryEntry<ArmorMaterial> parseArmorMaterial(String s) {
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

    private static Rarity parseRarity(String s) {
        try {
            return Rarity.valueOf(s);
        } catch (Exception e) {
            return Rarity.COMMON;
        }
    }

    private static ToolMaterials parseMaterial(String s) {
        try {
            return ToolMaterials.valueOf(s);
        } catch (Exception e) {
            return ToolMaterials.IRON;
        }
    }

    private static BlockSoundGroup parseSound(String s) {
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
