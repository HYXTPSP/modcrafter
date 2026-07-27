package dev.hyxt.modcrafter.event;

import dev.hyxt.modcrafter.data.ContentPack;
import dev.hyxt.modcrafter.data.EventDef;
import dev.hyxt.modcrafter.data.PackManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 事件运行时: 把 Fabric 的游戏事件分发到玩家定义的"触发器->动作"。
 * 索引结构: "触发器|完整元素id" -> 事件列表
 */
public final class EventRuntime {
    private static final Map<String, List<EventDef>> INDEX = new HashMap<>();

    private EventRuntime() {
    }

    public static void init() {
        rebuildIndex();

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity sp) {
                Item item = player.getStackInHand(hand).getItem();
                dispatch("ITEM_USE", Registries.ITEM.getId(item), sp, sp.getBlockPos());
            }
            return TypedActionResult.pass(player.getStackInHand(hand));
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity sp) {
                Item item = player.getStackInHand(hand).getItem();
                dispatch("ITEM_ATTACK_ENTITY", Registries.ITEM.getId(item), sp, entity.getBlockPos());
            }
            return ActionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity sp) {
                Block block = world.getBlockState(hitResult.getBlockPos()).getBlock();
                dispatch("BLOCK_USE", Registries.BLOCK.getId(block), sp, hitResult.getBlockPos());
            }
            return ActionResult.PASS;
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClient() && player instanceof ServerPlayerEntity sp) {
                dispatch("BLOCK_BREAK", Registries.BLOCK.getId(state.getBlock()), sp, pos);
            }
        });

        // 手持物品每秒触发
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            heldTickCounter++;
            if (heldTickCounter % 20 != 0) return;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                Item held = player.getMainHandStack().getItem();
                dispatch("ITEM_HELD_TICK", Registries.ITEM.getId(held), player, player.getBlockPos());
            }
        });
    }

    private static int heldTickCounter = 0;

    /** 编辑事件后重建索引 */
    public static void rebuildIndex() {
        INDEX.clear();
        for (ContentPack pack : PackManager.all()) {
            for (EventDef event : pack.events) {
                if (event.target == null || event.target.isEmpty()) continue;
                INDEX.computeIfAbsent(event.trigger + "|" + event.target, k -> new ArrayList<>()).add(event);
            }
        }
    }

    public static void onItemEaten(Item item, ServerPlayerEntity player) {
        dispatch("ITEM_EATEN", Registries.ITEM.getId(item), player, player.getBlockPos());
    }

    public static void onBlockSteppedOn(Block block, ServerPlayerEntity player, BlockPos pos) {
        dispatch("BLOCK_STEPPED_ON", Registries.BLOCK.getId(block), player, pos);
    }

    public static void onBlockPlaced(Block block, ServerPlayerEntity player, BlockPos pos) {
        dispatch("BLOCK_PLACED", Registries.BLOCK.getId(block), player, pos);
    }

    private static void dispatch(String trigger, Identifier elementId, ServerPlayerEntity player, BlockPos pos) {
        List<EventDef> events = INDEX.get(trigger + "|" + elementId.toString());
        if (events == null) return;
        for (EventDef event : events) {
            ActionExecutor.executeAll(event, player, pos);
        }
    }
}
