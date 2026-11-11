package uti.bot;

import com.google.errorprone.annotations.CheckReturnValue;
import org.json.JSONArray;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

    @CheckReturnValue
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

    public void deleteTeam(Team team){
        teams.remove(team);
        team.getChannel().delete().queue();
        team.getRole().delete().queue();
        save();
    }

    public boolean isScoreBestForProblem(Problem problem, int score){
        int problemIndex = ProblemManager.getInstance().getProblemIndex(problem);
        //if you find a single score that is the same or better, then false
        if(problem.isHigherIsBetter()){
            for(Team t:teams){
                if(score <= t.getScore(problemIndex)){
                    return false;
                }
            }
        } else {
            for(Team t:teams){
                if(score >= t.getScore(problemIndex)){
                    return false;
                }
            }
        }
        return true;
    }

    public List<TeamMapping> getRankOfTeams(){
        ProblemManager pr = ProblemManager.getInstance();
        int numberOfProblems = pr.size();
        List<Double> teamRanks = teams.stream().map(team -> {
            //get the ranks of each problem for each team
            ArrayList<Integer> probRanks = new ArrayList<>();
            for(int i=0;i<numberOfProblems;i++){
                probRanks.add(team.getRankForProblem(i,pr.get(i)));
            }
            return probRanks;
        }).map(ranks -> {
            //get their average ranks
            int sum = ranks.stream().reduce(0, Integer::sum);
            return sum / (double)ranks.size();
        }).toList();
        ArrayList<TeamMapping> rakeMaps = new ArrayList<>();
        for(int i=0;i<teamRanks.size();i++){
            rakeMaps.add(new TeamMapping(i,teamRanks.get(i)));
        }
        rakeMaps.sort(TeamMapping::compareTo);
        return rakeMaps;
    }

    /**Completely clear team list. Deletes the team file as well
     */
    public void wipe(){
        teams.clear();
        boolean ignored = new File("teams.json").delete();
    }

    public record TeamMapping (int teamIndex, double rank)implements Comparable<TeamMapping>{
        /**
         * Compares this object with the specified object for order.  Returns a
         * negative integer, zero, or a positive integer as this object is less
         * than, equal to, or greater than the specified object.
         *
         */
        @Override
        public int compareTo(TeamManager.@NonNull TeamMapping o) {
            if( rank < o.rank){
                return -1;
            } else if (rank > o.rank) {
                return 1;
            }
            return 0;
        }
    }
}
