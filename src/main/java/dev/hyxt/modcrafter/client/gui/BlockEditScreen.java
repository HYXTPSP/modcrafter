package dev.hyxt.modcrafter.client.gui;

import dev.hyxt.modcrafter.client.HotApply;
import dev.hyxt.modcrafter.data.BlockDef;
import dev.hyxt.modcrafter.data.ContentPack;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

/** 方块编辑器 */
public class BlockEditScreen extends BaseScreen {
    private final ContentPack pack;
    private final BlockDef editing;
    private final BlockDef work;

    private TextFieldWidget idField;
    private TextFieldWidget nameField;
    private TextFieldWidget hardnessField;
    private TextFieldWidget resistanceField;
    private TextFieldWidget luminanceField;
    private TextFieldWidget slipperinessField;
    private TextFieldWidget dropItemField;
    private TextFieldWidget dropMinField;
    private TextFieldWidget dropMaxField;
    private TextFieldWidget veinSizeField;
    private TextFieldWidget veinsPerChunkField;
    private TextFieldWidget minYField;
    private TextFieldWidget maxYField;
    private CheckboxWidget requiresToolBox;
    private CheckboxWidget transparentBox;
    private CheckboxWidget oreGenBox;

    private static final List<String> SOUNDS = List.of("STONE", "WOOD", "METAL", "GLASS", "GRASS", "SAND", "WOOL");
    private static final List<String> TOOL_TYPES = List.of("pickaxe", "axe", "shovel", "hoe");
    private static final List<String> TOOL_LEVELS = List.of("NONE", "STONE", "IRON", "DIAMOND");
    private static final List<String> DROP_MODES = List.of("SELF", "NONE", "CUSTOM");
    private static final List<String> TEX_MODES = List.of("SINGLE", "PER_FACE", "MODEL");
    private static final List<String> FACING_MODES = List.of("NONE", "HORIZONTAL", "ALL");

    public BlockEditScreen(Screen parent, ContentPack pack, BlockDef editing) {
        super(parent, Text.translatable(editing == null ? "modcrafter.title.new_block" : "modcrafter.title.edit_block"));
        this.pack = pack;
        this.editing = editing;
        this.work = copy(editing);
    }

    private static BlockDef copy(BlockDef src) {
        BlockDef d = new BlockDef();
        if (src != null) copyInto(src, d);
        return d;
    }

    private static void copyInto(BlockDef src, BlockDef d) {
        d.id = src.id;
        d.name = src.name;
        d.texture = src.texture;
        d.hardness = src.hardness;
        d.resistance = src.resistance;
        d.luminance = src.luminance;
        d.requiresTool = src.requiresTool;
        d.toolType = src.toolType;
        d.toolLevel = src.toolLevel;
        d.sound = src.sound;
        d.transparent = src.transparent;
        d.slipperiness = src.slipperiness;
        d.textureMode = src.textureMode;
        d.model = src.model;
        d.facingMode = src.facingMode;
        d.faces = new BlockDef.FacesDef();
        if (src.faces != null) {
            d.faces.up = src.faces.up;
            d.faces.down = src.faces.down;
            d.faces.north = src.faces.north;
            d.faces.south = src.faces.south;
            d.faces.east = src.faces.east;
            d.faces.west = src.faces.west;
        }
        d.drop = new BlockDef.DropDef();
        if (src.drop != null) {
            d.drop.mode = src.drop.mode;
            d.drop.item = src.drop.item;
            d.drop.min = src.drop.min;
            d.drop.max = src.drop.max;
        }
        d.oreGen = new BlockDef.OreGenDef();
        if (src.oreGen != null) {
            d.oreGen.enabled = src.oreGen.enabled;
            d.oreGen.veinSize = src.oreGen.veinSize;
            d.oreGen.veinsPerChunk = src.oreGen.veinsPerChunk;
            d.oreGen.minY = src.oreGen.minY;
            d.oreGen.maxY = src.oreGen.maxY;
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
        label(lx, y - 10, Text.translatable("modcrafter.label.elem_id"));
        idField = addField(lx, y, colW, 18, work.id);
        idField.setEditable(editing == null);
        y += 28;

        label(lx, y - 10, Text.translatable("modcrafter.label.elem_name"));
        nameField = addField(lx, y, colW, 18, work.name);
        y += 28;

        label(lx, y - 10, Text.translatable("modcrafter.label.hardness"));
        hardnessField = addField(lx, y, 60, 18, String.valueOf(work.hardness));
        label(lx + 70, y - 10, Text.translatable("modcrafter.label.resistance"));
        resistanceField = addField(lx + 70, y, 60, 18, String.valueOf(work.resistance));
        y += 28;

        label(lx, y - 10, Text.translatable("modcrafter.label.luminance"));
        luminanceField = addField(lx, y, 60, 18, String.valueOf(work.luminance));
        label(lx + 70, y - 10, Text.translatable("modcrafter.label.slipperiness"));
        slipperinessField = addField(lx + 70, y, 60, 18, String.valueOf(work.slipperiness));
        y += 26;

        this.addDrawableChild(CyclingButtonWidget.<String>builder(v -> Text.translatable("modcrafter.sound." + v.toLowerCase()))
            .values(SOUNDS).initially(SOUNDS.contains(work.sound) ? work.sound : "STONE")
            .build(lx, y, colW, 20, Text.translatable("modcrafter.label.sound"),
                (btn, v) -> work.sound = v));
        y += 22;

        transparentBox = CheckboxWidget.builder(Text.translatable("modcrafter.label.transparent"), this.textRenderer)
            .pos(lx, y).checked(work.transparent).build();
        this.addDrawableChild(transparentBox);
        y += 22;

        // 外观模式: 单一贴图 / 六面贴图 / 体素模型
        this.addDrawableChild(CyclingButtonWidget.<String>builder(v -> Text.translatable("modcrafter.texmode." + v.toLowerCase()))
            .values(TEX_MODES).initially(TEX_MODES.contains(work.textureMode) ? work.textureMode : "SINGLE")
            .build(lx, y, colW, 20, Text.translatable("modcrafter.label.tex_mode"),
                (btn, v) -> {
                    work.textureMode = v;
                    rebuild();
                }));
        y += 22;

        switch (work.textureMode) {
            case "PER_FACE" -> addBtn(lx, y, colW, 20, Text.translatable("modcrafter.btn.face_textures"),
                () -> this.client.setScreen(new FaceTexturesScreen(this, pack.id, work)));
            case "MODEL" -> {
                addBtn(lx, y, colW, 20, Text.translatable(work.model == null || work.model.isEmpty()
                        ? "modcrafter.btn.pick_model" : "modcrafter.btn.pick_model_set"),
                    () -> this.client.setScreen(new VoxelModelPickScreen(this, pack.id, work.model, name -> work.model = name)));
                if (work.model != null && !work.model.isEmpty()) {
                    label(lx + colW + 4, y + 6, Text.literal("§b" + work.model));
                }
            }
            default -> addBtn(lx, y, colW, 20, Text.translatable("modcrafter.btn.pick_texture"),
                () -> this.client.setScreen(new TexturePickScreen(this, pack.id, work.texture, ref -> work.texture = ref)));
        }

        // ===== 右列 =====
        y = 42;
        requiresToolBox = CheckboxWidget.builder(Text.translatable("modcrafter.label.requires_tool"), this.textRenderer)
            .pos(rx, y).checked(work.requiresTool).build();
        this.addDrawableChild(requiresToolBox);
        y += 26;

        this.addDrawableChild(CyclingButtonWidget.<String>builder(v -> Text.translatable("modcrafter.tooltype." + v))
            .values(TOOL_TYPES).initially(TOOL_TYPES.contains(work.toolType) ? work.toolType : "pickaxe")
            .build(rx, y, 95, 20, Text.translatable("modcrafter.label.tool_type"),
                (btn, v) -> work.toolType = v));
        this.addDrawableChild(CyclingButtonWidget.<String>builder(v -> Text.translatable("modcrafter.toollevel." + v.toLowerCase()))
            .values(TOOL_LEVELS).initially(TOOL_LEVELS.contains(work.toolLevel) ? work.toolLevel : "NONE")
            .build(rx + 100, y, 95, 20, Text.translatable("modcrafter.label.tool_level"),
                (btn, v) -> work.toolLevel = v));
        y += 26;

        this.addDrawableChild(CyclingButtonWidget.<String>builder(v -> Text.translatable("modcrafter.dropmode." + v.toLowerCase()))
            .values(DROP_MODES).initially(DROP_MODES.contains(work.drop.mode) ? work.drop.mode : "SELF")
            .build(rx, y, 95, 20, Text.translatable("modcrafter.label.drop_mode"),
                (btn, v) -> work.drop.mode = v));
        dropMinField = addField(rx + 100, y + 1, 40, 18, String.valueOf(work.drop.min));
        label(rx + 144, y + 6, Text.literal("~"));
        dropMaxField = addField(rx + 155, y + 1, 40, 18, String.valueOf(work.drop.max));
        y += 28;

        label(rx, y - 10, Text.translatable("modcrafter.label.drop_item"));
        dropItemField = addField(rx, y, 195, 18, work.drop.item);
        y += 28;

        // 矿石生成
        label(rx, y, Text.translatable("modcrafter.label.ore_section"), 0xD8B830);
        y += 12;
        oreGenBox = CheckboxWidget.builder(Text.translatable("modcrafter.label.ore_enabled"), this.textRenderer)
            .pos(rx, y).checked(work.oreGen.enabled).build();
        this.addDrawableChild(oreGenBox);
        y += 26;
        label(rx, y - 10, Text.translatable("modcrafter.label.vein_size"));
        veinSizeField = addField(rx, y, 40, 18, String.valueOf(work.oreGen.veinSize));
        label(rx + 50, y - 10, Text.translatable("modcrafter.label.veins_per_chunk"));
        veinsPerChunkField = addField(rx + 50, y, 40, 18, String.valueOf(work.oreGen.veinsPerChunk));
        label(rx + 100, y - 10, Text.literal("Y"));
        minYField = addField(rx + 100, y, 45, 18, String.valueOf(work.oreGen.minY));
        maxYField = addField(rx + 150, y, 45, 18, String.valueOf(work.oreGen.maxY));
        y += 26;

        // 朝向
        this.addDrawableChild(CyclingButtonWidget.<String>builder(v -> Text.translatable("modcrafter.facing." + v.toLowerCase()))
            .values(FACING_MODES).initially(FACING_MODES.contains(work.facingMode) ? work.facingMode : "NONE")
            .build(rx, y, 195, 20, Text.translatable("modcrafter.label.facing_mode"),
                (btn, v) -> work.facingMode = v));

        addBtn(cx - 105, this.height - 28, 100, 20, Text.translatable("modcrafter.btn.save"), this::save);
        addBtn(cx + 5, this.height - 28, 100, 20, Text.translatable("modcrafter.btn.back"), this::close);
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
        work.hardness = GuiUtil.parseFloat(hardnessField.getText(), 3.0f);
        work.resistance = GuiUtil.parseFloat(resistanceField.getText(), 6.0f);
        work.luminance = Math.max(0, Math.min(15, GuiUtil.parseInt(luminanceField.getText(), 0)));
        work.slipperiness = Math.max(0.4f, Math.min(0.999f, GuiUtil.parseFloat(slipperinessField.getText(), 0.6f)));
        work.requiresTool = requiresToolBox.isChecked();
        work.transparent = transparentBox.isChecked();
        work.drop.item = dropItemField.getText().trim();
        work.drop.min = GuiUtil.parseInt(dropMinField.getText(), 1);
        work.drop.max = GuiUtil.parseInt(dropMaxField.getText(), 1);
        work.oreGen.enabled = oreGenBox.isChecked();
        work.oreGen.veinSize = Math.max(1, GuiUtil.parseInt(veinSizeField.getText(), 6));
        work.oreGen.veinsPerChunk = Math.max(1, GuiUtil.parseInt(veinsPerChunkField.getText(), 8));
        work.oreGen.minY = GuiUtil.parseInt(minYField.getText(), -60);
        work.oreGen.maxY = GuiUtil.parseInt(maxYField.getText(), 40);

        if ("CUSTOM".equals(work.drop.mode) && !work.drop.item.isEmpty()
            && !GuiUtil.isValidItemId(work.drop.item) && !work.drop.item.startsWith(pack.id + ":")) {
            setFeedback(Text.translatable("modcrafter.msg.bad_item_id", work.drop.item), false);
            return;
        }

        boolean newOre = work.oreGen.enabled && (editing == null || editing.oreGen == null || !editing.oreGen.enabled);

        if (editing == null) {
            pack.blocks.add(work);
        } else {
            copyInto(work, editing);
        }
        HotApply.apply(pack);
        PackScreen ps = new PackScreen(new MainScreen(null), pack);
        ps.tab = PackScreen.Tab.BLOCKS;
        if (newOre) {
            ps.presetFeedback = Text.translatable("modcrafter.msg.ore_rejoin_hint");
        }
        this.client.setScreen(ps);
    }

    void rebuild() {
        this.clearChildren();
        this.init();
    }

    @Override
    protected void renderExtra(DrawContext context, int mouseX, int mouseY, float delta) {
        if ("SINGLE".equals(work.textureMode)) {
            int cx = this.width / 2;
            GuiUtil.drawPreview(context, pack.id, work.texture, cx - 155 + 146, 42 + 28 * 3 + 26 + 22 + 22 + 22 - 4, 24);
        }
    }
}
