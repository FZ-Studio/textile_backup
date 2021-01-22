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

package net.szum123321.textile_backup.core.restore;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.szum123321.textile_backup.Statics;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/*
    This thread waits some amount of time and then starts a new, independent thread
*/
public class AwaitThread extends Thread {
    private final static AtomicInteger threadCounter = new AtomicInteger(0);

    private int delay;
    private final int thisThreadId = threadCounter.getAndIncrement();
    private final Runnable taskRunnable;
    List<ServerPlayerEntity> allPlayers;

    public AwaitThread(int delay, Runnable taskRunnable, List allPlayers) {
        this.setName("Textile Backup await thread nr. " + thisThreadId);
        this.delay = delay;
        this.taskRunnable = taskRunnable;
        this.allPlayers = allPlayers;
    }

    @Override
    public void run() {
        Statics.LOGGER.info("Countdown begins... Waiting {} second.", delay);
        // 𝄞 This is final count down! Tu ruru Tu, Tu Ru Tu Tu ♪
        while (delay > 0) {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                Statics.LOGGER.info("Backup restoration cancelled.");
                allPlayers.forEach(action -> {
                    action.sendSystemMessage(
                            Statics.LOGGER.getPrefixText().shallowCopy().append(
                                    new LiteralText("Backup restoration cancelled.").formatted(Formatting.WHITE)),
                            Util.NIL_UUID);
                });
                return;
            }
            delay--;
            if ((delay <= 5 && delay > 0) || (delay >= 10 && delay < 60 && delay % 10 == 0)
                    || (delay >= 60 && delay % 60 == 0)) {
                allPlayers.forEach(action -> {
                    action.sendSystemMessage(Statics.LOGGER.getPrefixText().shallowCopy()
                            .append(new LiteralText(delay + "s").formatted(Formatting.WHITE)), Util.NIL_UUID);
                });
                Statics.LOGGER.info("{}s", delay);
            }
        }

        /*
         * We're leaving together, But still it's farewell And maybe we'll come back
         */
        new Thread(taskRunnable, "Textile Backup restore thread nr. " + thisThreadId).start();
    }
}
