
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.fimastgd.lavaflood.init;

import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.client.gui.screens.MenuScreens;

import net.fimastgd.lavaflood.client.gui.StartGuiScreen;
import net.fimastgd.lavaflood.client.gui.MainGuiScreen;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class LavafloodModScreens {
	@SubscribeEvent
	public static void clientLoad(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			MenuScreens.register(LavafloodModMenus.MAIN_GUI, MainGuiScreen::new);
			MenuScreens.register(LavafloodModMenus.START_GUI, StartGuiScreen::new);
		});
	}
}
