/*
 * Copyright (C) 2021 Chewbotcca
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package pw.chew.chewbotcca.commands.util;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.menu.EmbedPaginator;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import pw.chew.chewbotcca.util.JDAUtilUtil;
import pw.chew.chewbotcca.util.PropertiesManager;
import pw.chew.chewbotcca.util.ResponseHelper;
import pw.chew.chewbotcca.util.RestClient;

import java.util.Collections;

// %^define command
public class DefineCommand extends SlashCommand {

    public DefineCommand() {
        this.name = "define";
        this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.guildOnly = false;
        this.help = "Defines a word.";
        this.options = Collections.singletonList(
            new OptionData(OptionType.STRING, "word", "The word to define").setRequired(true)
        );
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        // Get the word from the command
        String word = ResponseHelper.guaranteeStringOption(event, "word", "");

        // Send message then edit it
        event.replyEmbeds(new EmbedBuilder().setDescription("Checking the dictionary...").build()).queue(interactionHook -> {
            interactionHook.retrieveOriginal().queue(message -> {
                buildPaginator(word, event.getUser()).paginate(message, 1);
            });
        });
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        // Get the word from the command
        String word = commandEvent.getArgs();

        // Send message then edit it
        buildPaginator(word, commandEvent.getAuthor()).paginate(commandEvent.getChannel(), 1);
    }

    private EmbedPaginator buildPaginator(String word, User author) {
        // Attempt to grab the word, if it doesn't exist let them know
        JSONArray grabbedword;
        try {
            grabbedword = new JSONArray(RestClient.get("https://api.wordnik.com/v4/word.json/" + word + "/definitions?includeRelated=true&useCanonical=false&includeTags=false&api_key=" + PropertiesManager.getWordnikToken()));
        } catch(JSONException e) {
            throw new IllegalArgumentException("Word not found! Check your local spell-checker!");
        }

        EmbedPaginator.Builder paginator = JDAUtilUtil.makeEmbedPaginator();
        paginator.setUsers(author);

        int definitions = grabbedword.length();

        for(int i = 0; i < definitions; i++) {
            JSONObject definition = grabbedword.getJSONObject(i);

            String defined = "*No definition available, try navigating with the reactions below.*";

            if (definition.has("text")) {
                defined = definition.getString("text")
                    // Get rid of xrefs and other unused tags entirely
                    .replaceAll("<xref (.*?)>(.*?)<\\/xref>", "$2")
                    .replaceAll("<internalXref (.*?)>(.*?)<\\/internalXref>", "$2")
                    .replaceAll("<xref>(.*?)<\\/xref>", "$1")
                    .replaceAll("<altname>(.*?)<\\/altname>", "$1")
                    .replaceAll("<spn>(.*?)<\\/spn>", "$1")
                    .replaceAll("<stype>(.*?)<\\/stype>", "$1")
                    // Italics
                    .replaceAll("<em>(.*?)</em>", "*$1*")
                    .replaceAll("<i>(.*?)</i>", "*$1*")
                    // Bold
                    .replaceAll("<strong>(.*?)</strong>", "**$1**");
            }

            // Build the definition embed
            EmbedBuilder e = new EmbedBuilder()
                .setTitle("Definition for " + word, definition.getString("wordnikUrl"))
                .setColor(0xd084)
                .setDescription(defined)
                .setAuthor("Dictionary", null, "https://icons.iconarchive.com/icons/johanchalibert/mac-osx-yosemite/1024/dictionary-icon.png");

            // Only put part of speech if there is one
            if (definition.has("partOfSpeech")) {
                e.addField("Part of Speech", definition.getString("partOfSpeech"), true);
            }

            e.setFooter("Definition " + (i+1) + "/" + definitions);

            paginator.addItems(e.build());
        }
        // Send it off!
        paginator.setText("");
        return paginator.build();
    }
}
