package dev.hyxt.modcrafter.mixin;

import net.minecraft.registry.SimpleRegistry;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * 访问 SimpleRegistry 的私有字段,用于游戏内"热注册"新物品/方块:
 * 临时解冻注册表 -> 注册 -> 重新冻结。
 */
@Mixin(SimpleRegistry.class)
public interface SimpleRegistryAccessor<T> {

    @Accessor("frozen")
    boolean modcrafter$isFrozen();

    @Accessor("frozen")
    void modcrafter$setFrozen(boolean frozen);

    @Accessor("intrusiveValueToEntry")
    Map<T, RegistryEntry.Reference<T>> modcrafter$getIntrusiveValueToEntry();

    @Accessor("intrusiveValueToEntry")
    void modcrafter$setIntrusiveValueToEntry(Map<T, RegistryEntry.Reference<T>> map);
}
