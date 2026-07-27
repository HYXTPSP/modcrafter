package dev.hyxt.modcrafter.runtime;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.hyxt.modcrafter.data.VoxelModel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 体素模型 -> 原版 JSON 模型。
 *
 * 做法:
 *  1. 贪心合并: 相邻同色体素合并成尽量大的长方体(elements 数量可控)
 *  2. 调色板贴图: 每个模型生成一张 16×16 调色板 PNG,每个颜色占一个像素,
 *     元素面用单像素 UV 采样对应颜色(原版特性,无需任何自定义渲染代码)
 *  3. 被完全遮挡的面直接剔除,贴模型边界的面加 cullface
 */
public final class VoxelMesher {
    /** 元素数量硬上限(超过说明模型太碎,渲染会卡) */
    public static final int MAX_ELEMENTS = 1200;

    public record Box(int x1, int y1, int z1, int x2, int y2, int z2, int color) {
    }

    private VoxelMesher() {
    }

    /** 贪心合并。grid[x][y][z] = 调色板下标或 -1 */
    public static List<Box> mesh(int[][][] grid, int size) {
        boolean[][][] used = new boolean[size][size][size];
        List<Box> boxes = new ArrayList<>();
        for (int y = 0; y < size; y++) {
            for (int z = 0; z < size; z++) {
                for (int x = 0; x < size; x++) {
                    if (used[x][y][z] || grid[x][y][z] < 0) continue;
                    int color = grid[x][y][z];
                    // 沿 x 扩展
                    int x2 = x;
                    while (x2 + 1 < size && !used[x2 + 1][y][z] && grid[x2 + 1][y][z] == color) x2++;
                    // 沿 z 扩展
                    int z2 = z;
                    outerZ:
                    while (z2 + 1 < size) {
                        for (int xi = x; xi <= x2; xi++) {
                            if (used[xi][y][z2 + 1] || grid[xi][y][z2 + 1] != color) break outerZ;
                        }
                        z2++;
                    }
                    // 沿 y 扩展
                    int y2 = y;
                    outerY:
                    while (y2 + 1 < size) {
                        for (int zi = z; zi <= z2; zi++) {
                            for (int xi = x; xi <= x2; xi++) {
                                if (used[xi][y2 + 1][zi] || grid[xi][y2 + 1][zi] != color) break outerY;
                            }
                        }
                        y2++;
                    }
                    for (int yi = y; yi <= y2; yi++) {
                        for (int zi = z; zi <= z2; zi++) {
                            for (int xi = x; xi <= x2; xi++) {
                                used[xi][yi][zi] = true;
                            }
                        }
                    }
                    boxes.add(new Box(x, y, z, x2, y2, z2, color));
                }
            }
        }
        return boxes;
    }

    private static boolean filled(int[][][] grid, int size, int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= size || y >= size || z >= size) return false;
        return grid[x][y][z] >= 0;
    }

    /** 判断盒子某个面是否被相邻体素完全遮挡 */
    private static boolean faceHidden(int[][][] grid, int size, Box b, String face) {
        switch (face) {
            case "up" -> {
                for (int z = b.z1(); z <= b.z2(); z++)
                    for (int x = b.x1(); x <= b.x2(); x++)
                        if (!filled(grid, size, x, b.y2() + 1, z)) return false;
            }
            case "down" -> {
                for (int z = b.z1(); z <= b.z2(); z++)
                    for (int x = b.x1(); x <= b.x2(); x++)
                        if (!filled(grid, size, x, b.y1() - 1, z)) return false;
            }
            case "north" -> {
                for (int y = b.y1(); y <= b.y2(); y++)
                    for (int x = b.x1(); x <= b.x2(); x++)
                        if (!filled(grid, size, x, y, b.z1() - 1)) return false;
            }
            case "south" -> {
                for (int y = b.y1(); y <= b.y2(); y++)
                    for (int x = b.x1(); x <= b.x2(); x++)
                        if (!filled(grid, size, x, y, b.z2() + 1)) return false;
            }
            case "west" -> {
                for (int y = b.y1(); y <= b.y2(); y++)
                    for (int z = b.z1(); z <= b.z2(); z++)
                        if (!filled(grid, size, b.x1() - 1, y, z)) return false;
            }
            case "east" -> {
                for (int y = b.y1(); y <= b.y2(); y++)
                    for (int z = b.z1(); z <= b.z2(); z++)
                        if (!filled(grid, size, b.x2() + 1, y, z)) return false;
            }
        }
        return true;
    }

    /**
     * 生成模型 elements。paletteRef 是贴图变量名(如 "#pal")。
     * 返回 null 表示元素超限。
     */
    public static JsonArray elements(VoxelModel model) {
        int size = model.size;
        int[][][] grid = model.decode();
        List<Box> boxes = mesh(grid, size);
        if (boxes.size() > MAX_ELEMENTS) return null;

        float s = 16.0f / size;
        JsonArray elements = new JsonArray();
        String[] faces = {"down", "up", "north", "south", "west", "east"};

        for (Box b : boxes) {
            JsonObject element = new JsonObject();
            element.add("from", arr(b.x1() * s, b.y1() * s, b.z1() * s));
            element.add("to", arr((b.x2() + 1) * s, (b.y2() + 1) * s, (b.z2() + 1) * s));
            JsonObject faceObj = new JsonObject();
            int u = b.color() % 16;
            int v = b.color() / 16;
            for (String face : faces) {
                if (faceHidden(grid, size, b, face)) continue;
                JsonObject f = new JsonObject();
                f.addProperty("texture", "#pal");
                JsonArray uv = new JsonArray();
                // 单像素采样,稍微内缩避免采到邻格
                uv.add(u + 0.25f);
                uv.add(v + 0.25f);
                uv.add(u + 0.75f);
                uv.add(v + 0.75f);
                f.add("uv", uv);
                String cull = cullface(b, size, face);
                if (cull != null) f.addProperty("cullface", cull);
                faceObj.add(face, f);
            }
            if (faceObj.size() == 0) continue; // 全部被遮挡
            element.add("faces", faceObj);
            elements.add(element);
        }
        return elements;
    }

    private static String cullface(Box b, int size, String face) {
        return switch (face) {
            case "down" -> b.y1() == 0 ? "down" : null;
            case "up" -> b.y2() == size - 1 ? "up" : null;
            case "north" -> b.z1() == 0 ? "north" : null;
            case "south" -> b.z2() == size - 1 ? "south" : null;
            case "west" -> b.x1() == 0 ? "west" : null;
            case "east" -> b.x2() == size - 1 ? "east" : null;
            default -> null;
        };
    }

    private static JsonArray arr(float a, float b, float c) {
        JsonArray array = new JsonArray();
        array.add(round(a));
        array.add(round(b));
        array.add(round(c));
        return array;
    }

    private static float round(float v) {
        return Math.round(v * 100f) / 100f;
    }

    /** 生成 16×16 调色板 PNG 字节 */
    public static byte[] paletteTexture(VoxelModel model) throws IOException {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < Math.min(model.palette.size(), VoxelModel.MAX_PALETTE); i++) {
            img.setRGB(i % 16, i / 16, model.colorAt(i));
        }
        return png(img);
    }

    /** 生成 8×8 主色调粒子贴图字节 */
    public static byte[] particleTexture(VoxelModel model) throws IOException {
        // 统计使用最多的颜色
        int[][][] grid = model.decode();
        Map<Integer, Integer> counts = new HashMap<>();
        for (int x = 0; x < model.size; x++)
            for (int y = 0; y < model.size; y++)
                for (int z = 0; z < model.size; z++)
                    if (grid[x][y][z] >= 0) counts.merge(grid[x][y][z], 1, Integer::sum);
        int dominant = counts.entrySet().stream()
            .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(0);
        int base = model.colorAt(dominant);
        BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                // 轻微噪点让粒子不那么死板
                int jitter = ((x * 31 + y * 17) % 5) * 6 - 12;
                img.setRGB(x, y, shift(base, jitter));
            }
        }
        return png(img);
    }

    private static int shift(int argb, int d) {
        int r = Math.max(0, Math.min(255, ((argb >> 16) & 0xFF) + d));
        int g = Math.max(0, Math.min(255, ((argb >> 8) & 0xFF) + d));
        int b = Math.max(0, Math.min(255, (argb & 0xFF) + d));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static byte[] png(BufferedImage img) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", out);
        return out.toByteArray();
    }

    /** 体素包围盒(0-16 坐标系): [minX,minY,minZ,maxX,maxY,maxZ];空模型返回满格 */
    public static double[] bounds(VoxelModel model) {
        int size = model.size;
        int[][][] grid = model.decode();
        int minX = size, minY = size, minZ = size, maxX = -1, maxY = -1, maxZ = -1;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    if (grid[x][y][z] >= 0) {
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        minZ = Math.min(minZ, z);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                        maxZ = Math.max(maxZ, z);
                    }
                }
            }
        }
        if (maxX < 0) return new double[]{0, 0, 0, 16, 16, 16};
        double s = 16.0 / size;
        return new double[]{minX * s, minY * s, minZ * s, (maxX + 1) * s, (maxY + 1) * s, (maxZ + 1) * s};
    }
}
