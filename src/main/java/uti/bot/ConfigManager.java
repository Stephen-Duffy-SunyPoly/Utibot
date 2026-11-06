package uti.bot;

import org.json.JSONObject;

import java.io.IOException;

public class ConfigManager {

    private final long serverId;

    private boolean serverSetup = false;
    private long registeredRoleId;
    private long registerChannelId;
    private long registerChannelMessageId;

    private String registerChannelMessage = "# Register Message!\nSet the content of this message in the config file";

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
}
