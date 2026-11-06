package uti.bot;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class AutocompleteListenerAdapter extends ListenerAdapter {
    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if(event.getGuild() == null || event.getGuild().getIdLong() != Main.competitionServer.getIdLong()){
            return;
        }

        if(Main.config.isServerSetup()){
            switch (event.getName()){
                case "team" -> {
                    User user = UserManager.getInstance().get(event.getUser().getIdLong());
                    if(user == null){
                        //they have no options!!!
                        event.replyChoices().queue();
                        return;
                    }
                    if(user.getTeam() == null) {
                        event.replyChoice("create", "create").queue();
                        return;
                    } else {
                        //if the user is on a team
                    }
                }
            }
        }
    }
}
