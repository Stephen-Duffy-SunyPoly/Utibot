package uti.bot;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.util.EnumSet;
import java.util.Optional;

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
                    event.deferReply().setEphemeral(true).queue();
                    UserManager.getInstance().add(new User(userId,name,email));
                    Main.competitionServer.addRoleToMember(event.getUser(),Main.getRegisteredRole()).queue();
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
                    if(teamName.isEmpty()){
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
            }
        }
    }
}
