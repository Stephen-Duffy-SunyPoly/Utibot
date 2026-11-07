package uti.bot;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

public class SubmissionRunner extends Thread {

    static int submissionGlobalId = 0;

    static synchronized int getNextId(){
        int t = submissionGlobalId;
        submissionGlobalId++;
        return t;
    }

    private Language submissionLanguage;
    private int id;

    private String dockerImage;
    private String containerMainDirectory;
    private String container_score_path;
    private String container_output_path;

    private String userFilePath;

    private String containerUserFile;

    private String command;

    HashMap<String,ContainerBindFile> volumes = new HashMap<>();

    public SubmissionRunner(ModalInteractionEvent event, Problem problem, Team team, Message.Attachment attachment) throws IOException {
        super("Submission Runner");
        submissionLanguage = Language.fromFileExtension(attachment.getFileExtension());
        if(submissionLanguage == null){
            event.reply("Invalid language").complete();
            throw new RuntimeException("Invalid language");
        }
        id = getNextId();
        //create io folders
        File inputFolder = new File("submissions/"+id+"/input/");
        File outputFolder = new File("submissions/"+id+"/output/");
        boolean ignored = inputFolder.mkdirs();
                ignored = outputFolder.mkdirs();

        //copy problem files to input
        //maybe this is not necessary
        userFilePath = new File(inputFolder,attachment.getFileName()).getPath().replace("\\","/");
        byte[] userFile = Downloader.downloadContent(attachment.getUrl());
        try (FileOutputStream fos = new FileOutputStream(userFilePath)){
            fos.write(userFile);
        }

        //create the output files
        FileOutputStream scoreOut = new FileOutputStream(new File(outputFolder,"score.txt"));
        scoreOut.close();
        FileOutputStream outputOut = new FileOutputStream(new File(outputFolder,"output.txt"));
        outputOut.close();

        dockerImage = "utibot-submission-"+submissionLanguage.image+"_app";
        containerMainDirectory = "/app";
        container_score_path = containerMainDirectory+"/outputs/score.txt";
        container_output_path = containerMainDirectory+"/outputs/output.txt";
        containerUserFile = containerMainDirectory+"/"+problem.getName()+"/"+attachment.getFileName();

        volumes.put(new File(outputFolder,"score.txt").getPath().replace("\\","/"), new ContainerBindFile(container_score_path,"wr"));
        volumes.put(new File(outputFolder,"output.txt").getPath().replace("\\","/"), new ContainerBindFile(container_output_path,"wr"));
        volumes.put(userFilePath, new ContainerBindFile(containerUserFile,"wr"));

        int timeoutScore = 0;

        //replacements:
        //{PROBLEM_NAME}
        //{CONTAINER_USER_FILE}
        //{TIMEOUT_SCORE} the score to use upon failure
        //{CONTAINER_SCORE_PATH}
        //{CONTAINER_OUTPUT_PATH}
        command = "'bash -c \"cd \\\"/app/{PROBLEM_NAME}\\\" &&"+
                //f'{compiler} \"{container_user_file}\" -o a.out;
                submissionLanguage.preProcessCommand +
                "status=$?;"+
                "if [ $status -ne 0 ]; then"+
                    "echo {TIMEOUT_SCORE} > {CONTAINER_SCORE_PATH};"+
                    "echo Compilation failed > {CONTAINER_OUTPUT_PATH};"+
                    "exit 1;"+
                "fi;"+
                //this is where the running actually is goes
                "output=$(timeout 60s python3 \""+problem.getJudgePythonProgramPath()+"\" "+submissionLanguage.evaluationCommand +");"+
                "status=$?; "+
                "if [ $status -eq 124 ]; then "+
                    "echo {TIMEOUT_SCORE} > {CONTAINER_SCORE_PATH}; "+
                    "echo Your program timed out > {CONTAINER_OUTPUT_PATH};"+
                "else"+
                    "score=$(echo $output | cut -d \" \" -f1); "+
                    "filepath=$(echo $output | cut -d \" \" -f2-); "+
                    "echo $score > {CONTAINER_SCORE_PATH}; "+
                    "cat \"$filepath\" > {CONTAINER_OUTPUT_PATH}; "+
                "fi\"";

        command = command.replace("{PROBLEM_NAME}","\""+problem.getName()+"\"");
        command = command.replace("{CONTAINER_USER_FILE}","\""+containerUserFile+"\"");
        command = command.replace("{TIMEOUT_SCORE}",""+timeoutScore);
        command = command.replace("{CONTAINER_SCORE_PATH}",container_score_path);
        command = command.replace("{CONTAINER_OUTPUT_PATH}", container_output_path);

    }

    @Override
    public void run() {

    }
}
