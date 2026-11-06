package uti.bot;

import org.json.JSONArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

public class UserManager {

    private final ArrayList<User> users = new ArrayList<>();

    private static UserManager instance;

    private UserManager(){
        //load the file (if it exsists)
        try {
            JSONArray userFile = Util.loadJSONArray("users.json");
            for(int i=0;i<userFile.length();i++){
                users.add(new User(userFile.getJSONObject(i)));
            }
        } catch (IOException e) {
            System.err.println("Unable to open users file. Creating new file.");
            save();
        }

    }

    public static synchronized void init(){
        if(instance == null){
            instance = new UserManager();
        }
    }

    public static UserManager getInstance(){
        return instance;
    }

    public synchronized void save(){
        JSONArray userFile = new JSONArray();
        for (User user : users) {
            userFile.put(user.asJSON());
        }
        Util.saveJSONArray(userFile,"users.json");
    }

    public int size(){
        return users.size();
    }

    public User get(int index){
        return users.get(index);
    }

    public User get(long userId){
        Optional<User> match = users.stream().filter(user -> user.getUserID() == userId).findFirst();
        return match.orElse(null);
    }

    public void add(User newUser){
        users.add(newUser);
        save();
    }
}
