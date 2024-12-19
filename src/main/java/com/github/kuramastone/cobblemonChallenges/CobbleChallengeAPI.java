package com.github.kuramastone.cobblemonChallenges;

import com.github.kuramastone.bUtilities.ComponentEditor;
import com.github.kuramastone.bUtilities.SimpleAPI;
import com.github.kuramastone.bUtilities.yaml.YamlConfig;
import com.github.kuramastone.cobblemonChallenges.challenges.Challenge;
import com.github.kuramastone.cobblemonChallenges.challenges.ChallengeList;
import com.github.kuramastone.cobblemonChallenges.challenges.requirements.Progression;
import com.github.kuramastone.cobblemonChallenges.player.ChallengeProgress;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import com.github.kuramastone.cobblemonChallenges.utils.ConfigOptions;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.util.*;

public class CobbleChallengeAPI implements SimpleAPI {

    private Map<String, Challenge> allChallengesByName;
    private Map<String, ChallengeList> challengeListMap;
    private ConfigOptions configOptions;

    private Map<UUID, PlayerProfile> profileMap;

    public CobbleChallengeAPI() {
        profileMap = new HashMap<>();
        allChallengesByName = new HashMap<>();
    }

    public void init() {
        loadConfigs();
        loadProfiles();
    }

    public void loadProfiles() {
        YamlConfig data = new YamlConfig(FabricLoader.getInstance().getConfigDir(), "player-data.yml");

        for (String strUUID : data.getKeys("", false)) {
            UUID uuid = UUID.fromString(strUUID);
            PlayerProfile profile = getOrCreateProfile(uuid);

            YamlConfig section = data.getSection(strUUID);
            List<String> completedChallenges = section.get("completed-list", new ArrayList<>());
            profile.setCompletedChallenges(completedChallenges);

            if (section.containsKey("progression")) {
                YamlConfig progressionSection = section.getSection("progression");
                // iterate over each challenge list
                for (String strList : progressionSection.getKeys("", false)) {
                    YamlConfig listSection = progressionSection.getSection(strList);
                    ChallengeList list = getChallengeList(strList);

                    // iterate over each challenge
                    for (String strChallenge : listSection.getKeys("", false)) {
                        Challenge challenge = list.getChallenge(strChallenge);
                        ChallengeProgress progress = list.buildNewProgressForQuest(challenge, profile);
                        YamlConfig challengeSection = listSection.getSection(strChallenge);

                        // iterate over each requirement for challenge
                        int index = 0;
                        for (Map.Entry<String, Progression<?>> progSet : progress.getProgressionMap().entrySet()) {
                            YamlConfig progSection = challengeSection.getSection(index++ + "." + progSet.getKey());
                            progSet.getValue().loadFrom(uuid, progSection);
                        }

                        profile.addActiveChallenge(progress);
                    }
                }
            }

        }

    }

    public void saveProfiles() {
        YamlConfig data = new YamlConfig(CobbleChallengeMod.defaultDataFolder(), "player-data.yml");
        data.clear();

        for (PlayerProfile profile : getProfiles()) {
            YamlConfig profileEntry = data.getOrCreateSection(profile.getUUID().toString());
            profileEntry.set("completed-list", profile.getCompletedChallenges());
            for (Map.Entry<String, List<ChallengeProgress>> set : profile.getActiveChallengesMap().entrySet()) {
                for (ChallengeProgress cp : set.getValue()) {
                    int index = 0;
                    for (Map.Entry<String, Progression<?>> progSet : cp.getProgressionMap().entrySet()) {
                        YamlConfig progSection = profileEntry.getOrCreateSection("progression.%s.%s.%s.%s".formatted(set.getKey(), cp.getActiveChallenge().getName(), index++, progSet.getKey()));
                        progSet.getValue().writeTo(progSection);
                    }
                }
            }
        }

        data.save();
    }

    public void loadConfigs() {
        loadConfigOptions(); // load messages
        RequirementLoader.init(); // use messages for requirements loader
        loadChallenges(); // load challenges using the customized requirements
    }

    private void loadConfigOptions() {
        YamlConfig config = new YamlConfig(CobbleChallengeMod.defaultDataFolder(), null, null, "config.yml", getClass());
        configOptions = new ConfigOptions(config);
    }

    private void loadChallenges() {
        File parentFolder = new File(CobbleChallengeMod.defaultDataFolder(), "challenges");
        if (parentFolder.listFiles() == null || parentFolder.listFiles().length == 0) {
            parentFolder.delete();
        }

        challengeListMap = new HashMap<>();
        allChallengesByName = new HashMap<>();

        for (File yamlFile : YamlConfig.getYamlFiles(parentFolder)) {
            YamlConfig config = new YamlConfig(yamlFile.getParentFile(), null, null, yamlFile.getName(), getClass());

            // remove ending
            String name = yamlFile.getName();
            if (name.contains(".")) {
                name = name.substring(0, name.lastIndexOf('.')); // remove extension if possible
            }

            if (challengeListMap.containsKey(name)) {
                CobbleChallengeMod.logger.error(String.format("Unable to load duplicate ChallengeList '%s'. Try giving it a unique name.", name));
                continue;
            }

            ChallengeList cl = ChallengeList.load(this, name, config.getSection("challenge-list"));
            challengeListMap.put(name, cl);
            CobbleChallengeMod.logger.info(String.format("Loading %s config with %s challenges...", name, cl.getChallengeMap().size()));
        }
    }

    /**
     * @param challenge
     * @return Returns false if a challenge is already registered under that name
     */
    public boolean registerChallenge(Challenge challenge) {
        if (this.allChallengesByName.containsKey(challenge.getName())) {
            return false;
        }

        this.allChallengesByName.put(challenge.getName(), challenge);
        return true;
    }

    public ConfigOptions getConfigOptions() {
        return configOptions;
    }

    public ChallengeList getChallengeList(String id) {
        return this.challengeListMap.get(id);
    }

    public CobbleChallengeMod getMod() {
        return CobbleChallengeMod.instance;
    }

    public PlayerProfile getOrCreateProfile(UUID uuid) {
        PlayerProfile pp = profileMap.computeIfAbsent(uuid, id -> new PlayerProfile(this, id));
        pp.addUnrestrictedChallenges(); // add challenges that dont require selection
        return pp;
    }

    @Override
    public ComponentEditor getMessage(String key, Object... replacements) {
        ComponentEditor edit = configOptions.messages.getOrDefault(key, new ComponentEditor(key)).copy();

        // used to prevent infinite loops
        if (!key.equals("prefix")) {
            edit = edit.replace("{prefix}", getMessage("prefix").getText());
        }

        if (replacements.length % 2 != 0) {
            throw new IllegalArgumentException("Key was not provided with a replacement");
        }

        if (replacements.length > 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                edit = edit.replace(replacements[i].toString(), replacements[i + 1].toString());
            }
        }

        return edit;
    }

    public Collection<ChallengeList> getChallengeLists() {
        return new HashSet<>(challengeListMap.values());
    }

    public void reloadConfig() {
        loadConfigOptions();
        loadChallenges();
        for (PlayerProfile profile : getProfiles()) {
            profile.addUnrestrictedChallenges();
        }
    }

    public List<PlayerProfile> getProfiles() {
        return new ArrayList<>(profileMap.values());
    }
}
