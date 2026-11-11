package uti.bot;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;

public class ButtonInteractionListenerAdapter extends ListenerAdapter {
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {//when a button press event is received from discord
        if(!event.isFromGuild()){//if the button press was in a DM
            switch (event.getButton().getCustomId()){
                case "cancel" -> {
                    event.deferReply(true).queue();
                    Message message = event.getMessage();
                    message.delete().queue();
                    event.getHook().deleteOriginal().queue();
                }
                case "team-join-accept" -> joinTeam(event);

                case null, default -> {}
            }
        }
        //only respond to other button presses from the specific comp server
        if(event.getGuild() == null || event.getGuild().getIdLong() != Main.competitionServer.getIdLong()){
            return;
        }
        if(Main.config.isServerSetup()){

            switch (event.getButton().getCustomId()){
                case "cancel" -> {
                    event.deferReply(true).queue();
                    Message message = event.getMessage();
                    message.delete().queue();
                    event.getHook().deleteOriginal().queue();
                }
                case "reset-server" -> {
                    event.deferReply(true).complete();
                    event.getMessage().delete().complete();
                    event.getHook().deleteOriginal().queue();
                    resetServer();
                }

                case "register" -> showRegisterModal(event);

                case "delete-problem" -> deleteProblem(event);

                case "score-submission" -> subjectiveScore(event);

                case null ->{
                    System.err.println("null button event");
                }

                default -> {
                    System.err.println("Unknown button event: "+event.getButton().getCustomId());
                }
            }
        }
    }

    private void subjectiveScore(ButtonInteractionEvent event) {
        event.replyModal(Modal.create("subjective-score","Submit Score")
                .addComponents(Label.of("Score",TextInput.create("score",TextInputStyle.SHORT).setRequired(true).setMaxLength(16).build())).build()).queue();
    }

    private void deleteProblem(ButtonInteractionEvent event) {
        //parse the team name
        String problemName = event.getMessage().getContentRaw().substring("Are you sure you want to delete ".length());
        event.getMessage().delete().queue();
        Problem problem = ProblemManager.getInstance().get(problemName);
        if(problem == null){
            event.reply("A problem by that name was not found").setEphemeral(true).queue();
            return;
        }
        ProblemManager.getInstance().deleteProblem(problem);
        event.reply("Deleted problem: "+problem.getName()).queue();
    }

    void joinTeam(ButtonInteractionEvent event){
        User user = UserManager.getInstance().get(event.getUser().getIdLong());
        if(user.getTeam() !=  null){
            event.reply("You are already on a team").queue();
            return;
        }
        Message message = event.getMessage();
        String [] messageData = message.getContentRaw().split("\n");
        String teamName = messageData[2].replace("`","");
        Team team = TeamManager.getInstance().get(teamName);
        if(team == null){
            event.reply("An error occurred. Team not found").queue();
            return;
        }
        if(team.getNumberOfMembers() >= 4){
            event.reply("Unfortunately, this team is full").queue();
            return;
        }
        event.deferReply(true).queue();
        user.setTeam(team);
        Main.competitionServer.addRoleToMember(event.getUser(),team.getRole()).queue();
        team.getChannel().sendMessage("<@"+user.getUserID()+"> has joined the team").queue();
        message.delete().queue();
        event.getHook().deleteOriginal().queue();
    }

    void showRegisterModal(ButtonInteractionEvent event){
        if(UserManager.getInstance().get(event.getUser().getIdLong())!= null){
            event.reply("You have already registered for this event!").setEphemeral(true).queue();
            return;
        }

        event.replyModal(Modal.create(
                "register", "Register for Competition"
        ).addComponents(
                Label.of("Name","Enter your name here", TextInput.create("name", TextInputStyle.SHORT).setRequiredRange(4,50).setRequired(true).build()),
                Label.of("Email", "Enter your email here (school preferred)", TextInput.create("email", TextInputStyle.SHORT).setRequiredRange(4,50).setRequired(true).build())
        ).build()).queue();
    }

    void resetServer(){
        //note: all channels must be deleted separately

        //delete the admin channel
        MessageChannel tmp = Main.competitionServer.getTextChannelById(Main.config.getAdminChannelId());
        if(tmp != null) {
            tmp.delete().queue();
        }
        ProblemManager pm = ProblemManager.getInstance();
        //delete all subjective scoring channel
        for(int i=0;i<pm.size();i++){
            tmp = pm.get(i).getScoringChannel();
            if(tmp != null) {
                tmp.delete().queue();
            }
        }
        //delete the judge category
        Category tmpCat = Main.competitionServer.getCategoryById(Main.config.getJudgeCategoryId());
        if(tmpCat!= null){
            tmpCat.delete().queue();
        }

        //delete the team channels
        TeamManager tm = TeamManager.getInstance();
        for(int i=0;i<tm.size();i++){
            Team t = tm.get(i);
            tmp = t.getChannel();
            if(tmp != null) {
                tmp.delete().queue();
            }
        }

        //delete the team category
        tmpCat = Main.competitionServer.getCategoryById(Main.config.getTeamCategoryId());
        if(tmpCat != null){
            tmpCat.delete().queue();
        }

        //delete the registration channel
        tmp = Main.competitionServer.getTextChannelById(Main.config.getRegisterChannelId());
        if(tmp != null){
            tmp.delete().queue();
        }

        //delete the info channel
        tmp = Main.competitionServer.getTextChannelById(Main.config.getInfoChannelId());
        if(tmp != null){
            tmp.delete().queue();
        }

        //delete the problems channel
        tmp = Main.competitionServer.getTextChannelById(Main.config.getProblemsChannelId());
        if(tmp != null){
            tmp.delete().queue();
        }

        //delete the feed channel
        tmp = Main.competitionServer.getTextChannelById(Main.config.getFeedChannelId());
        if(tmp != null){
            tmp.delete().queue();
        }


        //delete the registered role
        Role tmpRole = Main.competitionServer.getRoleById(Main.config.getRegisteredRoleId());
        if(tmpRole != null){
            tmpRole.delete().queue();
        }
        //delete the team roles
        for(int i=0;i<tm.size();i++){
            Team t = tm.get(i);
            tmpRole = t.getRole();
            if(tmpRole != null) {
                tmpRole.delete().queue();
            }
        }

        //give @everyone the ability to view channels
        tmpRole = Main.competitionServer.getPublicRole();
        tmpRole.getManager().setPermissions(tmpRole.getPermissionsRaw() | Permission.VIEW_CHANNEL.getRawValue()).queue();


        UserManager.getInstance().wipe();
        TeamManager.getInstance().wipe();
        ProblemManager.getInstance().wipe();

        Main.config.setCompEnded(false);
        Main.config.setCompStarted(false);
        Main.config.setServerSetup(false);
        //remove the docker containers
        ContainerManager.getInstance().deleteLanguageContainers();
        ContainerManager.getInstance().deleteDockerIgnore();
        Main.registerCommandsServerNotSetUp();
        System.out.println("Server fully reset to initial configuration");
    }
}
