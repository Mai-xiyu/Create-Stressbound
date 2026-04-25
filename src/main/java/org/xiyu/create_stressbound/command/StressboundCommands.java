package org.xiyu.create_stressbound.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.xiyu.create_stressbound.compat.MovingStructureSupport;
import org.xiyu.create_stressbound.content.link.StressLinkRecord;
import org.xiyu.create_stressbound.content.link.StressLinkSavedData;
import org.xiyu.create_stressbound.content.link.StressLinkService;

public final class StressboundCommands {
    private StressboundCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("stressbound")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("links")
                .then(Commands.literal("list")
                    .executes(StressboundCommands::listLinks))
                .then(Commands.literal("remove")
                    .then(Commands.argument("id", UuidArgument.uuid())
                        .executes(StressboundCommands::removeLink)))
                .then(Commands.literal("removeplayer")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(StressboundCommands::removePlayerLinks)))
                .then(Commands.literal("setstress")
                    .then(Commands.argument("id", UuidArgument.uuid())
                        .then(Commands.argument("su", IntegerArgumentType.integer(1))
                            .executes(StressboundCommands::setLinkStress))))
                .then(Commands.literal("setallstress")
                    .then(Commands.argument("su", IntegerArgumentType.integer(1))
                        .executes(StressboundCommands::setAllLinkStress))))
            .then(Commands.literal("compat")
                .executes(StressboundCommands::compatSummary));

        event.getDispatcher().register(root);
    }

    private static int listLinks(CommandContext<CommandSourceStack> context) {
        StressLinkSavedData data = StressLinkSavedData.get(context.getSource().getServer());
        if (data.all().isEmpty()) {
            context.getSource().sendSuccess(() -> Component.translatable("command.create_stressbound.links.empty"), false);
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.translatable("command.create_stressbound.links.header", data.all().size()), false);
        for (StressLinkRecord record : data.all()) {
            context.getSource().sendSuccess(() -> Component.translatable("command.create_stressbound.links.entry",
                record.id().toString(),
                record.owner().toString(),
                record.transmitter().describe(),
                record.receiver().describe(),
                record.requestedStress()), false);
        }
        return data.all().size();
    }

    private static int removeLink(CommandContext<CommandSourceStack> context) {
        UUID linkId = UuidArgument.getUuid(context, "id");
        boolean removed = StressLinkService.removeLink(context.getSource().getServer(), linkId);
        if (removed) {
            context.getSource().sendSuccess(() -> Component.translatable("command.create_stressbound.links.removed", linkId.toString()), true);
        } else {
            context.getSource().sendFailure(Component.translatable("command.create_stressbound.links.not_found", linkId.toString()));
        }
        return removed ? 1 : 0;
    }

    private static int removePlayerLinks(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var player = EntityArgument.getPlayer(context, "player");
        int removed = StressLinkService.removeLinksByOwner(context.getSource().getServer(), player.getUUID());
        context.getSource().sendSuccess(() -> Component.translatable("command.create_stressbound.links.removed_player",
            removed, player.getGameProfile().getName()), true);
        return removed;
    }

    private static int setLinkStress(CommandContext<CommandSourceStack> context) {
        UUID linkId = UuidArgument.getUuid(context, "id");
        int requestedStress = IntegerArgumentType.getInteger(context, "su");
        var updated = StressLinkService.setRequestedStress(context.getSource().getServer(), linkId, requestedStress);
        if (updated.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("command.create_stressbound.links.not_found", linkId.toString()));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.translatable("command.create_stressbound.links.stress_set",
            linkId.toString(), updated.get().requestedStress()), true);
        return updated.get().requestedStress();
    }

    private static int setAllLinkStress(CommandContext<CommandSourceStack> context) {
        int requestedStress = IntegerArgumentType.getInteger(context, "su");
        int updated = StressLinkService.setAllRequestedStress(context.getSource().getServer(), requestedStress);
        context.getSource().sendSuccess(() -> Component.translatable("command.create_stressbound.links.stress_set_all",
            updated, requestedStress), true);
        return updated;
    }

    private static int compatSummary(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.translatable("command.create_stressbound.compat", MovingStructureSupport.describeRuntime()), false);
        return 1;
    }
}
