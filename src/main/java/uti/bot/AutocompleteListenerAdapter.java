package uti.bot;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;

import java.util.EnumSet;
import java.util.List;

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
                        event.replyChoices(new Command.Choice("leave","leave"), new Command.Choice("invite","invite"), new Command.Choice("list members","list")).queue();
                    }
                }
                case "problem" -> {
                    if(event.getFocusedOption().getName().equals("problem")){
                        List<String> problemNameList = ProblemManager.getInstance().getNameList();
                        String userQuery = event.getFocusedOption().getValue().toLowerCase();
                        event.replyChoices(
                                problemNameList.stream().filter(pName->pName.toLowerCase().contains(userQuery)).map(name -> new Command.Choice(name,name)).toList()
                        ).queue();
                    }
                }

                case "submit" -> {
                    //TODO check if the comp has started yet
                    if(event.getFocusedOption().getName().equals("problem")){
                        List<String> problemNameList = ProblemManager.getInstance().getNameList();
                        String userQuery = event.getFocusedOption().getValue().toLowerCase();
                        event.replyChoices(
                                problemNameList.stream().filter(pName->pName.toLowerCase().contains(userQuery)).map(name -> new Command.Choice(name,name)).toList()
                        ).queue();
                    }
                }
            }
        }
    }
}
