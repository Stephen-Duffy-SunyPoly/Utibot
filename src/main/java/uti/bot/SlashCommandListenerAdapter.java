package uti.bot;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.attachmentupload.AttachmentUpload;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.checkbox.Checkbox;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.FileType;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SlashCommandListenerAdapter extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if(event.getGuild() == null || event.getGuild().getIdLong() != Main.competitionServer.getIdLong()){
            return;
        }
        if(Main.config.isServerSetup()){
            switch (event.getName()){
                case "reset" ->resetServer(event);
                case "team" -> handleTeamCommand(event);
                case "problem" -> handleProblemCommand(event);
                case "submit" -> handleSubmit(event);
                case "shutdown" -> shutdownBot(event);
                case "problems" -> showProblems(event);
                case "leaderboard" -> showLeaderBoard(event);
                case "start" -> startComp(event);
                case "end" -> endComp(event);
            }
        } else {
            if(event.getName().equals("setup")){
                event.reply("Setting up server").queue();
                setupServer();
            } else if(event.getName().equals("shutdown")){
                shutdownBot(event);
            }
        }
    }

    private void endComp(SlashCommandInteractionEvent event) {
        Main.config.setCompStarted(false);
        Main.config.setCompEnded(true);
        Main.registerCommandsServerIsSetUp();
        event.reply("The competition has Ended!").queue();
        //send a competition end message in the feed channel
        MessageChannel feedChannel = Main.competitionServer.getTextChannelById(Main.config.getFeedChannelId());
        if(feedChannel == null){
            return;
        }
        feedChannel.sendMessage("The competition has ended!").queue();
    }

    private void startComp(SlashCommandInteractionEvent event) {
        Main.config.setCompStarted(true);
        Main.config.setCompEnded(false);
        Main.registerCommandsServerIsSetUp();
        //publish the problems in the problems channel
        MessageChannel problemChannel = Main.competitionServer.getTextChannelById(Main.config.getProblemsChannelId());
        if(problemChannel == null){
            event.reply("Unable to find problems channel").queue();
            return;
        }
        event.reply("The competition has Started!").queue();
        for (int i = 0; i < ProblemManager.getInstance().size(); i++) {
            ProblemManager.getInstance().get(i).displayProblem(problemChannel);
        }
        //mega zip file
        ByteArrayOutputStream megaZipBytes = new ByteArrayOutputStream();
        ZipOutputStream megaZipOut = new ZipOutputStream(megaZipBytes);
        try {
            for(int i=0;i<ProblemManager.getInstance().size();i++){
                Problem p = ProblemManager.getInstance().get(i);
                megaZipOut.putNextEntry(new ZipEntry(p.getName()+".zip"));
                byte[] data = FileUtil.downloadContent(p.getInfoFileLink());
                megaZipOut.write(data);
                megaZipOut.closeEntry();
            }
            megaZipOut.flush();
            megaZipOut.close();
        } catch (IOException e) {
            event.reply(e.toString()).queue();
            return;
        }

        FileUpload megaZipUpload = FileUpload.fromData(megaZipBytes.toByteArray(),"All Problems.zip");
        problemChannel.sendFiles(megaZipUpload).addContent("all problems in a single file").queue();
        //send a competition start message in the feed channel
        MessageChannel feedChannel = Main.competitionServer.getTextChannelById(Main.config.getFeedChannelId());
        if(feedChannel == null){
            return;
        }
        feedChannel.sendMessage("The competition has started!").queue();
    }

    private void showLeaderBoard(SlashCommandInteractionEvent event) {
        if (Main.config.hasCompEnded()||Main.config.hasCompStarted()) {
            TeamManager tm = TeamManager.getInstance();
            List<TeamManager.TeamMapping> rankings = tm.getRankOfTeams();
            StringBuilder results = new StringBuilder();
            results.append("## Current team rankings:\n");
            for (int i = 0; i < rankings.size(); i++) {
                results.append(i + 1).append(". ").append(tm.get(rankings.get(i).teamIndex()).getName()).append("\n");
            }
            event.reply(results.toString()).queue();
        } else {
            event.reply("The competition has not started yet").setEphemeral(true).queue();
        }
    }

    private void showProblems(SlashCommandInteractionEvent event) {
        if (Main.config.hasCompEnded()||Main.config.hasCompStarted()) {
            event.reply("Here are the problems:").complete();
            for (int i = 0; i < ProblemManager.getInstance().size(); i++) {
                ProblemManager.getInstance().get(i).displayProblem(event.getChannel());
            }
        } else {
            event.reply("The competition has not started yet").setEphemeral(true).queue();
        }
    }

    private void shutdownBot(SlashCommandInteractionEvent event) {
        event.reply("Shutting bot down!").complete();
        event.getJDA().shutdownNow();
        System.out.println("JDA shutdown");
        //shutdown docker stuff
        ContainerManager.getInstance().shutdown();
        System.out.println("Exiting");
        System.exit(0);
    }

    private void handleSubmit(SlashCommandInteractionEvent event) {
        if(Main.config.hasCompStarted() && ! Main.config.hasCompEnded()) {
            User user = UserManager.getInstance().get(event.getUser().getIdLong());
            if (user == null) {
                event.reply("You need to register for the event first.").setEphemeral(true).queue();
                return;
            }
            if (user.getTeam() == null) {
                event.reply("You need to be on a team to use this feature").setEphemeral(true).queue();
                return;
            }
            long teamChannel = user.getTeam().getTextChannelId();
            if (event.getChannelIdLong() != teamChannel) {
                event.reply("This command can only be used in your team channel: <#" + teamChannel + ">").setEphemeral(true).queue();
                return;
            }
            OptionMapping probOm = event.getOption("problem");
            if (probOm == null) {
                return;
            }

            Problem problem = ProblemManager.getInstance().get(probOm.getAsString());
            if (problem == null) {
                event.reply("That is not a valid problem").setEphemeral(true).queue();
                return;
            }
            AttachmentUpload.Builder attachmentField = AttachmentUpload.create("submission").setRequired(true);
            if (!problem.isSubjective()) {
                attachmentField.setFileTypes(
                        Arrays.stream(Language.values()).map(lang -> FileType.ofExtension(lang.extension)).toArray(FileType[]::new)
                );
            }
            event.replyModal(Modal.create("submit-prob " + problem.getName(), "Create Submission for " + problem.getName()).addComponents(Label.of("file", "Your submission", attachmentField.build())).build()).queue();
        } else {
            event.reply("Submissions are not allowed at this time").setEphemeral(true).queue();
        }
    }

    void handleProblemCommand(SlashCommandInteractionEvent event){

        switch (event.getSubcommandGroup()){
            case "edit" -> {
                OptionMapping pnop = event.getOption("problem");
                if(pnop == null){
                    System.out.println("problem name was null");
                    return;
                }
                String problemName = pnop.getAsString();
                Problem problem = ProblemManager.getInstance().get(problemName);
                if(problem == null){
                    event.reply("No problem by that name found").setEphemeral(true).queue();
                    return;
                }
                switch (event.getSubcommandName()){
                    case "name" -> {
                        String modalId = "prob-edit-name "+problemName;
                        event.replyModal(Modal.create(modalId,"Edit Problem Name")
                            .addComponents(
                                    Label.of("Name","current name: "+problemName,TextInput.create("name",TextInputStyle.SHORT).setRequired(true).setRequiredRange(2,40).setValue(problemName).build())
                            ).build()).queue();
                    }
                    case "higher-is-better" -> {
                        String modalId = "prob-edit-high "+problemName;
                        event.replyModal(Modal.create(modalId,"Edit Problem score ranking")
                                .addComponents(
                                        Label.of("Is higher better","is a higher score better?: "+problemName,Checkbox.of("higher-is-better",problem.isHigherIsBetter()))
                                ).build()).queue();
                    }
                    case "info-file" -> {
                        String modalId = "prob-edit-info "+problemName;
                        event.replyModal(Modal.create(modalId,"Edit Problem info file")
                                .addComponents(
                                        Label.of("replace info content","Warning: this will reset the content of the problem folder",AttachmentUpload.create("info-file").setRequired(true).setFileTypes(FileType.ofExtension("zip")).build())
                                ).build()).queue();
                    }
                    case "judge-program" -> {
                        if(problem.getJudgePythonProgramPath().isEmpty()){
                            event.reply("that problem does not have a judging program").setEphemeral(true).queue();
                            return;
                        }
                        String modalId = "prob-edit-juge "+problemName;
                        event.replyModal(Modal.create(modalId,"Edit Problem judge program")
                                .addComponents(
                                        Label.of("replace judge program","upload a new judging program",AttachmentUpload.create("judge-program").setRequired(true).setFileTypes(FileType.ofExtension("py")).build())
                                ).build()).queue();
                    }
                    case null, default -> {}
                }
            }

            case null, default -> {
                switch (event.getSubcommandName()){
                    case "add" -> {
                        event.replyModal(Modal.create("add-problem","Create Problem")
                                .addComponents(
                                    Label.of("Name","The problem name",TextInput.create("name",TextInputStyle.SHORT).setRequired(true).setRequiredRange(2,40).build()),
                                    Label.of("Subjective","If this problem is subjectively scored", Checkbox.of("subjective")),
                                    Label.of("Higher is Better","If a higher score should be consider better",Checkbox.of("higher-is-better")),
                                    Label.of("Info file","zip file containing the problem description and test file", AttachmentUpload.create("info-file").setRequired(true).setFileTypes(FileType.ofExtension("zip")).build()),
                                    Label.of("Jude Program","Python program for scoring submissions, not necessary for subjective problems", AttachmentUpload.create("judge-program").setRequired(false).setFileTypes(FileType.ofExtension("py")).build())
                                )
                            .build()).queue();
                    }
                    case "delete" -> {
                        OptionMapping pmap = event.getOption("problem");
                        if(pmap==null){
                            return;
                        }
                        Problem problem = ProblemManager.getInstance().get(pmap.getAsString());
                        if(problem == null){
                            event.reply("A problem with that name does not exist").setEphemeral(true).queue();
                            return;
                        }
                        event.reply("Are you sure you want to delete "+problem.getName()).setEphemeral(true).addComponents(ActionRow.of(
                                Button.danger("delete-problem","Delete"),
                                Button.secondary("cancel","Cancel")
                        )).queue();
                    }

                    case "preview" ->{
                        OptionMapping pmap = event.getOption("problem");
                        if(pmap==null){
                            return;
                        }
                        Problem problem = ProblemManager.getInstance().get(pmap.getAsString());
                        if(problem == null){
                            event.reply("A problem with that name does not exist").setEphemeral(true).queue();
                            return;
                        }
                        problem.displayProblem(event.getChannel());
                        event.deferReply().queue();
                        event.getHook().deleteOriginal().queue();
                    }
                    case null, default -> {}
                }
            }
        }
    }

    void handleTeamCommand(SlashCommandInteractionEvent event){
        //validate the option they chose
        User user = UserManager.getInstance().get(event.getUser().getIdLong());
        if(user == null){
            event.reply("You need to register for the event first.").setEphemeral(true).queue();
            return;
        }
        String command = event.getOption("command", OptionMapping::getAsString);
        if(command == null){
            return;
        }

        //if they are not on a team
        if(user.getTeam() == null) {
            if (command.equals("create")) {
                event.replyModal(Modal.create("create-team", "Create new Team")
                        .addComponents(Label.of("Team Name", "what to name the team", TextInput.create("name", TextInputStyle.SHORT).setRequiredRange(4, 25).setRequired(true).build()))
                        .build()).queue();
            } else {
                event.reply("That is not a valid team command at this time").setEphemeral(true).queue();
            }
        } else {
            switch (command) {
                case "leave" -> {
                    Team t = user.getTeam();
                    user.setTeam(null);
                    event.reply("You have left " + t.getName()).queue();
                    Main.competitionServer.removeRoleFromMember(event.getUser(),t.getRole()).queue();
                    if (t.getNumberOfMembers() == 0) {
                        TeamManager.getInstance().deleteTeam(t);
                    }
                }
                case "list" -> {
                    ArrayList<User> teamMemers = user.getTeam().getMembers();
                    StringBuilder response = new StringBuilder(user.getTeam().getName() + " has the following members: ");
                    for (User u : teamMemers) {
                        response.append("\n ").append(u.getName());
                    }
                    event.reply(response.toString()).queue();
                }
                case "invite" -> event.replyModal(Modal.create("team-invite", "Invite to Team")
                        .addComponents(
                                Label.of("User", EntitySelectMenu.create("user", EntitySelectMenu.SelectTarget.USER).setRequired(true).build())
                        ).build()).queue();
                case "rank" -> {
                    if(Main.config.hasCompStarted() || Main.config.hasCompEnded()) {
                        Team team = user.getTeam();
                        StringBuilder content = new StringBuilder("Problem | Your score | Your Rank\n");
                        for (int i = 0; i < ProblemManager.getInstance().size(); i++) {
                            content.append(team.getProblemRank(i)).append("\n");
                        }
                        event.reply(content.toString()).queue();
                    } else {
                        event.reply("This information is not available at this time").setEphemeral(true).queue();
                    }
                }
                default -> event.reply("That is not a valid team command at this time").setEphemeral(true).queue();
            }
        }

    }
    void setupServer(){
        long botId = Main.competitionServer.getJDA().getSelfUser().getIdLong();
        //create a registered role
        //set the new role to be able to view channels by default
        Role registerdRole = Main.competitionServer.createRole().setName("Registered").setPermissions(Permission.VIEW_CHANNEL).complete();
        Main.config.setRegisteredRoleId(registerdRole.getIdLong());
        //set @ everyone to not be able to view channels by default
        Role atEveryone = Main.competitionServer.getPublicRole();
        atEveryone.getManager().setPermissions(atEveryone.getPermissionsRaw() & ~Permission.VIEW_CHANNEL.getRawValue()).complete();

        //create a register channel
        //make that channel viewable to everyone but not writable
        ArrayList<Permission> registerChannelEveryoneAllowed = new ArrayList<>();
        ArrayList<Permission> registerChannelEveryoneDeny = new ArrayList<>();
        registerChannelEveryoneAllowed.add(Permission.VIEW_CHANNEL);
        registerChannelEveryoneDeny.add(Permission.MESSAGE_SEND);
        registerChannelEveryoneDeny.add(Permission.CREATE_PUBLIC_THREADS);
        registerChannelEveryoneDeny.add(Permission.CREATE_PRIVATE_THREADS);

        MessageChannel registerChannel = Main.competitionServer.createTextChannel("Register").addRolePermissionOverride(atEveryone.getIdLong(),
                registerChannelEveryoneAllowed,
                registerChannelEveryoneDeny).addMemberPermissionOverride(botId, EnumSet.of(Permission.MESSAGE_SEND),null)
                .setTopic("Register your self for the competition here")
                .complete();
        Main.config.setRegisterChannelId(registerChannel.getIdLong());
        //send a registration message in that channel

        registerChannel.sendMessage(Main.config.getRegisterChannelMessage()).addComponents(
                ActionRow.of(Button.success("register","Register"))
        ).queue((message)->{
            Main.config.setRegisterChannelMessageId(message.getIdLong());
        });

        //create the team category
        //deny registered permission to view this category
        Category teamCategory = Main.competitionServer.createCategory("Team").addRolePermissionOverride(registerdRole.getIdLong(),
                null,EnumSet.of(Permission.VIEW_CHANNEL))
                .addMemberPermissionOverride(botId,EnumSet.of(Permission.VIEW_CHANNEL),null)
                .addRolePermissionOverride(atEveryone.getIdLong(),null,EnumSet.of(Permission.VIEW_CHANNEL))
                .complete();
        Main.config.setTeamCategoryId(teamCategory.getIdLong());

        //create the judge category
        //deny registered permission to view this category
        Category judgeCategory = Main.competitionServer.createCategory("Judge").addRolePermissionOverride(registerdRole.getIdLong(),
                        null,EnumSet.of(Permission.VIEW_CHANNEL))
                .addRolePermissionOverride(atEveryone.getIdLong(),null,EnumSet.of(Permission.VIEW_CHANNEL))
                .addMemberPermissionOverride(botId,EnumSet.of(Permission.VIEW_CHANNEL),null)
                .complete();
        Main.config.setJudgeCategoryId(judgeCategory.getIdLong());
        //create the admin channel in this category
        MessageChannel adminChannel = Main.competitionServer.createTextChannel("Admin",judgeCategory).setTopic("A private space for competition administration").syncPermissionOverrides().complete();
        Main.config.setAdminChannelId(adminChannel.getIdLong());
        adminChannel.sendMessage("This is the admin channel. Use this channel to configure and administration the challenge. Users can not see this channel.\n\n" +
                "Commands available for your use are:\n" +
                "`/problem` create, delete and edit problems\n" +
                "`/start` start the competition, releasing the problems\n" +
                "`/end` end the competition, preventing new submissions\n" +
                "`/shutdown` turn this bot off\n" +
                "`/reset` bring this server back to \"factory setup\" and delete most bot data from the host").queue();

        MessageChannel feedChannel = Main.competitionServer.createTextChannel("feed").addPermissionOverride(atEveryone,null,EnumSet.of(Permission.MESSAGE_SEND)).addMemberPermissionOverride(botId,EnumSet.of(Permission.MESSAGE_SEND),null).setTopic("Updates and high scores throughout the event").complete();
        Main.config.setFeedChannelId(feedChannel.getIdLong());
        feedChannel.sendMessage("Real time updates will appear here during the event").queue();

        MessageChannel problemsChannel = Main.competitionServer.createTextChannel("problems").addPermissionOverride(atEveryone,null,EnumSet.of(Permission.MESSAGE_SEND)).addMemberPermissionOverride(botId,EnumSet.of(Permission.MESSAGE_SEND),null).setTopic("Available problems will be posted here").complete();
        Main.config.setProblemsChannelId(problemsChannel.getIdLong());
        problemsChannel.sendMessage("Problems will be posted here once the competition starts.").queue();


        MessageChannel infoChannel = Main.competitionServer.createTextChannel("info").addPermissionOverride(atEveryone,null,EnumSet.of(Permission.MESSAGE_SEND)).addMemberPermissionOverride(botId,EnumSet.of(Permission.MESSAGE_SEND),null).setTopic("Information about this server and automated system").complete();
        Main.config.setInfoChannelId(infoChannel.getIdLong());
        infoChannel.sendMessage("# Welcome to the competition\n" +
                "This bot is used to run the event, below is some information you will find useful\n\n" +
                "## Teams\n" +
                "Inorder to participate in this competition you will need to be on a team.\n" +
                "To **start a team**, you will need to run the command `/team create` in any open text channel. You will then be prompted to enter the name of your new team.\n" +
                "Once your team has been crated, you will have access to a private text channel for any team communications. All submissions must be made in this channel.\n" +
                "There are 2 ways to **invite someone to a team**, the first way is to run the `/team invite` command in any open text channel, then select the person you want to invite from the list provided. " +
                "The other way to invite someone is to right click (or equivalent) on their profile, and select `invite to team` under the apps sections. NOTE: a team can have no more then 4 people at a time.\n" +
                "To **join a team** first ask someone on that team to invite you. Then look for a message from this bot in your DMs. Click on the accept button to join the team.\n" +
                "To **Leave a team** run the command `/team leave` in any open text channel. Note: empty teams will be deleted.\n" +
                "To **Check who is on your team** run the command `/team list` in any open text channel.\n" +
                "To check how well your team is doing, run the `/team rank` command in any open text channel.").queue();
        infoChannel.sendMessage("## Problems\n" +
                "Once the competition starts, the available problems will be posted in the <#"+problemsChannel.getId()+"> channel.\n" +
                "If at any time you want to quickly see the problems available, run the `/problems` command in any open text channel.\n\n" +
                "## Leaderboard\n" +
                "To view the **current leaderboard**, run the `/leaderboard` command in any open text channel.\n" +
                "To view how your team ranks in all problems, run the command `/team rank` in any open text channel.\n\n" +
                "## Submitting\n" +
                "To submit a solution for any problem, run the command `/submit <problem name>` in your team channel. Then drag your submission file into the dialog that pops up.\n" +
                "Automatically scored problems will then be executed, with the score and program output being returned to you when execution finishes. note: A strict 1 minute of run time is allowed for the entire execution and judging of your program. " +
                "\n" +
                "## Feed\n" +
                "The <#"+feedChannel.getId()+"> channel will be updated during the completion with the latest high scores and other announcements.").queue();

        Main.config.setServerSetup(true);
        Main.registerCommandsServerIsSetUp();
    }

    void resetServer(SlashCommandInteractionEvent event){
        event.reply("# Are You Sure?\nThis action will ***DELETE*** all completion channels including:\n- all team chatter\n- any subjective judging\n- any administration commands.\n- any roles created by this bot\n\nAny competition data stored by the bot will deleted including:\n- teams\n- problem data\n- user registration information\n\nAre you sure you want to proceed?")
                .addComponents(ActionRow.of(
                        Button.danger("reset-server","Proceed"),
                        Button.secondary("cancel","Cancel")
                )).setEphemeral(true).queue();
    }
}
