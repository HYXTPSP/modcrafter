package dev.hyxt.modcrafter.runtime.content;

import dev.hyxt.modcrafter.data.BlockDef;
import dev.hyxt.modcrafter.event.EventRuntime;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** 自定义方块 */
public class McBlock extends Block {
    public final BlockDef def;
    public final String packId;

    public McBlock(BlockDef def, String packId, Settings settings) {
        super(settings);
        this.def = def;
        this.packId = packId;
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        super.onSteppedOn(world, pos, state, entity);
        if (!world.isClient && entity instanceof ServerPlayerEntity player) {
            EventRuntime.onBlockSteppedOn(this, player, pos);
        }
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!world.isClient && placer instanceof ServerPlayerEntity player) {
            EventRuntime.onBlockPlaced(this, player, pos);
        }
    }
}
