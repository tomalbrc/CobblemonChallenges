package com.github.kuramastone.cobblemonChallenges.guis;

import com.github.kuramastone.cobblemonChallenges.CobbleChallengeAPI;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.challenges.Challenge;
import com.github.kuramastone.cobblemonChallenges.challenges.ChallengeList;
import com.github.kuramastone.cobblemonChallenges.gui.GuiConfig;
import com.github.kuramastone.cobblemonChallenges.gui.SimpleWindow;
import com.github.kuramastone.cobblemonChallenges.gui.WindowItem;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;

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
            WindowItem item = new WindowItem(window, new ChallengeItem(profile, challenge));
            item.setAutoUpdate(20, () -> profile.isChallengeInProgress(challenge.getName()));
            item.setRunnableOnClick(onChallengeClick(challenge, item));
            contents.add(item);
        }

        window.setContents(contents);
    }

    private Runnable onChallengeClick(Challenge challenge, WindowItem item) {
        return () -> {
            if (!profile.isChallengeCompleted(challenge.getName()) && challenge.doesNeedSelection()) {
                profile.addActiveChallenge(challengeList, challenge);
                profile.checkCompletion(challengeList);
                window.notifyAllItems();
            }
        };
    }

    public void open() {
        window.show(profile.getPlayerEntity());
    }
}




















