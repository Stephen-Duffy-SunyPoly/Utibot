package uti.bot;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

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
                    String email = emailMapping.getAsString();
                    event.deferReply().queue();
                    UserManager.getInstance().add(new User(userId,name,email));
                    Main.competitionServer.addRoleToMember(event.getUser(),Main.getRegisteredRole()).queue();
                    event.getHook().deleteOriginal().queue();

                }
            }
        }
    }
}
