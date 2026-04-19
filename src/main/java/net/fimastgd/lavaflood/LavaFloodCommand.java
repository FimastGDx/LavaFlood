package net.mcreator.lavaflood.handler;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.fimastgd.lavaflood.handler.LavaFloodHandler;

@Mod.EventBusSubscriber(modid = "lavaflood")
public class LavaFloodCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("lavaflood")
                .requires(src -> src.hasPermission(2)); // op-level 2

        // /lavaflood autofix <seconds>
        root.then(
                Commands.literal("autofix")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 1800))
                                .executes(ctx -> {
                                    int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
                                    return runAutofix(ctx.getSource(), seconds);
                                })));

        // /lavaflood fix <y> <seconds>
        root.then(
                Commands.literal("fix")
                        .then(Commands.argument("y", IntegerArgumentType.integer(-64, 320))
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 1800))
                                        .executes(ctx -> {
                                            int y = IntegerArgumentType.getInteger(ctx, "y");
                                            int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
                                            return runFix(ctx.getSource(), y, seconds);
                                        }))));

        // /lavaflood stop
        root.then(
                Commands.literal("stop")
                        .executes(ctx -> runStop(ctx.getSource())));

        event.getDispatcher().register(root);
    }

    // /lavaflood autofix <seconds>
    private static int runAutofix(CommandSourceStack src, int seconds) {
        try {
            ServerPlayer player = src.getPlayerOrException();
            ServerLevel level = (ServerLevel) player.getLevel();

            int cx = (int) player.getX();
            int cz = (int) player.getZ();
            int minY = level.getMinBuildHeight(); // -64
            int maxY = level.getMaxBuildHeight(); // 320

            src.sendSuccess(new TextComponent("§eScanning world for lava level..."), false);

            int foundY = minY - 1;

            outer: for (int y = minY; y < maxY; y++) {
                int lavaCount = 0;
                for (int probe = 0; probe < 25; probe++) {
                    int dx = (int) (Math.random() * 250) - 125;
                    int dz = (int) (Math.random() * 250) - 125;
                    BlockPos pos = new BlockPos(cx + dx, y, cz + dz);
                    if (!level.isLoaded(pos))
                        continue;
                    if (level.getBlockState(pos).getBlock() == Blocks.LAVA) {
                        lavaCount++;
                    }
                }
                if (lavaCount >= 15) {
                    foundY = y;
                }
            }

            if (foundY < minY) {
                src.sendSuccess(new TextComponent(
                        "§cCould not detect any lava layer. "
                                + "Use §f/lavaflood fix <y> <seconds>§c instead."),
                        false);
                return 0;
            }

            int resumeY = foundY + 1; // next layer
            resumeFlood(level, player, cx, cz, resumeY, seconds);

            src.sendSuccess(new TextComponent(
                    "§aAutofix complete! Detected lava top: §eY=" + foundY
                            + "§a. Resuming from §eY=" + resumeY
                            + "§a with interval §e" + seconds + "s§a."),
                    false);
            return 1;

        } catch (Exception e) {
            src.sendFailure(new TextComponent("§cMust be run by a player. " + e.getMessage()));
            return 0;
        }
    }

    // /lavaflood fix <y> <seconds>
    private static int runFix(CommandSourceStack src, int y, int seconds) {
        try {
            ServerPlayer player = src.getPlayerOrException();
            ServerLevel level = (ServerLevel) player.getLevel();

            int cx = (int) player.getX();
            int cz = (int) player.getZ();

            resumeFlood(level, player, cx, cz, y, seconds);

            src.sendSuccess(new TextComponent(
                    "§aFlood resumed from §eY=" + y
                            + "§a, center §e(" + cx + ", " + cz + ")"
                            + "§a, interval §e" + seconds + "s§a."),
                    false);
            return 1;

        } catch (Exception e) {
            src.sendFailure(new TextComponent("§cMust be run by a player. " + e.getMessage()));
            return 0;
        }
    }

    // /lavaflood stop
    private static int runStop(CommandSourceStack src) {
        LavaFloodHandler.stop();
        src.sendSuccess(new TextComponent("§cLava flood stopped."), true);
        return 1;
    }

    private static void resumeFlood(ServerLevel level, ServerPlayer player,
            int cx, int cz, int y, int seconds) {
        WorldBorder border = level.getWorldBorder();

        // Трогаем границу только если она ещё не выставлена (размер по умолчанию —
        // 60млн)
        if (border.getSize() > 1000) {
            border.setCenter(cx + 0.5, cz + 0.5);
            border.setSize(250);
        }

        LavaFloodHandler.resume(level, cx, cz, y, seconds);
    }
}