package com.github.kuramastone.cobblemonChallenges.player;

import com.github.kuramastone.cobblemonChallenges.events.ChallengeCompletedEvent;
import com.github.kuramastone.cobblemonChallenges.listeners.ChallengeListener;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeAPI;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.RequirementLoader;
import com.github.kuramastone.cobblemonChallenges.challenges.Challenge;
import com.github.kuramastone.cobblemonChallenges.challenges.ChallengeList;
import com.github.kuramastone.cobblemonChallenges.challenges.requirements.Progression;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Used to track a player's progress within a challenge
 */
public class ChallengeProgress {

    private CobbleChallengeAPI api;

    private PlayerProfile profile;
    private ChallengeList parentList;
    private Challenge activeChallenge;
    private Map<String, Progression<?>> progressionMap; // <requirement type name, RequirementProgress>
    private long startTime;

    public ChallengeProgress(CobbleChallengeAPI api, PlayerProfile profile, ChallengeList parentList, Challenge activeChallenge, Map<String, Progression<?>> progressionMap, long startTime) {
        this.api = api;
        this.profile = profile;
        this.parentList = parentList;
        this.activeChallenge = activeChallenge;
        this.progressionMap = progressionMap;
        this.startTime = startTime;
    }

    public boolean hasTimeRanOut() {
        if (activeChallenge.getMaxTimeInMilliseconds() == -1) {
            return false;
        }
        return (startTime + activeChallenge.getMaxTimeInMilliseconds()) < System.currentTimeMillis();
    }

    public long getTimeRemaining() {
        if (activeChallenge.getMaxTimeInMilliseconds() == -1) {
            return -1;
        }
        return Math.max(0, (startTime + activeChallenge.getMaxTimeInMilliseconds()) - System.currentTimeMillis());
    }

    private void completedActiveChallenge() {
        if (activeChallenge == null) {
            return;
        }
        profile.completeChallenge(parentList, activeChallenge);
        profile.removeActiveChallenge(this);

        ChallengeListener.onChallengeCompleted(new ChallengeCompletedEvent(profile, parentList, activeChallenge));
    }

    public void progress(@Nullable Object obj) {
        if (this.activeChallenge == null) {
            return;
        }

        if (obj != null) {
            for (Progression<?> prog : this.progressionMap.values()) {
                try {
                    if (prog.matchesMethod(obj)) {
                        prog.progress(obj);
                    }
                }
                catch (Exception e) {
                    CobbleChallengeMod.logger.error("Error progressing challenge!");
                    e.printStackTrace();
                }
            }
        }

        //only play if this progression made it level up
        if (isCompleted()) {
            completedActiveChallenge();
        }
    }

    private boolean isCompleted() {
        for (Progression<?> prog : this.progressionMap.values()) {
            if (!prog.isCompleted()) {
                return false;
            }
        }
        return true;
    }

    public PlayerProfile getProfile() {
        return profile;
    }

    public ChallengeList getParentList() {
        return parentList;
    }

    public Challenge getActiveChallenge() {
        return activeChallenge;
    }

    public Map<String, Progression<?>> getProgressionMap() {
        return progressionMap;
    }

    public void timeRanOut() {
        profile.sendMessage(api.getMessage("challenges.failure.time-ran-out", "{challenge}", activeChallenge.getName()).build());
        profile.removeActiveChallenge(this);
    }

    public String getProgressListAsString() {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, Progression<?>> set : this.progressionMap.entrySet()) {
            String reqTitle = RequirementLoader.getTitleByName(set.getKey());
            reqTitle = (reqTitle == null) ? "unknown" : reqTitle;
            sb.append(api.getMessage("progression.progression-entry",
                    "{requirement-title}", reqTitle,
                    "{progression-string}", set.getValue().getProgressString()).getText()
            ).append("\n");
        }

        return sb.substring(0, sb.length() - "\n".length()); // substring out the final \n
    }
}
