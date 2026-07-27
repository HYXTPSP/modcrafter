package dev.hyxt.modcrafter.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 一个"内容包" —— 玩家在游戏内制作出来的一组内容(物品/方块/配方/事件)。
 * 以 JSON 形式保存在 config/modcrafter/packs/<id>/pack.json。
 * pack 的 id 同时作为注册命名空间(namespace)。
 */
public class ContentPack {
    public String id = "";
    public String name = "";
    public String description = "";
    public String author = "";
    public String version = "1.0.0";

    public List<ItemDef> items = new ArrayList<>();
    public List<BlockDef> blocks = new ArrayList<>();
    public List<RecipeDef> recipes = new ArrayList<>();
    public List<EventDef> events = new ArrayList<>();

    public ItemDef findItem(String id) {
        for (ItemDef d : items) if (d.id.equals(id)) return d;
        return null;
    }

    public BlockDef findBlock(String id) {
        for (BlockDef d : blocks) if (d.id.equals(id)) return d;
        return null;
    }

    public RecipeDef findRecipe(String id) {
        for (RecipeDef d : recipes) if (d.id.equals(id)) return d;
        return null;
    }

    public EventDef findEvent(String id) {
        for (EventDef d : events) if (d.id.equals(id)) return d;
        return null;
    }

    /** 校验 pack id / 元素 id 是否为合法命名空间字符串 */
    public static boolean isValidId(String s) {
        if (s == null || s.isEmpty() || s.length() > 32) return false;
        if (!Character.isLetter(s.charAt(0))) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_')) return false;
        }
        return true;
    }
}
