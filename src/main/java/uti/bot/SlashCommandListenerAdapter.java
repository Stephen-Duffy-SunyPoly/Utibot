package uti.bot;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.attachmentupload.AttachmentUpload;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.checkbox.Checkbox;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.FileType;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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
                case "team" -> handleTeamCommand(event);
                case "problem" -> handleProblemCommand(event);
            }
        } else {
            if(event.getName().equals("setup")){
                event.reply("Setting up server").queue();
                setupServer();
            }
        }
    }

    void handleProblemCommand(SlashCommandInteractionEvent event){

        switch (event.getSubcommandGroup()){
            case "edit" -> {
                switch (event.getSubcommandName()){
                    case "name" -> {}
                    case "higher-is-better" -> {}
                    case "info-file" -> {}
                    case "judge-program" -> {}
                    case null, default -> {}
                }
            }

            case null, default -> {
                switch (event.getSubcommandName()){
                    case "add" -> {
                        event.replyModal(Modal.create("add-problem","Create Problem")
                                .addComponents(
                                    Label.of("Name","The problem name",TextInput.create("name",TextInputStyle.SHORT).setRequired(true).setRequiredRange(2,40).build()),
                                    Label.of("Subjective","If this problem is subjectively scored", Checkbox.of("subjective")),
                                    Label.of("Higher is Better","If a higher score should be consider better",Checkbox.of("higher-is-better")),
                                    Label.of("Info file","zip file containing the problem description and test file", AttachmentUpload.create("info-file").setRequired(true).setFileTypes(FileType.ofExtension("zip")).build()),
                                    Label.of("Jude Program","Python program for scoring submissions, not necessary for subjective problems", AttachmentUpload.create("judge-program").setRequired(false).setFileTypes(FileType.ofExtension("py")).build())
                                )
                            .build()).queue();
                    }
                    case "delete" -> {
                        OptionMapping pmap = event.getOption("problem");
                        if(pmap==null){
                            return;
                        }
                        Problem problem = ProblemManager.getInstance().get(pmap.getAsString());
                        if(problem == null){
                            event.reply("A problem with that name does not exist").setEphemeral(true).queue();
                            return;
                        }
                        event.reply("Are you sure you want to delete "+problem.getName()).setEphemeral(true).addComponents(ActionRow.of(
                                Button.danger("delete-problem","Delete"),
                                Button.secondary("cancel","Cancel")
                        )).queue();
                    }
                    case null, default -> {}
                }
            }
        }
    }

    void handleTeamCommand(SlashCommandInteractionEvent event){
        //validate the option they chose
        User user = UserManager.getInstance().get(event.getUser().getIdLong());
        if(user == null){
            event.reply("You need to register for the event first.").setEphemeral(true).queue();
            return;
        }
        String command = event.getOption("command", OptionMapping::getAsString);
        if(command == null){
            return;
        }

        //if they are not on a team
        if(user.getTeam() == null) {
            if (command.equals("create")) {
                event.replyModal(Modal.create("create-team", "Create new Team")
                        .addComponents(Label.of("Team Name", "what to name the team", TextInput.create("name", TextInputStyle.SHORT).setRequiredRange(4, 25).setRequired(true).build()))
                        .build()).queue();
            } else {
                event.reply("That is not a valid team command at this time").setEphemeral(true).queue();
            }
        } else {
            switch (command) {
                case "leave" -> {
                    Team t = user.getTeam();
                    user.setTeam(null);
                    event.reply("You have left " + t.getName()).queue();
                    Main.competitionServer.removeRoleFromMember(event.getUser(),t.getRole()).queue();
                    if (t.getNumberOfMembers() == 0) {
                        TeamManager.getInstance().deleteTeam(t);
                    }
                }
                case "list" -> {
                    ArrayList<User> teamMemers = user.getTeam().getMembers();
                    StringBuilder response = new StringBuilder(user.getTeam().getName() + " has the following members: ");
                    for (User u : teamMemers) {
                        response.append("\n ").append(u.getName());
                    }
                    event.reply(response.toString()).queue();
                }
                case "invite" -> event.replyModal(Modal.create("team-invite", "Invite to Team")
                        .addComponents(
                                Label.of("User", EntitySelectMenu.create("user", EntitySelectMenu.SelectTarget.USER).setRequired(true).build())
                        ).build()).queue();
                default -> event.reply("That is not a valid team command at this time").setEphemeral(true).queue();
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
                .addRolePermissionOverride(atEveryone.getIdLong(),null,EnumSet.of(Permission.VIEW_CHANNEL))
                .complete();
        Main.config.setTeamCategoryId(teamCategory.getIdLong());

        //create the judge category
        //deny registered permission to view this category
        Category judgeCategory = Main.competitionServer.createCategory("Judge").addRolePermissionOverride(registerdRole.getIdLong(),
                        null,EnumSet.of(Permission.VIEW_CHANNEL))
                .addRolePermissionOverride(atEveryone.getIdLong(),null,EnumSet.of(Permission.VIEW_CHANNEL))
                .addMemberPermissionOverride(botId,EnumSet.of(Permission.VIEW_CHANNEL),null)
                .complete();
        Main.config.setJudgeCategoryId(judgeCategory.getIdLong());
        //create the admin channel in this category
        MessageChannel adminChannel = Main.competitionServer.createTextChannel("Admin",judgeCategory).setTopic("A private space for competition administration").syncPermissionOverrides().complete();
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
