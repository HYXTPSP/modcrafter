package dev.hyxt.modcrafter.client.gui;

import dev.hyxt.modcrafter.data.ItemDef;
import dev.hyxt.modcrafter.data.PackManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.nio.file.Files;
import java.util.List;

/** 盔甲穿戴外观配置: 原版 / 任意颜色染色 / 自定义护甲层贴图 */
public class ArmorLookScreen extends BaseScreen {
    private final String packId;
    private final String itemId;
    private final ItemDef work;

    private TextFieldWidget colorField;

    private static final List<String> MODES = List.of("VANILLA", "TINT", "CUSTOM");

    public ArmorLookScreen(Screen parent, String packId, String itemId, ItemDef work) {
        super(parent, Text.translatable("modcrafter.title.armor_look"));
        this.packId = packId;
        this.itemId = itemId;
        this.work = work;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int lx = cx - 130;
        int y = 44;

        label(lx, y - 10, Text.translatable("modcrafter.label.armor_tex_mode"));
        this.addDrawableChild(CyclingButtonWidget.<String>builder(v -> Text.translatable("modcrafter.armortex." + v.toLowerCase()))
            .values(MODES).initially(MODES.contains(work.armorTexMode) ? work.armorTexMode : "VANILLA")
            .build(lx, y, 260, 20, Text.translatable("modcrafter.label.armor_tex_mode"),
                (btn, v) -> {
                    work.armorTexMode = v;
                    rebuild();
                }));
        y += 34;

        switch (work.armorTexMode) {
            case "TINT" -> {
                label(lx, y - 10, Text.translatable("modcrafter.label.armor_color"));
                colorField = addField(lx, y, 80, 18, work.armorColor);
                colorField.setMaxLength(7);
                colorField.setChangedListener(text -> work.armorColor = text.trim());
                y += 30;
                label(lx, y, Text.translatable("modcrafter.label.tint_hint"), 0x808080);
                y += 12;
                label(lx, y, Text.translatable("modcrafter.label.tint_hint2"), 0x808080);
            }
            case "CUSTOM" -> {
                for (int layer = 1; layer <= 2; layer++) {
                    final int l = layer;
                    boolean exists = Files.exists(PackManager.armorDir(packId).resolve(itemId + "_layer_" + l + ".png"));
                    label(lx, y + 5, Text.translatable("modcrafter.label.armor_layer" + l));
                    label(lx + 190, y + 5, exists
                        ? Text.translatable("modcrafter.label.imported")
                        : Text.translatable("modcrafter.label.not_imported"), exists ? 0x80FF80 : 0xFF8080);
                    addBtn(lx + 78, y, 105, 20, Text.translatable("modcrafter.btn.import_armor_layer"), () ->
                        this.client.setScreen(ImportTextureScreen.armorLayer(this, packId, itemId, l)));
                    y += 26;
                }
                y += 6;
                label(lx, y, Text.translatable("modcrafter.label.armor_custom_hint"), 0x808080);
                y += 12;
                label(lx, y, Text.translatable("modcrafter.label.armor_custom_hint2"), 0x808080);
            }
            default -> label(lx, y, Text.translatable("modcrafter.label.armor_vanilla_hint"), 0x808080);
        }

        addBtn(cx - 50, this.height - 26, 100, 20, Text.translatable("modcrafter.btn.done"), this::close);
    }

    void rebuild() {
        this.clearChildren();
        this.init();
    }

    @Override
    protected void renderExtra(DrawContext context, int mouseX, int mouseY, float delta) {
        if ("TINT".equals(work.armorTexMode) && colorField != null) {
            int color;
            try {
                color = (int) Long.parseLong(colorField.getText().replace("#", "").trim(), 16) | 0xFF000000;
            } catch (Exception e) {
                color = 0xFFFF5555;
            }
            int x = this.width / 2 - 130 + 90;
            int y = 44 + 34;
            context.fill(x - 1, y - 1, x + 19, y + 19, 0xFF888888);
            context.fill(x, y, x + 18, y + 18, color);
        }
    }
}
