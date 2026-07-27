package dev.hyxt.modcrafter.runtime;

import dev.hyxt.modcrafter.ModCrafter;
import dev.hyxt.modcrafter.data.ContentPack;
import dev.hyxt.modcrafter.event.EventRuntime;
import dev.hyxt.modcrafter.mixin.SimpleRegistryAccessor;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.SimpleRegistry;

import java.util.IdentityHashMap;

/**
 * 游戏内"热注册": 在注册表已冻结之后注册新的物品/方块。
 * 原理: 通过 Mixin 访问器临时解冻 SimpleRegistry,并恢复 intrusive holder 支持,
 * 注册完成后重新冻结。仅建议在单人游戏(集成服务器)中使用;
 * 失败时优雅降级为"重启后生效"。
 */
public final class HotAdd {

    private HotAdd() {
    }

    /**
     * 尝试热注册内容包中的新元素。
     * @return 新注册的元素数量; -1 表示热注册失败(需要重启)
     */
    public static int tryHotRegister(ContentPack pack) {
        try {
            int added;
            unfreeze(Registries.ITEM);
            unfreeze(Registries.BLOCK);
            try {
                added = PackRegistrar.registerPack(pack);
            } finally {
                refreeze(Registries.ITEM);
                refreeze(Registries.BLOCK);
            }
            EventRuntime.rebuildIndex();
            return added;
        } catch (Throwable t) {
            ModCrafter.LOGGER.error("热注册失败,内容将在重启后生效", t);
            EventRuntime.rebuildIndex();
            return -1;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> void unfreeze(Registry<T> registry) {
        if (!(registry instanceof SimpleRegistry)) return;
        SimpleRegistryAccessor<T> accessor = (SimpleRegistryAccessor) registry;
        accessor.modcrafter$setFrozen(false);
        if (accessor.modcrafter$getIntrusiveValueToEntry() == null) {
            accessor.modcrafter$setIntrusiveValueToEntry(new IdentityHashMap<>());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> void refreeze(Registry<T> registry) {
        if (!(registry instanceof SimpleRegistry)) return;
        SimpleRegistryAccessor<T> accessor = (SimpleRegistryAccessor) registry;
        accessor.modcrafter$setFrozen(true);
        // 把 intrusive map 置回 null,恢复冻结后的正常状态
        // (map 中此时应为空 —— 我们注册的条目都已绑定)
        var map = accessor.modcrafter$getIntrusiveValueToEntry();
        if (map != null && map.isEmpty()) {
            accessor.modcrafter$setIntrusiveValueToEntry(null);
        }
    }

    /** 检查某个 Item 类是否是我们的运行时会构造 intrusive holder 的类型 —— 仅诊断用 */
    public static boolean registriesLookHealthy() {
        try {
            return Registries.ITEM.containsId(net.minecraft.util.Identifier.of("minecraft", "stone"))
                && Registries.BLOCK.containsId(net.minecraft.util.Identifier.of("minecraft", "stone"));
        } catch (Throwable t) {
            return false;
        }
    }
}
