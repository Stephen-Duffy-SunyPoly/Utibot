package uti.bot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class Main {

    public static Guild competitionServer;
    public static ConfigManager config;

    public static void main(String[] args) throws InterruptedException {
        final String TOKEN = System.getenv("token");
        if(TOKEN == null){
            System.err.println("Bot token not found, please provide your bot token through the 'token' environment variable");
            System.exit(1);
        }

        config = new ConfigManager();
        TeamManager.init();
        UserManager.init();

        //load / generate config here

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
                Commands.user("invite to team")
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
