package uti.bot;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.json.JSONObject;

import java.util.ArrayList;

public class Team {

    private final long roleId;
    private final String name;

    private final long textChannelId;

    public Team(String name, long roleId, long textChannelId){
        this.name = name;
        this.roleId = roleId;
        this.textChannelId = textChannelId;
    }

    public Team(JSONObject json){
        name = json.getString("name");
        roleId = json.getLong("role");
        textChannelId = json.getLong("channel");
    }

    public JSONObject asJSON(){
        JSONObject json = new JSONObject();
        json.put("name",name);
        json.put("role",roleId);
        json.put("channel",textChannelId);
        return json;
    }

    public String getName() {
        return name;
    }

    public long getRoleId() {
        return roleId;
    }

    public Role getRole(){
        return Main.competitionServer.getRoleById(roleId);
    }

    public long getTextChannelId() {
        return textChannelId;
    }

    public MessageChannel getChannel(){
        return Main.competitionServer.getTextChannelById(textChannelId);
    }

    public int getNumberOfMembers(){
        UserManager um =UserManager.getInstance();
        int count = 0;
        for(int i=0;i<um.size();i++){
            if(equals(um.get(i).getTeam())){
                count++;
            }
        }
        return count;
    }

    public ArrayList<User> getMembers(){
        UserManager um =UserManager.getInstance();
        ArrayList<User> users = new ArrayList<>();
        for(int i=0;i<um.size();i++){
            if(equals(um.get(i).getTeam())){
                users.add(um.get(i));
            }
        }
        return users;
    }

    @Override
    public String toString() {
        return "Team{" +
                "roleId=" + roleId +
                ", name='" + name + '\'' +
                ", textChannelId=" + textChannelId +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Team team){
            return name.equals(team.name);
        }
        return false;
    }
}
