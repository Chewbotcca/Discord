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
package pw.chew.chewbotcca.commands.info;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import pw.chew.chewbotcca.util.ResponseHelper;
import pw.chew.chewbotcca.util.RestClient;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.Locale;

// %^rubygem command
public class RubyGemsCommand extends SlashCommand {

    public RubyGemsCommand() {
        this.name = "rubygem";
        this.help = "Searches for and returns some basic info about a specified ruby gem";
        this.aliases = new String[]{"gem", "rgem", "rubyg", "gems"};
        this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.guildOnly = false;
        this.options = Collections.singletonList(
            new OptionData(OptionType.STRING, "gem", "The gem to lookup").setRequired(true)
        );
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        try {
            event.replyEmbeds(gatherGemData(ResponseHelper.guaranteeStringOption(event, "gem", ""))).queue();
        } catch (IllegalArgumentException e) {
            event.replyEmbeds(ResponseHelper.generateFailureEmbed(null, e.getMessage())).setEphemeral(true).queue();
        }
    }

    @Override
    protected void execute(CommandEvent event) {
        // Start typing
        event.getChannel().sendTyping().queue();

        try {
            event.reply(gatherGemData(event.getArgs().trim()));
        } catch (IllegalArgumentException e) {
            event.replyWarning(e.getMessage());
        }
    }

    private MessageEmbed gatherGemData(String name) {
        JSONObject data;
        // Get gem if it exists
        try {
            data = new JSONObject(RestClient.get("https://rubygems.org/api/v1/gems/" + name + ".json"));
        } catch (JSONException e) {
            throw new IllegalArgumentException("Invalid ruby gem!");
        }
        // Get ranking from best gems
        int rank = -1;
        try {
            rank = new JSONArray(RestClient.get("https://bestgems.org/api/v1/gems/" + name + "/total_ranking.json")).getJSONObject(0).getInt("total_ranking");
        } catch (JSONException ignored) {
        }

        // Get info
        EmbedBuilder e = new EmbedBuilder();
        e.setTitle(data.getString("name") + " (" + data.getString("version") + ")", data.getString("project_uri"));
        e.setAuthor("RubyGem Information", null, "https://cdn.discordapp.com/emojis/232899886419410945.png");
        e.setDescription(data.getString("info"));

        e.addField("Authors", data.getString("authors"), true);

        e.addField("Downloads", "For Version: " + NumberFormat.getNumberInstance(Locale.US).format(data.getInt("version_downloads")) + "\n" +
            "Total: " + NumberFormat.getNumberInstance(Locale.US).format(data.getInt("downloads")), true);

        if (!data.isNull("licenses"))
            e.addField("License", data.getJSONArray("licenses").getString(0), true);
        if (rank > -1)
            e.addField("Rank", "#" + NumberFormat.getNumberInstance(Locale.US).format(rank), true);
        else
            e.addField("Rank", "Not Ranked Yet", true);

        // Get links
        StringBuilder links = new StringBuilder();
        links.append("[Project](").append(data.getString("project_uri")).append(")\n");
        links.append("[Gem](").append(data.getString("gem_uri")).append(")\n");
        if(!data.isNull("documentation_uri"))
            links.append("[Documentation](").append(data.getString("documentation_uri")).append(")\n");
        if(!data.isNull("homepage_uri"))
            links.append("[Homepage](").append(data.getString("homepage_uri")).append(")\n");
        if(!data.isNull("wiki_uri"))
            links.append("[Wiki](").append(data.getString("wiki_uri")).append(")\n");
        if(!data.isNull("mailing_list_uri"))
            links.append("[Mailing List](").append(data.getString("mailing_list_uri")).append(")\n");
        if(!data.isNull("source_code_uri"))
            links.append("[Source Code](").append(data.getString("source_code_uri")).append(")\n");
        if(!data.isNull("bug_tracker_uri"))
            links.append("[Bug Tracker](").append(data.getString("bug_tracker_uri")).append(")\n");
        if(!data.isNull("changelog_uri"))
            links.append("[Changelog](").append(data.getString("changelog_uri")).append(")");

        e.addField("Links", links.toString(), true);

        return e.build();
    }
}
