package dev.hyxt.modcrafter.client.gui;

import dev.hyxt.modcrafter.data.ContentPack;
import dev.hyxt.modcrafter.data.PackManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/** 新建内容包 */
public class NewPackScreen extends BaseScreen {
    private TextFieldWidget idField;
    private TextFieldWidget nameField;
    private TextFieldWidget authorField;

    public NewPackScreen(Screen parent) {
        super(parent, Text.translatable("modcrafter.title.new_pack"));
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int y = 50;

        label(cx - 100, y - 12, Text.translatable("modcrafter.label.pack_id"));
        idField = addField(cx - 100, y, 200, 18, idField != null ? idField.getText() : "");
        y += 36;

        label(cx - 100, y - 12, Text.translatable("modcrafter.label.pack_name"));
        nameField = addField(cx - 100, y, 200, 18, nameField != null ? nameField.getText() : "");
        y += 36;

        label(cx - 100, y - 12, Text.translatable("modcrafter.label.pack_author"));
        authorField = addField(cx - 100, y, 200, 18, authorField != null ? authorField.getText() : "");
        y += 36;

        label(cx - 100, y, Text.translatable("modcrafter.label.pack_id_rule"), 0x808080);
        y += 24;

        addBtn(cx - 100, y, 95, 20, Text.translatable("modcrafter.btn.create"), this::create);
        addBtn(cx + 5, y, 95, 20, Text.translatable("modcrafter.btn.back"), this::close);
    }

    private void create() {
        String id = idField.getText().trim().toLowerCase();
        if (!ContentPack.isValidId(id)) {
            setFeedback(Text.translatable("modcrafter.msg.bad_id"), false);
            return;
        }
        if (id.equals("minecraft") || id.equals("modcrafter") || PackManager.get(id) != null) {
            setFeedback(Text.translatable("modcrafter.msg.id_taken"), false);
            return;
        }
        ContentPack pack = PackManager.create(id, nameField.getText().trim(), authorField.getText().trim());
        this.client.setScreen(new PackScreen(new MainScreen(null), pack));
    }
}
