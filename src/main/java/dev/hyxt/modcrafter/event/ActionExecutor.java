package dev.hyxt.modcrafter.event;

import dev.hyxt.modcrafter.ModCrafter;
import dev.hyxt.modcrafter.data.ActionDef;
import dev.hyxt.modcrafter.data.EventDef;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Locale;
import java.util.Optional;

/** 执行事件中的动作列表 */
public final class ActionExecutor {

    private ActionExecutor() {
    }

    public static void executeAll(EventDef event, ServerPlayerEntity player, BlockPos pos) {
        for (ActionDef action : event.actions) {
            try {
                execute(action, player, pos);
            } catch (Exception e) {
                ModCrafter.LOGGER.error("执行动作失败: " + action.type, e);
            }
        }
    }

    public static void execute(ActionDef a, ServerPlayerEntity player, BlockPos pos) {
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
            default -> ModCrafter.LOGGER.warn("未知动作类型: {}", a.type);
        }
    }

    /** GUI 用: 动作类型列表 */
    public static final String[] TYPES = {
        "MESSAGE", "GIVE_EFFECT", "EXPLOSION", "LIGHTNING", "COMMAND",
        "GIVE_ITEM", "PLAY_SOUND", "LAUNCH", "HEAL", "DAMAGE", "SET_FIRE",
        "SPAWN_ENTITY", "GIVE_XP", "TELEPORT_RELATIVE", "SET_WEATHER", "PARTICLE"
    };

    /** GUI 用: 触发器类型列表 */
    public static final String[] TRIGGERS = {
        "ITEM_USE", "ITEM_ATTACK_ENTITY", "ITEM_EATEN", "ITEM_HELD_TICK",
        "BLOCK_BREAK", "BLOCK_USE", "BLOCK_STEPPED_ON", "BLOCK_PLACED"
    };

    public static String triggerKey(String trigger) {
        return "modcrafter.trigger." + trigger.toLowerCase(Locale.ROOT);
    }

    public static String actionKey(String action) {
        return "modcrafter.action." + action.toLowerCase(Locale.ROOT);
    }
}
