package uti.bot;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;


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
                    } else {
                        List<AcChoice> options = List.of(new AcChoice("leave"),new AcChoice("invite"),new AcChoice("list members","list"),new AcChoice("get team rank","rank"));
                        //if the user is on a team
                        event.replyChoices(options.stream().filter(o -> o.name.contains(event.getFocusedOption().getValue().toLowerCase())).map(AcChoice::asCommandChoice).toList()).queue();
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
                    if(Main.config.hasCompStarted() && ! Main.config.hasCompEnded()) {
                        if (event.getFocusedOption().getName().equals("problem")) {
                            List<String> problemNameList = ProblemManager.getInstance().getNameList();
                            String userQuery = event.getFocusedOption().getValue().toLowerCase();
                            event.replyChoices(
                                    problemNameList.stream().filter(pName -> pName.toLowerCase().contains(userQuery)).map(name -> new Command.Choice(name, name)).toList()
                            ).queue();
                        }
                    } else {
                        event.replyChoices().queue();
                    }
                }
            }
        }
    }

    record AcChoice(String name,String value){
        public Command.Choice asCommandChoice(){
            return new Command.Choice(name,value);
        }
        public AcChoice(String name){
            this(name,name);
        }
    }
}
