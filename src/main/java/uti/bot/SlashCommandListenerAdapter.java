package uti.bot;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
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
            switch (event.getName()){
                case "reset" ->resetServer(event);
            }
        } else {
            if(event.getName().equals("setup")){
                event.reply("Setting up server").queue();
                setupServer();
            }
        }
    }

    void setupServer(){
        long botId = Main.competitionServer.getJDA().getSelfUser().getIdLong();
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
                registerChannelEveryoneDeny).addMemberPermissionOverride(botId, EnumSet.of(Permission.MESSAGE_SEND),null)
                .setTopic("Register your self for the competition here")
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
        Category teamCategory = Main.competitionServer.createCategory("Team").addRolePermissionOverride(registerdRole.getIdLong(),
                null,EnumSet.of(Permission.VIEW_CHANNEL))
                .addMemberPermissionOverride(botId,EnumSet.of(Permission.VIEW_CHANNEL),null)
                .complete();
        Main.config.setTeamCategoryId(teamCategory.getIdLong());

        //create the judge category
        //deny registered permission to view this category
        Category judgeCategory = Main.competitionServer.createCategory("Judge").addRolePermissionOverride(registerdRole.getIdLong(),
                        null,EnumSet.of(Permission.VIEW_CHANNEL))
                .addMemberPermissionOverride(botId,EnumSet.of(Permission.VIEW_CHANNEL),null)
                .complete();
        Main.config.setJudgeCategoryId(judgeCategory.getIdLong());
        //create the admin channel in this category
        MessageChannel adminChannel = Main.competitionServer.createTextChannel("Admin",judgeCategory).setTopic("A private space for competition administration").complete();
        Main.config.setAdminChannelId(adminChannel.getIdLong());
        adminChannel.sendMessage("This is the admin channel. Use this channel to configure and administration the challenge. Users can not see this channel.").queue();



        Main.config.setServerSetup(true);
        Main.registerCommandsServerIsSetUp();
    }

    void resetServer(SlashCommandInteractionEvent event){
        event.reply("# Are You Sure?\nThis action will ***DELETE*** all completion channels including:\n- all team chatter\n- any subjective judging\n- any administration commands.\n- any roles created by this bot\n\nAny competition data stored by the bot will deleted including:\n- teams\n- problem data\n- user registration information\n\nAre you sure you want to proceed?")
                .addComponents(ActionRow.of(
                        Button.danger("reset-server","Proceed"),
                        Button.secondary("cancel","Cancel")
                )).setEphemeral(true).queue();
    }
}
