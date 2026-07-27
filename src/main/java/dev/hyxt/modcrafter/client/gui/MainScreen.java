package dev.hyxt.modcrafter.client.gui;

import dev.hyxt.modcrafter.data.ContentPack;
import dev.hyxt.modcrafter.data.PackManager;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;

/** 主界面: 内容包列表 */
public class MainScreen extends BaseScreen {
    private int page = 0;
    private static final int PAGE_SIZE = 5;

    public MainScreen(Screen parent) {
        super(parent, Text.translatable("modcrafter.title.main"));
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        List<ContentPack> packs = PackManager.all();
        int maxPage = Math.max(0, (packs.size() - 1) / PAGE_SIZE);
        if (page > maxPage) page = maxPage;

        int y = 40;
        label(cx - 150, y - 12, Text.translatable("modcrafter.label.packs", packs.size()));
        for (int i = page * PAGE_SIZE; i < Math.min(packs.size(), (page + 1) * PAGE_SIZE); i++) {
            ContentPack pack = packs.get(i);
            String title = pack.name + " §7(" + pack.id + ") §8" +
                pack.items.size() + "物品 " + pack.blocks.size() + "方块 " +
                pack.recipes.size() + "配方 " + pack.events.size() + "事件";
            addBtn(cx - 150, y, 240, 20, Text.literal(title),
                () -> this.client.setScreen(new PackScreen(this, pack)));
            addBtn(cx + 94, y, 56, 20, Text.translatable("modcrafter.btn.delete"), () -> confirmDelete(pack));
            y += 24;
        }

        if (packs.size() > PAGE_SIZE) {
            addBtn(cx - 150, 165, 70, 20, Text.literal("<"), () -> {
                if (page > 0) {
                    page--;
                    rebuild();
                }
            });
            addBtn(cx + 80, 165, 70, 20, Text.literal(">"), () -> {
                if ((page + 1) * PAGE_SIZE < PackManager.all().size()) {
                    page++;
                    rebuild();
                }
            });
        }

        addBtn(cx - 150, this.height - 30, 145, 20, Text.translatable("modcrafter.btn.new_pack"),
            () -> this.client.setScreen(new NewPackScreen(this)));
        addBtn(cx + 5, this.height - 30, 145, 20, Text.translatable("modcrafter.btn.close"), this::close);

        label(cx - 150, this.height - 55, Text.translatable("modcrafter.label.hint_main"), 0x808080);
    }

    private void confirmDelete(ContentPack pack) {
        this.client.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                PackManager.delete(pack.id);
            }
            this.client.setScreen(new MainScreen(parent));
        }, Text.translatable("modcrafter.confirm.delete_pack", pack.name),
            Text.translatable("modcrafter.confirm.delete_pack_desc")));
    }

    void rebuild() {
        this.clearChildren();
        this.init();
    }
}
