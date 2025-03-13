package com.github.kuramastone.cobblemonChallenges.player;

import com.github.kuramastone.cobblemonChallenges.challenges.requirements.MineBlockRequirement;
import com.github.kuramastone.cobblemonChallenges.challenges.requirements.PlaceBlockRequirement;
import com.github.kuramastone.cobblemonChallenges.events.ChallengeCompletedEvent;
import com.github.kuramastone.cobblemonChallenges.listeners.ChallengeListener;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeAPI;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.challenges.Challenge;
import com.github.kuramastone.cobblemonChallenges.challenges.ChallengeList;
import com.github.kuramastone.cobblemonChallenges.challenges.requirements.Progression;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Used to track a player's progress within a challenge
 */
public class ChallengeProgress {

    private CobbleChallengeAPI api;

    private PlayerProfile profile;
    private ChallengeList parentList;
    private Challenge activeChallenge;
    private List<Pair<String, Progression<?>>> progressionMap; // <requirement type name, RequirementProgress>
    private long startTime;

    public ChallengeProgress(CobbleChallengeAPI api, PlayerProfile profile, ChallengeList parentList, Challenge activeChallenge, List<Pair<String, Progression<?>>> progressionMap, long startTime) {
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
            for (Pair<String, Progression<?>> pair : this.progressionMap) {
                Progression<?> prog = pair.getRight();
                try {
                    if (prog.matchesMethod(obj)) {
                        prog.progress(obj);
                    }
                } catch (Exception e) {
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
        for (Pair<String, Progression<?>> pair : this.progressionMap) {
            Progression<?> prog = pair.getRight();
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

    public List<Pair<String, Progression<?>>> getProgressionMap() {
        return progressionMap;
    }

    public void timeRanOut() {
        profile.sendMessage(api.getMessage("challenges.failure.time-ran-out", "{challenge}", activeChallenge.getName()).build());
        profile.removeActiveChallenge(this);
    }

    public String getProgressListAsString() {
        StringBuilder sb = new StringBuilder();

        for (Pair<String, Progression<?>> set : this.progressionMap) {
            String reqTitle = api.getMessage("requirements.progression-shorthand.%s".formatted(set.getKey().toLowerCase())).getText();
            reqTitle = (reqTitle == null) ? set.getKey() : reqTitle;
            String blockData = "";

            if (set.getValue() instanceof MineBlockRequirement.MineBlockProgression mineBlockProgression)
                blockData = getPrettyBlockTypeOfFirst(mineBlockProgression.requirement.blockType) + " ";
            else if (set.getValue() instanceof PlaceBlockRequirement.PlaceBlockProgression placeBlockProgression)
                blockData = getPrettyBlockTypeOfFirst(placeBlockProgression.requirement.blockType) + " ";

            sb.append(api.getMessage("progression.progression-entry",
                    "block_data?", blockData,
                    "{requirement-title}", reqTitle,
                    "{progression-string}", set.getValue().getProgressString()).getText()
            ).append("\n");
        }

        String result = sb.substring(0, sb.length() - "\n".length());
        return result; // substring out the final \n
    }

    private String getPrettyBlockTypeOfFirst(String blockIdentifierGroup) {
        String[] blockIdentifierArray = blockIdentifierGroup.split("/");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < blockIdentifierArray.length; i++) {
            String blockIdentifier = blockIdentifierArray[i];


            try {
                Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockIdentifier));
                builder.append(block.getName().getString());
            } catch (Exception e) {
                builder.append(blockIdentifier);
            }

            break;
        }

        return builder.toString();
    }
}
