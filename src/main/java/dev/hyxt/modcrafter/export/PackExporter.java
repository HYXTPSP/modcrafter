package dev.hyxt.modcrafter.export;

import dev.hyxt.modcrafter.data.ContentPack;
import dev.hyxt.modcrafter.data.PackManager;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** 导出内容包为可分享的 zip(别人把它丢进 config/modcrafter/packs/ 即可) */
public final class PackExporter {

    private PackExporter() {
    }

    public static Path exportZip(ContentPack pack) throws IOException {
        PackManager.save(pack);
        Path src = PackManager.packDir(pack.id);
        Path out = PackManager.exportsDir().resolve(pack.id + ".zip");
        Files.createDirectories(out.getParent());
        try (OutputStream fos = Files.newOutputStream(out);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            try (Stream<Path> walk = Files.walk(src)) {
                for (Path p : walk.filter(Files::isRegularFile).toList()) {
                    String rel = src.relativize(p).toString().replace('\\', '/');
                    zos.putNextEntry(new ZipEntry(rel));
                    Files.copy(p, zos);
                    zos.closeEntry();
                }
            }
        }
        return out;
    }
}
