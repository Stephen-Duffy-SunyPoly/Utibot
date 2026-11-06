package uti.bot;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;

public class SlashCommandListenerAdapter extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if(event.getGuild() == null || event.getGuild().getIdLong() != Main.competitionServer.getIdLong()){
            return;
        }
        if(Main.config.isServerSetup()){

        } else {
            if(event.getName().equals("setup")){
                event.reply("Setting up server").queue();
                setupServer();
            }
        }
    }

    void setupServer(){
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
                registerChannelEveryoneDeny).addMemberPermissionOverride(registerdRole.getJDA().getSelfUser().getIdLong(), EnumSet.of(Permission.MESSAGE_SEND),null)
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

        //create the judge category
        //deny registered permission to view this category
        //create the admin channel in this category



        Main.config.setServerSetup(true);
    }
}
