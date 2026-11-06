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
        Category tmpCat = Main.competitionServer.getCategoryById(Main.config.getJudgeCategoryId());
        if(tmpCat!= null){
            tmpCat.delete().queue();
        }

        //TODO delete team channels
        tmpCat = Main.competitionServer.getCategoryById(Main.config.getTeamCategoryId());
        if(tmpCat != null){
            tmpCat.delete().queue();
        }

        tmp = Main.competitionServer.getTextChannelById(Main.config.getRegisterChannelId());
        if(tmp != null){
            tmp.delete().queue();
        }

        //TODO delete team roles
        Role tmpRole = Main.competitionServer.getRoleById(Main.config.getRegisteredRoleId());
        if(tmpRole != null){
            tmpRole.delete().queue();
        }

        //give @everyone the ability to view channels
        tmpRole = Main.competitionServer.getPublicRole();
        tmpRole.getManager().setPermissions(tmpRole.getPermissionsRaw() | Permission.VIEW_CHANNEL.getRawValue()).queue();

        //TODO delete the files for users, teams and problems

        Main.config.setServerSetup(false);
        Main.registerCommandsServerNotSetUp();
        System.out.println("Server fully reset to initial configuration");
    }
}
