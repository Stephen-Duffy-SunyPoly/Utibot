package uti.bot;

import org.json.JSONObject;

import java.io.IOException;

public class ConfigManager {

    private final long serverId;

    private boolean serverSetup = false;
    private long registeredRoleId;
    private long registerChannelId;
    private long registerChannelMessageId;

    private long teamCategoryId;
    private long judgeCategoryId;
    private long adminChannelId;
    private long infoChannelId;
    private long problemsChannelId;
    private long feedChannelId;

    private String registerChannelMessage = "# Register Message!\nSet the content of this message in the config file";

    private boolean compStarted;
    private boolean compEnded;

    private boolean useSingleSubjectiveMessage = true;

    public ConfigManager(){
        boolean saveFile = false;
        try {
            JSONObject configJson = Util.loadJSONObject("config.json");
            if(configJson.has("server-id")){
                serverId = configJson.getLong("server-id");
            } else {
                throw new RuntimeException("A valid server id was not provided in the config file!");
            }
            if(serverId == 0){
                throw new RuntimeException("A valid server id was not provided in the config file!");
            }
            if(configJson.has("server-setup")){
                serverSetup = configJson.getBoolean("server-setup");
            } else {
                saveFile = true;
            }

            if(configJson.has("registered-role")){
                registeredRoleId = configJson.getLong("registered-role");
            } else {
                saveFile = true;
            }

            if(configJson.has("register-channel")){
                registerChannelId = configJson.getLong("register-channel");
            } else {
                saveFile = true;
            }

            if(configJson.has("register-message")){
                registerChannelMessage = configJson.getString("register-message");
            } else {
                saveFile = true;
            }

            if(configJson.has("register-message-id")){
                registerChannelMessageId = configJson.getLong("register-message-id");
            } else {
                saveFile = true;
            }

            if(configJson.has("team-category")){
                teamCategoryId = configJson.getLong("team-category");
            } else {
                saveFile = true;
            }
            if(configJson.has("judge-category")){
                judgeCategoryId = configJson.getLong("judge-category");
            } else {
                saveFile = true;
            }
            if(configJson.has("admin-channel")){
                adminChannelId = configJson.getLong("admin-channel");
            } else {
                saveFile = true;
            }
            if(configJson.has("comp-started")){
                compStarted = configJson.getBoolean("comp-started");
            } else {
                saveFile = true;
            }
            if(configJson.has("comp-ended")){
                compEnded = configJson.getBoolean("comp-ended");
            } else {
                saveFile = true;
            }
            if(configJson.has("info-channel")){
                infoChannelId = configJson.getLong("info-channel");
            } else {
                saveFile = true;
            }
            if(configJson.has("problem-channel")){
                problemsChannelId = configJson.getLong("problem-channel");
            } else {
                saveFile = true;
            }
            if(configJson.has("feed-channel")){
                feedChannelId = configJson.getLong("feed-channel");
            } else {
                saveFile = true;
            }
            if(configJson.has("single-subjective-message")){
                useSingleSubjectiveMessage = configJson.getBoolean("single-subjective-message");
            } else {
                saveFile = true;
            }

        } catch (IOException e) {
            System.err.println("Failed to load config file, generating new config.");
            save();
            throw new RuntimeException("Failed to load config file",e);
        }
        if(saveFile){
            save();
        }
    }

    public synchronized void save(){
        JSONObject configJson = new JSONObject();
        configJson.put("server-id",serverId);
        configJson.put("server-setup",serverSetup);
        configJson.put("registered-role",registeredRoleId);
        configJson.put("register-channel",registerChannelId);
        configJson.put("register-message",registerChannelMessage);
        configJson.put("register-message-id",registerChannelMessageId);
        configJson.put("team-category",teamCategoryId);
        configJson.put("judge-category",judgeCategoryId);
        configJson.put("admin-channel",adminChannelId);
        configJson.put("comp-started",compStarted);
        configJson.put("comp-ended",compEnded);
        configJson.put("info-channel",infoChannelId);
        configJson.put("problem-channel",problemsChannelId);
        configJson.put("feed-channel",feedChannelId);
        configJson.put("single-subjective-message",useSingleSubjectiveMessage);

        Util.saveJSONObject(configJson,"config.json");
    }

    public long getServerId(){
        return serverId;
    }

    public boolean isServerSetup(){
        return serverSetup;
    }

    public void setServerSetup(boolean serverSetup) {
        this.serverSetup = serverSetup;
        save();
    }

    public long getRegisteredRoleId() {
        return registeredRoleId;
    }

    public void setRegisteredRoleId(long registeredRoleId) {
        this.registeredRoleId = registeredRoleId;
        save();
    }

    public long getRegisterChannelId() {
        return registerChannelId;
    }

    public void setRegisterChannelId(long registerChannelId) {
        this.registerChannelId = registerChannelId;
    }

    public String getRegisterChannelMessage() {
        return registerChannelMessage;
    }

    public long getRegisterChannelMessageId() {
        return registerChannelMessageId;
    }

    public void setRegisterChannelMessageId(long registerChannelMessageId) {
        this.registerChannelMessageId = registerChannelMessageId;
    }

    public long getAdminChannelId() {
        return adminChannelId;
    }

    public long getJudgeCategoryId() {
        return judgeCategoryId;
    }

    public long getTeamCategoryId() {
        return teamCategoryId;
    }

    public void setAdminChannelId(long adminChannelId) {
        this.adminChannelId = adminChannelId;
        save();
    }

    public void setJudgeCategoryId(long judgeCategoryId) {
        this.judgeCategoryId = judgeCategoryId;
        save();
    }

    public void setTeamCategoryId(long teamCategoryId) {
        this.teamCategoryId = teamCategoryId;
        save();
    }

    public boolean hasCompStarted() {
        return compStarted;
    }

    public void setCompStarted(boolean compStarted) {
        this.compStarted = compStarted;
        save();
    }

    public boolean hasCompEnded() {
        return compEnded;
    }

    public void setCompEnded(boolean compEnded) {
        this.compEnded = compEnded;
        save();
    }

    public long getInfoChannelId() {
        return infoChannelId;
    }

    public void setInfoChannelId(long infoChannelId) {
        this.infoChannelId = infoChannelId;
        save();
    }

    public long getProblemsChannelId() {
        return problemsChannelId;
    }

    public void setProblemsChannelId(long problemsChannelId) {
        this.problemsChannelId = problemsChannelId;
        save();
    }

    public long getFeedChannelId() {
        return feedChannelId;
    }

    public void setFeedChannelId(long feedChannelId) {
        this.feedChannelId = feedChannelId;
        save();
    }

    public boolean shouldUseSingleSubjectiveMessage() {
        return useSingleSubjectiveMessage;
    }
}
