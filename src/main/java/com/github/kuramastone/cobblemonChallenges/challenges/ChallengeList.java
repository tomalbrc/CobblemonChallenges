package com.github.kuramastone.cobblemonChallenges.challenges;

import com.github.kuramastone.bUtilities.yaml.YamlConfig;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeAPI;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.challenges.requirements.Progression;
import com.github.kuramastone.cobblemonChallenges.challenges.requirements.Requirement;
import com.github.kuramastone.cobblemonChallenges.player.ChallengeProgress;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class ChallengeList {

    private CobbleChallengeAPI api;

    private String name;
    private List<Challenge> challengeMap;
    private int maxChallengesPerPlayer;

    public ChallengeList(CobbleChallengeAPI api, String name, List<Challenge> challengeMap, int maxChallengesPerPlayer) {
        this.api = api;
        this.name = name;
        this.challengeMap = challengeMap;
        this.maxChallengesPerPlayer = maxChallengesPerPlayer;
    }

    public static ChallengeList load(CobbleChallengeAPI api, String challengeListID, YamlConfig section) {
        Objects.requireNonNull(section, "Cannot load data from a null section.");

        List<Challenge> challengeList = new ArrayList<>();
        for (String challengeID : section.getKeys("challenges", false)) {
            Challenge challenge = Challenge.load(challengeID, section.getSection("challenges." + challengeID));
            if(challenge == null) continue; // something went wrong, skip
            boolean valid = api.registerChallenge(challenge);
            if (valid) {
                challengeList.add(challenge);
            }
            else {
                CobbleChallengeMod.logger.error("Unable to load duplicate Challenge name '{}'. Try renaming it! Ignoring this challenge.", challengeID);
            }
        }
        int maxChallengesPerPlayer = section.get("maxChallengesPerPlayer", 1);

        return new ChallengeList(api, challengeListID, challengeList, maxChallengesPerPlayer);
    }

    public String getName() {
        return name;
    }

    public List<Challenge> getChallengeMap() {
        return challengeMap;
    }

    public int getMaxChallengesPerPlayer() {
        return maxChallengesPerPlayer;
    }

    public Challenge getChallengeAt(Integer level) {
        return challengeMap.get(level);
    }

    /**
     * Create a progression for this challenge. This can be used to keep track of a player's progress
     *
     * @param challenge
     * @param profile
     * @return
     */
    public ChallengeProgress buildNewProgressForQuest(Challenge challenge, PlayerProfile profile) {

        List<Pair<String, Progression<?>>> progs = new ArrayList<>();
        for (Requirement requirement : challenge.getRequirements()) {
            progs.add(Pair.of(requirement.getName(), requirement.buildProgression(profile)));
        }

        return new ChallengeProgress(api, profile, this, challenge, progs, System.currentTimeMillis());
    }

    public Challenge getChallenge(String challengeName) {
        for(Challenge challenge : this.challengeMap) {
            if (challenge.getName().equals(challengeName)) {
                return challenge;
            }
        }

        return null;
    }
}
