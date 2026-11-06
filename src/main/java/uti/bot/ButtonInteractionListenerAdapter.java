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
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        //there is 1 exception to this, accepting team invites will happen in the DMs
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
                    event.deferReply(true).queue();
                    resetServer();
                    event.getHook().deleteOriginal().queue();
                }

                case "register" -> showRegisterModal(event);

                case null ->{
                    System.err.println("null button event");
                }

                default -> {
                    System.err.println("Unknown button event: "+event.getButton().getCustomId());
                }
            }
        }
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

        MessageChannel tmp = Main.competitionServer.getTextChannelById(Main.config.getAdminChannelId());
        if(tmp != null) {
            tmp.delete().queue();
        }
        //TODO delete subjective judging channels
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

        //TODO delete the files for users, teams and problems
        UserManager.getInstance().wipe();
        TeamManager.getInstance().wipe();

        Main.config.setServerSetup(false);
        Main.registerCommandsServerNotSetUp();
        System.out.println("Server fully reset to initial configuration");
    }
}
