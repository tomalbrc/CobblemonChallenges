package com.github.kuramastone.cobblemonChallenges.guis;

import com.github.kuramastone.cobblemonChallenges.CobbleChallengeAPI;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.challenges.Challenge;
import com.github.kuramastone.cobblemonChallenges.challenges.ChallengeList;
import com.github.kuramastone.cobblemonChallenges.challenges.requirements.MilestoneTimePlayedRequirement;
import com.github.kuramastone.cobblemonChallenges.gui.GuiConfig;
import com.github.kuramastone.cobblemonChallenges.gui.ItemProvider;
import com.github.kuramastone.cobblemonChallenges.gui.SimpleWindow;
import com.github.kuramastone.cobblemonChallenges.gui.WindowItem;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import com.github.kuramastone.cobblemonChallenges.utils.PermissionUtils;

import java.util.ArrayList;
import java.util.List;

public class ChallengeListGUI {

    private final CobbleChallengeAPI api;
    private final PlayerProfile profile;
    private final ChallengeList challengeList;

    private final SimpleWindow window;

    public ChallengeListGUI(CobbleChallengeAPI api, PlayerProfile profile, ChallengeList challengeList, GuiConfig config) {
        this.api = api;
        this.profile = profile;
        this.challengeList = challengeList;
        window = new SimpleWindow(config);
        build();
    }

    private void build() {
        List<WindowItem> contents = new ArrayList<>();

        //window is already built aesthetically, but now we need to insert each challenge
        for (Challenge challenge : challengeList.getChallengeMap()) {
            var perm = challenge.getPermission();
            if (perm != null && !PermissionUtils.hasPermission(profile.getPlayerEntity(), perm)) {
                WindowItem item = new WindowItem(window, new ItemProvider.ItemWrapper(CobbleChallengeMod.instance.getAPI().getConfigOptions().getNoPermChallengeItem()));
                contents.add(item);
                continue;
            }

            WindowItem item = new WindowItem(window, new ChallengeItem(window, profile, challenge));
            if (challenge.doesNeedSelection() && profile.isChallengeInProgress(challenge.getName()))
                item.setAutoUpdate(15, () ->
                        // check if this challenge requirement should auto-update
                        challenge.getRequirements().stream().anyMatch(it -> it instanceof MilestoneTimePlayedRequirement)
                                // check if challenge has a timer that needs ticking
                                || profile.isChallengeInProgress(challenge.getName())
                );
            item.setRunnableOnClick(onChallengeClick(challenge, item));
            contents.add(item);
        }

        window.setContents(contents);
    }

    private Runnable onChallengeClick(Challenge challenge, WindowItem item) {
        return () -> {
            if (!profile.isChallengeInProgress(challenge.getName()) && !profile.isChallengeCompleted(challenge.getName()) && challenge.doesNeedSelection()) {
                profile.addActiveChallenge(challengeList, challenge);
                profile.checkCompletion(challengeList);
                item.setAutoUpdate(10, () -> true); // set to auto update to allow timer to keep updating
                item.notifyWindow();
            }
        };
    }

    public void open() {
        window.show(profile.getPlayerEntity());
    }
}




















