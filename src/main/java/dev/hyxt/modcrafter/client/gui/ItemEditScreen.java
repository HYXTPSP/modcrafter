package dev.hyxt.modcrafter.client.gui;

import dev.hyxt.modcrafter.client.HotApply;
import dev.hyxt.modcrafter.data.ContentPack;
import dev.hyxt.modcrafter.data.ItemDef;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** 物品编辑器 */
public class ItemEditScreen extends BaseScreen {
    private final ContentPack pack;
    private final ItemDef editing; // null = 新建
    private final ItemDef work;    // 编辑中的副本

    private TextFieldWidget idField;
    private TextFieldWidget nameField;
    private TextFieldWidget tooltipField;
    private TextFieldWidget maxCountField;
    private TextFieldWidget maxDamageField;
    private TextFieldWidget nutritionField;
    private TextFieldWidget saturationField;
    private TextFieldWidget attackDamageField;
    private TextFieldWidget attackSpeedField;
    private CheckboxWidget fireproofBox;
    private CheckboxWidget glintBox;
    private CheckboxWidget alwaysEdibleBox;

    private static final List<String> TYPES = List.of("ITEM", "FOOD", "SWORD", "PICKAXE", "AXE", "SHOVEL", "HOE",
        "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS");
    private static final List<String> RARITIES = List.of("COMMON", "UNCOMMON", "RARE", "EPIC");
    private static final List<String> MATERIALS = List.of("WOOD", "STONE", "IRON", "GOLD", "DIAMOND", "NETHERITE");
    private static final List<String> ARMOR_MATERIALS = List.of("LEATHER", "CHAIN", "IRON", "GOLD", "DIAMOND", "NETHERITE", "TURTLE");

    public ItemEditScreen(Screen parent, ContentPack pack, ItemDef editing) {
        super(parent, Text.translatable(editing == null ? "modcrafter.title.new_item" : "modcrafter.title.edit_item"));
        this.pack = pack;
        this.editing = editing;
        this.work = copy(editing);
    }

    private static ItemDef copy(ItemDef src) {
        ItemDef d = new ItemDef();
        if (src == null) return d;
        copyInto(src, d);
        return d;
    }

    private static void copyInto(ItemDef src, ItemDef d) {
        d.id = src.id;
        d.name = src.name;
        d.tooltip = new ArrayList<>(src.tooltip == null ? List.of() : src.tooltip);
        d.texture = src.texture;
        d.type = src.type;
        d.armorMaterial = src.armorMaterial;
        d.maxCount = src.maxCount;
        d.maxDamage = src.maxDamage;
        d.rarity = src.rarity;
        d.fireproof = src.fireproof;
        d.glint = src.glint;
        if (src.food != null) {
            d.food = new ItemDef.FoodDef();
            d.food.nutrition = src.food.nutrition;
            d.food.saturation = src.food.saturation;
            d.food.alwaysEdible = src.food.alwaysEdible;
        }
        if (src.tool != null) {
            d.tool = new ItemDef.ToolDef();
            d.tool.material = src.tool.material;
            d.tool.attackDamage = src.tool.attackDamage;
            d.tool.attackSpeed = src.tool.attackSpeed;
        }
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int lx = cx - 155;
        int rx = cx + 15;
        int colW = 140;

        int y = 42;
        // 左列
        label(lx, y - 10, Text.translatable("modcrafter.label.elem_id"));
        idField = addField(lx, y, colW, 18, work.id);
        idField.setEditable(editing == null);
        y += 32;

        label(lx, y - 10, Text.translatable("modcrafter.label.elem_name"));
        nameField = addField(lx, y, colW, 18, work.name);
        y += 32;

        label(lx, y - 10, Text.translatable("modcrafter.label.tooltip"));
        tooltipField = addField(lx, y, colW, 18,
            work.tooltip.isEmpty() ? "" : work.tooltip.get(0));
        y += 32;

        label(lx, y - 10, Text.translatable("modcrafter.label.type"));
        this.addDrawableChild(CyclingButtonWidget.<String>builder(v -> Text.translatable("modcrafter.itemtype." + v.toLowerCase()))
            .values(TYPES).initially(TYPES.contains(work.type) ? work.type : "ITEM")
            .build(lx, y, colW, 20, Text.translatable("modcrafter.label.type"),
                (btn, v) -> work.type = v));
        y += 32;

        label(lx, y - 10, Text.translatable("modcrafter.label.rarity"));
        this.addDrawableChild(CyclingButtonWidget.<String>builder(v -> Text.translatable("modcrafter.rarity." + v.toLowerCase()))
            .values(RARITIES).initially(RARITIES.contains(work.rarity) ? work.rarity : "COMMON")
            .build(lx, y, colW, 20, Text.translatable("modcrafter.label.rarity"),
                (btn, v) -> work.rarity = v));
        y += 32;

        addBtn(lx, y, colW, 20, Text.translatable("modcrafter.btn.pick_texture"),
            () -> this.client.setScreen(new TexturePickScreen(this, pack.id, work.texture, ref -> work.texture = ref)));

        // 右列
        y = 42;
        label(rx, y - 10, Text.translatable("modcrafter.label.max_count"));
        maxCountField = addField(rx, y, 60, 18, String.valueOf(work.maxCount));
        label(rx + 70, y - 10, Text.translatable("modcrafter.label.max_damage"));
        maxDamageField = addField(rx + 70, y, 60, 18, String.valueOf(work.maxDamage));
        y += 32;

        fireproofBox = CheckboxWidget.builder(Text.translatable("modcrafter.label.fireproof"), this.textRenderer)
            .pos(rx, y).checked(work.fireproof).build();
        this.addDrawableChild(fireproofBox);
        glintBox = CheckboxWidget.builder(Text.translatable("modcrafter.label.glint"), this.textRenderer)
            .pos(rx + 90, y).checked(work.glint).build();
        this.addDrawableChild(glintBox);
        y += 28;

        // 食物参数
        label(rx, y, Text.translatable("modcrafter.label.food_section"), 0xD8B830);
        y += 14;
        ItemDef.FoodDef food = work.food != null ? work.food : new ItemDef.FoodDef();
        label(rx, y + 4, Text.translatable("modcrafter.label.nutrition"));
        nutritionField = addField(rx + 60, y, 40, 18, String.valueOf(food.nutrition));
        label(rx + 110, y + 4, Text.translatable("modcrafter.label.saturation"));
        saturationField = addField(rx + 155, y, 40, 18, String.valueOf(food.saturation));
        y += 24;
        alwaysEdibleBox = CheckboxWidget.builder(Text.translatable("modcrafter.label.always_edible"), this.textRenderer)
            .pos(rx, y).checked(food.alwaysEdible).build();
        this.addDrawableChild(alwaysEdibleBox);
        y += 28;

        // 工具参数
        label(rx, y, Text.translatable("modcrafter.label.tool_section"), 0xD8B830);
        y += 14;
        ItemDef.ToolDef tool = work.tool != null ? work.tool : new ItemDef.ToolDef();
        this.addDrawableChild(CyclingButtonWidget.<String>builder(v -> Text.translatable("modcrafter.material." + v.toLowerCase()))
            .values(MATERIALS).initially(MATERIALS.contains(tool.material) ? tool.material : "IRON")
            .build(rx, y, 95, 20, Text.translatable("modcrafter.label.material"),
                (btn, v) -> workTool().material = v));
        label(rx + 102, y + 6, Text.translatable("modcrafter.label.attack_damage"));
        attackDamageField = addField(rx + 145, y, 50, 18, String.valueOf(tool.attackDamage));
        y += 26;
        label(rx + 102, y + 6, Text.translatable("modcrafter.label.attack_speed"));
        attackSpeedField = addField(rx + 145, y, 50, 18, String.valueOf(tool.attackSpeed));

        // 盔甲材质(类型=盔甲时生效)
        this.addDrawableChild(CyclingButtonWidget.<String>builder(v -> Text.translatable("modcrafter.material." + v.toLowerCase()))
            .values(ARMOR_MATERIALS).initially(ARMOR_MATERIALS.contains(work.armorMaterial) ? work.armorMaterial : "IRON")
            .build(rx, y, 95, 20, Text.translatable("modcrafter.label.armor_material"),
                (btn, v) -> work.armorMaterial = v));

        // 底部
        addBtn(cx - 105, this.height - 28, 100, 20, Text.translatable("modcrafter.btn.save"), this::save);
        addBtn(cx + 5, this.height - 28, 100, 20, Text.translatable("modcrafter.btn.back"), this::close);
    }

    private ItemDef.ToolDef workTool() {
        if (work.tool == null) work.tool = new ItemDef.ToolDef();
        return work.tool;
    }

    private void save() {
        String id = idField.getText().trim().toLowerCase();
        if (!ContentPack.isValidId(id)) {
            setFeedback(Text.translatable("modcrafter.msg.bad_id"), false);
            return;
        }
        if (editing == null && (pack.findItem(id) != null || pack.findBlock(id) != null)) {
            setFeedback(Text.translatable("modcrafter.msg.id_taken"), false);
            return;
        }
        work.id = id;
        work.name = nameField.getText().trim().isEmpty() ? id : nameField.getText().trim();
        work.tooltip = new ArrayList<>();
        if (!tooltipField.getText().trim().isEmpty()) work.tooltip.add(tooltipField.getText().trim());
        work.maxCount = GuiUtil.parseInt(maxCountField.getText(), 64);
        work.maxDamage = GuiUtil.parseInt(maxDamageField.getText(), 0);
        work.fireproof = fireproofBox.isChecked();
        work.glint = glintBox.isChecked();

        if (work.food == null) work.food = new ItemDef.FoodDef();
        work.food.nutrition = GuiUtil.parseInt(nutritionField.getText(), 4);
        work.food.saturation = GuiUtil.parseFloat(saturationField.getText(), 0.3f);
        work.food.alwaysEdible = alwaysEdibleBox.isChecked();

        if (work.tool == null) work.tool = new ItemDef.ToolDef();
        work.tool.attackDamage = GuiUtil.parseFloat(attackDamageField.getText(), 3.0f);
        work.tool.attackSpeed = GuiUtil.parseFloat(attackSpeedField.getText(), -2.4f);

        if (editing == null) {
            pack.items.add(work);
        } else {
            // 原地覆盖字段,让已注册物品持有的 def 引用立即看到新数据
            // (提示文字/附魔光效即时生效;类型/耐久等需重启)
            copyInto(work, editing);
        }
        HotApply.apply(pack);
        PackScreen ps = new PackScreen(new MainScreen(null), pack);
        ps.tab = PackScreen.Tab.ITEMS;
        this.client.setScreen(ps);
    }

    @Override
    protected void renderExtra(DrawContext context, int mouseX, int mouseY, float delta) {
        // 贴图预览: 左列底部按钮右侧
        int cx = this.width / 2;
        GuiUtil.drawPreview(context, pack.id, work.texture, cx - 155 + 146, 42 + 32 * 5 - 6, 32);
    }
}
