package dev.hyxt.modcrafter.client.gui;

import dev.hyxt.modcrafter.data.ContentPack;
import dev.hyxt.modcrafter.data.PackManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * 从电脑文件导入贴图: 支持 PNG/JPG,自动居中裁剪成正方形并缩放到目标尺寸。
 * 也支持保留原尺寸(方形且 ≤128 时)。
 */
public class ImportTextureScreen extends BaseScreen {
    private final String packId;
    private final Consumer<String> callback;

    private TextFieldWidget pathField;
    private TextFieldWidget nameField;
    private String sizeChoice = "16";
    private BufferedImage previewImage = null;
    private String loadedPath = "";

    private static final List<String> SIZES = List.of("16", "32", "64", "原始");

    public ImportTextureScreen(Screen parent, String packId, Consumer<String> callback) {
        super(parent, Text.translatable("modcrafter.title.import_texture"));
        this.packId = packId;
        this.callback = callback;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int lx = cx - 150;
        int y = 46;

        label(lx, y - 10, Text.translatable("modcrafter.label.import_path"));
        pathField = addField(lx, y, 300, 18, pathField != null ? pathField.getText() : "");
        y += 30;

        addBtn(lx, y, 90, 20, Text.translatable("modcrafter.btn.load_preview"), this::loadPreview);
        this.addDrawableChild(CyclingButtonWidget.<String>builder(Text::literal)
            .values(SIZES).initially(sizeChoice)
            .build(lx + 96, y, 90, 20, Text.translatable("modcrafter.label.import_size"),
                (btn, v) -> sizeChoice = v));
        y += 30;

        label(lx, y - 10, Text.translatable("modcrafter.label.texture_name"));
        nameField = addField(lx, y, 140, 18,
            nameField != null ? nameField.getText() : "img_" + (PackManager.listCustomTextures(packId).size() + 1));
        y += 34;

        addBtn(lx, y, 90, 20, Text.translatable("modcrafter.btn.do_import"), this::doImport);

        label(lx, y + 30, Text.translatable("modcrafter.label.import_hint"), 0x808080);
        label(lx, y + 42, Text.translatable("modcrafter.label.import_hint2"), 0x808080);

        addBtn(cx - 50, this.height - 26, 100, 20, Text.translatable("modcrafter.btn.back"), this::close);
    }

    private void loadPreview() {
        String path = pathField.getText().trim().replace("\"", "");
        try {
            Path file = Path.of(path);
            if (!Files.exists(file)) {
                setFeedback(Text.translatable("modcrafter.msg.file_not_found"), false);
                return;
            }
            BufferedImage img = ImageIO.read(file.toFile());
            if (img == null) {
                setFeedback(Text.translatable("modcrafter.msg.not_an_image"), false);
                return;
            }
            previewImage = img;
            loadedPath = path;
            setFeedback(Text.translatable("modcrafter.msg.image_loaded", img.getWidth(), img.getHeight()), true);
        } catch (Exception e) {
            setFeedback(Text.literal("读取失败: " + e.getMessage()), false);
        }
    }

    private void doImport() {
        if (previewImage == null || !pathField.getText().trim().replace("\"", "").equals(loadedPath)) {
            loadPreview();
            if (previewImage == null) return;
        }
        String name = nameField.getText().trim().toLowerCase();
        if (!ContentPack.isValidId(name)) {
            setFeedback(Text.translatable("modcrafter.msg.bad_id"), false);
            return;
        }
        try {
            BufferedImage result = process(previewImage, sizeChoice);
            Path out = PackManager.textureFile(packId, name);
            Files.createDirectories(out.getParent());
            ImageIO.write(result, "PNG", out.toFile());
            GuiUtil.invalidateTexture(packId, "custom:" + name);
            callback.accept("custom:" + name);
            this.close();
        } catch (Exception e) {
            setFeedback(Text.literal("导入失败: " + e.getMessage()), false);
        }
    }

    /** 居中裁剪成正方形,再缩放到目标尺寸 */
    static BufferedImage process(BufferedImage src, String sizeChoice) {
        int side = Math.min(src.getWidth(), src.getHeight());
        int sx = (src.getWidth() - side) / 2;
        int sy = (src.getHeight() - side) / 2;
        BufferedImage square = src.getSubimage(sx, sy, side, side);

        int target;
        if ("原始".equals(sizeChoice)) {
            // 保留原始尺寸,但限制到 128 以内(必须是方形,上面已裁剪)
            target = Math.min(side, 128);
        } else {
            target = Integer.parseInt(sizeChoice);
        }
        BufferedImage out = new BufferedImage(target, target, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        // 缩小到小尺寸像素画: 邻近采样保持像素感;大图缩小时用双线性更平滑
        Object hint = target <= 32 && side > target * 4
            ? RenderingHints.VALUE_INTERPOLATION_BILINEAR
            : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
        g.drawImage(square, 0, 0, target, target, null);
        g.dispose();
        return out;
    }

    @Override
    protected void renderExtra(DrawContext context, int mouseX, int mouseY, float delta) {
        // 简易预览: 用平均色块示意(避免注册临时纹理的复杂度,导入后可在选择器中看到真实效果)
        if (previewImage != null) {
            int cx = this.width / 2;
            int px = cx + 60;
            int py = 110;
            int cell = 4;
            int n = 16;
            BufferedImage small = process(previewImage, "16");
            for (int yy = 0; yy < n; yy++) {
                for (int xx = 0; xx < n; xx++) {
                    int c = small.getRGB(xx, yy);
                    if ((c >>> 24) > 0) {
                        context.fill(px + xx * cell, py + yy * cell,
                            px + xx * cell + cell, py + yy * cell + cell, c);
                    }
                }
            }
        }
    }
}
