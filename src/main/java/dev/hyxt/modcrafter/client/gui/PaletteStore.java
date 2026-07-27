package dev.hyxt.modcrafter.client.gui;

import com.google.gson.reflect.TypeToken;
import dev.hyxt.modcrafter.data.PackManager;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** 玩家自定义颜色槽(最多 12 个,持久化到 config/modcrafter/palette.json) */
public final class PaletteStore {
    public static final int MAX = 12;
    private static List<Integer> colors = null;

    private PaletteStore() {
    }

    private static Path file() {
        return PackManager.rootDir().resolve("palette.json");
    }

    public static List<Integer> colors() {
        if (colors == null) {
            colors = new ArrayList<>();
            try {
                if (Files.exists(file())) {
                    String json = Files.readString(file(), StandardCharsets.UTF_8);
                    List<String> hex = PackManager.GSON.fromJson(json, new TypeToken<List<String>>() {
                    }.getType());
                    if (hex != null) {
                        for (String h : hex) {
                            try {
                                colors.add((int) Long.parseLong(h.replace("#", ""), 16) | 0xFF000000);
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return colors;
    }

    public static void add(int argb) {
        List<Integer> list = colors();
        Integer boxed = argb;
        list.remove(boxed);
        list.add(0, argb);
        while (list.size() > MAX) list.remove(list.size() - 1);
        save();
    }

    private static void save() {
        try {
            Files.createDirectories(file().getParent());
            List<String> hex = new ArrayList<>();
            for (int c : colors()) {
                hex.add(String.format("#%06X", c & 0xFFFFFF));
            }
            Files.writeString(file(), PackManager.GSON.toJson(hex), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }
}
