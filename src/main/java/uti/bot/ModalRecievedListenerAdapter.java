package uti.bot;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jspecify.annotations.Nullable;

import java.io.*;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public class ModalRecievedListenerAdapter extends ListenerAdapter {
    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if(event.getGuild() == null || event.getGuild().getIdLong() != Main.competitionServer.getIdLong()){
            return;
        }
        if(Main.config.isServerSetup()){
            switch (event.getModalId()){
                case "register" -> {
                    long userId = event.getUser().getIdLong();
                    ModalMapping nameMapping = event.getValue("name");
                    ModalMapping emailMapping = event.getValue("email");
                    if(nameMapping == null || emailMapping == null){
                        event.reply("An error occurred with the submission").setEphemeral(true).queue();
                        return;
                    }
                    String name = nameMapping.getAsString();
                    if(!Util.isValidName(name)){
                        event.reply("You entered an invalid name. Please enter a valid name").setEphemeral(true).queue();
                        return;
                    }
                    String email = emailMapping.getAsString();
                    event.deferReply().setEphemeral(true).queue();
                    UserManager.getInstance().add(new User(userId,name,email));
                    Main.competitionServer.addRoleToMember(event.getUser(),Main.getRegisteredRole()).queue();
                    Member member = event.getMember();
                    if(member!=null) {
                        member.modifyNickname(name).queue();
                    }
                    event.getHook().deleteOriginal().queue();

                }
                case "create-team" -> {
                    ModalMapping nameMapping = event.getValue("name");
                    if(nameMapping == null){
                        return;
                    }
                    net.dv8tion.jda.api.entities.User discordUser =event.getUser();
                    User user = UserManager.getInstance().get(discordUser.getIdLong());
                    String teamName = nameMapping.getAsOptionalString();
                    if(teamName == null){
                        return;
                    }
                    teamName = teamName.trim();
                    if(teamName.isEmpty() || !Util.isValidName(teamName)){
                        event.reply("Invalid team name").setEphemeral(true).queue();
                        return;
                    }

                    if(TeamManager.getInstance().get(teamName)!= null){
                        event.reply("A team with that name already exsists").setEphemeral(true).queue();
                        return;
                    }
                    int roleColor = (int)(Math.random() * 16777216);
                    Role teamRole = Main.competitionServer.createRole().setName("Team "+teamName).setColor(roleColor).setMentionable(true).complete();
                    Category teamsCategory = Main.competitionServer.getCategoryById(Main.config.getTeamCategoryId());
                    MessageChannel channel = Main.competitionServer.createTextChannel("Team "+teamName,teamsCategory)
                            .setTopic("Submissions and chatter for "+teamName)
                            .syncPermissionOverrides()
                            .addRolePermissionOverride(teamRole.getIdLong(), EnumSet.of(Permission.VIEW_CHANNEL),null)
                            .complete();
                    Team team = new Team(teamName, teamRole.getIdLong(), channel.getIdLong());
                    TeamManager.getInstance().add(team);
                    user.setTeam(team);
                    Main.competitionServer.addRoleToMember(discordUser,teamRole).queue();
                    channel.sendMessage("<@&"+teamRole.getId()+"> You will use this channel to communicate with your team mates and submit answers.").queue();
                    event.reply("Created team: "+teamName).queue();
                }

                case "team-invite" -> {
                    ModalMapping userMapping = event.getValue("user");
                    if(userMapping == null){
                        return;
                    }
                    net.dv8tion.jda.api.entities.User discordUser = userMapping.getAsMentions().getUsers().getFirst();
                    User otherUser = UserManager.getInstance().get(discordUser.getIdLong());
                    if(otherUser == null){
                        event.reply("That user has not yet registered for the competition").setEphemeral(true).queue();
                        return;
                    }
                    if(otherUser.getTeam() != null){
                        event.reply("That user is already on a team").setEphemeral(true).queue();
                        return;
                    }

                    User user = UserManager.getInstance().get(event.getUser().getIdLong());

                    PrivateChannel userDm = discordUser.openPrivateChannel().complete();
                    userDm.sendMessage("## Team Invite\n"+user.getName() +" has invited you to join the team:\n`"+user.getTeam().getName()+"`")
                            .addComponents(ActionRow.of(
                                    Button.success("team-join-accept","Accept"),
                                    Button.danger("cancel","Decline")
                            )).queue();

                    event.reply(otherUser.getName() +" has been invited to join your team, tell them to check their DMs").queue();
                }

                case "add-problem" -> {
                    ModalMapping nameMapping = event.getValue("name");
                    ModalMapping subjectiveMapping = event.getValue("subjective");
                    ModalMapping higherIsBetterMapping = event.getValue("higher-is-better");
                    ModalMapping infoFileMapping = event.getValue("info-file");
                    ModalMapping judgeProgramMapping = event.getValue("judge-program");
                    if(nameMapping == null || subjectiveMapping == null || higherIsBetterMapping == null || infoFileMapping == null){
                        return;
                    }
                    String name = nameMapping.getAsString();
                    if(!Util.isValidName(name)){
                        event.reply("Invalid problem name").setEphemeral(true).queue();
                        return;
                    }
                    if(ProblemManager.getInstance().getNameList().contains(name)){
                        event.reply("A problem with that name already exsists").setEphemeral(true).queue();
                        return;
                    }
                    boolean subjective = subjectiveMapping.getAsBoolean();
                    boolean higherIsBetter = higherIsBetterMapping.getAsBoolean();
                    Message.Attachment infoFileAttachment = infoFileMapping.getAsAttachmentList().getFirst();
                    Message.Attachment judgeProgramAttachment = null;
                    if(!subjective && judgeProgramMapping == null){
                        return;
                    }
                    long subjectiveChannelId = 0;
                    if(!subjective){
                        List<Message.Attachment> atl = judgeProgramMapping.getAsAttachmentList();
                        if(atl.isEmpty()){
                            return;
                        }
                        event.deferReply().queue();
                        judgeProgramAttachment = judgeProgramMapping.getAsAttachmentList().getFirst();
                    } else {
                        event.deferReply().queue();
                        //create a channel is subjective
                        MessageChannel subjectiveChannel = Main.competitionServer.createTextChannel("Problem "+name+" scoring",Main.competitionServer.getCategoryById(Main.config.getJudgeCategoryId()))
                                .syncPermissionOverrides().setTopic("Submissions for the problem "+name+" will appear here").complete();
                        subjectiveChannelId = subjectiveChannel.getIdLong();
                        subjectiveChannel.sendMessage("Team submissions for the "+name+" problem will appear here. Make sure to score them in a timely manner").queue();
                    }

                    //create a problem folder
                    boolean ignored = new File("problems/"+name+"/").mkdirs();

                    //download and extract the info file into the folder
                    byte[] infoFileData = Downloader.downloadContent(infoFileAttachment.getUrl());
                    try {
                        Downloader.decompressZipBytesToDisc(infoFileData,"problems/"+name+"/");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    String judgeProgramPath = "";
                    if(!subjective) {
                        //if not subjective, download the judge program and put it in the folder
                        byte[] judgeProgramBytes = Downloader.downloadContent(judgeProgramAttachment.getUrl());
                        try (FileOutputStream fos = new FileOutputStream("problems/"+name+"/"+judgeProgramAttachment.getFileName())) {
                            fos.write(judgeProgramBytes);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        judgeProgramPath = judgeProgramAttachment.getFileName();
                    }

                    Problem problem = new Problem(name,higherIsBetter,subjective,infoFileAttachment.getUrl(),judgeProgramPath,subjectiveChannelId);
                    ProblemManager.getInstance().add(problem);//this also regenrates images if nessarry

                    event.getHook().sendMessage("Created new problem "+name+"\nRegenerating containers, this may take some time.").queue();
                    //any other setup to run automatic submissions
                }

                case "subjective-score" ->{
                    ModalMapping scoreMapping = event.getValue("score");
                    if(scoreMapping == null){
                        return;
                    }
                    String scoreString = scoreMapping.getAsString();
                    try {
                        int score = Integer.parseInt(scoreString);
                        long channelId = event.getChannelIdLong();
                        Problem problem = null;
                        for (int i=0;i<ProblemManager.getInstance().size();i++){
                            if(channelId == ProblemManager.getInstance().get(i).getSubjectiveScoringChannelId()){
                                problem =  ProblemManager.getInstance().get(i);
                                break;
                            }
                        }
                        if(problem == null){
                            event.reply("Unable to locate problem!").setEphemeral(true).queue();
                            return;
                        }
                        Message message = event.getMessage();
                        if(message == null){
                            event.reply("Unable to fin source message!").setEphemeral(true).queue();
                            return;
                        }
                        Role teamRole = message.getMentions().getRoles().getFirst();
                        //find the team
                        Team team = null;
                        for(int i=0;i<TeamManager.getInstance().size();i++){
                            if(TeamManager.getInstance().get(i).getRoleId() == teamRole.getIdLong()){
                                team = TeamManager.getInstance().get(i);
                            }
                        }
                        if(team == null){
                            event.reply("Unable to find team").setEphemeral(true).queue();
                            return;
                        }
                        //we can now register the score
                        int problemIndex = ProblemManager.getInstance().getProblemIndex(problem);

                        int currentScore = team.getScore(problemIndex);
                        if(problem.isHigherIsBetter() == score > currentScore){
                            //better score
                            team.setScore(problemIndex,score);
                            team.getChannel().sendMessage("You achieved a new team high score of "+score+" in "+problem.getName()).queue();
                            TeamManager.getInstance().save();
                        } else {
                            team.getChannel().sendMessage("You received a score of "+score+" in "+problem.getName()+"\nyour team best is: "+currentScore).queue();
                        }
                        event.getMessage().editMessageComponents().queue();
                        event.deferReply().queue();
                        event.getHook().deleteOriginal().queue();
                    } catch (Exception e){
                        event.reply("Scores must be integers!").setEphemeral(true).queue();
                    }
                }

                default -> {
                    if(event.getModalId().startsWith("prob-edit-")){
                        if(event.getModalId().startsWith("prob-edit-name ")){
                            String problemName = event.getModalId().substring("prob-edit-name ".length());
                            Problem problem = ProblemManager.getInstance().get(problemName);
                            if(problem == null){
                                return;
                            }
                            ModalMapping nameMapping = event.getValue("name");
                            if(nameMapping == null){
                                return;
                            }
                            String newName = nameMapping.getAsString();
                            if(!Util.isValidName(newName)){
                                event.reply("Invalid problem name").setEphemeral(true).queue();
                            }
                            if(ProblemManager.getInstance().getNameList().contains(newName)){
                                event.reply("A problem with that name already exsists").setEphemeral(true).queue();
                                return;
                            }
                            problem.setName(newName);
                            if(problem.isSubjective()){
                                Objects.requireNonNull(Main.competitionServer.getGuildChannelById(problem.getSubjectiveScoringChannelId())).getManager().setName("Problem "+newName+" scoring").queue();
                            } else {
                                //have the images regenerated
                                new Thread(() -> ContainerManager.getInstance().requestRegenerateImages()).start();
                            }
                            event.reply("Changed problem name to: "+newName+"\nRegenerating containers, this may take some time.").queue();

                        } else if(event.getModalId().startsWith("prob-edit-high ")){
                            String problemName = event.getModalId().substring("prob-edit-high ".length());
                            Problem problem = ProblemManager.getInstance().get(problemName);
                            if(problem == null){
                                return;
                            }
                            ModalMapping higherMapping = event.getValue("higher-is-better");
                            if(higherMapping == null){
                                return;
                            }
                            problem.setHigherIsBetter(higherMapping.getAsBoolean());
                            ProblemManager.getInstance().save();
                            ProblemManager.getInstance().updateProblemRanking(problem);
                            event.reply("Updated problem ranking").queue();
                            //"prob-edit-info "
                        } else if(event.getModalId().startsWith("prob-edit-info ")){
                            String problemName = event.getModalId().substring("prob-edit-info ".length());
                            Problem problem = ProblemManager.getInstance().get(problemName);
                            if(problem == null){
                                return;
                            }
                            ModalMapping attachMapping = event.getValue("info-file");
                            if(attachMapping == null){
                                return;
                            }
                            Message.Attachment attachment = attachMapping.getAsAttachmentList().getFirst();
                            //copy the judge program (if one exsists)
                            byte[] judgeProgram = getJudgeProgramBytes(problem);
                            //delete the folder
                            Downloader.recursiveDelete(new File("problems/"+problem.getName()+"/"));
                            //download the attachment
                            byte[] newInfo = Downloader.downloadContent(attachment.getUrl());
                            //extract the attachment
                            try {
                                Downloader.decompressZipBytesToDisc(newInfo,"problems/"+problem.getName()+"/");
                                if(!problem.getJudgePythonProgramPath().isEmpty()) {
                                    //recopy the judge program
                                    try (FileOutputStream fos = new FileOutputStream("problems/" + problem.getName() + "/" + problem.getJudgePythonProgramPath())) {
                                        assert judgeProgram != null;
                                        fos.write(judgeProgram);
                                    }
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            problem.setInfoFileLink(attachment.getUrl());
                            ProblemManager.getInstance().save();
                            if(!problem.isSubjective()) {
                                //have the images regenerated
                                new Thread(() -> ContainerManager.getInstance().requestRegenerateImages()).start();
                            }
                            event.reply("Replaced the problem info for: "+problemName+"\nRegenerating containers, this may take some time.").queue();
                        } else if (event.getModalId().startsWith("prob-edit-juge ")){
                            String problemName = event.getModalId().substring("prob-edit-juge ".length());
                            Problem problem = ProblemManager.getInstance().get(problemName);
                            if(problem == null){
                                return;
                            }
                            ModalMapping attachMapping = event.getValue("judge-program");
                            if(attachMapping == null){
                                return;
                            }
                            Message.Attachment attachment = attachMapping.getAsAttachmentList().getFirst();
                            //delete current judder
                            boolean ignored = new File("problems/" + problem.getName() + "/" + problem.getJudgePythonProgramPath()).delete();
                            //download the new one
                            byte[] judgeBytes = Downloader.downloadContent(attachment.getUrl());
                            try (FileOutputStream fos = new FileOutputStream("problems/" + problem.getName() + "/" +attachment.getFileName())){
                                fos.write(judgeBytes);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                            //save the file path
                            problem.setJudgePythonProgramPath(attachment.getFileName());
                            ProblemManager.getInstance().save();
                            //reload the containers
                            new Thread(() -> ContainerManager.getInstance().requestRegenerateImages()).start();
                            event.reply("Judging program updated for "+problemName+"\nRegenerating containers, this may take some time.").queue();
                        }
                    } else if(event.getModalId().startsWith("submit-prob ")){
                        //validate submissions accepted
                        String problemName = event.getModalId().substring("submit-prob ".length());
                        Problem problem = ProblemManager.getInstance().get(problemName);
                        if(problem == null){
                            return;
                        }
                        ModalMapping attachMapping = event.getValue("submission");
                        if(attachMapping == null){
                            return;
                        }
                        Message.Attachment attachment = attachMapping.getAsAttachmentList().getFirst();

                        User user = UserManager.getInstance().get(event.getUser().getIdLong());
                        if(user == null){
                            event.reply("You need to register for the event first.").setEphemeral(true).queue();
                            return;
                        }
                        if (user.getTeam() == null) {
                            event.reply("You need to be on a team to use this feature").setEphemeral(true).queue();
                            return;
                        }
                        Team team = user.getTeam();

                        FileUpload file = attachment.getProxy().downloadAsFileUpload(attachment.getFileName());
                        if(problem.isSubjective()){
                            problem.getScoringChannel().sendMessage("<@&"+team.getRoleId()+"> has made a submission")
                                    .addFiles(file)
                                    .addComponents(ActionRow.of(Button.primary("score-submission","Score")))
                                    .queue();
                            event.reply("Submission made for "+problemName).queue();
                        } else {
                            SubmissionRunner runner;
                            try {
                                runner = new SubmissionRunner(event,problem,team,attachment);
                            } catch (IOException e) {
                                event.reply("An error occurred!").setEphemeral(true).queue();
                                throw new RuntimeException(e);
                            }
                            event.deferReply().queue();
                            //compile and run the program
                            Thread.ofVirtual().start(runner);
                        }

                    }
                }
            }
        }
    }

    private static byte @Nullable [] getJudgeProgramBytes(Problem problem) {
        byte[] judgeProgram = null;
        if(!problem.getJudgePythonProgramPath().isEmpty()){
            try(FileInputStream fis = new FileInputStream("problems/"+ problem.getName()+"/"+ problem.getJudgePythonProgramPath())){
                judgeProgram = fis.readAllBytes();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return judgeProgram;
    }
}
