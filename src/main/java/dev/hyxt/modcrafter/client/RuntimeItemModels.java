package dev.hyxt.modcrafter.client;

import dev.hyxt.modcrafter.runtime.RuntimeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

import java.util.Map;

/**
 * 修复热注册物品的物品栏图标不显示的问题。
 *
 * 原因: ItemRenderer 在客户端启动时把 物品->模型ID 的映射快照进 ItemModels
 * (遍历一次物品注册表),资源重载只会刷新映射里已有的条目;
 * 之后热注册的物品不在映射里,图标永远查不到模型,直到重启。
 *
 * 修复: 在每次热注册后、reloadResources 之前,把我们运行期注册的所有物品
 * 补进 ItemModels 映射(幂等,可重复调用)。
 */
public final class RuntimeItemModels {

    private RuntimeItemModels() {
    }

    /** 热注册后、MinecraftClient.reloadResources() 之前调用 */
    public static void syncAll(MinecraftClient client) {
        for (Map.Entry<Identifier, Item> e : RuntimeRegistry.ITEMS.entrySet()) {
            client.getItemRenderer().getModels()
                .putModel(e.getValue(), new ModelIdentifier(e.getKey(), "inventory"));
        }
    }
}
