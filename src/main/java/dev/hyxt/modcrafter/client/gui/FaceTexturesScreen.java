package dev.hyxt.modcrafter.client.gui;

import dev.hyxt.modcrafter.data.BlockDef;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** 方块六面贴图配置 */
public class FaceTexturesScreen extends BaseScreen {
    private final String packId;
    private final BlockDef.FacesDef faces;

    private record FaceRow(String key, Supplier<String> get, Consumer<String> set) {
    }

    private FaceRow[] rows;

    public FaceTexturesScreen(Screen parent, String packId, BlockDef work) {
        super(parent, Text.translatable("modcrafter.title.face_textures"));
        this.packId = packId;
        if (work.faces == null) work.faces = new BlockDef.FacesDef();
        this.faces = work.faces;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int lx = cx - 120;

        rows = new FaceRow[]{
            new FaceRow("up", () -> faces.up, v -> faces.up = v),
            new FaceRow("down", () -> faces.down, v -> faces.down = v),
            new FaceRow("north", () -> faces.north, v -> faces.north = v),
            new FaceRow("south", () -> faces.south, v -> faces.south = v),
            new FaceRow("west", () -> faces.west, v -> faces.west = v),
            new FaceRow("east", () -> faces.east, v -> faces.east = v),
        };

        int y = 40;
        for (FaceRow row : rows) {
            label(lx, y + 6, Text.translatable("modcrafter.face." + row.key()));
            addBtn(lx + 60, y, 110, 20, Text.translatable("modcrafter.btn.pick_texture"),
                () -> this.client.setScreen(new TexturePickScreen(this, packId, row.get().get(), row.set())));
            addBtn(lx + 174, y, 60, 20, Text.translatable("modcrafter.btn.copy_to_all"), () -> {
                String tex = row.get().get();
                faces.up = tex;
                faces.down = tex;
                faces.north = tex;
                faces.south = tex;
                faces.west = tex;
                faces.east = tex;
            });
            y += 26;
        }

        label(lx, y + 4, Text.translatable("modcrafter.label.face_hint"), 0x808080);

        addBtn(cx - 50, this.height - 26, 100, 20, Text.translatable("modcrafter.btn.done"), this::close);
    }

    @Override
    protected void renderExtra(DrawContext context, int mouseX, int mouseY, float delta) {
        int cx = this.width / 2;
        int lx = cx - 120;
        int y = 40;
        if (rows == null) return;
        for (FaceRow row : rows) {
            GuiUtil.drawPreview(context, packId, row.get().get(), lx + 238, y + 2, 16);
            y += 26;
        }
    }
}
