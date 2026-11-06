package uti.bot;

import org.json.JSONArray;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

public class TeamManager {
    private final ArrayList<Team> teams = new ArrayList<>();

    private static TeamManager instance;

    private TeamManager(){
        //load the file (if it exsists)
        try {
            JSONArray teamFile = Util.loadJSONArray("teams.json");
            for(int i=0;i<teamFile.length();i++){
                teams.add(new Team(teamFile.getJSONObject(i)));
            }
        } catch (IOException e) {
            System.err.println("Unable to open teams file. Creating new file.");
            save();
        }

    }

    public static synchronized void init(){
        if(instance == null){
            instance = new TeamManager();
        }
    }

    public static TeamManager getInstance(){
        return instance;
    }

    public synchronized void save(){
        JSONArray userFile = new JSONArray();
        for (Team team : teams) {
            userFile.put(team.asJSON());
        }
        Util.saveJSONArray(userFile,"teams.json");
    }

    public int size(){
        return teams.size();
    }

    public Team get(int index){
        return teams.get(index);
    }

    public Team get(String name){
        Optional<Team> match = teams.stream().filter(team -> team.getName().equals(name)).findFirst();
        return match.orElse(null);
    }

    public void add(Team newTeam){
        teams.add(newTeam);
        save();
    }

    /**Completely clear team list. Deletes the team file as well
     */
    public void wipe(){
        teams.clear();
        boolean ignored = new File("teams.json").delete();
    }
}
