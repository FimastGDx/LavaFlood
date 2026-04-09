package net.fimastgd.lavaflood.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;

@Mod.EventBusSubscriber(modid = "lavaflood", value = Dist.CLIENT)
public class LavaFloodHudOverlay {

    // сброс при выходе из мира / отключении
    @SubscribeEvent
    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        LavaFloodClientData.reset();
    }

    // рендер
    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.options.hideGui) return;

        PoseStack ps      = event.getMatrixStack();
        int       screenW = mc.getWindow().getGuiScaledWidth();
        int       screenH = mc.getWindow().getGuiScaledHeight();

        long dayTime = mc.level.getDayTime();
        long day     = dayTime / 24000L + 1;           // день 1 с самого начала

        // определяем время суток внутри дня
        long timeOfDay = dayTime % 24000L;
        String period;
        if (timeOfDay < 6000)       period = "§eMorning";
        else if (timeOfDay < 12000) period = "§6Day";
        else if (timeOfDay < 18000) period = "§9Evening";
        else                        period = "§1Night";

        String dayLine = "§7Day §f" + day + " §7(" + period + "§7)";

        int yDay = screenH - 60;   // чуть выше блока лавы
        mc.font.drawShadow(ps, dayLine,
                (screenW - mc.font.width(dayLine)) / 2f, yDay, 0xFFFFFF);

        if (!LavaFloodClientData.active) return;

        String line1 = "§cLava level: §eY = " + LavaFloodClientData.currentY;
        String line2 = "§fNext rise in: §b" + LavaFloodClientData.secondsRemaining + "s";

        int y1 = screenH - 48;
        int y2 = screenH - 37;

        mc.font.drawShadow(ps, line1,
                (screenW - mc.font.width(line1)) / 2f, y1, 0xFFFFFF);
        mc.font.drawShadow(ps, line2,
                (screenW - mc.font.width(line2)) / 2f, y2, 0xFFFFFF);
    }
}