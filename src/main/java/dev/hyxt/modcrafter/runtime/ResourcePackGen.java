package dev.hyxt.modcrafter.runtime;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.hyxt.modcrafter.ModCrafter;
import dev.hyxt.modcrafter.data.BlockDef;
import dev.hyxt.modcrafter.data.ContentPack;
import dev.hyxt.modcrafter.data.ItemDef;
import dev.hyxt.modcrafter.data.PackManager;
import dev.hyxt.modcrafter.data.VoxelModel;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 生成"资源包"(模型 / 贴图 / 语言文件)到 resourcepacks/ModCrafter 目录。
 * 客户端会自动启用该资源包(见 ModCrafterClient)。
 */
public final class ResourcePackGen {
    public static final String PACK_DIR_NAME = "ModCrafter";
    public static final String PACK_PROFILE_ID = "file/" + PACK_DIR_NAME;
    /** 1.21.1 资源包格式 */
    private static final int PACK_FORMAT = 34;

    private ResourcePackGen() {
    }

    public static Path packDir() {
        return FabricLoader.getInstance().getGameDir().resolve("resourcepacks").resolve(PACK_DIR_NAME);
    }

    /** 重新生成整个资源包目录 */
    public static void regenerate() {
        try {
            Path dir = packDir();
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

            JsonObject meta = new JsonObject();
            JsonObject packObj = new JsonObject();
            packObj.addProperty("pack_format", PACK_FORMAT);
            packObj.addProperty("description", "ModCrafter 自动生成的资源包");
            meta.add("pack", packObj);
            writeJson(dir.resolve("pack.mcmeta"), meta);

            for (ContentPack pack : PackManager.all()) {
                generateInto(dir.resolve("assets").resolve(pack.id), pack);
            }
        } catch (Exception e) {
            ModCrafter.LOGGER.error("生成资源包失败", e);
        }
    }

    /** 把一个内容包的 assets 生成到指定目录(资源包与导出工程共用) */
    public static void generateInto(Path assets, ContentPack pack) throws IOException {
        JsonObject lang = new JsonObject();

        // ===== 体素模型(方块与物品共用的几何与调色板贴图) =====
        Set<String> neededModels = new LinkedHashSet<>();
        for (ItemDef def : pack.items) {
            if (def.model != null && !def.model.isEmpty()) neededModels.add(def.model);
        }
        for (BlockDef def : pack.blocks) {
            if ("MODEL".equals(def.textureMode) && def.model != null && !def.model.isEmpty()) {
                neededModels.add(def.model);
            }
        }
        Map<String, JsonArray> okModels = new LinkedHashMap<>();
        for (String name : neededModels) {
            VoxelModel model = PackManager.loadVoxelModel(pack.id, name);
            if (model == null || model.isEmpty()) continue;
            JsonArray elements = VoxelMesher.elements(model);
            if (elements == null) {
                ModCrafter.LOGGER.warn("体素模型 {}:{} 元素超过 {} 个,已跳过(请合并颜色或降低分辨率)",
                    pack.id, name, VoxelMesher.MAX_ELEMENTS);
                continue;
            }
            okModels.put(name, elements);

            // 几何模型(方块风格,带标准显示变换)
            JsonObject blockModel = new JsonObject();
            blockModel.addProperty("parent", "minecraft:block/block");
            JsonObject textures = new JsonObject();
            textures.addProperty("pal", pack.id + ":block/voxel_" + name + "_pal");
            textures.addProperty("particle", pack.id + ":block/voxel_" + name + "_particle");
            blockModel.add("textures", textures);
            blockModel.add("elements", elements);
            writeJson(assets.resolve("models").resolve("block").resolve("voxel_" + name + ".json"), blockModel);

            Path texDir = assets.resolve("textures").resolve("block");
            Files.createDirectories(texDir);
            Files.write(texDir.resolve("voxel_" + name + "_pal.png"), VoxelMesher.paletteTexture(model));
            Files.write(texDir.resolve("voxel_" + name + "_particle.png"), VoxelMesher.particleTexture(model));
        }

        // ===== 物品 =====
        for (ItemDef def : pack.items) {
            lang.addProperty("item." + pack.id + "." + def.id,
                def.name == null || def.name.isEmpty() ? def.id : def.name);

            JsonObject model;
            if (def.model != null && !def.model.isEmpty() && okModels.containsKey(def.model)) {
                // 体素 3D 物品模型(独立文件,带手持/GUI显示变换)
                model = new JsonObject();
                JsonObject textures = new JsonObject();
                textures.addProperty("pal", pack.id + ":block/voxel_" + def.model + "_pal");
                textures.addProperty("particle", pack.id + ":block/voxel_" + def.model + "_particle");
                model.add("textures", textures);
                model.add("elements", okModels.get(def.model));
                model.add("display", voxelItemDisplay());
            } else {
                model = new JsonObject();
                model.addProperty("parent", def.isTool() ? "minecraft:item/handheld" : "minecraft:item/generated");
                JsonObject textures = new JsonObject();
                textures.addProperty("layer0", pack.id + ":item/" + def.id);
                model.add("textures", textures);
                copyTexture(pack, def.texture, assets.resolve("textures").resolve("item").resolve(def.id + ".png"));
            }
            writeJson(assets.resolve("models").resolve("item").resolve(def.id + ".json"), model);

            // 盔甲自定义护甲层贴图
            if (def.isArmor() && "CUSTOM".equals(def.armorTexMode)) {
                copyArmorLayers(pack, def, assets);
            }
        }

        // ===== 方块 =====
        for (BlockDef def : pack.blocks) {
            lang.addProperty("block." + pack.id + "." + def.id,
                def.name == null || def.name.isEmpty() ? def.id : def.name);

            String blockModelRef;
            if ("MODEL".equals(def.textureMode) && okModels.containsKey(def.model)) {
                blockModelRef = pack.id + ":block/voxel_" + def.model;
            } else if ("PER_FACE".equals(def.textureMode)) {
                BlockDef.FacesDef faces = def.faces != null ? def.faces : new BlockDef.FacesDef();
                JsonObject blockModel = new JsonObject();
                blockModel.addProperty("parent", "minecraft:block/cube");
                JsonObject textures = new JsonObject();
                String[][] faceMap = {
                    {"up", faces.up}, {"down", faces.down}, {"north", faces.north},
                    {"south", faces.south}, {"east", faces.east}, {"west", faces.west}
                };
                for (String[] fm : faceMap) {
                    String texName = def.id + "_" + fm[0];
                    textures.addProperty(fm[0], pack.id + ":block/" + texName);
                    copyTexture(pack, fm[1], assets.resolve("textures").resolve("block").resolve(texName + ".png"));
                }
                textures.addProperty("particle", pack.id + ":block/" + def.id + "_north");
                blockModel.add("textures", textures);
                writeJson(assets.resolve("models").resolve("block").resolve(def.id + ".json"), blockModel);
                blockModelRef = pack.id + ":block/" + def.id;
            } else {
                JsonObject blockModel = new JsonObject();
                blockModel.addProperty("parent", "minecraft:block/cube_all");
                JsonObject textures = new JsonObject();
                textures.addProperty("all", pack.id + ":block/" + def.id);
                blockModel.add("textures", textures);
                writeJson(assets.resolve("models").resolve("block").resolve(def.id + ".json"), blockModel);
                copyTexture(pack, def.texture, assets.resolve("textures").resolve("block").resolve(def.id + ".png"));
                blockModelRef = pack.id + ":block/" + def.id;
            }

            // blockstate(含朝向变体)
            writeJson(assets.resolve("blockstates").resolve(def.id + ".json"),
                blockstateJson(blockModelRef, def.facingMode));

            // 方块物品模型
            JsonObject itemModel = new JsonObject();
            itemModel.addProperty("parent", blockModelRef);
            writeJson(assets.resolve("models").resolve("item").resolve(def.id + ".json"), itemModel);
        }

        // 语言文件: 中文和英文写同样的字面名
        writeJson(assets.resolve("lang").resolve("en_us.json"), lang);
        writeJson(assets.resolve("lang").resolve("zh_cn.json"), lang);
    }

    /** 朝向 blockstate: 基准模型面向北 */
    private static JsonObject blockstateJson(String modelRef, String facingMode) {
        JsonObject blockstate = new JsonObject();
        JsonObject variants = new JsonObject();
        if ("HORIZONTAL".equals(facingMode) || "ALL".equals(facingMode)) {
            variants.add("facing=north", variant(modelRef, 0, 0));
            variants.add("facing=east", variant(modelRef, 0, 90));
            variants.add("facing=south", variant(modelRef, 0, 180));
            variants.add("facing=west", variant(modelRef, 0, 270));
            if ("ALL".equals(facingMode)) {
                variants.add("facing=up", variant(modelRef, 270, 0));
                variants.add("facing=down", variant(modelRef, 90, 0));
            }
        } else {
            variants.add("", variant(modelRef, 0, 0));
        }
        blockstate.add("variants", variants);
        return blockstate;
    }

    private static JsonObject variant(String modelRef, int x, int y) {
        JsonObject v = new JsonObject();
        v.addProperty("model", modelRef);
        if (x != 0) v.addProperty("x", x);
        if (y != 0) v.addProperty("y", y);
        return v;
    }

    /** 体素物品模型的显示变换(与原版 block/block 一致) */
    private static JsonObject voxelItemDisplay() {
        JsonObject display = new JsonObject();
        display.add("gui", transform(new float[]{30, 225, 0}, null, new float[]{0.625f, 0.625f, 0.625f}));
        display.add("ground", transform(null, new float[]{0, 3, 0}, new float[]{0.25f, 0.25f, 0.25f}));
        display.add("fixed", transform(null, null, new float[]{0.5f, 0.5f, 0.5f}));
        display.add("thirdperson_righthand", transform(new float[]{75, 45, 0}, new float[]{0, 2.5f, 0}, new float[]{0.375f, 0.375f, 0.375f}));
        display.add("firstperson_righthand", transform(new float[]{0, 45, 0}, null, new float[]{0.40f, 0.40f, 0.40f}));
        display.add("firstperson_lefthand", transform(new float[]{0, 225, 0}, null, new float[]{0.40f, 0.40f, 0.40f}));
        return display;
    }

    private static JsonObject transform(float[] rotation, float[] translation, float[] scale) {
        JsonObject t = new JsonObject();
        if (rotation != null) t.add("rotation", floats(rotation));
        if (translation != null) t.add("translation", floats(translation));
        if (scale != null) t.add("scale", floats(scale));
        return t;
    }

    private static JsonArray floats(float[] values) {
        JsonArray arr = new JsonArray();
        for (float v : values) arr.add(v);
        return arr;
    }

    /** 复制(或生成兜底的)自定义护甲层贴图 */
    private static void copyArmorLayers(ContentPack pack, ItemDef def, Path assets) throws IOException {
        Path armorAssets = assets.resolve("textures").resolve("models").resolve("armor");
        Files.createDirectories(armorAssets);
        for (int layer = 1; layer <= 2; layer++) {
            Path src = PackManager.armorDir(pack.id).resolve(def.id + "_layer_" + layer + ".png");
            Path target = armorAssets.resolve(def.id + "_layer_" + layer + ".png");
            if (Files.exists(src)) {
                Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.write(target, fallbackArmorLayer());
            }
        }
    }

    private static byte[] FALLBACK_ARMOR = null;

    /** 兜底护甲层: 64×32 中性灰 */
    private static byte[] fallbackArmorLayer() throws IOException {
        if (FALLBACK_ARMOR == null) {
            java.awt.image.BufferedImage img =
                new java.awt.image.BufferedImage(64, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 32; y++) {
                for (int x = 0; x < 64; x++) {
                    int shade = 150 + ((x + y) % 3) * 8;
                    img.setRGB(x, y, 0xFF000000 | (shade << 16) | (shade << 8) | shade);
                }
            }
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(img, "PNG", out);
            FALLBACK_ARMOR = out.toByteArray();
        }
        return FALLBACK_ARMOR;
    }

    /**
     * 把贴图引用复制到目标位置。
     * "preset:xxx" -> 模组内置贴图; "custom:xxx" -> 内容包 textures/xxx.png
     */
    private static void copyTexture(ContentPack pack, String ref, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        if (ref != null && ref.startsWith("custom:")) {
            Path src = PackManager.textureFile(pack.id, ref.substring("custom:".length()));
            if (Files.exists(src)) {
                Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING);
                return;
            }
        }
        String presetName = ref != null && ref.startsWith("preset:")
            ? ref.substring("preset:".length()) : "missing";
        if (!copyPreset(presetName, target)) {
            copyPreset("missing", target);
        }
    }

    private static boolean copyPreset(String name, Path target) throws IOException {
        try (InputStream in = ResourcePackGen.class.getResourceAsStream(
            "/assets/modcrafter/textures/preset/" + name + ".png")) {
            if (in == null) return false;
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        }
    }

    private static void writeJson(Path path, JsonObject json) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, PackManager.GSON.toJson(json), StandardCharsets.UTF_8);
    }
}
