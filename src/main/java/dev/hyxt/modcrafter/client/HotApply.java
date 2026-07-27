package dev.hyxt.modcrafter.client;

import dev.hyxt.modcrafter.ModCrafter;
import dev.hyxt.modcrafter.data.ContentPack;
import dev.hyxt.modcrafter.data.PackManager;
import dev.hyxt.modcrafter.event.EventRuntime;
import dev.hyxt.modcrafter.runtime.DatapackGen;
import dev.hyxt.modcrafter.runtime.HotAdd;
import dev.hyxt.modcrafter.runtime.ResourcePackGen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

/**
 * "应用更改": 保存内容包 -> 热注册新元素 -> 重新生成并重载资源包/数据包。
 * 返回给玩家看的结果消息。
 */
public final class HotApply {

    private HotApply() {
    }

    public static Text apply(ContentPack pack) {
        PackManager.save(pack);

        int added = HotAdd.tryHotRegister(pack);

        // 模型可能被重新编辑,刷新已注册方块的碰撞/轮廓箱
        dev.hyxt.modcrafter.runtime.PackRegistrar.refreshShapes();

        // 客户端资源(模型/贴图/名字)
        ResourcePackGen.regenerate();
        MinecraftClient client = MinecraftClient.getInstance();
        ModCrafterClient.ensureResourcePackEnabled(client);
        // 把热注册的物品补进 ItemRenderer 的模型映射,否则物品栏图标要重启才显示
        RuntimeItemModels.syncAll(client);
        // 透明方块需要 cutout 渲染层
        ModCrafterClient.applyRenderLayers();
        client.reloadResources();

        // 服务端数据(配方/掉落/标签) —— 世界运行中才有
        MinecraftServer server = ModCrafter.runningServer;
        if (server != null) {
            server.execute(() -> DatapackGen.writeAndEnable(server));
        }

        EventRuntime.rebuildIndex();

        if (added < 0) {
            return Text.translatable("modcrafter.msg.apply_restart");
        }
        if (added > 0) {
            return Text.translatable("modcrafter.msg.apply_ok_new", added);
        }
        return Text.translatable("modcrafter.msg.apply_ok");
    }
}
