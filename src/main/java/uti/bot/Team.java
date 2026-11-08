package uti.bot;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class Team {

    private final long roleId;
    private final String name;

    private final long textChannelId;

    private final ArrayList<Integer> scores = new ArrayList<>();

    public Team(String name, long roleId, long textChannelId){
        this.name = name;
        this.roleId = roleId;
        this.textChannelId = textChannelId;
        for(int i=0;i<ProblemManager.getInstance().size();i++){
            Problem p = ProblemManager.getInstance().get(i);
            if(p.isHigherIsBetter()){
                scores.add(0);
            } else {
                scores.add(Integer.MAX_VALUE);
            }
        }
    }

    public Team(JSONObject json){
        name = json.getString("name");
        roleId = json.getLong("role");
        textChannelId = json.getLong("channel");
        JSONArray scores = json.getJSONArray("scores");
        for(int i=0;i< scores.length();i++){
            this.scores.add(scores.getInt(i));
        }
    }

    public JSONObject asJSON(){
        JSONObject json = new JSONObject();
        json.put("name",name);
        json.put("role",roleId);
        json.put("channel",textChannelId);
        JSONArray scoresOut = new JSONArray();
        for (Integer score : scores) {
            scoresOut.put(score);
        }
        json.put("scores",scoresOut);
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

    public String getProblemRank(int problemId) {
        ProblemManager pr = ProblemManager.getInstance();
        Problem problem = pr.get(problemId);
        String result = problem.getName() + "  |  "+scores.get(problemId);
        int betterThanMe = getRankForProblem(problemId, problem);
        result += "  |  " + (betterThanMe + 1);

        return result;
    }

    public int getRankForProblem(int problemId, Problem problem) {
        int myScore = scores.get(problemId);
        int betterThanMe = 0;
        for(int i=0;i<TeamManager.getInstance().size();i++){
            if(TeamManager.getInstance().get(i).getScore(problemId) > myScore == problem.isHigherIsBetter()){
                betterThanMe ++;
            }
        }
        return betterThanMe;
    }

    public void removeScore(int index){
        scores.remove(index);
    }

    public void setScore(int index,int score){
        scores.set(index,score);
    }

    public int getScore(int index){
        return scores.get(index);
    }

    public void addNewScore(int value){
        scores.add(value);
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
