package dev.hyxt.modcrafter.client.gui;

import dev.hyxt.modcrafter.data.ActionDef;
import dev.hyxt.modcrafter.event.ActionExecutor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.List;

/** 单个动作编辑器 */
public class ActionEditScreen extends BaseScreen {
    private final List<ActionDef> actions;
    private final ActionDef editing;
    private final ActionDef work;

    private TextFieldWidget textField;
    private TextFieldWidget effectField;
    private TextFieldWidget durationField;
    private TextFieldWidget amplifierField;
    private TextFieldWidget powerField;
    private TextFieldWidget commandField;
    private TextFieldWidget itemField;
    private TextFieldWidget countField;
    private TextFieldWidget soundField;
    private TextFieldWidget amountField;
    private TextFieldWidget secondsField;
    private TextFieldWidget entityField;
    private TextFieldWidget xpField;
    private TextFieldWidget dxField;
    private TextFieldWidget dyField;
    private TextFieldWidget dzField;
    private TextFieldWidget particleField;
    private CheckboxWidget breakBlocksBox;
    private String weatherChoice = null;

    public ActionEditScreen(Screen parent, List<ActionDef> actions, ActionDef editing) {
        super(parent, Text.translatable(editing == null ? "modcrafter.title.new_action" : "modcrafter.title.edit_action"));
        this.actions = actions;
        this.editing = editing;
        this.work = editing == null ? new ActionDef() : copyAction(editing);
    }

    static ActionDef copyAction(ActionDef src) {
        ActionDef d = new ActionDef();
        d.type = src.type;
        d.text = src.text;
        d.effect = src.effect;
        d.duration = src.duration;
        d.amplifier = src.amplifier;
        d.power = src.power;
        d.breakBlocks = src.breakBlocks;
        d.command = src.command;
        d.item = src.item;
        d.count = src.count;
        d.sound = src.sound;
        d.amount = src.amount;
        d.seconds = src.seconds;
        d.entity = src.entity;
        d.xp = src.xp;
        d.dx = src.dx;
        d.dy = src.dy;
        d.dz = src.dz;
        d.weather = src.weather;
        d.particle = src.particle;
        return d;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int lx = cx - 130;
        int w = 260;

        int y = 42;
        label(lx, y - 10, Text.translatable("modcrafter.label.action_type"));
        List<String> types = Arrays.asList(ActionExecutor.TYPES);
        this.addDrawableChild(CyclingButtonWidget.<String>builder(v -> Text.translatable(ActionExecutor.actionKey(v)))
            .values(types).initially(types.contains(work.type) ? work.type : "MESSAGE")
            .build(lx, y, w, 20, Text.translatable("modcrafter.label.action_type"),
                (btn, v) -> {
                    work.type = v;
                    rebuild();
                }));
        y += 34;

        switch (work.type) {
            case "MESSAGE" -> {
                label(lx, y - 10, Text.translatable("modcrafter.label.message_text"));
                textField = addField(lx, y, w, 18, work.text);
            }
            case "GIVE_EFFECT" -> {
                label(lx, y - 10, Text.translatable("modcrafter.label.effect_id"));
                effectField = addField(lx, y, w, 18, work.effect);
                y += 32;
                label(lx, y - 10, Text.translatable("modcrafter.label.duration"));
                durationField = addField(lx, y, 80, 18, String.valueOf(work.duration));
                label(lx + 90, y - 10, Text.translatable("modcrafter.label.amplifier"));
                amplifierField = addField(lx + 90, y, 80, 18, String.valueOf(work.amplifier));
            }
            case "EXPLOSION" -> {
                label(lx, y - 10, Text.translatable("modcrafter.label.power"));
                powerField = addField(lx, y, 80, 18, String.valueOf(work.power));
                y += 28;
                breakBlocksBox = CheckboxWidget.builder(Text.translatable("modcrafter.label.break_blocks"), this.textRenderer)
                    .pos(lx, y).checked(work.breakBlocks).build();
                this.addDrawableChild(breakBlocksBox);
            }
            case "LIGHTNING" -> label(lx, y, Text.translatable("modcrafter.label.no_params"), 0x808080);
            case "COMMAND" -> {
                label(lx, y - 10, Text.translatable("modcrafter.label.command"));
                commandField = addField(lx, y, w, 18, work.command);
                y += 26;
                label(lx, y, Text.translatable("modcrafter.label.command_hint"), 0x808080);
            }
            case "GIVE_ITEM" -> {
                label(lx, y - 10, Text.translatable("modcrafter.label.item_id"));
                itemField = addField(lx, y, w, 18, work.item);
                y += 32;
                label(lx, y - 10, Text.translatable("modcrafter.label.count"));
                countField = addField(lx, y, 80, 18, String.valueOf(work.count));
            }
            case "PLAY_SOUND" -> {
                label(lx, y - 10, Text.translatable("modcrafter.label.sound_id"));
                soundField = addField(lx, y, w, 18, work.sound);
            }
            case "LAUNCH" -> {
                label(lx, y - 10, Text.translatable("modcrafter.label.power"));
                powerField = addField(lx, y, 80, 18, String.valueOf(work.power));
            }
            case "HEAL", "DAMAGE" -> {
                label(lx, y - 10, Text.translatable("modcrafter.label.amount"));
                amountField = addField(lx, y, 80, 18, String.valueOf(work.amount));
            }
            case "SET_FIRE" -> {
                label(lx, y - 10, Text.translatable("modcrafter.label.seconds"));
                secondsField = addField(lx, y, 80, 18, String.valueOf(work.seconds));
            }
            case "SPAWN_ENTITY" -> {
                label(lx, y - 10, Text.translatable("modcrafter.label.entity_id"));
                entityField = addField(lx, y, w, 18, work.entity);
                y += 32;
                label(lx, y - 10, Text.translatable("modcrafter.label.count"));
                countField = addField(lx, y, 80, 18, String.valueOf(work.count));
            }
            case "GIVE_XP" -> {
                label(lx, y - 10, Text.translatable("modcrafter.label.xp_amount"));
                xpField = addField(lx, y, 80, 18, String.valueOf(work.xp));
            }
            case "TELEPORT_RELATIVE" -> {
                label(lx, y - 10, Text.translatable("modcrafter.label.teleport_offset"));
                dxField = addField(lx, y, 60, 18, String.valueOf(work.dx));
                dyField = addField(lx + 70, y, 60, 18, String.valueOf(work.dy));
                dzField = addField(lx + 140, y, 60, 18, String.valueOf(work.dz));
            }
            case "SET_WEATHER" -> {
                label(lx, y - 10, Text.translatable("modcrafter.label.weather"));
                List<String> weathers = List.of("CLEAR", "RAIN", "THUNDER");
                weatherChoice = weathers.contains(work.weather) ? work.weather : "RAIN";
                this.addDrawableChild(CyclingButtonWidget.<String>builder(v -> Text.translatable("modcrafter.weather." + v.toLowerCase()))
                    .values(weathers).initially(weatherChoice)
                    .build(lx, y, 120, 20, Text.translatable("modcrafter.label.weather"),
                        (btn, v) -> weatherChoice = v));
            }
            case "PARTICLE" -> {
                label(lx, y - 10, Text.translatable("modcrafter.label.particle_id"));
                particleField = addField(lx, y, w, 18, work.particle);
                y += 32;
                label(lx, y - 10, Text.translatable("modcrafter.label.count"));
                countField = addField(lx, y, 80, 18, String.valueOf(work.count));
            }
        }

        addBtn(cx - 105, this.height - 28, 100, 20, Text.translatable("modcrafter.btn.ok"), this::save);
        addBtn(cx + 5, this.height - 28, 100, 20, Text.translatable("modcrafter.btn.back"), this::close);
    }

    private void save() {
        if (textField != null) work.text = textField.getText();
        if (effectField != null) work.effect = effectField.getText().trim();
        if (durationField != null) work.duration = GuiUtil.parseInt(durationField.getText(), 200);
        if (amplifierField != null) work.amplifier = GuiUtil.parseInt(amplifierField.getText(), 0);
        if (powerField != null) work.power = GuiUtil.parseFloat(powerField.getText(), 2.0f);
        if (breakBlocksBox != null) work.breakBlocks = breakBlocksBox.isChecked();
        if (commandField != null) work.command = commandField.getText();
        if (itemField != null) work.item = itemField.getText().trim();
        if (countField != null) work.count = GuiUtil.parseInt(countField.getText(), 1);
        if (soundField != null) work.sound = soundField.getText().trim();
        if (amountField != null) work.amount = GuiUtil.parseFloat(amountField.getText(), 4.0f);
        if (secondsField != null) work.seconds = GuiUtil.parseInt(secondsField.getText(), 3);
        if (entityField != null) work.entity = entityField.getText().trim();
        if (xpField != null) work.xp = GuiUtil.parseInt(xpField.getText(), 10);
        if (dxField != null) work.dx = GuiUtil.parseFloat(dxField.getText(), 0);
        if (dyField != null) work.dy = GuiUtil.parseFloat(dyField.getText(), 10);
        if (dzField != null) work.dz = GuiUtil.parseFloat(dzField.getText(), 0);
        if (particleField != null) work.particle = particleField.getText().trim();
        if (weatherChoice != null) work.weather = weatherChoice;

        if (editing == null) {
            actions.add(work);
        } else {
            int i = actions.indexOf(editing);
            if (i >= 0) actions.set(i, work);
        }
        this.close();
    }

    void rebuild() {
        this.clearChildren();
        this.init();
    }
}
