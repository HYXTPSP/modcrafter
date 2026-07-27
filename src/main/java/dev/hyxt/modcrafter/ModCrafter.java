package dev.hyxt.modcrafter;

import dev.hyxt.modcrafter.data.PackManager;
import dev.hyxt.modcrafter.event.EventRuntime;
import dev.hyxt.modcrafter.runtime.DatapackGen;
import dev.hyxt.modcrafter.runtime.PackRegistrar;
import dev.hyxt.modcrafter.runtime.RuntimeRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModCrafter implements ModInitializer {
    public static final String MOD_ID = "modcrafter";
    public static final Logger LOGGER = LoggerFactory.getLogger("ModCrafter");

    /** 当前运行中的服务器(单人=集成服务器),用于编辑后刷新数据包 */
    public static MinecraftServer runningServer = null;

    public static final ItemGroup GROUP = FabricItemGroup.builder()
        .icon(RuntimeRegistry::groupIcon)
        .displayName(Text.translatable("itemGroup.modcrafter.main"))
        .entries((context, entries) -> {
            for (Item item : RuntimeRegistry.ITEMS.values()) {
                entries.add(item);
            }
        })
        .build();

    @Override
    public void onInitialize() {
        LOGGER.info("ModCrafter 模组工坊启动中……");

        // 1. 从磁盘加载全部内容包
        PackManager.loadAll();

        // 2. 注册创造物品栏分组
        Registry.register(Registries.ITEM_GROUP, Identifier.of(MOD_ID, "main"), GROUP);

        // 3. 注册所有内容包里的物品和方块
        PackRegistrar.registerAll();

        // 4. 挂接事件运行时(触发器 -> 动作)
        EventRuntime.init();

        // 4.5 矿石世界生成
        dev.hyxt.modcrafter.runtime.OreGen.init();

        // 5. 服务器生命周期: 生成并启用配方/战利品数据包
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            runningServer = server;
            DatapackGen.writeAndEnable(server);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            if (runningServer == server) runningServer = null;
        });

        LOGGER.info("ModCrafter 就绪: {} 个内容包, {} 个物品, {} 个方块",
            PackManager.all().size(), RuntimeRegistry.ITEMS.size(), RuntimeRegistry.BLOCKS.size());
    }
}
