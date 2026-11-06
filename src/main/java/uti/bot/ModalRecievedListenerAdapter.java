package uti.bot;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.EnumSet;

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

                case "add-problem" -> {
                    ModalMapping nameMapping = event.getValue("name");
                    ModalMapping subjectiveMapping = event.getValue("subjective");
                    ModalMapping higherIsBetterMapping = event.getValue("higher-is-better");
                    ModalMapping infoFileMapping = event.getValue("info-file");
                    ModalMapping judgeProgramMapping = event.getValue("judge-program");
                    if(nameMapping == null || subjectiveMapping == null || higherIsBetterMapping == null || infoFileMapping == null){
                        return;
                    }
                    String name = nameMapping.getAsString();
                    boolean subjective = subjectiveMapping.getAsBoolean();
                    boolean higherIsBetter = higherIsBetterMapping.getAsBoolean();
                    Message.Attachment infoFileAttachment = infoFileMapping.getAsAttachmentList().getFirst();
                    Message.Attachment judgeProgramAttachment = null;
                    if(!subjective && judgeProgramMapping == null){
                        return;
                    }
                    event.deferReply().queue();
                    long subjectiveChannelId = 0;
                    if(!subjective){
                        judgeProgramAttachment = judgeProgramMapping.getAsAttachmentList().getFirst();
                    } else {
                        //create a channel is subjective
                        Channel subjectievChannel = Main.competitionServer.createTextChannel("Problem "+name+" scoring",Main.competitionServer.getCategoryById(Main.config.getJudgeCategoryId()))
                                .syncPermissionOverrides().setTopic("Submissions for the problem "+name+" will appear here").complete();
                        subjectiveChannelId = subjectievChannel.getIdLong();
                    }

                    //create a problem folder
                    boolean ignored = new File("problems/"+name+"/").mkdirs();

                    //download and extract the info file into the folder
                    byte[] infoFileData = Downloader.downloadContent(infoFileAttachment.getUrl());
                    try {
                        Downloader.decompressZipBytesToDisc(infoFileData,"problems/"+name+"/");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    String judgeProgramPath = "";
                    if(!subjective) {
                        //if not subjective, download the judge program and put it in the folder
                        byte[] judgeProgramBytes = Downloader.downloadContent(judgeProgramAttachment.getUrl());
                        try (FileOutputStream fos = new FileOutputStream("problems/"+name+"/"+judgeProgramAttachment.getFileName())) {
                            fos.write(judgeProgramBytes);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        judgeProgramPath = judgeProgramAttachment.getFileName();
                    }

                    Problem problem = new Problem(name,higherIsBetter,subjective,infoFileAttachment.getUrl(),judgeProgramPath,subjectiveChannelId);
                    ProblemManager.getInstance().add(problem);

                    event.getHook().sendMessage("Created new problem "+name).queue();

                    //any other setup to run automatic submissions
                }
            }
        }
    }
}
