package dev.hyxt.modcrafter.client.gui;

import dev.hyxt.modcrafter.data.ContentPack;
import dev.hyxt.modcrafter.data.PackManager;
import dev.hyxt.modcrafter.data.VoxelModel;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;

/** 体素模型库: 选择 / 新建 / 编辑 / 删除 */
public class VoxelModelPickScreen extends BaseScreen {
    private final String packId;
    private final String current;
    private final Consumer<String> callback;

    private TextFieldWidget nameField;
    private String sizeChoice = "16";
    private int page = 0;
    private static final int PAGE_SIZE = 4;

    public VoxelModelPickScreen(Screen parent, String packId, String current, Consumer<String> callback) {
        super(parent, Text.translatable("modcrafter.title.pick_model"));
        this.packId = packId;
        this.current = current == null ? "" : current;
        this.callback = callback;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int lx = cx - 160;

        addBtn(lx, 34, 150, 20, Text.translatable("modcrafter.btn.no_model"), () -> {
            callback.accept("");
            this.close();
        });

        List<String> models = PackManager.listVoxelModels(packId);
        int maxPage = Math.max(0, (models.size() - 1) / PAGE_SIZE);
        if (page > maxPage) page = maxPage;

        int y = 62;
        for (int i = page * PAGE_SIZE; i < Math.min(models.size(), (page + 1) * PAGE_SIZE); i++) {
            String name = models.get(i);
            VoxelModel model = PackManager.loadVoxelModel(packId, name);
            String info = model == null ? "?" : model.size + "³, " + model.palette.size() + "色";
            String mark = name.equals(current) ? "§e▶ §r" : "";
            addBtn(lx, y, 170, 20, Text.literal(mark + name + " §7(" + info + ")"), () -> {
                callback.accept(name);
                this.close();
            });
            addBtn(lx + 174, y, 50, 20, Text.translatable("modcrafter.btn.edit_model"), () -> {
                VoxelModel m = PackManager.loadVoxelModel(packId, name);
                if (m != null) {
                    this.client.setScreen(new VoxelEditorScreen(this, this.parent, packId, name, m, callback));
                }
            });
            addBtn(lx + 228, y, 50, 20, Text.translatable("modcrafter.btn.delete"), () -> {
                PackManager.deleteVoxelModel(packId, name);
                rebuild();
            });
            y += 24;
        }

        if (models.size() > PAGE_SIZE) {
            addBtn(lx, 162, 60, 20, Text.literal("<"), () -> {
                if (page > 0) {
                    page--;
                    rebuild();
                }
            });
            addBtn(lx + 220, 162, 60, 20, Text.literal(">"), () -> {
                if ((page + 1) * PAGE_SIZE < PackManager.listVoxelModels(packId).size()) {
                    page++;
                    rebuild();
                }
            });
        }

        // 新建
        int ny = 190;
        label(lx, ny - 10, Text.translatable("modcrafter.label.new_model"));
        nameField = addField(lx, ny, 110, 18, nameField != null ? nameField.getText()
            : "model_" + (PackManager.listVoxelModels(packId).size() + 1));
        this.addDrawableChild(CyclingButtonWidget.<String>builder(v -> Text.literal(v + "³"))
            .values(List.of("8", "16", "32")).initially(sizeChoice)
            .build(lx + 116, ny - 1, 70, 20, Text.translatable("modcrafter.label.model_size"),
                (btn, v) -> sizeChoice = v));
        addBtn(lx + 190, ny - 1, 90, 20, Text.translatable("modcrafter.btn.new_model"), this::createNew);

        label(lx, ny + 26, Text.translatable("modcrafter.label.model_hint"), 0x808080);

        addBtn(cx - 50, this.height - 26, 100, 20, Text.translatable("modcrafter.btn.back"), this::close);
    }

    private void createNew() {
        String name = nameField.getText().trim().toLowerCase();
        if (!ContentPack.isValidId(name)) {
            setFeedback(Text.translatable("modcrafter.msg.bad_id"), false);
            return;
        }
        if (PackManager.listVoxelModels(packId).contains(name)) {
            setFeedback(Text.translatable("modcrafter.msg.id_taken"), false);
            return;
        }
        VoxelModel model = new VoxelModel();
        model.size = Integer.parseInt(sizeChoice);
        this.client.setScreen(new VoxelEditorScreen(this, this.parent, packId, name, model, callback));
    }

    void rebuild() {
        this.clearChildren();
        this.init();
    }
}
