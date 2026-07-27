package dev.hyxt.modcrafter.client.gui;

import dev.hyxt.modcrafter.data.ContentPack;
import dev.hyxt.modcrafter.data.PackManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * 16x16 像素画板。
 * 支持: 固定调色板 / RGB 与十六进制自定义颜色 / 自定义色槽(持久化) / 取色器 / 填充 / 清空。
 */
public class PainterScreen extends BaseScreen {
    private final String packId;
    private final Consumer<String> callback;

    private final int[][] canvas = new int[16][16];
    private int currentColor = 0xFF303030;
    private boolean eyedropper = false;

    private TextFieldWidget nameField;
    private TextFieldWidget rField;
    private TextFieldWidget gField;
    private TextFieldWidget bField;
    private TextFieldWidget hexField;
    private ButtonWidget eyedropperBtn;

    private int canvasX, canvasY;
    private static final int PIXEL = 11;

    private int paletteX, paletteY;
    private int customY;
    private int rgbY;
    private static final int SWATCH = 16;
    private static final int[] PALETTE = {
        0x00000000, // 橡皮擦(透明)
        0xFF1A1A1A, 0xFF4C4C4C, 0xFF808080, 0xFFB3B3B3, 0xFFE6E6E6, 0xFFFFFFFF,
        0xFF7A2E2E, 0xFFC83737, 0xFFE06666, 0xFFE1953F, 0xFFE8C24A, 0xFFF5EE9E,
        0xFF3F6B2F, 0xFF55AF47, 0xFF9CDB8C, 0xFF2F6B62, 0xFF3CB9B9, 0xFF9CE0DB,
        0xFF2F3F8F, 0xFF4B73D7, 0xFF9FB8F0, 0xFF5E2F8F, 0xFFA04BC8, 0xFFD3A0EC,
        0xFF6B4226, 0xFFA0653C, 0xFFD2A679
    };

    private final String initialName;

    public PainterScreen(Screen parent, String packId, String initialName, Consumer<String> callback) {
        super(parent, Text.translatable("modcrafter.title.painter"));
        this.packId = packId;
        this.callback = callback;
        this.initialName = initialName;
        if (initialName != null) {
            loadExisting(initialName);
        }
    }

    private void loadExisting(String name) {
        try {
            Path file = PackManager.textureFile(packId, name);
            if (Files.exists(file)) {
                BufferedImage img = ImageIO.read(file.toFile());
                if (img != null) {
                    for (int y = 0; y < Math.min(16, img.getHeight()); y++) {
                        for (int x = 0; x < Math.min(16, img.getWidth()); x++) {
                            canvas[y][x] = img.getRGB(x, y);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        canvasX = cx - 155;
        canvasY = 40;
        paletteX = cx + 35;
        paletteY = 40;

        // 固定调色板占 5 行;自定义色槽 2 行(每行 6 个)
        customY = paletteY + 5 * (SWATCH + 2) + 12;
        rgbY = customY + 2 * (SWATCH + 2) + 12;

        label(paletteX, paletteY - 10, Text.translatable("modcrafter.label.palette"));
        label(paletteX, customY - 10, Text.translatable("modcrafter.label.custom_colors"));

        // RGB / HEX 输入
        label(paletteX, rgbY - 10, Text.translatable("modcrafter.label.rgb"));
        rField = smallField(paletteX, rgbY, 34, (currentColor >> 16) & 0xFF);
        gField = smallField(paletteX + 38, rgbY, 34, (currentColor >> 8) & 0xFF);
        bField = smallField(paletteX + 76, rgbY, 34, currentColor & 0xFF);
        hexField = new TextFieldWidget(this.textRenderer, paletteX + 114, rgbY, 56, 18, Text.empty());
        hexField.setMaxLength(7);
        hexField.setText(String.format("#%06X", currentColor & 0xFFFFFF));
        this.addDrawableChild(hexField);
        addBtn(paletteX + 174, rgbY - 1, 40, 20, Text.translatable("modcrafter.btn.apply_color"), this::applyColor);

        int nameY = rgbY + 28;
        label(paletteX, nameY - 10, Text.translatable("modcrafter.label.texture_name"));
        if (nameField == null) {
            nameField = new TextFieldWidget(this.textRenderer, paletteX, nameY, 130, 18, Text.empty());
            nameField.setMaxLength(32);
            nameField.setText(initialName != null ? initialName
                : "tex_" + (PackManager.listCustomTextures(packId).size() + 1));
        } else {
            nameField.setPosition(paletteX, nameY);
        }
        this.addDrawableChild(nameField);

        // 画布下方工具行
        int toolY = canvasY + 16 * PIXEL + 8;
        eyedropperBtn = addBtn(canvasX, toolY, 58, 20, eyedropperText(), () -> {
            eyedropper = !eyedropper;
            eyedropperBtn.setMessage(eyedropperText());
        });
        addBtn(canvasX + 62, toolY, 58, 20, Text.translatable("modcrafter.btn.fill"), () -> {
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++)
                    if (canvas[y][x] == 0) canvas[y][x] = currentColor;
        });
        addBtn(canvasX + 124, toolY, 58, 20, Text.translatable("modcrafter.btn.clear"), () -> {
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++)
                    canvas[y][x] = 0;
        });
        label(canvasX, toolY + 24, Text.translatable("modcrafter.label.painter_hint"), 0x808080);

        addBtn(cx - 105, this.height - 26, 100, 20, Text.translatable("modcrafter.btn.save_texture"), this::save);
        addBtn(cx + 5, this.height - 26, 100, 20, Text.translatable("modcrafter.btn.back"), this::close);
    }

    private Text eyedropperText() {
        return Text.translatable(eyedropper ? "modcrafter.btn.eyedropper_on" : "modcrafter.btn.eyedropper");
    }

    private TextFieldWidget smallField(int x, int y, int w, int value) {
        TextFieldWidget f = new TextFieldWidget(this.textRenderer, x, y, w, 18, Text.empty());
        f.setMaxLength(3);
        f.setText(String.valueOf(value));
        this.addDrawableChild(f);
        return f;
    }

    /** 应用 RGB/HEX 输入为当前颜色,并存入自定义色槽 */
    private void applyColor() {
        int color;
        String hex = hexField.getText().trim().replace("#", "");
        String rgbHexNow = String.format("%06X", currentColor & 0xFFFFFF);
        if (hex.length() == 6 && !hex.equalsIgnoreCase(rgbHexNow)) {
            // 十六进制被修改过 → 用它
            try {
                color = (int) Long.parseLong(hex, 16) | 0xFF000000;
            } catch (Exception e) {
                color = rgbColor();
            }
        } else {
            color = rgbColor();
        }
        setColor(color);
        PaletteStore.add(color);
    }

    private int rgbColor() {
        int r = Math.max(0, Math.min(255, GuiUtil.parseInt(rField.getText(), 48)));
        int g = Math.max(0, Math.min(255, GuiUtil.parseInt(gField.getText(), 48)));
        int b = Math.max(0, Math.min(255, GuiUtil.parseInt(bField.getText(), 48)));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private void setColor(int color) {
        currentColor = color;
        rField.setText(String.valueOf((color >> 16) & 0xFF));
        gField.setText(String.valueOf((color >> 8) & 0xFF));
        bField.setText(String.valueOf(color & 0xFF));
        hexField.setText(String.format("#%06X", color & 0xFFFFFF));
    }

    private void save() {
        String name = nameField.getText().trim().toLowerCase();
        if (!ContentPack.isValidId(name)) {
            setFeedback(Text.translatable("modcrafter.msg.bad_id"), false);
            return;
        }
        try {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    img.setRGB(x, y, canvas[y][x]);
                }
            }
            Path file = PackManager.textureFile(packId, name);
            Files.createDirectories(file.getParent());
            ImageIO.write(img, "PNG", file.toFile());
            GuiUtil.invalidateTexture(packId, "custom:" + name);
            callback.accept("custom:" + name);
            this.close();
        } catch (Exception e) {
            setFeedback(Text.literal("保存失败: " + e.getMessage()), false);
        }
    }

    private boolean paintAt(double mouseX, double mouseY, int button) {
        if (mouseX >= canvasX && mouseX < canvasX + 16 * PIXEL && mouseY >= canvasY && mouseY < canvasY + 16 * PIXEL) {
            int px = (int) ((mouseX - canvasX) / PIXEL);
            int py = (int) ((mouseY - canvasY) / PIXEL);
            if (px >= 0 && px < 16 && py >= 0 && py < 16) {
                if (eyedropper) {
                    if (canvas[py][px] != 0) {
                        setColor(canvas[py][px]);
                    }
                    eyedropper = false;
                    eyedropperBtn.setMessage(eyedropperText());
                    return true;
                }
                canvas[py][px] = button == 1 ? 0 : currentColor;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 固定调色板
        for (int i = 0; i < PALETTE.length; i++) {
            int x = paletteX + (i % 6) * (SWATCH + 2);
            int y = paletteY + (i / 6) * (SWATCH + 2);
            if (mouseX >= x && mouseX < x + SWATCH && mouseY >= y && mouseY < y + SWATCH) {
                if (PALETTE[i] == 0) {
                    currentColor = 0;
                } else {
                    setColor(PALETTE[i]);
                }
                return true;
            }
        }
        // 自定义色槽
        List<Integer> custom = PaletteStore.colors();
        for (int i = 0; i < custom.size(); i++) {
            int x = paletteX + (i % 6) * (SWATCH + 2);
            int y = customY + (i / 6) * (SWATCH + 2);
            if (mouseX >= x && mouseX < x + SWATCH && mouseY >= y && mouseY < y + SWATCH) {
                setColor(custom.get(i));
                return true;
            }
        }
        if (paintAt(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!eyedropper && paintAt(mouseX, mouseY, button)) return true;
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    protected void renderExtra(DrawContext context, int mouseX, int mouseY, float delta) {
        // 画布
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int sx = canvasX + x * PIXEL;
                int sy = canvasY + y * PIXEL;
                int bg = ((x + y) % 2 == 0) ? 0xFF2B2B2B : 0xFF383838;
                context.fill(sx, sy, sx + PIXEL, sy + PIXEL, bg);
                int c = canvas[y][x];
                if (c != 0) {
                    context.fill(sx, sy, sx + PIXEL, sy + PIXEL, c);
                }
            }
        }
        context.fill(canvasX - 1, canvasY - 1, canvasX + 16 * PIXEL + 1, canvasY, 0xFF666666);
        context.fill(canvasX - 1, canvasY + 16 * PIXEL, canvasX + 16 * PIXEL + 1, canvasY + 16 * PIXEL + 1, 0xFF666666);
        context.fill(canvasX - 1, canvasY, canvasX, canvasY + 16 * PIXEL, 0xFF666666);
        context.fill(canvasX + 16 * PIXEL, canvasY, canvasX + 16 * PIXEL + 1, canvasY + 16 * PIXEL, 0xFF666666);

        // 固定调色板
        for (int i = 0; i < PALETTE.length; i++) {
            int x = paletteX + (i % 6) * (SWATCH + 2);
            int y = paletteY + (i / 6) * (SWATCH + 2);
            boolean selected = PALETTE[i] == currentColor || (PALETTE[i] == 0 && currentColor == 0);
            context.fill(x - 1, y - 1, x + SWATCH + 1, y + SWATCH + 1, selected ? 0xFFFFD770 : 0xFF555555);
            if (PALETTE[i] == 0) {
                context.fill(x, y, x + SWATCH, y + SWATCH, 0xFF202020);
                context.drawTextWithShadow(this.textRenderer, "×", x + 5, y + 4, 0xFF6666);
            } else {
                context.fill(x, y, x + SWATCH, y + SWATCH, PALETTE[i]);
            }
        }

        // 自定义色槽
        List<Integer> custom = PaletteStore.colors();
        for (int i = 0; i < PaletteStore.MAX; i++) {
            int x = paletteX + (i % 6) * (SWATCH + 2);
            int y = customY + (i / 6) * (SWATCH + 2);
            if (i < custom.size()) {
                boolean selected = custom.get(i) == currentColor;
                context.fill(x - 1, y - 1, x + SWATCH + 1, y + SWATCH + 1, selected ? 0xFFFFD770 : 0xFF555555);
                context.fill(x, y, x + SWATCH, y + SWATCH, custom.get(i));
            } else {
                context.fill(x - 1, y - 1, x + SWATCH + 1, y + SWATCH + 1, 0xFF3A3A3A);
                context.fill(x, y, x + SWATCH, y + SWATCH, 0xFF232323);
            }
        }

        // 当前颜色预览(名称行右侧)
        int pv = paletteX + 138;
        int pvY = rgbY + 28;
        context.fill(pv - 1, pvY - 1, pv + 19, pvY + 19, 0xFF888888);
        if (currentColor == 0) {
            context.fill(pv, pvY, pv + 18, pvY + 18, 0xFF202020);
        } else {
            context.fill(pv, pvY, pv + 18, pvY + 18, currentColor);
        }
    }
}
