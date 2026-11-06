package uti.bot;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class UserContextListenerAdapter extends ListenerAdapter {
    @Override
    public void onUserContextInteraction(UserContextInteractionEvent event) {
        if(event.getGuild() == null || event.getGuild().getIdLong() != Main.competitionServer.getIdLong()){
            return;
        }
        if(Main.config.isServerSetup()){
            if(event.getName().equals("invite to team")) {
                User user = UserManager.getInstance().get(event.getUser().getIdLong());
                if (user == null) {
                    event.reply("You need to register for the competition before you can use this feature").setEphemeral(true).queue();
                    return;
                }
                if (user.getTeam() == null) {
                    event.reply("You need to be on a team to use this feature").setEphemeral(true).queue();
                    return;
                }
                net.dv8tion.jda.api.entities.User discordInvitee = event.getTarget();
                User invitee = UserManager.getInstance().get(discordInvitee.getIdLong());
                if (invitee == null) {
                    event.reply("The target has not yet registered for the competition").setEphemeral(true).queue();
                    return;
                }
                if (invitee.getTeam() != null) {
                    event.reply("The target is already on a team").setEphemeral(true).queue();
                    return;
                }
                PrivateChannel userDm = discordInvitee.openPrivateChannel().complete();
                userDm.sendMessage("## Team Invite\n" + user.getName() + " has invited you to join the team:\n`" + user.getTeam().getName() + "`")
                        .addComponents(ActionRow.of(
                                Button.success("team-join-accept", "Accept"),
                                Button.danger("cancel", "Decline")
                        )).queue();
                event.reply(invitee.getName() + " has been invited to join your team, tell them to check their DMs").queue();
            }
        }
    }
}
