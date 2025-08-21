package com.github.kuramastone.cobblemonChallenges.gui;

import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.listeners.TickScheduler;
import com.mojang.brigadier.StringReader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class WindowItem {

    @Nullable
    private SimpleWindow window;
    private ItemProvider builder;

    private BiConsumer<ClickType, Integer> runnableOnClick; // runnable to run on click
    private List<String> commands; // commands to run on click
    private int pagesToTurn = 0; // pages to turn on click

    private TickScheduler.ForgeTask task; // used if the item has a scheduled update

    public WindowItem(@Nullable SimpleWindow window, ItemProvider builder) {
        this.window = window;
        this.builder = builder;
    }

    public WindowItem(@Nullable SimpleWindow window, ItemProvider builder, BiConsumer<ClickType, Integer> runnableOnClick, List<String> commands, int pagesToTurn) {
        this.window = window;
        this.builder = builder;
        this.runnableOnClick = runnableOnClick;
        this.commands = commands;
        this.pagesToTurn = pagesToTurn;
    }

    public void setWindow(SimpleWindow window) {
        this.window = window;
    }

    public ItemStack getDisplayItem() {
        return builder.build();
    }

    public ItemProvider getBuilder() {
        return builder;
    }

    public ItemStack handleClick(ClickType type, int dragType, Player player) {
        // handle pages
        if (pagesToTurn != 0) {
            window.setCurrentPage(window.getCurrentPage() + pagesToTurn);
        }

        // handle commands
        if (commands != null) {
            MinecraftServer server = CobbleChallengeMod.getMinecraftServer();
            for (String cmd : commands) {
                cmd = cmd.replace("{player}", player.getName().getString());

                server.getCommands()
                        .performCommand(
                                server.getCommands().getDispatcher()
                                        .parse(new StringReader(cmd), server.createCommandSourceStack()), cmd);
            }
        }

        // use runnable if set
        if (runnableOnClick != null) {
            runnableOnClick.accept(type, dragType);
        }

        // return nothing to the player's cursor by default
        return ItemStack.EMPTY;
    }

    /**
     * Tells the Window to update this slot's display item
     */
    public void notifyWindow() {
        Objects.requireNonNull(window);
        window.updateSlot(this);
    }

    public BiConsumer<ClickType, Integer> getRunnableOnClick() {
        return runnableOnClick;
    }

    public void setRunnableOnClick(BiConsumer<ClickType, Integer> runnableOnClick) {
        this.runnableOnClick = runnableOnClick;
    }

    public WindowItem copy() {
        return new WindowItem(window, builder.copy(), this.runnableOnClick, commands == null ? null : new ArrayList<>(commands), pagesToTurn);
    }

    public void addCommandsOnClick(ArrayList<String> commands) {
        this.commands = commands;
    }

    public int getPagesToTurn() {
        return pagesToTurn;
    }

    public void setPagesToTurn(int pagesToTurn) {
        this.pagesToTurn = pagesToTurn;
    }

    /**
     *
     * @param i
     * @param condition The item will only update if this returns true
     */
    public void setAutoUpdate(int i, Callable<Boolean> condition) {
        final long timeoutBegin = System.currentTimeMillis();

        // cancel old task if possible
        if (task != null) {
            task.setCancelled(true);
        }

        task = TickScheduler.scheduleRepeating(i, () -> {
            if (condition.call())
                notifyWindow();

            // after 5 minutes, it will stop updating even if it has viewers. this is a safety to prevent it from continuing forever if something goes wrong
            if (timeoutBegin + 1000 * 60 * 5 < System.currentTimeMillis()) {
                return false;
            }

            // wait for window assignment
            if (window == null)
                return true;

            // stop repeating if nobody is viewing
            return window.isAnyoneViewing();
        });
    }
}
