package dev.hyxt.modcrafter.runtime;

import com.google.gson.JsonObject;
import dev.hyxt.modcrafter.ModCrafter;
import dev.hyxt.modcrafter.data.BlockDef;
import dev.hyxt.modcrafter.data.ContentPack;
import dev.hyxt.modcrafter.data.ItemDef;
import dev.hyxt.modcrafter.data.PackManager;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
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

        for (ItemDef def : pack.items) {
            lang.addProperty("item." + pack.id + "." + def.id,
                def.name == null || def.name.isEmpty() ? def.id : def.name);

            JsonObject model = new JsonObject();
            model.addProperty("parent", def.isTool() ? "minecraft:item/handheld" : "minecraft:item/generated");
            JsonObject textures = new JsonObject();
            textures.addProperty("layer0", pack.id + ":item/" + def.id);
            model.add("textures", textures);
            writeJson(assets.resolve("models").resolve("item").resolve(def.id + ".json"), model);

            copyTexture(pack, def.texture, assets.resolve("textures").resolve("item").resolve(def.id + ".png"));
        }

        for (BlockDef def : pack.blocks) {
            lang.addProperty("block." + pack.id + "." + def.id,
                def.name == null || def.name.isEmpty() ? def.id : def.name);

            JsonObject blockstate = new JsonObject();
            JsonObject variants = new JsonObject();
            JsonObject variant = new JsonObject();
            variant.addProperty("model", pack.id + ":block/" + def.id);
            variants.add("", variant);
            blockstate.add("variants", variants);
            writeJson(assets.resolve("blockstates").resolve(def.id + ".json"), blockstate);

            JsonObject blockModel = new JsonObject();
            blockModel.addProperty("parent", "minecraft:block/cube_all");
            JsonObject textures = new JsonObject();
            textures.addProperty("all", pack.id + ":block/" + def.id);
            blockModel.add("textures", textures);
            writeJson(assets.resolve("models").resolve("block").resolve(def.id + ".json"), blockModel);

            JsonObject itemModel = new JsonObject();
            itemModel.addProperty("parent", pack.id + ":block/" + def.id);
            writeJson(assets.resolve("models").resolve("item").resolve(def.id + ".json"), itemModel);

            copyTexture(pack, def.texture, assets.resolve("textures").resolve("block").resolve(def.id + ".png"));
        }

        // 语言文件: 中文和英文写同样的字面名
        writeJson(assets.resolve("lang").resolve("en_us.json"), lang);
        writeJson(assets.resolve("lang").resolve("zh_cn.json"), lang);
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
