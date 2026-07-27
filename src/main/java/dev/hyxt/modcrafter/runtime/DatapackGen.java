package dev.hyxt.modcrafter.runtime;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.hyxt.modcrafter.ModCrafter;
import dev.hyxt.modcrafter.data.BlockDef;
import dev.hyxt.modcrafter.data.ContentPack;
import dev.hyxt.modcrafter.data.PackManager;
import dev.hyxt.modcrafter.data.RecipeDef;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 生成"数据包"(配方 + 方块战利品表 + 挖掘标签)到当前世界的 datapacks 目录,并自动启用。
 * 数据包名固定为 ModCrafterData,启用 id 为 "file/ModCrafterData"。
 */
public final class DatapackGen {
    public static final String PACK_DIR_NAME = "ModCrafterData";
    public static final String PACK_PROFILE_ID = "file/" + PACK_DIR_NAME;
    /** 1.21.1 数据包格式 */
    private static final int PACK_FORMAT = 48;

    private DatapackGen() {
    }

    /** 重新生成数据包文件并(重新)加载。必须在服务器线程调用。 */
    public static void writeAndEnable(MinecraftServer server) {
        if (PackManager.all().isEmpty()) return; // 没有内容包就不折腾
        try {
            Path dir = server.getSavePath(WorldSavePath.DATAPACKS).resolve(PACK_DIR_NAME);
            regenerateFiles(dir);

            ResourcePackManager manager = server.getDataPackManager();
            manager.scanPacks();
            List<String> enabled = new ArrayList<>(manager.getEnabledIds());
            if (!enabled.contains(PACK_PROFILE_ID)) {
                enabled.add(PACK_PROFILE_ID);
            }
            server.reloadResources(enabled).exceptionally(e -> {
                ModCrafter.LOGGER.error("重载数据包失败", e);
                return null;
            });
        } catch (Exception e) {
            ModCrafter.LOGGER.error("生成数据包失败", e);
        }
    }

    /** 只生成文件,不触发重载 */
    public static void regenerateFiles(Path dir) throws IOException {
        // 清空旧目录
        if (Files.exists(dir)) {
            try (Stream<Path> walk = Files.walk(dir)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                    }
                });
            }
        }
        Files.createDirectories(dir);

        // pack.mcmeta
        JsonObject meta = new JsonObject();
        JsonObject packObj = new JsonObject();
        packObj.addProperty("pack_format", PACK_FORMAT);
        packObj.addProperty("description", "ModCrafter 自动生成的数据包");
        meta.add("pack", packObj);
        write(dir.resolve("pack.mcmeta"), meta);

        // 收集挖掘标签 (跨所有内容包合并) 与可染色物品标签
        Map<String, List<String>> mineable = new LinkedHashMap<>();
        Map<String, List<String>> needsTool = new LinkedHashMap<>();
        List<String> dyeable = new ArrayList<>();

        for (ContentPack pack : PackManager.all()) {
            Path data = dir.resolve("data").resolve(pack.id);

            for (dev.hyxt.modcrafter.data.ItemDef item : pack.items) {
                if (item.isArmor() && "TINT".equals(item.armorTexMode)) {
                    dyeable.add(pack.id + ":" + item.id);
                }
            }

            for (RecipeDef recipe : pack.recipes) {
                JsonObject json = recipeJson(recipe);
                if (json != null) {
                    write(data.resolve("recipe").resolve(recipe.id + ".json"), json);
                }
            }

            for (BlockDef block : pack.blocks) {
                String fullId = pack.id + ":" + block.id;
                JsonObject loot = lootTableJson(block, fullId);
                if (loot != null) {
                    write(data.resolve("loot_table").resolve("blocks").resolve(block.id + ".json"), loot);
                }
                writeOreGen(data, block, fullId, pack.id);
                if (block.requiresTool || !"NONE".equals(block.toolLevel)) {
                    String tool = switch (block.toolType == null ? "pickaxe" : block.toolType) {
                        case "axe" -> "axe";
                        case "shovel" -> "shovel";
                        case "hoe" -> "hoe";
                        default -> "pickaxe";
                    };
                    mineable.computeIfAbsent(tool, k -> new ArrayList<>()).add(fullId);
                    switch (block.toolLevel == null ? "NONE" : block.toolLevel) {
                        case "STONE" -> needsTool.computeIfAbsent("needs_stone_tool", k -> new ArrayList<>()).add(fullId);
                        case "IRON" -> needsTool.computeIfAbsent("needs_iron_tool", k -> new ArrayList<>()).add(fullId);
                        case "DIAMOND" -> needsTool.computeIfAbsent("needs_diamond_tool", k -> new ArrayList<>()).add(fullId);
                        default -> {
                        }
                    }
                }
            }
        }

        Path mcTags = dir.resolve("data").resolve("minecraft").resolve("tags").resolve("block");
        for (Map.Entry<String, List<String>> e : mineable.entrySet()) {
            write(mcTags.resolve("mineable").resolve(e.getKey() + ".json"), tagJson(e.getValue()));
        }
        for (Map.Entry<String, List<String>> e : needsTool.entrySet()) {
            write(mcTags.resolve(e.getKey() + ".json"), tagJson(e.getValue()));
        }
        if (!dyeable.isEmpty()) {
            write(dir.resolve("data").resolve("minecraft").resolve("tags").resolve("item")
                .resolve("dyeable.json"), tagJson(dyeable));
        }
    }

    /** 导出独立模组工程用: 只生成单个内容包的 data 目录(无 pack.mcmeta) */
    public static void generateSinglePackData(Path dataRoot, ContentPack pack) throws IOException {
        Path data = dataRoot.resolve(pack.id);
        Map<String, List<String>> mineable = new LinkedHashMap<>();
        Map<String, List<String>> needsTool = new LinkedHashMap<>();
        List<String> dyeable = new ArrayList<>();

        for (dev.hyxt.modcrafter.data.ItemDef item : pack.items) {
            if (item.isArmor() && "TINT".equals(item.armorTexMode)) {
                dyeable.add(pack.id + ":" + item.id);
            }
        }

        for (RecipeDef recipe : pack.recipes) {
            JsonObject json = recipeJson(recipe);
            if (json != null) {
                write(data.resolve("recipe").resolve(recipe.id + ".json"), json);
            }
        }
        for (BlockDef block : pack.blocks) {
            String fullId = pack.id + ":" + block.id;
            JsonObject loot = lootTableJson(block, fullId);
            if (loot != null) {
                write(data.resolve("loot_table").resolve("blocks").resolve(block.id + ".json"), loot);
            }
            writeOreGen(data, block, fullId, pack.id);
            if (block.requiresTool || !"NONE".equals(block.toolLevel)) {
                String tool = switch (block.toolType == null ? "pickaxe" : block.toolType) {
                    case "axe" -> "axe";
                    case "shovel" -> "shovel";
                    case "hoe" -> "hoe";
                    default -> "pickaxe";
                };
                mineable.computeIfAbsent(tool, k -> new ArrayList<>()).add(fullId);
                switch (block.toolLevel == null ? "NONE" : block.toolLevel) {
                    case "STONE" -> needsTool.computeIfAbsent("needs_stone_tool", k -> new ArrayList<>()).add(fullId);
                    case "IRON" -> needsTool.computeIfAbsent("needs_iron_tool", k -> new ArrayList<>()).add(fullId);
                    case "DIAMOND" -> needsTool.computeIfAbsent("needs_diamond_tool", k -> new ArrayList<>()).add(fullId);
                    default -> {
                    }
                }
            }
        }
        Path mcTags = dataRoot.resolve("minecraft").resolve("tags").resolve("block");
        for (Map.Entry<String, List<String>> e : mineable.entrySet()) {
            write(mcTags.resolve("mineable").resolve(e.getKey() + ".json"), tagJson(e.getValue()));
        }
        for (Map.Entry<String, List<String>> e : needsTool.entrySet()) {
            write(mcTags.resolve(e.getKey() + ".json"), tagJson(e.getValue()));
        }
        if (!dyeable.isEmpty()) {
            write(dataRoot.resolve("minecraft").resolve("tags").resolve("item")
                .resolve("dyeable.json"), tagJson(dyeable));
        }
    }

    /** 矿石生成: 生成 configured_feature + placed_feature JSON */
    private static void writeOreGen(Path data, BlockDef block, String fullId, String ns) throws IOException {
        BlockDef.OreGenDef ore = block.oreGen;
        if (ore == null || !ore.enabled) return;

        // configured_feature
        JsonObject configured = new JsonObject();
        configured.addProperty("type", "minecraft:ore");
        JsonObject config = new JsonObject();
        config.addProperty("size", Math.max(1, Math.min(64, ore.veinSize)));
        config.addProperty("discard_chance_on_air_exposure", 0.0);
        JsonArray targets = new JsonArray();
        for (String tag : new String[]{"minecraft:stone_ore_replaceables", "minecraft:deepslate_ore_replaceables"}) {
            JsonObject target = new JsonObject();
            JsonObject predicate = new JsonObject();
            predicate.addProperty("predicate_type", "minecraft:tag_match");
            predicate.addProperty("tag", tag);
            target.add("target", predicate);
            JsonObject state = new JsonObject();
            state.addProperty("Name", fullId);
            target.add("state", state);
            targets.add(target);
        }
        config.add("targets", targets);
        configured.add("config", config);
        write(data.resolve("worldgen").resolve("configured_feature").resolve("ore_" + block.id + ".json"), configured);

        // placed_feature
        JsonObject placed = new JsonObject();
        placed.addProperty("feature", ns + ":ore_" + block.id);
        JsonArray placement = new JsonArray();
        JsonObject count = new JsonObject();
        count.addProperty("type", "minecraft:count");
        count.addProperty("count", Math.max(1, Math.min(64, ore.veinsPerChunk)));
        placement.add(count);
        JsonObject inSquare = new JsonObject();
        inSquare.addProperty("type", "minecraft:in_square");
        placement.add(inSquare);
        JsonObject heightRange = new JsonObject();
        heightRange.addProperty("type", "minecraft:height_range");
        JsonObject height = new JsonObject();
        height.addProperty("type", "minecraft:uniform");
        JsonObject min = new JsonObject();
        min.addProperty("absolute", Math.max(-64, ore.minY));
        JsonObject max = new JsonObject();
        max.addProperty("absolute", Math.min(319, Math.max(ore.minY, ore.maxY)));
        height.add("min_inclusive", min);
        height.add("max_inclusive", max);
        heightRange.add("height", height);
        placement.add(heightRange);
        JsonObject biome = new JsonObject();
        biome.addProperty("type", "minecraft:biome");
        placement.add(biome);
        placed.add("placement", placement);
        write(data.resolve("worldgen").resolve("placed_feature").resolve("ore_" + block.id + ".json"), placed);
    }

    private static JsonObject tagJson(List<String> values) {
        JsonObject json = new JsonObject();
        json.addProperty("replace", false);
        JsonArray arr = new JsonArray();
        for (String v : values) arr.add(v);
        json.add("values", arr);
        return json;
    }

    public static JsonObject recipeJson(RecipeDef recipe) {
        if (recipe.result == null || recipe.result.isEmpty()) return null;
        JsonObject json = new JsonObject();
        switch (recipe.type) {
            case "SHAPED" -> {
                RecipeDef.Pattern p = recipe.buildPattern();
                if (p.rows.isEmpty()) return null;
                json.addProperty("type", "minecraft:crafting_shaped");
                JsonArray pattern = new JsonArray();
                for (String row : p.rows) pattern.add(row);
                json.add("pattern", pattern);
                JsonObject key = new JsonObject();
                for (Map.Entry<String, String> e : p.key.entrySet()) {
                    key.add(e.getKey(), ingredient(e.getValue()));
                }
                json.add("key", key);
                json.add("result", resultStack(recipe));
            }
            case "SHAPELESS" -> {
                List<String> ings = recipe.nonEmptyIngredients();
                if (ings.isEmpty()) return null;
                json.addProperty("type", "minecraft:crafting_shapeless");
                JsonArray arr = new JsonArray();
                for (String ing : ings) arr.add(ingredient(ing));
                json.add("ingredients", arr);
                json.add("result", resultStack(recipe));
            }
            case "SMELTING", "BLASTING", "SMOKING" -> {
                if (recipe.input == null || recipe.input.isEmpty()) return null;
                json.addProperty("type", "minecraft:" + recipe.type.toLowerCase());
                json.add("ingredient", ingredient(recipe.input));
                JsonObject result = new JsonObject();
                result.addProperty("id", recipe.result);
                json.add("result", result);
                json.addProperty("experience", recipe.experience);
                json.addProperty("cookingtime", Math.max(1, recipe.cookingTime));
            }
            case "STONECUTTING" -> {
                if (recipe.input == null || recipe.input.isEmpty()) return null;
                json.addProperty("type", "minecraft:stonecutting");
                json.add("ingredient", ingredient(recipe.input));
                json.add("result", resultStack(recipe));
            }
            default -> {
                return null;
            }
        }
        return json;
    }

    private static JsonObject ingredient(String itemId) {
        JsonObject obj = new JsonObject();
        obj.addProperty("item", itemId);
        return obj;
    }

    private static JsonObject resultStack(RecipeDef recipe) {
        JsonObject result = new JsonObject();
        result.addProperty("id", recipe.result);
        result.addProperty("count", Math.max(1, recipe.count));
        return result;
    }

    public static JsonObject lootTableJson(BlockDef block, String fullId) {
        BlockDef.DropDef drop = block.drop != null ? block.drop : new BlockDef.DropDef();
        String mode = drop.mode == null ? "SELF" : drop.mode;
        if ("NONE".equals(mode)) return null; // 无战利品表 = 不掉落

        String dropItem = "CUSTOM".equals(mode) && drop.item != null && !drop.item.isEmpty()
            ? drop.item : fullId;

        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", dropItem);
        if ("CUSTOM".equals(mode) && (drop.min != 1 || drop.max != 1)) {
            JsonArray functions = new JsonArray();
            JsonObject setCount = new JsonObject();
            setCount.addProperty("function", "minecraft:set_count");
            JsonObject count = new JsonObject();
            count.addProperty("type", "minecraft:uniform");
            count.addProperty("min", Math.max(0, drop.min));
            count.addProperty("max", Math.max(drop.min, drop.max));
            setCount.add("count", count);
            functions.add(setCount);
            entry.add("functions", functions);
        }

        JsonArray entries = new JsonArray();
        entries.add(entry);

        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1);
        pool.addProperty("bonus_rolls", 0);
        pool.add("entries", entries);
        JsonArray conditions = new JsonArray();
        JsonObject cond = new JsonObject();
        cond.addProperty("condition", "minecraft:survives_explosion");
        conditions.add(cond);
        pool.add("conditions", conditions);

        JsonArray pools = new JsonArray();
        pools.add(pool);

        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:block");
        json.add("pools", pools);
        return json;
    }

    private static void write(Path path, JsonObject json) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, PackManager.GSON.toJson(json), StandardCharsets.UTF_8);
    }
}
