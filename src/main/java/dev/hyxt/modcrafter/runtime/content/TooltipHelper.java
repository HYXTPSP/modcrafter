package dev.hyxt.modcrafter.runtime.content;

import dev.hyxt.modcrafter.data.ItemDef;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public final class TooltipHelper {
    private TooltipHelper() {
    }

    public static void append(ItemDef def, List<Text> tooltip) {
        if (def.tooltip == null) return;
        for (String line : def.tooltip) {
            if (line == null || line.isEmpty()) continue;
            tooltip.add(Text.literal(line.replace('&', '§')).formatted(Formatting.GRAY));
        }
    }
}
