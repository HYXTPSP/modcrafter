package dev.hyxt.modcrafter.client.gui;

import dev.hyxt.modcrafter.export.ModProjectExporter;
import dev.hyxt.modcrafter.export.PackExporter;
import dev.hyxt.modcrafter.data.ContentPack;
import dev.hyxt.modcrafter.data.PackManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.nio.file.Path;

/** 导出界面 */
public class ExportScreen extends BaseScreen {
    private final ContentPack pack;

    public ExportScreen(Screen parent, ContentPack pack) {
        super(parent, Text.translatable("modcrafter.title.export"));
        this.pack = pack;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int y = 50;

        label(cx - 130, y - 8, Text.translatable("modcrafter.label.export_zip_desc"), 0x808080);
        y += 8;
        addBtn(cx - 130, y, 260, 20, Text.translatable("modcrafter.btn.export_zip"), () -> {
            try {
                Path out = PackExporter.exportZip(pack);
                setFeedback(Text.translatable("modcrafter.msg.exported", out.getFileName().toString()), true);
            } catch (Exception e) {
                setFeedback(Text.literal("导出失败: " + e.getMessage()), false);
            }
        });
        y += 40;

        label(cx - 130, y - 8, Text.translatable("modcrafter.label.export_mod_desc"), 0x808080);
        y += 8;
        addBtn(cx - 130, y, 260, 20, Text.translatable("modcrafter.btn.export_mod"), () -> {
            try {
                Path out = ModProjectExporter.export(pack);
                setFeedback(Text.translatable("modcrafter.msg.exported", out.getFileName().toString()), true);
            } catch (Exception e) {
                setFeedback(Text.literal("导出失败: " + e.getMessage()), false);
            }
        });
        y += 40;

        addBtn(cx - 130, y, 260, 20, Text.translatable("modcrafter.btn.open_exports"),
            () -> Util.getOperatingSystem().open(PackManager.exportsDir().toFile()));
        y += 30;

        addBtn(cx - 50, this.height - 28, 100, 20, Text.translatable("modcrafter.btn.back"), this::close);
    }
}
