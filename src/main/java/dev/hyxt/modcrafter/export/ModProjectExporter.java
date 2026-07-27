package dev.hyxt.modcrafter.export;

import dev.hyxt.modcrafter.ModCrafter;
import dev.hyxt.modcrafter.data.ContentPack;
import dev.hyxt.modcrafter.data.PackManager;
import dev.hyxt.modcrafter.runtime.DatapackGen;
import dev.hyxt.modcrafter.runtime.ResourcePackGen;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 把内容包导出为一个完整的、可独立编译的 Fabric 模组工程。
 * 生成的工程不依赖 ModCrafter —— 它内置一个小型运行时,
 * 从打包进 jar 的 pack.json 注册内容并执行事件。
 */
public final class ModProjectExporter {

    private ModProjectExporter() {
    }

    public static Path export(ContentPack pack) throws IOException {
        PackManager.save(pack);
        Path root = PackManager.exportsDir().resolve(pack.id + "_mod");
        deleteRecursively(root);
        Files.createDirectories(root);

        String pkg = "crafted." + pack.id;
        String pkgPath = pkg.replace('.', '/');

        // 1. Gradle 构建文件
        writeTemplate(root.resolve("build.gradle"), "build.gradle.tpl", pack, pkg);
        writeTemplate(root.resolve("settings.gradle"), "settings.gradle.tpl", pack, pkg);
        writeTemplate(root.resolve("gradle.properties"), "gradle.properties.tpl", pack, pkg);
        writeTemplate(root.resolve("README.md"), "README.md.tpl", pack, pkg);

        // 2. Gradle wrapper
        copyResource("wrapper/gradle-wrapper.jar", root.resolve("gradle/wrapper/gradle-wrapper.jar"));
        copyResource("wrapper/gradle-wrapper.properties", root.resolve("gradle/wrapper/gradle-wrapper.properties"));
        copyResource("wrapper/gradlew", root.resolve("gradlew"));
        copyResource("wrapper/gradlew.bat", root.resolve("gradlew.bat"));
        makeExecutable(root.resolve("gradlew"));

        // 3. Java 源码(小型运行时)
        Path javaDir = root.resolve("src/main/java").resolve(pkgPath);
        writeTemplate(javaDir.resolve("PackMod.java"), "PackMod.java.tpl", pack, pkg);
        writeTemplate(javaDir.resolve("PackModClient.java"), "PackModClient.java.tpl", pack, pkg);
        writeTemplate(javaDir.resolve("Defs.java"), "Defs.java.tpl", pack, pkg);
        writeTemplate(javaDir.resolve("ContentClasses.java"), "ContentClasses.java.tpl", pack, pkg);
        writeTemplate(javaDir.resolve("EventRt.java"), "EventRt.java.tpl", pack, pkg);

        // 4. 资源
        Path resources = root.resolve("src/main/resources");
        writeTemplate(resources.resolve("fabric.mod.json"), "fabric.mod.json.tpl", pack, pkg);

        // pack.json 原样打包进 jar
        Path packJson = PackManager.packDir(pack.id).resolve("pack.json");
        Files.createDirectories(resources.resolve("packdata"));
        Files.copy(packJson, resources.resolve("packdata/pack.json"), StandardCopyOption.REPLACE_EXISTING);

        // 体素模型方块的包围盒(碰撞/轮廓箱),导出时预计算
        java.util.Map<String, double[]> shapes = new java.util.LinkedHashMap<>();
        for (dev.hyxt.modcrafter.data.BlockDef blockDef : pack.blocks) {
            double[] bounds = dev.hyxt.modcrafter.runtime.PackRegistrar.modelBounds(blockDef, pack.id);
            if (bounds != null) shapes.put(blockDef.id, bounds);
        }
        Files.writeString(resources.resolve("packdata/shapes.json"),
            PackManager.GSON.toJson(shapes), StandardCharsets.UTF_8);

        // assets(模型/贴图/语言)与 data(配方/掉落/标签)
        ResourcePackGen.generateInto(resources.resolve("assets").resolve(pack.id), pack);
        DatapackGen.generateSinglePackData(resources.resolve("data"), pack);

        return root;
    }

    private static void writeTemplate(Path target, String templateName, ContentPack pack, String pkg) throws IOException {
        String content = readResource(templateName);
        content = content
            .replace("{{PACKAGE}}", pkg)
            .replace("{{PACK_ID}}", pack.id)
            .replace("{{PACK_NAME}}", escapeJson(pack.name == null || pack.name.isEmpty() ? pack.id : pack.name))
            .replace("{{PACK_DESC}}", escapeJson(pack.description == null ? "" : pack.description))
            .replace("{{PACK_AUTHOR}}", escapeJson(pack.author == null || pack.author.isEmpty() ? "ModCrafter" : pack.author))
            .replace("{{PACK_VERSION}}", pack.version == null || pack.version.isEmpty() ? "1.0.0" : pack.version);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content, StandardCharsets.UTF_8);
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String readResource(String name) throws IOException {
        try (InputStream in = ModProjectExporter.class.getResourceAsStream("/modproject_template/" + name)) {
            if (in == null) throw new IOException("缺少模板: " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void copyResource(String name, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        try (InputStream in = ModProjectExporter.class.getResourceAsStream("/modproject_template/" + name)) {
            if (in == null) throw new IOException("缺少模板: " + name);
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void makeExecutable(Path file) {
        try {
            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(file);
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            perms.add(PosixFilePermission.GROUP_EXECUTE);
            perms.add(PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(file, perms);
        } catch (Exception ignored) {
            // Windows 上没有 POSIX 权限,忽略
        }
    }

    private static void deleteRecursively(Path dir) {
        if (!Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    ModCrafter.LOGGER.warn("无法删除 {}", p);
                }
            });
        } catch (IOException ignored) {
        }
    }
}
