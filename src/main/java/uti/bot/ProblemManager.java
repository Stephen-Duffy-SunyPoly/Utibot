package uti.bot;

import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.json.JSONArray;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProblemManager {
    private final ArrayList<Problem> problems = new ArrayList<>();

    private static ProblemManager instance;

    private ProblemManager(){
        //load the file (if it exsists)
        try {
            JSONArray teamFile = Util.loadJSONArray("problems.json");
            for(int i=0;i<teamFile.length();i++){
                problems.add(new Problem(teamFile.getJSONObject(i)));
            }
        } catch (IOException e) {
            System.err.println("Unable to open problems file. Creating new file.");
            save();
        }

    }

    public static synchronized void init(){
        if(instance == null){
            instance = new ProblemManager();
        }
    }

    public static ProblemManager getInstance(){
        return instance;
    }

    public synchronized void save(){
        JSONArray userFile = new JSONArray();
        for (Problem team : problems) {
            userFile.put(team.asJSON());
        }
        Util.saveJSONArray(userFile,"problems.json");
    }

    public int size(){
        return problems.size();
    }

    public Problem get(int index){
        return problems.get(index);
    }

    public Problem get(String name){
        Optional<Problem> match = problems.stream().filter(team -> team.getName().equals(name)).findFirst();
        return match.orElse(null);
    }

    public void add(Problem newProblem){
        problems.add(newProblem);
        save();
        for (int j =0;j<TeamManager.getInstance().size();j++){
            TeamManager.getInstance().get(j).addNewScore(newProblem.isHigherIsBetter()?0:Integer.MAX_VALUE);
        }
        TeamManager.getInstance().save();
        //rebuild the containers
        if(!newProblem.isSubjective()){
            //have the images regenerated
            new Thread(() -> ContainerManager.getInstance().requestRegenerateImages()).start();
        }
    }

    public void deleteProblem(Problem problem){
        //get Problem id
        int i = getProblemIndex(problem);

        problems.remove(problem);

        for (int j =0;j<TeamManager.getInstance().size();j++){
            TeamManager.getInstance().get(j).removeScore(i);
        }
        TeamManager.getInstance().save();

        //delete the problem folder
        Downloader.recursiveDelete(new File("problems/"+problem.getName()+"/"));
        //delete the channel if there is one
        MessageChannel chanel = problem.getScoringChannel();
        if(chanel!=null){
            chanel.delete().queue();
        }

        if(!problem.isSubjective()){
            //have the images regenerated
            new Thread(() -> ContainerManager.getInstance().requestRegenerateImages()).start();
        }

        save();
    }

    public void updateProblemRanking(Problem p){
        //get Problem id
        int i = getProblemIndex(p);

        int extremeScore = p.isHigherIsBetter()? 0:Integer.MAX_VALUE;
        int otherExtreme = p.isHigherIsBetter()?Integer.MAX_VALUE:0;

        for (int j =0;j<TeamManager.getInstance().size();j++){
            int score = TeamManager.getInstance().get(j).getScore(i);
            if(score == otherExtreme){
                TeamManager.getInstance().get(j).setScore(i,extremeScore);
            }
        }
        TeamManager.getInstance().save();
    }

    public int getProblemIndex(Problem p) {
        int i;
        for(i=0;i< problems.size();i++){
            if(p.equals(problems.get(i))){
                break;
            }
        }
        return i;
    }

    public List<String> getNameList(){
        return problems.stream().map(Problem::getName).toList();
    }

    /**Completely clear problem list. Deletes the problem file as well
     */
    public void wipe(){
        problems.clear();
        boolean ignored = new File("problems.json").delete();
        Downloader.recursiveDelete(new File("problems/"));
    }
}
