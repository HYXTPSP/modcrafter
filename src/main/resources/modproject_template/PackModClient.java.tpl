package {{PACKAGE}};

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.block.Block;
import net.minecraft.client.render.RenderLayer;

import java.util.Map;
import net.minecraft.util.Identifier;

/** 客户端初始化: 透明方块使用 cutout 渲染层 */
public class PackModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        for (Map.Entry<Identifier, Block> e : PackMod.BLOCKS.entrySet()) {
            if (e.getValue() instanceof ContentClasses.McBlock mc && mc.def.transparent) {
                BlockRenderLayerMap.INSTANCE.putBlock(e.getValue(), RenderLayer.getCutout());
            }
        }
    }
}
