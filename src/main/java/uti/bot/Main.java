package uti.bot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.EnumSet;

public class Main {
    public static void main(String[] args) {
        final String TOKEN = System.getenv("token");
        if(TOKEN == null){
            System.err.println("Bot token not found, please provide your bot token through the 'token' environment variable");
            System.exit(1);
        }

        //load / generate config here

        JDA jda = JDABuilder.createLight(TOKEN, EnumSet.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT))
                .build();
    }
}
