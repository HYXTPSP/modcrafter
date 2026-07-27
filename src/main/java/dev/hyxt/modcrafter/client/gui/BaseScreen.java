package dev.hyxt.modcrafter.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** 所有 ModCrafter 界面的基类: 返回父界面 / 标签绘制 / 常用控件工厂 */
public abstract class BaseScreen extends Screen {
    protected final Screen parent;
    protected final List<Label> labels = new ArrayList<>();
    /** 底部反馈消息 */
    protected Text feedback = null;
    protected int feedbackColor = 0xFFFFFF;

    protected record Label(int x, int y, Text text, int color) {
    }

    protected BaseScreen(Screen parent, Text title) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        labels.clear();
        super.init();
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    protected ButtonWidget addBtn(int x, int y, int w, int h, Text text, Runnable action) {
        ButtonWidget btn = ButtonWidget.builder(text, b -> action.run()).dimensions(x, y, w, h).build();
        this.addDrawableChild(btn);
        return btn;
    }

    protected TextFieldWidget addField(int x, int y, int w, int h, String initial) {
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, x, y, w, h, Text.empty());
        field.setMaxLength(256);
        field.setText(initial == null ? "" : initial);
        this.addDrawableChild(field);
        return field;
    }

    protected void label(int x, int y, Text text) {
        labels.add(new Label(x, y, text, 0xA0A0A0));
    }

    protected void label(int x, int y, Text text, int color) {
        labels.add(new Label(x, y, text, color));
    }

    protected void setFeedback(Text text, boolean ok) {
        this.feedback = text;
        this.feedbackColor = ok ? 0x80FF80 : 0xFF8080;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
        for (Label l : labels) {
            context.drawTextWithShadow(this.textRenderer, l.text(), l.x(), l.y(), l.color());
        }
        if (feedback != null) {
            context.drawCenteredTextWithShadow(this.textRenderer, feedback, this.width / 2, this.height - 40, feedbackColor);
        }
        renderExtra(context, mouseX, mouseY, delta);
    }

    /** 子类的额外绘制(物品图标、贴图预览等) */
    protected void renderExtra(DrawContext context, int mouseX, int mouseY, float delta) {
    }
}
