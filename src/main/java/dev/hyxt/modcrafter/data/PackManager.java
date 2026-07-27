package dev.hyxt.modcrafter.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.hyxt.modcrafter.ModCrafter;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 内容包的磁盘管理: 加载 / 保存 / 新建 / 删除 / 自动解压分享包 zip。
 * 目录结构:
 *   config/modcrafter/packs/<packId>/pack.json
 *   config/modcrafter/packs/<packId>/textures/<name>.png
 *   config/modcrafter/exports/   (导出产物)
 */
public class PackManager {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final Map<String, ContentPack> PACKS = new LinkedHashMap<>();

    public static Path rootDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("modcrafter");
    }

    public static Path packsDir() {
        return rootDir().resolve("packs");
    }

    public static Path exportsDir() {
        return rootDir().resolve("exports");
    }

    public static Path packDir(String packId) {
        return packsDir().resolve(packId);
    }

    public static Path texturesDir(String packId) {
        return packDir(packId).resolve("textures");
    }

    public static Path textureFile(String packId, String name) {
        return texturesDir(packId).resolve(name + ".png");
    }

    /** 启动时调用: 解压 zip 分享包,然后加载全部 pack */
    public static void loadAll() {
        PACKS.clear();
        try {
            Files.createDirectories(packsDir());
            Files.createDirectories(exportsDir());
        } catch (IOException e) {
            ModCrafter.LOGGER.error("无法创建 ModCrafter 目录", e);
            return;
        }
        extractSharedZips();
        try (Stream<Path> dirs = Files.list(packsDir())) {
            dirs.filter(Files::isDirectory)
                .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .forEach(dir -> {
                    ContentPack pack = loadPack(dir);
                    if (pack != null) {
                        PACKS.put(pack.id, pack);
                        ModCrafter.LOGGER.info("已加载内容包 {} ({} 物品, {} 方块, {} 配方, {} 事件)",
                            pack.id, pack.items.size(), pack.blocks.size(), pack.recipes.size(), pack.events.size());
                    }
                });
        } catch (IOException e) {
            ModCrafter.LOGGER.error("读取内容包目录失败", e);
        }
    }

    /** 把玩家丢进 packs/ 里的 .zip 分享包解压成目录 */
    private static void extractSharedZips() {
        try (Stream<Path> files = Files.list(packsDir())) {
            for (Path zip : files.filter(p -> p.getFileName().toString().endsWith(".zip")).toList()) {
                String base = zip.getFileName().toString();
                base = base.substring(0, base.length() - 4);
                Path target = packsDir().resolve(base);
                if (Files.exists(target)) continue;
                try {
                    unzip(zip, target);
                    Files.deleteIfExists(zip);
                    ModCrafter.LOGGER.info("已导入分享包 {}", base);
                } catch (IOException e) {
                    ModCrafter.LOGGER.error("解压分享包失败: " + zip, e);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static void unzip(Path zip, Path targetDir) throws IOException {
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                Path out = targetDir.resolve(entry.getName()).normalize();
                if (!out.startsWith(targetDir)) continue; // zip slip 防护
                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    Files.copy(in, out);
                }
            }
        }
    }

    private static ContentPack loadPack(Path dir) {
        Path json = dir.resolve("pack.json");
        if (!Files.exists(json)) return null;
        try (InputStream in = Files.newInputStream(json)) {
            ContentPack pack = GSON.fromJson(new String(in.readAllBytes(), StandardCharsets.UTF_8), ContentPack.class);
            if (pack == null || pack.id == null || pack.id.isEmpty()) return null;
            // 目录名与 id 不一致时以目录名为准
            String dirName = dir.getFileName().toString();
            if (!pack.id.equals(dirName) && ContentPack.isValidId(dirName)) pack.id = dirName;
            sanitize(pack);
            return pack;
        } catch (Exception e) {
            ModCrafter.LOGGER.error("解析 pack.json 失败: " + json, e);
            return null;
        }
    }

    private static void sanitize(ContentPack pack) {
        if (pack.items == null) pack.items = new ArrayList<>();
        if (pack.blocks == null) pack.blocks = new ArrayList<>();
        if (pack.recipes == null) pack.recipes = new ArrayList<>();
        if (pack.events == null) pack.events = new ArrayList<>();
        pack.items.removeIf(d -> d == null || !ContentPack.isValidId(d.id));
        pack.blocks.removeIf(d -> d == null || !ContentPack.isValidId(d.id));
        pack.recipes.removeIf(d -> d == null || !ContentPack.isValidId(d.id));
        pack.events.removeIf(d -> d == null || d.id == null || d.id.isEmpty());
        for (RecipeDef r : pack.recipes) {
            if (r.grid == null) r.grid = new ArrayList<>();
            while (r.grid.size() < 9) r.grid.add("");
        }
        for (EventDef e : pack.events) {
            if (e.actions == null) e.actions = new ArrayList<>();
        }
    }

    public static void save(ContentPack pack) {
        try {
            Path dir = packDir(pack.id);
            Files.createDirectories(dir);
            Files.createDirectories(dir.resolve("textures"));
            Files.writeString(dir.resolve("pack.json"), GSON.toJson(pack), StandardCharsets.UTF_8);
        } catch (IOException e) {
            ModCrafter.LOGGER.error("保存内容包失败: " + pack.id, e);
        }
    }

    public static ContentPack create(String id, String name, String author) {
        ContentPack pack = new ContentPack();
        pack.id = id;
        pack.name = name.isEmpty() ? id : name;
        pack.author = author;
        PACKS.put(id, pack);
        save(pack);
        return pack;
    }

    public static void delete(String id) {
        PACKS.remove(id);
        Path dir = packDir(id);
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    public static ContentPack get(String id) {
        return PACKS.get(id);
    }

    public static List<ContentPack> all() {
        return new ArrayList<>(PACKS.values());
    }

    // ===== 体素模型 =====

    public static Path modelsDir(String packId) {
        return packDir(packId).resolve("models");
    }

    public static Path modelFile(String packId, String name) {
        return modelsDir(packId).resolve(name + ".json");
    }

    /** 盔甲层贴图目录 (64×32 PNG) */
    public static Path armorDir(String packId) {
        return packDir(packId).resolve("armor");
    }

    public static List<String> listVoxelModels(String packId) {
        List<String> names = new ArrayList<>();
        Path dir = modelsDir(packId);
        if (!Files.isDirectory(dir)) return names;
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json"))
                .sorted()
                .forEach(p -> {
                    String n = p.getFileName().toString();
                    names.add(n.substring(0, n.length() - 5));
                });
        } catch (IOException ignored) {
        }
        return names;
    }

    public static VoxelModel loadVoxelModel(String packId, String name) {
        Path file = modelFile(packId, name);
        if (!Files.exists(file)) return null;
        try {
            VoxelModel model = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), VoxelModel.class);
            if (model == null || model.size < 4 || model.size > 32) return null;
            if (model.palette == null) model.palette = new ArrayList<>();
            return model;
        } catch (Exception e) {
            ModCrafter.LOGGER.error("读取体素模型失败: " + file, e);
            return null;
        }
    }

    public static void saveVoxelModel(String packId, String name, VoxelModel model) {
        try {
            Path file = modelFile(packId, name);
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(model), StandardCharsets.UTF_8);
        } catch (IOException e) {
            ModCrafter.LOGGER.error("保存体素模型失败: " + name, e);
        }
    }

    public static void deleteVoxelModel(String packId, String name) {
        try {
            Files.deleteIfExists(modelFile(packId, name));
        } catch (IOException ignored) {
        }
    }

    /** 列出某个包的自定义贴图名(不含扩展名) */
    public static List<String> listCustomTextures(String packId) {
        List<String> names = new ArrayList<>();
        Path dir = texturesDir(packId);
        if (!Files.isDirectory(dir)) return names;
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".png"))
                .sorted()
                .forEach(p -> {
                    String n = p.getFileName().toString();
                    names.add(n.substring(0, n.length() - 4));
                });
        } catch (IOException ignored) {
        }
        return names;
    }
}
