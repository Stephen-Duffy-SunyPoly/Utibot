package uti.bot;

import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.json.JSONObject;

public class Problem {
    private String name;

    private boolean higherIsBetter;

    private final boolean subjective;

    private String infoFileLink;

    private String judgePythonProgramPath;

    private final long subjectiveScoringChannelId;

    public Problem(String name, boolean higherIsBetter, boolean subjective, String infoFileLink, String judgePythonProgramPath, long subjectiveScoringChannelId){
        this.name = name;
        this.higherIsBetter = higherIsBetter;
        this.subjective = subjective;
        this.infoFileLink = infoFileLink;
        this.judgePythonProgramPath = judgePythonProgramPath;
        this.subjectiveScoringChannelId = subjectiveScoringChannelId;
        if(subjective && subjectiveScoringChannelId == 0){
            throw new RuntimeException("Subjective problems must have an associated channel");
        }
    }

    public Problem(JSONObject json){
        name = json.getString("name");
        higherIsBetter = json.getBoolean("higher-is-better");
        subjective = json.getBoolean("subjective");
        infoFileLink = json.getString("info-file-link");
        judgePythonProgramPath = json.getString("judge-program-path");
        subjectiveScoringChannelId = json.getLong("subjective-channel");
    }

    public JSONObject asJSON(){
        JSONObject json = new JSONObject();
        json.put("name",name);
        json.put("higher-is-better",higherIsBetter);
        json.put("subjective",subjective);
        json.put("info-file-link",infoFileLink);
        json.put("judge-program-path",judgePythonProgramPath);
        json.put("subjective-channel",subjectiveScoringChannelId);
        return json;
    }

    @Override
    public String toString() {
        return "Problem{" +
                "subjective=" + subjective +
                ", higherIsBetter=" + higherIsBetter +
                ", name='" + name + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isHigherIsBetter() {
        return higherIsBetter;
    }

    public void setHigherIsBetter(boolean higherIsBetter) {
        this.higherIsBetter = higherIsBetter;
    }

    public String getInfoFileLink() {
        return infoFileLink;
    }

    public void setInfoFileLink(String infoFileLink) {
        this.infoFileLink = infoFileLink;
    }

    public String getJudgePythonProgramPath() {
        return judgePythonProgramPath;
    }

    public void setJudgePythonProgramPath(String judgePythonProgramPath) {
        this.judgePythonProgramPath = judgePythonProgramPath;
    }

    public boolean isSubjective() {
        return subjective;
    }

    public long getSubjectiveScoringChannelId() {
        return subjectiveScoringChannelId;
    }

    public MessageChannel getScoringChannel(){
        if(subjectiveScoringChannelId == 0){
            return null;
        }
        return Main.competitionServer.getTextChannelById(subjectiveScoringChannelId);
    }
}
