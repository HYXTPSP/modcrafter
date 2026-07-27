package dev.hyxt.modcrafter.client.gui;

import dev.hyxt.modcrafter.client.HotApply;
import dev.hyxt.modcrafter.data.ContentPack;
import dev.hyxt.modcrafter.data.RecipeDef;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** 配方编辑器: 3x3 摆放 + 熔炼 */
public class RecipeEditScreen extends BaseScreen {
    private final ContentPack pack;
    private final RecipeDef editing;
    private final RecipeDef work;

    private TextFieldWidget idField;
    private TextFieldWidget cellField;
    private TextFieldWidget inputField;
    private TextFieldWidget resultField;
    private TextFieldWidget countField;
    private TextFieldWidget expField;
    private TextFieldWidget timeField;

    private int selectedCell = 0;
    private int gridX, gridY;
    private static final int CELL = 22;

    private static final List<String> TYPES = List.of("SHAPED", "SHAPELESS", "SMELTING", "BLASTING", "SMOKING", "STONECUTTING");

    public RecipeEditScreen(Screen parent, ContentPack pack, RecipeDef editing) {
        super(parent, Text.translatable(editing == null ? "modcrafter.title.new_recipe" : "modcrafter.title.edit_recipe"));
        this.pack = pack;
        this.editing = editing;
        this.work = copy(editing);
    }

    private static RecipeDef copy(RecipeDef src) {
        RecipeDef d = new RecipeDef();
        if (src != null) copyInto(src, d);
        return d;
    }

    private static void copyInto(RecipeDef src, RecipeDef d) {
        d.id = src.id;
        d.type = src.type;
        d.grid = new ArrayList<>(src.grid);
        while (d.grid.size() < 9) d.grid.add("");
        d.input = src.input;
        d.result = src.result;
        d.count = src.count;
        d.experience = src.experience;
        d.cookingTime = src.cookingTime;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int lx = cx - 160;
        int rx = cx + 20;

        int y = 42;
        label(lx, y - 10, Text.translatable("modcrafter.label.elem_id"));
        idField = addField(lx, y, 140, 18, work.id);
        idField.setEditable(editing == null);
        y += 32;

        label(lx, y - 10, Text.translatable("modcrafter.label.recipe_type"));
        this.addDrawableChild(CyclingButtonWidget.<String>builder(v -> Text.translatable("modcrafter.recipetype." + v.toLowerCase()))
            .values(TYPES).initially(TYPES.contains(work.type) ? work.type : "SHAPED")
            .build(lx, y, 140, 20, Text.translatable("modcrafter.label.recipe_type"),
                (btn, v) -> {
                    work.type = v;
                    rebuild();
                }));
        y += 34;

        if (work.isSingleInput()) {
            label(lx, y - 10, Text.translatable("modcrafter.label.smelt_input"));
            inputField = addField(lx, y, 140, 18, work.input);
            y += 32;
            if (work.isCooking()) {
                label(lx, y - 10, Text.translatable("modcrafter.label.experience"));
                expField = addField(lx, y, 60, 18, String.valueOf(work.experience));
                label(lx + 70, y - 10, Text.translatable("modcrafter.label.cook_time"));
                timeField = addField(lx + 70, y, 60, 18, String.valueOf(work.cookingTime));
                y += 32;
            }
        } else {
            // 3x3 摆放区(自绘,见 renderExtra / mouseClicked)
            gridX = lx;
            gridY = y;
            y += CELL * 3 + 10;
            label(lx, y - 6, Text.translatable("modcrafter.label.cell_item", selectedCell % 3 + 1, selectedCell / 3 + 1));
            cellField = addField(lx, y + 4, 140, 18, work.grid.get(selectedCell));
            cellField.setChangedListener(text -> work.grid.set(selectedCell, text.trim()));
            y += 30;
            label(lx, y, Text.translatable("modcrafter.label.grid_hint"), 0x808080);
        }

        // 右列: 产物
        int ry = 42;
        label(rx, ry - 10, Text.translatable("modcrafter.label.result"));
        resultField = addField(rx, ry, 140, 18, work.result);
        ry += 32;
        label(rx, ry - 10, Text.translatable("modcrafter.label.result_count"));
        countField = addField(rx, ry, 60, 18, String.valueOf(work.count));
        ry += 40;
        label(rx, ry, Text.translatable("modcrafter.label.recipe_hint"), 0x808080);
        ry += 12;
        label(rx, ry, Text.translatable("modcrafter.label.recipe_hint2"), 0x808080);

        addBtn(cx - 105, this.height - 28, 100, 20, Text.translatable("modcrafter.btn.save"), this::save);
        addBtn(cx + 5, this.height - 28, 100, 20, Text.translatable("modcrafter.btn.back"), this::close);
    }

    private void save() {
        String id = idField.getText().trim().toLowerCase();
        if (!ContentPack.isValidId(id)) {
            setFeedback(Text.translatable("modcrafter.msg.bad_id"), false);
            return;
        }
        if (editing == null && pack.findRecipe(id) != null) {
            setFeedback(Text.translatable("modcrafter.msg.id_taken"), false);
            return;
        }
        work.id = id;
        work.result = resultField.getText().trim();
        work.count = Math.max(1, GuiUtil.parseInt(countField.getText(), 1));
        if (work.isSingleInput()) {
            work.input = inputField.getText().trim();
            if (work.isCooking()) {
                work.experience = GuiUtil.parseFloat(expField.getText(), 0.7f);
                work.cookingTime = Math.max(1, GuiUtil.parseInt(timeField.getText(), 200));
            }
            if (work.input.isEmpty()) {
                setFeedback(Text.translatable("modcrafter.msg.recipe_no_input"), false);
                return;
            }
        } else {
            if (work.nonEmptyIngredients().isEmpty()) {
                setFeedback(Text.translatable("modcrafter.msg.recipe_no_input"), false);
                return;
            }
        }
        if (work.result.isEmpty()) {
            setFeedback(Text.translatable("modcrafter.msg.recipe_no_result"), false);
            return;
        }

        if (editing == null) {
            pack.recipes.add(work);
        } else {
            copyInto(work, editing);
        }
        HotApply.apply(pack);
        PackScreen ps = new PackScreen(new MainScreen(null), pack);
        ps.tab = PackScreen.Tab.RECIPES;
        this.client.setScreen(ps);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!work.isSingleInput()) {
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    int x = gridX + c * CELL;
                    int y = gridY + r * CELL;
                    if (mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL) {
                        selectedCell = r * 3 + c;
                        rebuild();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    void rebuild() {
        this.clearChildren();
        this.init();
    }

    @Override
    protected void renderExtra(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!work.isSingleInput()) {
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    int i = r * 3 + c;
                    int x = gridX + c * CELL;
                    int y = gridY + r * CELL;
                    int border = i == selectedCell ? 0xFFFFD770 : 0xFF555555;
                    context.fill(x, y, x + CELL - 2, y + CELL - 2, border);
                    context.fill(x + 1, y + 1, x + CELL - 3, y + CELL - 3, 0xFF222222);
                    String ing = work.grid.get(i);
                    if (!ing.isEmpty()) {
                        ItemStack stack = GuiUtil.stackOf(ing);
                        if (!stack.isEmpty()) {
                            context.drawItem(stack, x + 2, y + 2);
                        } else {
                            context.drawTextWithShadow(this.textRenderer, "?", x + 8, y + 6, 0xFF5555);
                        }
                    }
                }
            }
        }
        // 产物预览
        ItemStack result = GuiUtil.stackOf(resultField == null ? work.result : resultField.getText().trim());
        int rx = this.width / 2 + 20;
        if (!result.isEmpty()) {
            context.drawItem(result, rx + 146, 42);
        }
    }
}
