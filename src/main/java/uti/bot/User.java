package uti.bot;

import org.json.JSONObject;

public class User {
    private final long userID;
    private final String name;
    private final String email;

    private Team team;

    public User(long userID, String name, String email){
        this.userID = userID;
        this.name = name;
        this.email = email;
    }

    public User(JSONObject json){
        userID = json.getLong("user-id");
        name = json.getString("name");
        email = json.getString("email");
        if(json.has("team")) {
            String teamName = json.getString("team");
            if (teamName != null) {
                team = TeamManager.getInstance().get(teamName);
            }
        }
    }

    JSONObject asJSON(){
        JSONObject json = new JSONObject();
        json.put("user-id",userID);
        json.put("name",name);
        json.put("email",email);
        if(team!=null){
            json.put("team",team.getName());
        }
        return json;
    }

    @Override
    public String toString() {
        return "User{" +
                "userID=" + userID +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof User other){
            return userID == other.userID;
        }
        return false;
    }

    public long getUserID() {
        return userID;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public Team getTeam(){
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
        UserManager.getInstance().save();
    }
}
