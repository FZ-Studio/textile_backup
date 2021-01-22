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

package net.szum123321.textile_backup.core.create.compressors;

import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.szum123321.textile_backup.Statics;
import net.szum123321.textile_backup.core.ActionInitiator;
import net.szum123321.textile_backup.core.Utilities;
import net.szum123321.textile_backup.core.create.BackupContext;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;

public abstract class AbstractCompressor {
    public void createArchive(File inputFile, File outputFile, BackupContext ctx, int coreLimit) {
        Instant start = Instant.now();

        try (FileOutputStream outStream = new FileOutputStream(outputFile);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outStream);
                OutputStream arc = createArchiveOutputStream(bufferedOutputStream, ctx, coreLimit)) {

            Files.walk(inputFile.toPath()).filter(path -> !Utilities.isBlacklisted(inputFile.toPath().relativize(path)))
                    .map(Path::toFile).filter(File::isFile).forEach(file -> {
                        try {
                            addEntry(file, inputFile.toPath().relativize(file.toPath()).toString(), arc);
                        } catch (IOException e) {
                            Statics.LOGGER.error("An exception occurred while trying to compress: {}", file.getName(),
                                    e);

                            if (ctx.getInitiator() == ActionInitiator.Player)
                                Statics.LOGGER.sendError(ctx, "Something went wrong while compressing files!");
                        }
                    });

            finish(arc);
        } catch (IOException | InterruptedException | ExecutionException e) {
            Statics.LOGGER.error("An exception occurred!", e);

            if (ctx.getInitiator() == ActionInitiator.Player)
                Statics.LOGGER.sendError(ctx, "Something went wrong while compressing files!");
        }

        close();

        final MutableText message0 = Statics.LOGGER.getPrefixText().shallowCopy()
                .append(new LiteralText("Compression took: "
                        + Utilities.formatDuration(Duration.between(start, Instant.now())) + " seconds.")
                                .formatted(Formatting.WHITE));
        ctx.getServer().getPlayerManager().getPlayerList().forEach(action -> {
            action.sendSystemMessage(message0, Util.NIL_UUID);
        });
        Statics.LOGGER.info("Compression took: {} seconds.",
                Utilities.formatDuration(Duration.between(start, Instant.now())));
    }

    protected abstract OutputStream createArchiveOutputStream(OutputStream stream, BackupContext ctx, int coreLimit)
            throws IOException;

    protected abstract void addEntry(File file, String entryName, OutputStream arc) throws IOException;

    protected void finish(OutputStream arc) throws InterruptedException, ExecutionException, IOException {
        ;// Basically this function is only needed for the ParallelZipCompressor to write
         // out ParallelScatterZipCreator
    }

    protected void close() {
        ;// Same as above, just for ParallelGzipCompressor to shutdown ExecutorService
    }
}
