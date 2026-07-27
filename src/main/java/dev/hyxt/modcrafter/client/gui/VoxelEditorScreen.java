package dev.hyxt.modcrafter.client.gui;

import dev.hyxt.modcrafter.data.PackManager;
import dev.hyxt.modcrafter.data.VoxelModel;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 体素建模器: 像搭积木一样逐层(切片)建模,右侧等轴 3D 预览。
 * 俯视视角编辑: 网格横向=东西(x),纵向=南北(z),上边缘是北。
 */
public class VoxelEditorScreen extends BaseScreen {
    private final String packId;
    private final String modelName;
    private final Consumer<String> onUse;   // 可空: "保存并使用"回调
    private final Screen returnTo;          // 保存并使用后回到哪

    private final int size;
    private final int[][][] grid;           // [x][y][z] -> 调色板下标, -1 空
    private final List<Integer> colors = new ArrayList<>(); // 模型调色板(ARGB)

    private int currentColor = 0xFFC83737;
    private int layerY = 0;
    private int rotation = 0;
    private boolean eyedropper = false;

    private TextFieldWidget rField, gField, bField, hexField;
    private ButtonWidget eyedropperBtn;

    private int canvasX, canvasY, cell;
    private int paletteX, paletteY, customY, rgbY, isoX, isoY;
    private static final int SWATCH = 16;

    public VoxelEditorScreen(Screen parent, Screen returnTo, String packId, String modelName,
                             VoxelModel model, Consumer<String> onUse) {
        super(parent, Text.translatable("modcrafter.title.voxel_editor", modelName, model.size));
        this.packId = packId;
        this.modelName = modelName;
        this.onUse = onUse;
        this.returnTo = returnTo;
        this.size = model.size;
        this.grid = model.decode();
        if (model.palette != null) {
            for (int i = 0; i < model.palette.size(); i++) {
                colors.add(model.colorAt(i));
            }
        }
        this.cell = size <= 8 ? 18 : size <= 16 ? 11 : 6;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        canvasX = cx - 165;
        canvasY = 38;
        paletteX = cx + 48;
        paletteY = 38;
        customY = paletteY + 5 * (SWATCH + 2) + 10;
        rgbY = customY + 2 * (SWATCH + 2) + 10;
        isoX = paletteX + 10;
        isoY = rgbY + 30;

        // 画布下方工具
        int toolY = canvasY + size * cell + 6;
        addBtn(canvasX, toolY, 20, 20, Text.literal("▼"), () -> {
            if (layerY > 0) layerY--;
        });
        addBtn(canvasX + 24, toolY, 20, 20, Text.literal("▲"), () -> {
            if (layerY < size - 1) layerY++;
        });
        eyedropperBtn = addBtn(canvasX + 48, toolY, 46, 20, eyedropperText(), () -> {
            eyedropper = !eyedropper;
            eyedropperBtn.setMessage(eyedropperText());
        });
        addBtn(canvasX + 98, toolY, 66, 20, Text.translatable("modcrafter.btn.copy_below"), () -> {
            if (layerY > 0) {
                for (int x = 0; x < size; x++)
                    for (int z = 0; z < size; z++)
                        grid[x][layerY][z] = grid[x][layerY - 1][z];
            }
        });
        int toolY2 = toolY + 24;
        addBtn(canvasX, toolY2, 60, 20, Text.translatable("modcrafter.btn.fill_layer"), () -> {
            int idx = idxFor(currentColor);
            if (idx < 0) return;
            for (int x = 0; x < size; x++)
                for (int z = 0; z < size; z++)
                    if (grid[x][layerY][z] < 0) grid[x][layerY][z] = idx;
        });
        addBtn(canvasX + 64, toolY2, 60, 20, Text.translatable("modcrafter.btn.clear_layer"), () -> {
            for (int x = 0; x < size; x++)
                for (int z = 0; z < size; z++)
                    grid[x][layerY][z] = -1;
        });
        addBtn(canvasX + 128, toolY2, 60, 20, Text.translatable("modcrafter.btn.rotate_preview"),
            () -> rotation = (rotation + 1) % 4);

        label(canvasX, toolY2 + 24, Text.translatable("modcrafter.label.voxel_hint"), 0x808080);
        label(canvasX, toolY2 + 36, Text.translatable("modcrafter.label.voxel_hint2"), 0x808080);

        // 右侧: 调色板标签
        label(paletteX, paletteY - 10, Text.translatable("modcrafter.label.palette"));
        label(paletteX, customY - 10, Text.translatable("modcrafter.label.custom_colors"));

        // RGB / HEX
        rField = smallField(paletteX, rgbY, 34, (currentColor >> 16) & 0xFF);
        gField = smallField(paletteX + 38, rgbY, 34, (currentColor >> 8) & 0xFF);
        bField = smallField(paletteX + 76, rgbY, 34, currentColor & 0xFF);
        hexField = new TextFieldWidget(this.textRenderer, paletteX + 114, rgbY, 56, 18, Text.empty());
        hexField.setMaxLength(7);
        hexField.setText(VoxelModel.toHex(currentColor));
        this.addDrawableChild(hexField);
        addBtn(paletteX + 174, rgbY - 1, 40, 20, Text.translatable("modcrafter.btn.apply_color"), this::applyColor);

        // 底部
        int by = this.height - 26;
        addBtn(cx - 155, by, 95, 20, Text.translatable("modcrafter.btn.save_model"), () -> save(false));
        if (onUse != null) {
            addBtn(cx - 55, by, 110, 20, Text.translatable("modcrafter.btn.save_and_use"), () -> save(true));
        }
        addBtn(cx + 60, by, 95, 20, Text.translatable("modcrafter.btn.back"), this::close);
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

    private void applyColor() {
        int color;
        String hex = hexField.getText().trim().replace("#", "");
        String currentHex = String.format("%06X", currentColor & 0xFFFFFF);
        if (hex.length() == 6 && !hex.equalsIgnoreCase(currentHex)) {
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
        int r = clamp255(GuiUtil.parseInt(rField.getText(), 200));
        int g = clamp255(GuiUtil.parseInt(gField.getText(), 55));
        int b = clamp255(GuiUtil.parseInt(bField.getText(), 55));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private void setColor(int color) {
        currentColor = color;
        rField.setText(String.valueOf((color >> 16) & 0xFF));
        gField.setText(String.valueOf((color >> 8) & 0xFF));
        bField.setText(String.valueOf(color & 0xFF));
        hexField.setText(VoxelModel.toHex(color));
    }

    /** 颜色 -> 模型调色板下标(按需追加);调色板满返回 -1 */
    private int idxFor(int color) {
        for (int i = 0; i < colors.size(); i++) {
            if (colors.get(i) == color) return i;
        }
        if (colors.size() >= VoxelModel.MAX_PALETTE) {
            setFeedback(Text.translatable("modcrafter.msg.palette_full", VoxelModel.MAX_PALETTE), false);
            return -1;
        }
        colors.add(color);
        return colors.size() - 1;
    }

    private void save(boolean use) {
        boolean any = false;
        outer:
        for (int x = 0; x < size; x++)
            for (int y = 0; y < size; y++)
                for (int z = 0; z < size; z++)
                    if (grid[x][y][z] >= 0) {
                        any = true;
                        break outer;
                    }
        if (!any) {
            setFeedback(Text.translatable("modcrafter.msg.model_empty"), false);
            return;
        }
        VoxelModel model = new VoxelModel();
        model.size = size;
        model.palette = new ArrayList<>();
        for (int c : colors) model.palette.add(VoxelModel.toHex(c));
        model.encode(grid);
        PackManager.saveVoxelModel(packId, modelName, model);
        if (use && onUse != null) {
            onUse.accept(modelName);
            this.client.setScreen(returnTo);
        } else {
            setFeedback(Text.translatable("modcrafter.msg.model_saved", modelName), true);
        }
    }

    private boolean inCanvas(double mx, double my) {
        return mx >= canvasX && mx < canvasX + size * cell && my >= canvasY && my < canvasY + size * cell;
    }

    private boolean paintAt(double mx, double my, int button) {
        if (!inCanvas(mx, my)) return false;
        int x = (int) ((mx - canvasX) / cell);
        int z = (int) ((my - canvasY) / cell);
        if (x < 0 || x >= size || z < 0 || z >= size) return false;
        if (eyedropper) {
            int idx = grid[x][layerY][z];
            if (idx >= 0 && idx < colors.size()) {
                setColor(colors.get(idx));
            }
            eyedropper = false;
            eyedropperBtn.setMessage(eyedropperText());
            return true;
        }
        if (button == 1) {
            grid[x][layerY][z] = -1;
        } else {
            int idx = idxFor(currentColor);
            if (idx >= 0) grid[x][layerY][z] = idx;
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 固定调色板
        for (int i = 0; i < PaletteStore.FIXED.length; i++) {
            int x = paletteX + (i % 6) * (SWATCH + 2);
            int y = paletteY + (i / 6) * (SWATCH + 2);
            if (mouseX >= x && mouseX < x + SWATCH && mouseY >= y && mouseY < y + SWATCH) {
                if (PaletteStore.FIXED[i] != 0) setColor(PaletteStore.FIXED[i]);
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
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (inCanvas(mouseX, mouseY)) {
            if (verticalAmount > 0 && layerY < size - 1) layerY++;
            if (verticalAmount < 0 && layerY > 0) layerY--;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private int colorOf(int idx, int fallback) {
        return idx >= 0 && idx < colors.size() ? colors.get(idx) : fallback;
    }

    @Override
    protected void renderExtra(DrawContext context, int mouseX, int mouseY, float delta) {
        // ===== 切片画布 =====
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int sx = canvasX + x * cell;
                int sy = canvasY + z * cell;
                int bg = ((x + z) % 2 == 0) ? 0xFF2B2B2B : 0xFF383838;
                context.fill(sx, sy, sx + cell, sy + cell, bg);
                // 洋葱皮: 下一层的淡影
                if (layerY > 0 && grid[x][layerY][z] < 0) {
                    int below = grid[x][layerY - 1][z];
                    if (below >= 0) {
                        context.fill(sx, sy, sx + cell, sy + cell,
                            (colorOf(below, 0) & 0xFFFFFF) | 0x50000000);
                    }
                }
                int idx = grid[x][layerY][z];
                if (idx >= 0) {
                    context.fill(sx, sy, sx + cell, sy + cell, colorOf(idx, 0xFFFF00FF));
                }
            }
        }
        // 边框
        int cw = size * cell;
        context.fill(canvasX - 1, canvasY - 1, canvasX + cw + 1, canvasY, 0xFF666666);
        context.fill(canvasX - 1, canvasY + cw, canvasX + cw + 1, canvasY + cw + 1, 0xFF666666);
        context.fill(canvasX - 1, canvasY, canvasX, canvasY + cw, 0xFF666666);
        context.fill(canvasX + cw, canvasY, canvasX + cw + 1, canvasY + cw, 0xFF666666);
        // 层指示 + 北方向
        context.drawTextWithShadow(this.textRenderer,
            Text.translatable("modcrafter.label.layer", layerY + 1, size), canvasX, canvasY - 12, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "N↑", canvasX + cw - 16, canvasY - 12, 0x80B0FF);

        // ===== 固定调色板 =====
        for (int i = 0; i < PaletteStore.FIXED.length; i++) {
            int x = paletteX + (i % 6) * (SWATCH + 2);
            int y = paletteY + (i / 6) * (SWATCH + 2);
            boolean selected = PaletteStore.FIXED[i] == currentColor;
            context.fill(x - 1, y - 1, x + SWATCH + 1, y + SWATCH + 1, selected ? 0xFFFFD770 : 0xFF555555);
            if (PaletteStore.FIXED[i] == 0) {
                context.fill(x, y, x + SWATCH, y + SWATCH, 0xFF202020);
            } else {
                context.fill(x, y, x + SWATCH, y + SWATCH, PaletteStore.FIXED[i]);
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
        // 当前颜色预览(RGB 行右上)
        int pv = paletteX + 218;
        context.fill(pv - 1, rgbY - 1, pv + 19, rgbY + 19, 0xFF888888);
        context.fill(pv, rgbY, pv + 18, rgbY + 18, currentColor);

        // ===== 等轴 3D 预览 =====
        renderIso(context);
    }

    private void renderIso(DrawContext context) {
        int u = size <= 8 ? 7 : size <= 16 ? 4 : 2;
        int ox = isoX + size * u;
        int oy = isoY + 8;
        context.drawTextWithShadow(this.textRenderer,
            Text.translatable("modcrafter.label.preview_rot", rotation * 90), isoX, isoY - 10, 0xA0A0A0);
        // 由远及近绘制
        for (int sum = 0; sum <= 2 * (size - 1); sum++) {
            for (int rx = Math.max(0, sum - size + 1); rx <= Math.min(size - 1, sum); rx++) {
                int rz = sum - rx;
                int[] xz = unrotate(rx, rz);
                int gx = xz[0], gz = xz[1];
                for (int y = 0; y < size; y++) {
                    int idx = grid[gx][y][gz];
                    if (idx < 0) continue;
                    // 完全被包住的跳过
                    if (hidden(gx, y, gz)) continue;
                    int color = colorOf(idx, 0xFFFF00FF);
                    int sx = ox + (rx - rz) * u;
                    int sy = oy + (rx + rz) * u / 2 + (size - 1 - y) * u;
                    context.fill(sx, sy, sx + u, sy + u, color);
                    // 顶部高光
                    context.fill(sx, sy, sx + u, sy + Math.max(1, u / 3), lighten(color));
                }
            }
        }
    }

    private boolean hidden(int x, int y, int z) {
        return filled(x + 1, y, z) && filled(x - 1, y, z)
            && filled(x, y + 1, z) && filled(x, y - 1, z)
            && filled(x, y, z + 1) && filled(x, y, z - 1);
    }

    private boolean filled(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= size || y >= size || z >= size) return false;
        return grid[x][y][z] >= 0;
    }

    private int[] unrotate(int rx, int rz) {
        int n = size - 1;
        return switch (rotation) {
            case 1 -> new int[]{n - rz, rx};
            case 2 -> new int[]{n - rx, n - rz};
            case 3 -> new int[]{rz, n - rx};
            default -> new int[]{rx, rz};
        };
    }

    private static int lighten(int argb) {
        int r = Math.min(255, ((argb >> 16) & 0xFF) + 50);
        int g = Math.min(255, ((argb >> 8) & 0xFF) + 50);
        int b = Math.min(255, (argb & 0xFF) + 50);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
