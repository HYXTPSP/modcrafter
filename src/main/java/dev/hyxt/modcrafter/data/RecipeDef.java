package dev.hyxt.modcrafter.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 自定义配方定义 */
public class RecipeDef {
    public String id = "";
    /** SHAPED / SHAPELESS / SMELTING / BLASTING / SMOKING */
    public String type = "SHAPED";

    /** 有序配方: 9 格,空串表示空格子。索引 0-8 对应 3x3。 */
    public List<String> grid = new ArrayList<>();

    /** 无序配方原料(直接复用 grid 中非空格子) */

    /** 熔炼输入 */
    public String input = "";

    public String result = "";
    public int count = 1;

    public float experience = 0.7f;
    public int cookingTime = 200;

    public RecipeDef() {
        for (int i = 0; i < 9; i++) grid.add("");
    }

    /** 生成有序配方的 pattern + key */
    public Pattern buildPattern() {
        // 计算被占用的最小包围盒
        int minR = 3, maxR = -1, minC = 3, maxC = -1;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (!get(r, c).isEmpty()) {
                    if (r < minR) minR = r;
                    if (r > maxR) maxR = r;
                    if (c < minC) minC = c;
                    if (c > maxC) maxC = c;
                }
            }
        }
        Pattern p = new Pattern();
        if (maxR < 0) return p; // 空
        Map<String, Character> assigned = new LinkedHashMap<>();
        char next = 'A';
        for (int r = minR; r <= maxR; r++) {
            StringBuilder row = new StringBuilder();
            for (int c = minC; c <= maxC; c++) {
                String ing = get(r, c);
                if (ing.isEmpty()) {
                    row.append(' ');
                } else {
                    Character ch = assigned.get(ing);
                    if (ch == null) {
                        ch = next;
                        assigned.put(ing, ch);
                        next++;
                    }
                    row.append(ch);
                }
            }
            p.rows.add(row.toString());
        }
        for (Map.Entry<String, Character> e : assigned.entrySet()) {
            p.key.put(String.valueOf(e.getValue()), e.getKey());
        }
        return p;
    }

    public String get(int r, int c) {
        int i = r * 3 + c;
        if (i < 0 || i >= grid.size()) return "";
        String s = grid.get(i);
        return s == null ? "" : s;
    }

    public List<String> nonEmptyIngredients() {
        List<String> list = new ArrayList<>();
        for (String s : grid) if (s != null && !s.isEmpty()) list.add(s);
        return list;
    }

    public boolean isCooking() {
        return "SMELTING".equals(type) || "BLASTING".equals(type) || "SMOKING".equals(type);
    }

    /** 单原料配方(熔炼类 + 切石) */
    public boolean isSingleInput() {
        return isCooking() || "STONECUTTING".equals(type);
    }

    public static class Pattern {
        public List<String> rows = new ArrayList<>();
        public Map<String, String> key = new LinkedHashMap<>();
    }
}
