package dev.hyxt.modcrafter.client.gui;

import dev.hyxt.modcrafter.client.HotApply;
import dev.hyxt.modcrafter.data.ActionDef;
import dev.hyxt.modcrafter.data.BlockDef;
import dev.hyxt.modcrafter.data.ContentPack;
import dev.hyxt.modcrafter.data.EventDef;
import dev.hyxt.modcrafter.data.ItemDef;
import dev.hyxt.modcrafter.event.ActionExecutor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 事件编辑器: 触发器 + 目标 + 动作列表 */
public class EventEditScreen extends BaseScreen {
    private final ContentPack pack;
    private final EventDef editing;
    final EventDef work;

    public EventEditScreen(Screen parent, ContentPack pack, EventDef editing) {
        super(parent, Text.translatable(editing == null ? "modcrafter.title.new_event" : "modcrafter.title.edit_event"));
        this.pack = pack;
        this.editing = editing;
        this.work = copy(editing, pack);
    }

    private static EventDef copy(EventDef src, ContentPack pack) {
        EventDef d = new EventDef();
        if (src == null) {
            int n = pack.events.size() + 1;
            while (pack.findEvent("evt_" + n) != null) n++;
            d.id = "evt_" + n;
            return d;
        }
        d.id = src.id;
        d.trigger = src.trigger;
        d.target = src.target;
        d.actions = new ArrayList<>();
        for (ActionDef a : src.actions) d.actions.add(ActionEditScreen.copyAction(a));
        return d;
    }

    private List<String> targets() {
        List<String> list = new ArrayList<>();
        for (ItemDef d : pack.items) list.add(pack.id + ":" + d.id);
        for (BlockDef d : pack.blocks) list.add(pack.id + ":" + d.id);
        return list;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int lx = cx - 150;

        int y = 40;
        label(lx, y, Text.translatable("modcrafter.label.event_id", work.id), 0x808080);
        y += 16;

        label(lx, y - 2, Text.translatable("modcrafter.label.trigger"));
        List<String> triggers = Arrays.asList(ActionExecutor.TRIGGERS);
        this.addDrawableChild(CyclingButtonWidget.<String>builder(v -> Text.translatable(ActionExecutor.triggerKey(v)))
            .values(triggers).initially(triggers.contains(work.trigger) ? work.trigger : "ITEM_USE")
            .build(lx + 60, y - 6, 240, 20, Text.translatable("modcrafter.label.trigger"),
                (btn, v) -> work.trigger = v));
        y += 26;

        label(lx, y - 2, Text.translatable("modcrafter.label.target"));
        List<String> targets = targets();
        if (targets.isEmpty()) {
            label(lx + 60, y - 2, Text.translatable("modcrafter.msg.no_targets"), 0xFF8080);
        } else {
            if (work.target == null || work.target.isEmpty() || !targets.contains(work.target)) {
                work.target = targets.get(0);
            }
            this.addDrawableChild(CyclingButtonWidget.<String>builder(Text::literal)
                .values(targets).initially(work.target)
                .build(lx + 60, y - 6, 240, 20, Text.translatable("modcrafter.label.target"),
                    (btn, v) -> work.target = v));
        }
        y += 30;

        label(lx, y, Text.translatable("modcrafter.label.actions", work.actions.size()));
        y += 14;
        int shown = 0;
        for (int i = 0; i < work.actions.size() && shown < 4; i++, shown++) {
            final int index = i;
            ActionDef a = work.actions.get(i);
            addBtn(lx, y, 240, 20, Text.literal((i + 1) + ". " + summary(a)),
                () -> this.client.setScreen(new ActionEditScreen(this, work.actions, a)));
            addBtn(lx + 244, y, 56, 20, Text.translatable("modcrafter.btn.delete"), () -> {
                work.actions.remove(index);
                rebuild();
            });
            y += 24;
        }
        addBtn(lx, y, 145, 20, Text.translatable("modcrafter.btn.add_action"),
            () -> this.client.setScreen(new ActionEditScreen(this, work.actions, null)));

        addBtn(cx - 105, this.height - 28, 100, 20, Text.translatable("modcrafter.btn.save"), this::save);
        addBtn(cx + 5, this.height - 28, 100, 20, Text.translatable("modcrafter.btn.back"), this::close);
    }

    private String summary(ActionDef a) {
        String detail = switch (a.type) {
            case "MESSAGE" -> a.text;
            case "GIVE_EFFECT" -> a.effect + " x" + a.duration;
            case "EXPLOSION" -> "威力 " + a.power;
            case "COMMAND" -> a.command;
            case "GIVE_ITEM" -> a.item + " x" + a.count;
            case "PLAY_SOUND" -> a.sound;
            case "LAUNCH" -> "力度 " + a.power;
            case "HEAL", "DAMAGE" -> String.valueOf(a.amount);
            case "SET_FIRE" -> a.seconds + "s";
            case "SPAWN_ENTITY" -> a.entity + " x" + a.count;
            case "GIVE_XP" -> a.xp + "xp";
            case "TELEPORT_RELATIVE" -> a.dx + "," + a.dy + "," + a.dz;
            case "SET_WEATHER" -> a.weather;
            case "PARTICLE" -> a.particle + " x" + a.count;
            default -> "";
        };
        String label = Text.translatable(ActionExecutor.actionKey(a.type)).getString();
        if (detail.length() > 24) detail = detail.substring(0, 24) + "…";
        return label + (detail.isEmpty() ? "" : " §7" + detail);
    }

    private void save() {
        if (work.target == null || work.target.isEmpty()) {
            setFeedback(Text.translatable("modcrafter.msg.no_targets"), false);
            return;
        }
        if (work.actions.isEmpty()) {
            setFeedback(Text.translatable("modcrafter.msg.no_actions"), false);
            return;
        }
        if (editing == null) {
            pack.events.add(work);
        } else {
            editing.trigger = work.trigger;
            editing.target = work.target;
            editing.actions = work.actions;
        }
        HotApply.apply(pack);
        PackScreen ps = new PackScreen(new MainScreen(null), pack);
        ps.tab = PackScreen.Tab.EVENTS;
        this.client.setScreen(ps);
    }

    void rebuild() {
        this.clearChildren();
        this.init();
    }
}
