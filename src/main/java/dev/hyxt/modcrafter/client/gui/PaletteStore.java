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

    /** 固定调色板(画板与建模器共用),下标 0 为透明/橡皮擦 */
    public static final int[] FIXED = {
        0x00000000,
        0xFF1A1A1A, 0xFF4C4C4C, 0xFF808080, 0xFFB3B3B3, 0xFFE6E6E6, 0xFFFFFFFF,
        0xFF7A2E2E, 0xFFC83737, 0xFFE06666, 0xFFE1953F, 0xFFE8C24A, 0xFFF5EE9E,
        0xFF3F6B2F, 0xFF55AF47, 0xFF9CDB8C, 0xFF2F6B62, 0xFF3CB9B9, 0xFF9CE0DB,
        0xFF2F3F8F, 0xFF4B73D7, 0xFF9FB8F0, 0xFF5E2F8F, 0xFFA04BC8, 0xFFD3A0EC,
        0xFF6B4226, 0xFFA0653C, 0xFFD2A679
    };
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
