package dev.hyxt.modcrafter.client.gui;

import dev.hyxt.modcrafter.client.HotApply;
import dev.hyxt.modcrafter.data.BlockDef;
import dev.hyxt.modcrafter.data.ContentPack;
import dev.hyxt.modcrafter.data.EventDef;
import dev.hyxt.modcrafter.data.ItemDef;
import dev.hyxt.modcrafter.data.PackManager;
import dev.hyxt.modcrafter.data.RecipeDef;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** 内容包编辑主界面: 物品/方块/配方/事件 四个标签页 */
public class PackScreen extends BaseScreen {
    public enum Tab {ITEMS, BLOCKS, RECIPES, EVENTS}

    final ContentPack pack;
    Tab tab = Tab.ITEMS;
    /** 打开时要显示的一条提示(如矿石需重进世界) */
    Text presetFeedback = null;
    private int page = 0;
    private static final int PAGE_SIZE = 5;

    public PackScreen(Screen parent, ContentPack pack) {
        super(parent, Text.literal(pack.name + " §7(" + pack.id + ")"));
        this.pack = pack;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;

        if (presetFeedback != null) {
            setFeedback(presetFeedback, true);
            presetFeedback = null;
        }

        // 标签页
        int tabW = 74;
        int tx = cx - 150;
        tabBtn(tx, Tab.ITEMS, "modcrafter.tab.items");
        tabBtn(tx + tabW + 2, Tab.BLOCKS, "modcrafter.tab.blocks");
        tabBtn(tx + (tabW + 2) * 2, Tab.RECIPES, "modcrafter.tab.recipes");
        tabBtn(tx + (tabW + 2) * 3, Tab.EVENTS, "modcrafter.tab.events");

        int count = countFor(tab);
        int maxPage = Math.max(0, (count - 1) / PAGE_SIZE);
        if (page > maxPage) page = maxPage;

        int y = 66;
        for (int i = page * PAGE_SIZE; i < Math.min(count, (page + 1) * PAGE_SIZE); i++) {
            final int index = i;
            addBtn(cx - 150, y, 240, 20, Text.literal(rowTitle(index)), () -> openEditor(index));
            addBtn(cx + 94, y, 56, 20, Text.translatable("modcrafter.btn.delete"), () -> confirmDelete(index));
            y += 24;
        }

        if (count > PAGE_SIZE) {
            addBtn(cx - 150, 190, 70, 20, Text.literal("<"), () -> {
                if (page > 0) {
                    page--;
                    rebuild();
                }
            });
            addBtn(cx + 80, 190, 70, 20, Text.literal(">"), () -> {
                if ((page + 1) * PAGE_SIZE < countFor(tab)) {
                    page++;
                    rebuild();
                }
            });
        }

        addBtn(cx - 150, this.height - 54, 145, 20, Text.translatable("modcrafter.btn.add_" + tab.name().toLowerCase()),
            () -> openEditor(-1));
        addBtn(cx + 5, this.height - 54, 145, 20, Text.translatable("modcrafter.btn.apply"), () -> {
            Text result = HotApply.apply(pack);
            setFeedback(result, true);
        });
        addBtn(cx - 150, this.height - 30, 95, 20, Text.translatable("modcrafter.btn.export"),
            () -> this.client.setScreen(new ExportScreen(this, pack)));
        addBtn(cx - 50, this.height - 30, 95, 20, Text.translatable("modcrafter.btn.give_hint"), () -> {
            setFeedback(Text.translatable("modcrafter.msg.give_hint", pack.id), true);
        });
        addBtn(cx + 55, this.height - 30, 95, 20, Text.translatable("modcrafter.btn.back"),
            () -> this.client.setScreen(new MainScreen(null)));
    }

    private void tabBtn(int x, Tab t, String key) {
        var btn = addBtn(x, 34, 74, 20, Text.translatable(key), () -> {
            tab = t;
            page = 0;
            rebuild();
        });
        btn.active = tab != t;
    }

    private int countFor(Tab t) {
        return switch (t) {
            case ITEMS -> pack.items.size();
            case BLOCKS -> pack.blocks.size();
            case RECIPES -> pack.recipes.size();
            case EVENTS -> pack.events.size();
        };
    }

    private String rowTitle(int i) {
        switch (tab) {
            case ITEMS -> {
                ItemDef d = pack.items.get(i);
                return d.name + " §7(" + d.id + ", " + d.type + ")";
            }
            case BLOCKS -> {
                BlockDef d = pack.blocks.get(i);
                return d.name + " §7(" + d.id + ")";
            }
            case RECIPES -> {
                RecipeDef d = pack.recipes.get(i);
                return d.id + " §7(" + d.type + " → " + d.result + ")";
            }
            default -> {
                EventDef d = pack.events.get(i);
                return d.id + " §7(" + d.trigger + " @ " + d.target + ", " + d.actions.size() + "动作)";
            }
        }
    }

    private void openEditor(int index) {
        switch (tab) {
            case ITEMS -> this.client.setScreen(new ItemEditScreen(this, pack, index < 0 ? null : pack.items.get(index)));
            case BLOCKS -> this.client.setScreen(new BlockEditScreen(this, pack, index < 0 ? null : pack.blocks.get(index)));
            case RECIPES -> this.client.setScreen(new RecipeEditScreen(this, pack, index < 0 ? null : pack.recipes.get(index)));
            case EVENTS -> this.client.setScreen(new EventEditScreen(this, pack, index < 0 ? null : pack.events.get(index)));
        }
    }

    private void confirmDelete(int index) {
        this.client.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                switch (tab) {
                    case ITEMS -> pack.items.remove(index);
                    case BLOCKS -> pack.blocks.remove(index);
                    case RECIPES -> pack.recipes.remove(index);
                    case EVENTS -> pack.events.remove(index);
                }
                PackManager.save(pack);
            }
            this.client.setScreen(this);
            rebuild();
        }, Text.translatable("modcrafter.confirm.delete_element"),
            Text.translatable("modcrafter.confirm.delete_element_desc")));
    }

    void rebuild() {
        this.clearChildren();
        this.init();
    }
}
