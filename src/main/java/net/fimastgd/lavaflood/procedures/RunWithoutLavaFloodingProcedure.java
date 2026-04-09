package net.fimastgd.lavaflood.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;

import net.fimastgd.lavaflood.network.LavafloodModVariables;

public class RunWithoutLavaFloodingProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		LavafloodModVariables.WorldVariables.get(world).LavaFlooding = -1;
		LavafloodModVariables.WorldVariables.get(world).syncData(world);
		if (entity instanceof Player _player)
			_player.closeContainer();
	}
}
