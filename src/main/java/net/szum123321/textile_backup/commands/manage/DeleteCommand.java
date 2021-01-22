/*
 * A simple backup mod for Fabric
 * Copyright (C) 2020  Szum123321
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.szum123321.textile_backup.commands.manage;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.szum123321.textile_backup.commands.CommandExceptions;
import net.szum123321.textile_backup.Statics;
import net.szum123321.textile_backup.commands.FileSuggestionProvider;
import net.szum123321.textile_backup.core.Utilities;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Optional;

public class DeleteCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register() {
        return CommandManager.literal("delete")
                .then(CommandManager.argument("file", StringArgumentType.word())
                        .suggests(FileSuggestionProvider.Instance())
                        .executes(ctx -> execute(ctx.getSource(), StringArgumentType.getString(ctx, "file"))));
    }

    private static int execute(ServerCommandSource source, String fileName) throws CommandSyntaxException {
        LocalDateTime dateTime;

        try {
            dateTime = LocalDateTime.from(Statics.defaultDateTimeFormatter.parse(fileName));
        } catch (DateTimeParseException e) {
            throw CommandExceptions.DATE_TIME_PARSE_COMMAND_EXCEPTION_TYPE.create(e);
        }

        File root = Utilities.getBackupRootPath(Utilities.getLevelName(source.getMinecraftServer()));

        Optional<File> optionalFile = Arrays.stream(root.listFiles()).filter(Utilities::isValidBackup)
                .filter(file -> Utilities.getFileCreationTime(file).orElse(LocalDateTime.MIN).equals(dateTime))
                .findFirst();

        if (optionalFile.isPresent()) {
            if (Statics.untouchableFile == null
                    || (Statics.untouchableFile != null && !Statics.untouchableFile.equals(optionalFile.get()))) {
                if (optionalFile.get().delete()) {

                    final MutableText message0 = Statics.LOGGER.getPrefixText().shallowCopy()
                            .append(new LiteralText("File " + optionalFile.get().getName() + " successfully deleted!")
                                    .formatted(Formatting.WHITE));
                    source.getMinecraftServer().getPlayerManager().getPlayerList().forEach(action -> {
                        action.sendSystemMessage(message0, Util.NIL_UUID);
                    });

                    if (source.getEntity() instanceof PlayerEntity) {
                        Statics.LOGGER.info("Player {} deleted {}.", source.getPlayer().getName().getString(),
                                optionalFile.get().getName());
                    } else {
                        Statics.LOGGER.info("File {} successfully deleted!", optionalFile.get().getName());
                    }
                } else {
                    Statics.LOGGER.sendError(source, "Something went wrong while deleting file!");
                }
            } else {
                Statics.LOGGER.sendError(source, "Couldn't delete the file because it's being restored right now.");
                Statics.LOGGER.sendHint(source, "If you want to abort restoration then use: /backup killR");
            }
        } else {
            Statics.LOGGER.sendError(source, "Couldn't find file by this name.");
            Statics.LOGGER.sendHint(source, "Maybe try /backup list");
        }

        return 0;
    }
}
