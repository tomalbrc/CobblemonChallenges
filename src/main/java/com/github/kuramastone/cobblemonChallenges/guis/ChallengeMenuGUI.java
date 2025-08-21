package com.github.kuramastone.cobblemonChallenges.guis;

import com.github.kuramastone.bUtilities.yaml.YamlConfig;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeAPI;
import com.github.kuramastone.cobblemonChallenges.challenges.ChallengeList;
import com.github.kuramastone.cobblemonChallenges.gui.SimpleWindow;
import com.github.kuramastone.cobblemonChallenges.gui.WindowItem;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import net.minecraft.world.inventory.ClickType;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.BiConsumer;

public class ChallengeMenuGUI {

    private CobbleChallengeAPI api;

    private PlayerProfile profile;
    private SimpleWindow window;

    public ChallengeMenuGUI(CobbleChallengeAPI api, PlayerProfile profile) {
        this.api = api;
        this.profile = profile;
        build();
    }

    private void build() {
        window = new SimpleWindow(api.getConfigOptions().getMenuGuiConfig());

        //window is already built aesthetically, but we need to attach commands to certain characters
        for (Map.Entry<Character, WindowItem> set : new ArrayList<>(window.getGuiConfig().getIngredients().entrySet())) {
            char c = set.getKey();
            WindowItem item = set.getValue();

            YamlConfig data = window.getGuiConfig().getDataForIngredient(c);
            if (data == null) {
                continue;
            }

            String linked_to_challenge = data.get("linked_to", "");

            ChallengeList linkedTo = api.getChallengeList(linked_to_challenge);
            if (linkedTo == null) {
                continue;
            }

            item.setRunnableOnClick(onChallengeItemClick(linkedTo));
        }
    }

    private BiConsumer<ClickType, Integer> onChallengeItemClick(ChallengeList linkedTo) {
        return (type, dragType) -> {
            //PixelChallengeMod.logger.warn(String.format("Opening challenges for list %s", linkedTo.getName()));
            if (type == ClickType.PICKUP && dragType == 0) new ChallengeListGUI(api, profile, linkedTo, api.getConfigOptions().getChallengeGuiConfig(linkedTo.getName())).open();
        };
    }

    public void open() {
        window.show(profile.getPlayerEntity());
    }
}




















