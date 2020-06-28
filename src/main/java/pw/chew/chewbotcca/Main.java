package pw.chew.chewbotcca;

import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import io.sentry.Sentry;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.discordbots.api.client.DiscordBotListAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pw.chew.chewbotcca.commands.FeedbackCommand;
import pw.chew.chewbotcca.commands.LastFMCommand;
import pw.chew.chewbotcca.commands.MixerCommand;
import pw.chew.chewbotcca.commands.RubyGemsCommand;
import pw.chew.chewbotcca.commands.about.HelpCommand;
import pw.chew.chewbotcca.commands.about.InviteCommand;
import pw.chew.chewbotcca.commands.about.PingCommand;
import pw.chew.chewbotcca.commands.about.StatsCommand;
import pw.chew.chewbotcca.commands.english.DefineCommand;
import pw.chew.chewbotcca.commands.english.UrbanDictionaryCommand;
import pw.chew.chewbotcca.commands.fun.*;
import pw.chew.chewbotcca.commands.github.GHIssueCommand;
import pw.chew.chewbotcca.commands.github.GHRepoCommand;
import pw.chew.chewbotcca.commands.github.GHUserCommand;
import pw.chew.chewbotcca.commands.google.YouTubeCommand;
import pw.chew.chewbotcca.commands.info.*;
import pw.chew.chewbotcca.commands.minecraft.MCPHNodesCommand;
import pw.chew.chewbotcca.commands.minecraft.MCServerCommand;
import pw.chew.chewbotcca.commands.minecraft.MCStatusCommand;
import pw.chew.chewbotcca.commands.minecraft.MCUserCommand;
import pw.chew.chewbotcca.commands.owner.NewIssueCommand;
import pw.chew.chewbotcca.commands.owner.ShutdownCommand;
import pw.chew.chewbotcca.commands.quotes.AcronymCommand;
import pw.chew.chewbotcca.commands.quotes.QuoteCommand;
import pw.chew.chewbotcca.commands.quotes.TRBMBCommand;
import pw.chew.chewbotcca.commands.settings.ProfileCommand;
import pw.chew.chewbotcca.commands.settings.ServerSettingsCommand;
import pw.chew.chewbotcca.listeners.MagReactListener;
import pw.chew.chewbotcca.listeners.SendJoinedOrLeftGuildListener;

import javax.security.auth.login.LoginException;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Properties;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    static Properties prop = new Properties();
    public static JDA jda;
    public static Instant start;
    public static EventWaiter waiter;
    public static DiscordBotListAPI topgg;

    public static void main(String[] args) throws LoginException, IOException {
        prop.load(new FileInputStream("bot.properties"));

        Sentry.init(prop.getProperty("sentry-dsn")).setEnvironment(prop.getProperty("sentry-env"));

        topgg = new DiscordBotListAPI.Builder()
                .token(prop.getProperty("dbl"))
                .botId(prop.getProperty("client_id"))
                .build();

        waiter = new EventWaiter();

        CommandClientBuilder client = new CommandClientBuilder();

        client.useDefaultGame();
        client.setOwnerId(prop.getProperty("owner_id"));

        // Set your bot's prefix
        logger.info("Setting Prefix to " + prop.getProperty("prefix"));
        client.setPrefix(prop.getProperty("prefix"));

        client.useHelpBuilder(false);

        // Register commands
        client.addCommands(
                // About Module
                new HelpCommand(),
                new InviteCommand(),
                new PingCommand(),
                new StatsCommand(),

                // English Module
                new DefineCommand(),
                new UrbanDictionaryCommand(),

                // Fun Module
                new CatCommand(),
                new CatFactCommand(),
                new DogCommand(),
                new EightBallCommand(),
                new NumberFactCommand(),
                new QRCodeCommand(),
                new RollCommand(),

                // GitHub Module
                new GHIssueCommand(),
                new GHRepoCommand(),
                new GHUserCommand(),

                // Google Module
                new YouTubeCommand(),

                // Info Module
                new BotInfoCommand(),
                new ChannelInfoCommand(),
                new InfoCommand(),
                new RoleInfoCommand(),
                new ServerInfoCommand(),
                new UserInfoCommand(),

                // Minecraft Module
                new MCPHNodesCommand(),
                new MCServerCommand(),
                new MCStatusCommand(),
                new MCUserCommand(),

                // Owner Module
                new NewIssueCommand(),
                new ShutdownCommand(),

                // Quotes Module
                new AcronymCommand(),
                new QuoteCommand(),
                new TRBMBCommand(),

                // Settings Module
                new ProfileCommand(),
                new ServerSettingsCommand(),

                // Everything Else
                new FeedbackCommand(),
                new LastFMCommand(),
                new MixerCommand(),
                new RubyGemsCommand()
        );

        // Register JDA
        jda = JDABuilder.createDefault(prop.getProperty("token"))
                .setChunkingFilter(ChunkingFilter.ALL)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .enableIntents(GatewayIntent.GUILD_PRESENCES)
                .enableCache(CacheFlag.ACTIVITY)
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.playing("Booting..."))
                .addEventListeners(waiter, client.build())
                .build();

        start = Instant.now();

        // Register listeners
        jda.addEventListener(
                new MagReactListener(),
                new SendJoinedOrLeftGuildListener()
        );
    }

    public static JDA getJDA() {
        return jda;
    }

    public static EventWaiter getWaiter() {
        return waiter;
    }

    public static Properties getProp() {
        return prop;
    }
}