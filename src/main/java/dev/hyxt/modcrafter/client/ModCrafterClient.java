package dev.hyxt.modcrafter.client;

import dev.hyxt.modcrafter.client.gui.MainScreen;
import dev.hyxt.modcrafter.runtime.ResourcePackGen;
import dev.hyxt.modcrafter.runtime.RuntimeRegistry;
import dev.hyxt.modcrafter.runtime.content.McBlock;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ModCrafterClient implements ClientModInitializer {
    public static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        // 启动早期生成资源包目录,让首次资源加载就能读到
        ResourcePackGen.regenerate();

        // 透明方块使用 cutout 渲染层
        applyRenderLayers();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.modcrafter.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.modcrafter"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new MainScreen(null));
                }
            }
        });

        // 客户端启动完成后确保资源包被启用
        ClientLifecycleEvents.CLIENT_STARTED.register(ModCrafterClient::ensureResourcePackEnabled);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("modcrafter").executes(ctx -> {
                MinecraftClient client = MinecraftClient.getInstance();
                client.send(() -> client.setScreen(new MainScreen(null)));
                return 1;
            })));
    }

    /** 为所有"透明"自定义方块注册 cutout 渲染层(可随时重复调用) */
    public static void applyRenderLayers() {
        for (Block block : RuntimeRegistry.BLOCKS.values()) {
            if (block instanceof McBlock mc && mc.def.transparent) {
                BlockRenderLayerMap.INSTANCE.putBlock(block, RenderLayer.getCutout());
            }
        }
    }

    public static void ensureResourcePackEnabled(MinecraftClient client) {
        if (!client.options.resourcePacks.contains(ResourcePackGen.PACK_PROFILE_ID)) {
            client.options.resourcePacks.add(ResourcePackGen.PACK_PROFILE_ID);
            client.options.write();
            client.reloadResources();
        }
    }
}
