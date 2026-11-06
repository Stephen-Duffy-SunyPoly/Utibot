package uti.bot;

import org.json.JSONObject;

public class User {
    private final long userID;
    private final String name;
    private final String email;

    //TODO team

    public User(long userID, String name, String email){
        this.userID = userID;
        this.name = name;
        this.email = email;
    }

    public User(JSONObject json){
        userID = json.getLong("user-id");
        name = json.getString("name");
        email = json.getString("email");
    }

    JSONObject asJSON(){
        JSONObject json = new JSONObject();
        json.put("user-id",userID);
        json.put("name",name);
        json.put("email",email);

        return json;
    }

    @Override
    public String toString() {
        return "name: ["+name+"] email: "+email+" userId: "+userID;
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
}
