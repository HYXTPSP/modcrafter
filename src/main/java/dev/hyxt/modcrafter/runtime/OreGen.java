package dev.hyxt.modcrafter.runtime;

import dev.hyxt.modcrafter.ModCrafter;
import dev.hyxt.modcrafter.data.BlockDef;
import dev.hyxt.modcrafter.data.ContentPack;
import dev.hyxt.modcrafter.data.PackManager;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;

import java.util.HashSet;
import java.util.Set;

/**
 * 矿石世界生成: 把每个启用了 oreGen 的方块的 placed feature 注入主世界生物群系。
 *
 * placed feature 的 JSON 在世界数据包(ModCrafterData)里,由 DatapackGen 生成。
 * 世界第一次进入时数据包还没写入 -> feature key 不存在 -> addFeature 会抛异常,
 * 这里 try/catch 掉(该次进入不生成矿,数据包写入后重进世界即生效),避免崩溃。
 */
public final class OreGen {
    private static final Set<String> WARNED = new HashSet<>();

    private OreGen() {
    }

    public static void init() {
        BiomeModifications.create(Identifier.of(ModCrafter.MOD_ID, "ores"))
            .add(ModificationPhase.ADDITIONS, BiomeSelectors.foundInOverworld(), context -> {
                // 每次世界加载时读取"当前"的内容包定义(支持热添加的矿石在重进世界后生效)
                for (ContentPack pack : PackManager.all()) {
                    for (BlockDef block : pack.blocks) {
                        if (block.oreGen == null || !block.oreGen.enabled) continue;
                        Identifier featureId = Identifier.of(pack.id, "ore_" + block.id);
                        try {
                            context.getGenerationSettings().addFeature(
                                GenerationStep.Feature.UNDERGROUND_ORES,
                                RegistryKey.of(RegistryKeys.PLACED_FEATURE, featureId));
                        } catch (Exception e) {
                            // 数据包尚未写入该世界(首次进入): 跳过,重进世界后生效
                            if (WARNED.add(featureId.toString())) {
                                ModCrafter.LOGGER.warn("矿石 {} 的 placed feature 尚未加载,重进世界后开始生成", featureId);
                            }
                        }
                    }
                }
            });
    }
}
