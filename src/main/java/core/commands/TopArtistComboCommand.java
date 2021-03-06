package core.commands;

import core.apis.discogs.DiscogsApi;
import core.apis.discogs.DiscogsSingleton;
import core.apis.spotify.Spotify;
import core.apis.spotify.SpotifySingleton;
import core.exceptions.InstanceNotFoundException;
import core.exceptions.LastFmException;
import core.otherlisteners.Reactionary;
import core.parsers.*;
import core.parsers.params.ArtistParameters;
import core.parsers.params.NumberParameters;
import dao.ChuuService;
import dao.entities.GlobalStreakEntities;
import dao.entities.PrivacyMode;
import dao.entities.ScrobbledArtist;
import dao.entities.UsersWrapper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static core.parsers.ExtraParser.LIMIT_ERROR;

public class TopArtistComboCommand extends ConcurrentCommand<NumberParameters<ArtistParameters>> {

    private final DiscogsApi discogsApi;
    private final Spotify spotify;

    public TopArtistComboCommand(ChuuService dao) {
        super(dao);
        discogsApi = DiscogsSingleton.getInstanceUsingDoubleLocking();
        spotify = SpotifySingleton.getInstance();
    }

    @Override
    protected CommandCategory getCategory() {
        return CommandCategory.BOT_STATS;
    }

    @Override
    public Parser<NumberParameters<ArtistParameters>> getParser() {
        Map<Integer, String> map = new HashMap<>(2);
        map.put(LIMIT_ERROR, "The number introduced must be positive and not very big");
        String s = "You can also introduce a number to only get streak with more than that number of plays. ";
        NumberParser<ArtistParameters, ArtistParser> artistParametersArtistParserNumberParser = new NumberParser<>(new ArtistParser(getService(), lastFM),
                null,
                Integer.MAX_VALUE,
                map, s, false, true, true);
        artistParametersArtistParserNumberParser.addOptional(new OptionalEntity("--server", "only include people in this server"));
        return artistParametersArtistParserNumberParser;
    }

    @Override
    public String getDescription() {
        return "List of the top streaks for a specific artist in the bot";
    }

    @Override
    public List<String> getAliases() {
        return List.of("artistcombo", "artiststreaks", "acombo", "astreak", "streaka", "comboa");
    }

    @Override
    public String getName() {
        return "Top Artist Streaks";
    }

    @Override
    void onCommand(MessageReceivedEvent e) throws LastFmException, InstanceNotFoundException {
        NumberParameters<ArtistParameters> params = parser.parse(e);
        Long author = e.getAuthor().getIdLong();
        if (params == null) {
            return;
        }
        Long guildId = null;
        String title;
        if (e.isFromGuild() && params.hasOptional("--server")) {
            Guild guild = e.getGuild();
            guildId = guild.getIdLong();
            title = guild.getName();
        } else {
            SelfUser selfUser = e.getJDA().getSelfUser();
            title = selfUser.getName();
        }
        ArtistParameters innerParams = params.getInnerParams();
        ScrobbledArtist scrobbledArtist = new ScrobbledArtist(innerParams.getArtist(), 0, "");
        CommandUtil.validate(getService(), scrobbledArtist, lastFM, discogsApi, spotify, true, !innerParams.isNoredirect());
        List<GlobalStreakEntities> topStreaks = getService().getArtistTopStreaks(params.getExtraParam(), guildId, scrobbledArtist.getArtistId());

        Set<Long> showableUsers;
        if (params.getE().isFromGuild()) {
            showableUsers = getService().getAll(params.getE().getGuild().getIdLong()).stream().map(UsersWrapper::getDiscordID).collect(Collectors.toSet());
            showableUsers.add(author);
        } else {
            showableUsers = Set.of(author);
        }
        AtomicInteger atomicInteger = new AtomicInteger(1);
        AtomicInteger positionCounter = new AtomicInteger(1);

        Consumer<GlobalStreakEntities> consumer = (x) -> {
            PrivacyMode privacyMode = x.getPrivacyMode();
            if (showableUsers.contains(x.getDiscordId())) {
                privacyMode = PrivacyMode.DISCORD_NAME;
            }
            int andIncrement = positionCounter.getAndIncrement();
            String dayNumberSuffix = CommandUtil.getDayNumberSuffix(andIncrement);
            switch (privacyMode) {

                case STRICT:
                case NORMAL:
                    x.setCalculatedDisplayName(dayNumberSuffix + " **Private User #" + atomicInteger.getAndIncrement() + "**");
                    break;
                case DISCORD_NAME:
                    x.setCalculatedDisplayName(dayNumberSuffix + " **" + getUserString(params.getE(), x.getDiscordId()) + "**");
                    break;
                case TAG:
                    x.setCalculatedDisplayName(dayNumberSuffix + " **" + params.getE().getJDA().retrieveUserById(x.getDiscordId()).complete().getAsTag() + "**");
                    break;
                case LAST_NAME:
                    x.setCalculatedDisplayName(dayNumberSuffix + " **" + x.getLastfmId() + " (last.fm)**");
                    break;
            }

        };
        topStreaks
                .forEach(x ->
                        x.setDisplayer(consumer)
                );
        atomicInteger.set(0);
        topStreaks.forEach(x -> x.setDisplayer(consumer));
        if (topStreaks.isEmpty()) {
            sendMessageQueue(e, title + " doesn't have any stored streaks for " + scrobbledArtist.getArtist());
            return;
        }

        StringBuilder a = new StringBuilder();
        for (int i = 0; i < 5 && i < topStreaks.size(); i++) {
            a.append(i + 1).append(topStreaks.get(i).toString());
        }

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .
                        setAuthor(String.format("%s's top streaks in %s ", scrobbledArtist.getArtist(), CommandUtil.cleanMarkdownCharacter(title)))
                .setThumbnail(scrobbledArtist.getUrl())
                .setDescription(a)
                .setFooter(String.format("%s has a total of %d %s %s!", CommandUtil.cleanMarkdownCharacter(title), topStreaks.size(), scrobbledArtist.getArtist(), CommandUtil.singlePlural(topStreaks.size(), "streak", "streaks")));
        MessageBuilder messageBuilder = new MessageBuilder();
        e.getChannel().sendMessage(messageBuilder.setEmbed(embedBuilder.build()).build()).queue(message1 ->
                new Reactionary<>(topStreaks, message1, 5, embedBuilder));
    }
}
