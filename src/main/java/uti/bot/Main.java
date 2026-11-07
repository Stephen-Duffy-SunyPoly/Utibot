package uti.bot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;

import java.io.IOException;

public class Main {

    public static Guild competitionServer;
    public static ConfigManager config;

    public static void main(String[] args) throws InterruptedException {
        final String TOKEN = System.getenv("token");
        if(TOKEN == null){
            System.err.println("Bot token not found, please provide your bot token through the 'token' environment variable");
            System.exit(1);
        }

        //load / generate config here
        config = new ConfigManager();

        ContainerManager.init();

        ProblemManager.init();
        TeamManager.init();
        UserManager.init();

        try {
            ContainerManager.getInstance().buildLanguageContainers();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        JDA jda = JDABuilder.createLight(TOKEN)
                .addEventListeners(new SlashCommandListenerAdapter())
                .addEventListeners(new ButtonInteractionListenerAdapter())
                .addEventListeners(new ModalRecievedListenerAdapter())
                .addEventListeners(new AutocompleteListenerAdapter())
                .addEventListeners(new UserContextListenerAdapter())
                .build();

        jda.awaitReady();

        competitionServer = jda.getGuildById(config.getServerId());
        if(competitionServer == null){
            jda.shutdown();
            throw new RuntimeException("A valid server id was not provided in the config file!");
        }

        if(config.isServerSetup()){
            //register all the commands
            registerCommandsServerIsSetUp();
        } else {
            //only register the setup command
            registerCommandsServerNotSetUp();
        }
    }

    public static void registerCommandsServerNotSetUp(){
        competitionServer.updateCommands().addCommands(
                Commands.slash("setup","setup this server for a competition").setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
        ).queue(
                (e) -> {System.out.println("Commands successfully registered");},
                (e) -> {System.err.println("Failed to register commands!");e.printStackTrace();});
    }

    public static void registerCommandsServerIsSetUp(){
        competitionServer.updateCommands().addCommands(
                Commands.slash("reset","Reset this server to a similar state as when the competition was set up").setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
                Commands.slash("team","Manage your team status").addOption(OptionType.STRING,"command","The team specific command to execute", true, true),
                Commands.user("invite to team"),
                //each sub command is shown as a different command, this feels like an awful design decision
                Commands.slash("problem","Manage competition problems").setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                        .addSubcommands(
                                new SubcommandData("add","Create a new problem"),
                                new SubcommandData("delete","Delete a problem").addOption(OptionType.STRING,"problem","The problem to delete",true,true)
                        ).addSubcommandGroups(
                                new SubcommandGroupData("edit","Edit a problem").addSubcommands(
                                        new SubcommandData("name","Change the name of a problem").addOption(OptionType.STRING,"problem","The problem to edit",true,true),
                                        new SubcommandData("higher-is-better","Set if a higher score is better").addOption(OptionType.STRING,"problem","The problem to edit",true,true),
                                        new SubcommandData("info-file","change the info file that will distributed to the players").addOption(OptionType.STRING,"problem","The problem to edit",true,true),
                                        new SubcommandData("judge-program","change the python script used to score the submissions").addOption(OptionType.STRING,"problem","The problem to edit",true,true)
                                )
                        ),
                Commands.slash("submit","Submit a response for a problem").addOption(OptionType.STRING,  "problem", "The problem to submit for",true,true)
        ).queue((e) -> {System.out.println("Commands successfully registered");},
                (e) -> {System.err.println("Failed to register commands!");e.printStackTrace();});
    }

    public static Role getRegisteredRole(){
        if(!config.isServerSetup()){
            throw new RuntimeException("Attempted to get registered roll when server was not set up");
        }
        Role registered = competitionServer.getRoleById(config.getRegisteredRoleId());
        if(registered == null){
            throw new RuntimeException("Failed to find registered role!");
        }
        return registered;
    }
}
