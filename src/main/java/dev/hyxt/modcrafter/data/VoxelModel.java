package dev.hyxt.modcrafter.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 体素模型: size³ 的稀疏体素网格 + 调色板。
 * 持久化格式(JSON, packs/<id>/models/<name>.json):
 *   size: 8/16/32
 *   palette: ["#RRGGBB", ...]  最多 62 色
 *   layers: {"0": "<size*size 个字符>", ...}  每层一个字符串,'.'=空,
 *           字符表 0-9a-zA-Z 对应调色板下标 0-61;字符串按 z 行、x 列排列(俯视)
 */
public class VoxelModel {
    public static final String CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final int MAX_PALETTE = CHARS.length();

    public int size = 16;
    public List<String> palette = new ArrayList<>();
    public Map<String, String> layers = new LinkedHashMap<>();

    /** 解码为 [x][y][z] 下标网格,-1 = 空 */
    public int[][][] decode() {
        int[][][] grid = new int[size][size][size];
        for (int[][] plane : grid) {
            for (int[] row : plane) {
                java.util.Arrays.fill(row, -1);
            }
        }
        if (layers == null) return grid;
        for (Map.Entry<String, String> e : layers.entrySet()) {
            int y;
            try {
                y = Integer.parseInt(e.getKey());
            } catch (NumberFormatException ex) {
                continue;
            }
            if (y < 0 || y >= size || e.getValue() == null) continue;
            String s = e.getValue();
            for (int i = 0; i < Math.min(s.length(), size * size); i++) {
                char c = s.charAt(i);
                if (c == '.') continue;
                int idx = CHARS.indexOf(c);
                if (idx < 0) continue;
                int z = i / size;
                int x = i % size;
                grid[x][y][z] = idx;
            }
        }
        return grid;
    }

    /** 从网格编码回本对象的 layers */
    public void encode(int[][][] grid) {
        layers = new LinkedHashMap<>();
        for (int y = 0; y < size; y++) {
            StringBuilder sb = new StringBuilder(size * size);
            boolean any = false;
            for (int z = 0; z < size; z++) {
                for (int x = 0; x < size; x++) {
                    int idx = grid[x][y][z];
                    if (idx >= 0 && idx < MAX_PALETTE) {
                        sb.append(CHARS.charAt(idx));
                        any = true;
                    } else {
                        sb.append('.');
                    }
                }
            }
            if (any) {
                layers.put(String.valueOf(y), sb.toString());
            }
        }
    }

    /** 调色板颜色解析为 ARGB(不透明) */
    public int colorAt(int idx) {
        if (palette == null || idx < 0 || idx >= palette.size()) return 0xFFFF00FF;
        try {
            return (int) Long.parseLong(palette.get(idx).replace("#", ""), 16) | 0xFF000000;
        } catch (Exception e) {
            return 0xFFFF00FF;
        }
    }

    public boolean isEmpty() {
        return layers == null || layers.isEmpty();
    }

    public static String toHex(int argb) {
        return String.format("#%06X", argb & 0xFFFFFF);
    }
}
