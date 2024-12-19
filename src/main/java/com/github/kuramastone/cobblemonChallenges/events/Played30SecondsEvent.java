package com.github.kuramastone.cobblemonChallenges.events;

import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;

public class Played30SecondsEvent {
    private final PlayerProfile playerProfile;

    public Played30SecondsEvent(PlayerProfile playerProfile) {
        this.playerProfile = playerProfile;
    }

    public PlayerProfile getPlayerProfile() {
        return playerProfile;
    }
}
