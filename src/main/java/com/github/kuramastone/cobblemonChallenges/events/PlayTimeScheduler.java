package com.github.kuramastone.cobblemonChallenges.events;

import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.listeners.ChallengeListener;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayTimeScheduler {

    private static Set<UUID> cachedOnlinePlayers = new HashSet<>();
    private static long currentTick = 0;

    public static void onServerTick(MinecraftServer minecraftServer) {
        // We only want to run this on the END phase to ensure everything is processed in one tick
        currentTick++;

        // trigger every 30 seconds
        if (currentTick % (30 * 20) == 0) {
            oneSecondTick();
        }
    }

    private static void oneSecondTick() {
        for (ServerPlayer player : CobbleChallengeMod.getMinecraftServer().getPlayerList().getPlayers()) {
            if (cachedOnlinePlayers.contains(player.getUUID())) {
                PlayerProfile profile = CobbleChallengeMod.instance.getAPI().getOrCreateProfile(player.getUUID());
                ChallengeListener.on1SecondPlayed(new Played1SecondEvent(profile));
            }
        }

        // update cache
        cachedOnlinePlayers.clear();
        for (ServerPlayer player : CobbleChallengeMod.getMinecraftServer().getPlayerList().getPlayers()) {
            cachedOnlinePlayers.add(player.getUUID());
        }
    }

}
