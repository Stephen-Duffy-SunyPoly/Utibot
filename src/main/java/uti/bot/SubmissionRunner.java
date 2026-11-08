package uti.bot;

import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class SubmissionRunner implements Runnable {

    static int submissionGlobalId = 0;

    static synchronized int getNextId(){
        int t = submissionGlobalId;
        submissionGlobalId++;
        return t;
    }

    private final int id;

    private final String dockerImage;

    private String command;

    HashMap<String,ContainerBindFile> volumes = new HashMap<>();

    private final Problem problem;
    private final Team team;
    private final ModalInteractionEvent event;

    private final File scoreFile;
    private final File outputFile;

    public SubmissionRunner(ModalInteractionEvent event, Problem problem, Team team, Message.Attachment attachment) throws IOException {
        Language submissionLanguage = Language.fromFileExtension(attachment.getFileExtension());
        if(submissionLanguage == null){
            event.reply("Invalid language").complete();
            throw new RuntimeException("Invalid language");
        }
        id = getNextId();
        //create io folders
        File inputFolder = new File("submissions/"+id+"/input/");
        File outputFolder = new File("submissions/"+id+"/output/");
        boolean ignored = inputFolder.mkdirs();
        boolean ignored2 = outputFolder.mkdirs();

        //copy problem files to input
        //maybe this is not necessary
        String userFilePath = new File(inputFolder, attachment.getFileName()).getPath().replace("\\", "/");
        byte[] userFile = Downloader.downloadContent(attachment.getUrl());
        try (FileOutputStream fos = new FileOutputStream(userFilePath)){
            fos.write(userFile);
        }

        //create the output files
        scoreFile = new File(outputFolder,"score.txt");
        outputFile = new File(outputFolder,"output.txt");

        FileOutputStream scoreOut = new FileOutputStream(scoreFile);
        scoreOut.close();
        FileOutputStream outputOut = new FileOutputStream(outputFile);
        outputOut.close();

        dockerImage = "utibot-submission-"+ submissionLanguage.image+"_app";
        String containerMainDirectory = "/app";
        String container_score_path = containerMainDirectory + "/outputs/score.txt";
        String container_output_path = containerMainDirectory + "/outputs/output.txt";
        String containerUserFile = containerMainDirectory + "/" + problem.getName() + "/" + attachment.getFileName();

        volumes.put(scoreFile.getPath().replace("\\","/"), new ContainerBindFile(container_score_path,"wr"));
        volumes.put(outputFile.getPath().replace("\\","/"), new ContainerBindFile(container_output_path,"wr"));
        volumes.put(userFilePath, new ContainerBindFile(containerUserFile,"wr"));

        int timeoutScore = problem.isHigherIsBetter()? Integer.MIN_VALUE:Integer.MAX_VALUE;

        //replacements:
        //{PROBLEM_NAME}
        //{CONTAINER_USER_FILE}
        //{TIMEOUT_SCORE} the score to use upon failure
        //{CONTAINER_SCORE_PATH}
        //{CONTAINER_OUTPUT_PATH}

        //note that bash -c are being append separately when being passed in
        command = "cd \"/app/{PROBLEM_NAME}\" &&"+
                //f'{compiler} \"{container_user_file}\" -o a.out;
                "compile_output=$("+ submissionLanguage.preProcessCommand + "); "+
                "status=$?;"+
                "if [ $status -ne 0 ]; then "+
                    "echo {TIMEOUT_SCORE} > {CONTAINER_SCORE_PATH};"+
                    "echo Compilation failed > {CONTAINER_OUTPUT_PATH};"+
                    "echo $compile_output >> {CONTAINER_OUTPUT_PATH};"+
                    "exit 1;"+
                "fi;"+
                //this is where the running actually is goes
                //note: the judge program is expected to provide output of: <score> <logfile>
                "output=$(timeout 60s python3 \""+problem.getJudgePythonProgramPath()+"\" "+ submissionLanguage.evaluationCommand +");"+
                "status=$?; "+
                //if the program timed out
                "if [ $status -eq 124 ]; then "+
                    "echo {TIMEOUT_SCORE} > {CONTAINER_SCORE_PATH}; "+
                    "echo Your program timed out > {CONTAINER_OUTPUT_PATH};"+
                //if the program otherwise crashes
                "elif [ $status -ne 0 ]; then "+
                    "echo {TIMEOUT_SCORE} > {CONTAINER_SCORE_PATH};"+
                    "echo Execution failed > {CONTAINER_OUTPUT_PATH};"+
                    "echo $output >> {CONTAINER_OUTPUT_PATH};"+
                "else "+
                //if it completed successfully
                    //take the first item output and print it to the score file
                    "score=$(echo $output | cut -d \" \" -f1); "+
                    //take the second output item and print its content to the output file
                    "filepath=$(echo $output | cut -d \" \" -f2-); "+
                    "echo $score > {CONTAINER_SCORE_PATH}; "+
                    "cat \"$filepath\" > {CONTAINER_OUTPUT_PATH}; "+
                "fi";

        command = command.replace("{PROBLEM_NAME}","\""+problem.getName()+"\"");
        command = command.replace("{CONTAINER_USER_FILE}","\""+ containerUserFile +"\"");
        command = command.replace("{TIMEOUT_SCORE}",""+timeoutScore);
        command = command.replace("{CONTAINER_SCORE_PATH}", container_score_path);
        command = command.replace("{CONTAINER_OUTPUT_PATH}", container_output_path);

//        System.out.println("Running command:"+command);

        this.event = event;
        this.team = team;
        this.problem = problem;
    }

    @Override
    public void run() {
        int problemIndex = ProblemManager.getInstance().getProblemIndex(problem);

        ArrayList<Bind> binds = new ArrayList<>();
        for(String hostPath: volumes.keySet()){
            binds.add(new Bind(new File(hostPath).getAbsolutePath(),new Volume(volumes.get(hostPath).bind()), AccessMode.rw));
        }
        HostConfig hostConfig = HostConfig.newHostConfig().withBinds(binds).withNetworkMode("none").withAutoRemove(true);

        //judge it!!!
        ContainerManager.getInstance().runContainer(dockerImage,command,hostConfig);
        //now read the score file!!!!
        int score;
        try(Scanner readScoreFile = new Scanner(scoreFile)){
            score = readScoreFile.nextInt();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        String resultText;
        if(problem.isHigherIsBetter() == score > team.getScore(problemIndex)){
            //better
            resultText = "You achieved a new team high score of "+score+" in "+problem.getName();
            team.setScore(problemIndex,score);
            TeamManager.getInstance().save();
        } else {
            //worse
            resultText = "You received a score of "+score+" in "+problem.getName()+"\nyour team best is: "+team.getScore(problemIndex);
        }

        try(FileUpload programOutputResponse = FileUpload.fromData(outputFile)){
            event.getHook().sendMessage(resultText).addFiles(programOutputResponse).complete();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //delete the local files
        Downloader.recursiveDelete(new File("submissions/"+id+"/"));
    }
}
