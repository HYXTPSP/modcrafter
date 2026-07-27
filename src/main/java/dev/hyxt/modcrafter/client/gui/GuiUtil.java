package dev.hyxt.modcrafter.client.gui;

import dev.hyxt.modcrafter.data.PackManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** GUI 公共工具: 贴图预览 / 数字解析 / 物品 id 校验 */
public final class GuiUtil {
    /** 预设与自定义贴图的动态纹理缓存 */
    private static final Map<String, Identifier> TEXTURE_CACHE = new HashMap<>();

    private GuiUtil() {
    }

    /**
     * 取得贴图引用("preset:x" / "custom:x")对应的可绘制纹理。
     * custom 贴图属于某个内容包,packId 用于定位文件。
     */
    public static Identifier previewTexture(String packId, String ref) {
        if (ref == null || ref.isEmpty()) ref = "preset:missing";
        String cacheKey = ref.startsWith("custom:") ? packId + "/" + ref : ref;
        Identifier cached = TEXTURE_CACHE.get(cacheKey);
        if (cached != null) return cached;

        MinecraftClient client = MinecraftClient.getInstance();
        try {
            NativeImage image = null;
            if (ref.startsWith("preset:")) {
                try (InputStream in = GuiUtil.class.getResourceAsStream(
                    "/assets/modcrafter/textures/preset/" + ref.substring("preset:".length()) + ".png")) {
                    if (in != null) image = NativeImage.read(in);
                }
            } else if (ref.startsWith("custom:")) {
                Path file = PackManager.textureFile(packId, ref.substring("custom:".length()));
                if (Files.exists(file)) {
                    try (InputStream in = Files.newInputStream(file)) {
                        image = NativeImage.read(in);
                    }
                }
            }
            if (image == null) {
                return previewTexture(packId, "preset:missing");
            }
            String safe = cacheKey.replaceAll("[^a-z0-9_/]", "_").toLowerCase();
            Identifier id = Identifier.of("modcrafter", "dyn/" + safe);
            client.getTextureManager().registerTexture(id, new NativeImageBackedTexture(image));
            TEXTURE_CACHE.put(cacheKey, id);
            return id;
        } catch (Exception e) {
            return Identifier.of("modcrafter", "textures/preset/missing.png");
        }
    }

    /** 自定义贴图被重新绘制后清缓存 */
    public static void invalidateTexture(String packId, String ref) {
        String cacheKey = ref.startsWith("custom:") ? packId + "/" + ref : ref;
        Identifier id = TEXTURE_CACHE.remove(cacheKey);
        if (id != null) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(id);
        }
    }

    /** 画一个贴图预览(等比缩放到 size×size) */
    public static void drawPreview(DrawContext context, String packId, String ref, int x, int y, int size) {
        Identifier tex = previewTexture(packId, ref);
        context.drawTexture(tex, x, y, size, size, 0f, 0f, 16, 16, 16, 16);
    }

    public static int parseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    public static float parseFloat(String s, float fallback) {
        try {
            return Float.parseFloat(s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    /** 校验一个物品 id 是否已注册,返回其 ItemStack(无效返回空) */
    public static ItemStack stackOf(String itemId) {
        if (itemId == null || itemId.isEmpty()) return ItemStack.EMPTY;
        Identifier id = Identifier.tryParse(itemId);
        if (id == null) return ItemStack.EMPTY;
        if (!Registries.ITEM.containsId(id)) return ItemStack.EMPTY;
        Item item = Registries.ITEM.get(id);
        if (item == Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(item);
    }

    public static boolean isValidItemId(String itemId) {
        return !stackOf(itemId).isEmpty();
    }

    /** 预设贴图完整清单(与 gen_textures.py 保持一致) */
    public static final String[] PRESET_SHAPES = {
        "gem", "ingot", "dust", "orb", "apple", "potion",
        "sword", "pickaxe", "axe", "shovel",
        "block", "brick", "planks", "ore"
    };
    public static final String[] PRESET_COLORS = {
        "red", "orange", "yellow", "green", "cyan", "blue", "purple", "white"
    };

    public static java.util.List<String> allPresets() {
        java.util.List<String> list = new java.util.ArrayList<>();
        for (String shape : PRESET_SHAPES) {
            for (String color : PRESET_COLORS) {
                list.add(shape + "_" + color);
            }
        }
        return list;
    }
}
