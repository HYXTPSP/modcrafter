package dev.hyxt.modcrafter.client.gui;

import dev.hyxt.modcrafter.data.PackManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** 贴图选择器: 预设 + 自定义 + 画板入口 */
public class TexturePickScreen extends BaseScreen {
    private final String packId;
    private final String current;
    private final Consumer<String> callback;

    private final List<String> entries = new ArrayList<>();
    private int page = 0;

    private static final int COLS = 10;
    private static final int ROWS = 4;
    private static final int CELL = 26;
    private int gridX, gridY;

    public TexturePickScreen(Screen parent, String packId, String current, Consumer<String> callback) {
        super(parent, Text.translatable("modcrafter.title.pick_texture"));
        this.packId = packId;
        this.current = current;
        this.callback = callback;
    }

    private void buildEntries() {
        entries.clear();
        for (String name : PackManager.listCustomTextures(packId)) {
            entries.add("custom:" + name);
        }
        for (String name : GuiUtil.allPresets()) {
            entries.add("preset:" + name);
        }
    }

    @Override
    protected void init() {
        super.init();
        buildEntries();
        int cx = this.width / 2;
        gridX = cx - (COLS * CELL) / 2;
        gridY = 46;

        int perPage = COLS * ROWS;
        int maxPage = Math.max(0, (entries.size() - 1) / perPage);
        if (page > maxPage) page = maxPage;

        label(gridX, 34, Text.translatable("modcrafter.label.texture_page", page + 1, maxPage + 1));

        int belowY = gridY + ROWS * CELL + 8;
        addBtn(gridX, belowY, 60, 20, Text.literal("<"), () -> {
            if (page > 0) {
                page--;
                rebuild();
            }
        });
        addBtn(gridX + COLS * CELL - 60, belowY, 60, 20, Text.literal(">"), () -> {
            if ((page + 1) * perPage < entries.size()) {
                page++;
                rebuild();
            }
        });

        addBtn(cx - 160, this.height - 28, 100, 20, Text.translatable("modcrafter.btn.open_painter"),
            () -> this.client.setScreen(new PainterScreen(this.parent, packId, null, callback)));
        addBtn(cx - 52, this.height - 28, 100, 20, Text.translatable("modcrafter.btn.import_image"),
            () -> this.client.setScreen(new ImportTextureScreen(this.parent, packId, callback)));
        addBtn(cx + 56, this.height - 28, 100, 20, Text.translatable("modcrafter.btn.back"), this::close);

        label(gridX, belowY + 26, Text.translatable("modcrafter.label.custom_hint"), 0x808080);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int perPage = COLS * ROWS;
        for (int i = 0; i < perPage; i++) {
            int idx = page * perPage + i;
            if (idx >= entries.size()) break;
            int x = gridX + (i % COLS) * CELL;
            int y = gridY + (i / COLS) * CELL;
            if (mouseX >= x && mouseX < x + CELL - 2 && mouseY >= y && mouseY < y + CELL - 2) {
                String ref = entries.get(idx);
                if (button == 1 && ref.startsWith("custom:")) {
                    // 右键自定义贴图 -> 在画板中编辑
                    this.client.setScreen(new PainterScreen(this.parent, packId,
                        ref.substring("custom:".length()), callback));
                    return true;
                }
                callback.accept(ref);
                this.close();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderExtra(DrawContext context, int mouseX, int mouseY, float delta) {
        int perPage = COLS * ROWS;
        for (int i = 0; i < perPage; i++) {
            int idx = page * perPage + i;
            if (idx >= entries.size()) break;
            String ref = entries.get(idx);
            int x = gridX + (i % COLS) * CELL;
            int y = gridY + (i / COLS) * CELL;
            boolean selected = ref.equals(current);
            boolean hover = mouseX >= x && mouseX < x + CELL - 2 && mouseY >= y && mouseY < y + CELL - 2;
            int border = selected ? 0xFFFFD770 : hover ? 0xFFAAAAAA : 0xFF444444;
            context.fill(x - 1, y - 1, x + CELL - 1, y + CELL - 1, border);
            context.fill(x, y, x + CELL - 2, y + CELL - 2, 0xFF1A1A1A);
            GuiUtil.drawPreview(context, packId, ref, x + 2, y + 2, CELL - 6);
            if (ref.startsWith("custom:")) {
                context.drawTextWithShadow(this.textRenderer, "✎", x + CELL - 10, y + 1, 0xFFD770);
            }
        }
        // 悬停显示名称
        int hoverIdx = hoveredIndex(mouseX, mouseY);
        if (hoverIdx >= 0) {
            context.drawCenteredTextWithShadow(this.textRenderer, entries.get(hoverIdx),
                this.width / 2, gridY + ROWS * CELL + 34, 0xFFFFFF);
        }
    }

    private int hoveredIndex(int mouseX, int mouseY) {
        int perPage = COLS * ROWS;
        for (int i = 0; i < perPage; i++) {
            int idx = page * perPage + i;
            if (idx >= entries.size()) break;
            int x = gridX + (i % COLS) * CELL;
            int y = gridY + (i / COLS) * CELL;
            if (mouseX >= x && mouseX < x + CELL - 2 && mouseY >= y && mouseY < y + CELL - 2) return idx;
        }
        return -1;
    }

    void rebuild() {
        this.clearChildren();
        this.init();
    }
}
