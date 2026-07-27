package {{PACKAGE}};

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 事件运行时(ModCrafter 导出的小型运行时) */
public final class EventRt {
    private static final Map<String, List<Defs.EventD>> INDEX = new HashMap<>();

    private EventRt() {
    }

    public static void init(Defs.Pack pack) {
        for (Defs.EventD event : pack.events) {
            if (event.target == null || event.target.isEmpty()) continue;
            INDEX.computeIfAbsent(event.trigger + "|" + event.target, k -> new ArrayList<>()).add(event);
        }

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
        List<Defs.EventD> events = INDEX.get(trigger + "|" + elementId.toString());
        if (events == null) return;
        for (Defs.EventD event : events) {
            for (Defs.ActionD action : event.actions) {
                try {
                    execute(action, player, pos);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static void execute(Defs.ActionD a, ServerPlayerEntity player, BlockPos pos) {
        ServerWorld world = player.getServerWorld();
        switch (a.type) {
            case "MESSAGE" -> {
                if (a.text != null && !a.text.isEmpty()) {
                    player.sendMessage(Text.literal(a.text.replace('&', '§')), false);
                }
            }
            case "GIVE_EFFECT" -> {
                Identifier id = Identifier.tryParse(a.effect);
                if (id == null) return;
                Optional<RegistryEntry.Reference<StatusEffect>> entry =
                    Registries.STATUS_EFFECT.getEntry(RegistryKey.of(RegistryKeys.STATUS_EFFECT, id));
                entry.ifPresent(e -> player.addStatusEffect(
                    new StatusEffectInstance(e, Math.max(1, a.duration), Math.max(0, a.amplifier))));
            }
            case "EXPLOSION" -> {
                float power = Math.min(10f, Math.max(0.5f, a.power));
                world.createExplosion(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    power, a.breakBlocks ? World.ExplosionSourceType.TNT : World.ExplosionSourceType.NONE);
            }
            case "LIGHTNING" -> {
                LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world);
                if (bolt != null) {
                    bolt.refreshPositionAfterTeleport(Vec3d.ofBottomCenter(pos));
                    world.spawnEntity(bolt);
                }
            }
            case "COMMAND" -> {
                if (a.command == null || a.command.isEmpty()) return;
                String cmd = a.command
                    .replace("{player}", player.getGameProfile().getName())
                    .replace("{x}", String.valueOf(pos.getX()))
                    .replace("{y}", String.valueOf(pos.getY()))
                    .replace("{z}", String.valueOf(pos.getZ()));
                player.getServer().getCommandManager().executeWithPrefix(
                    player.getCommandSource().withLevel(4).withSilent(), cmd);
            }
            case "GIVE_ITEM" -> {
                Identifier id = Identifier.tryParse(a.item);
                if (id == null) return;
                Item item = Registries.ITEM.get(id);
                if (item != Items.AIR) {
                    player.getInventory().offerOrDrop(new ItemStack(item, Math.max(1, Math.min(64, a.count))));
                }
            }
            case "PLAY_SOUND" -> {
                Identifier id = Identifier.tryParse(a.sound);
                if (id == null) return;
                SoundEvent sound = Registries.SOUND_EVENT.get(id);
                if (sound != null) {
                    world.playSound(null, pos, sound, SoundCategory.PLAYERS, 1.0f, 1.0f);
                }
            }
            case "LAUNCH" -> {
                player.addVelocity(0, Math.min(5f, Math.max(0.1f, a.power)), 0);
                player.velocityModified = true;
            }
            case "HEAL" -> player.heal(Math.max(0f, a.amount));
            case "DAMAGE" -> player.damage(player.getDamageSources().generic(), Math.max(0f, a.amount));
            case "SET_FIRE" -> player.setOnFireFor(Math.max(1, a.seconds));
            case "SPAWN_ENTITY" -> {
                Identifier id = Identifier.tryParse(a.entity);
                if (id == null || !Registries.ENTITY_TYPE.containsId(id)) return;
                EntityType<?> type = Registries.ENTITY_TYPE.get(id);
                int n = Math.max(1, Math.min(10, a.count));
                for (int i = 0; i < n; i++) {
                    type.spawn(world, pos.up(), SpawnReason.TRIGGERED);
                }
            }
            case "GIVE_XP" -> player.addExperience(Math.max(0, a.xp));
            case "TELEPORT_RELATIVE" -> player.teleport(world,
                player.getX() + a.dx, player.getY() + a.dy, player.getZ() + a.dz,
                player.getYaw(), player.getPitch());
            case "SET_WEATHER" -> {
                switch (a.weather == null ? "RAIN" : a.weather) {
                    case "CLEAR" -> world.setWeather(6000, 0, false, false);
                    case "THUNDER" -> world.setWeather(0, 6000, true, true);
                    default -> world.setWeather(0, 6000, true, false);
                }
            }
            case "PARTICLE" -> {
                Identifier id = Identifier.tryParse(a.particle);
                if (id == null) return;
                var type = Registries.PARTICLE_TYPE.get(id);
                if (type instanceof SimpleParticleType simple) {
                    world.spawnParticles(simple,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        Math.max(1, Math.min(200, a.count)), 0.5, 0.5, 0.5, 0.05);
                }
            }
            default -> {
            }
        }
    }
}
