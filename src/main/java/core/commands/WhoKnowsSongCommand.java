package core.commands;

import core.Chuu;
import core.apis.discogs.DiscogsApi;
import core.apis.discogs.DiscogsSingleton;
import core.apis.spotify.Spotify;
import core.apis.spotify.SpotifySingleton;
import core.exceptions.LastFmException;
import core.parsers.ArtistSongParser;
import core.parsers.OptionalEntity;
import core.parsers.Parser;
import core.parsers.params.ArtistAlbumParameters;
import dao.ChuuService;
import dao.entities.AlbumUserPlays;
import dao.entities.ReturnNowPlaying;
import dao.entities.Track;
import dao.entities.UsersWrapper;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WhoKnowsSongCommand extends WhoKnowsAlbumCommand {
    private final DiscogsApi discogsApi;
    private final Spotify spotify;

    public WhoKnowsSongCommand(ChuuService dao) {
        super(dao);
        this.discogsApi = DiscogsSingleton.getInstanceUsingDoubleLocking();
        this.spotify = SpotifySingleton.getInstance();
    }

    @Override
    protected CommandCategory getCategory() {
        return CommandCategory.SERVER_STATS;
    }

    @Override
    public Parser<ArtistAlbumParameters> getParser() {
        ArtistSongParser parser = new ArtistSongParser(getService(), lastFM, new OptionalEntity("--list", "display in list format")
                , new OptionalEntity("--pie", "display it as a chart pie"));
        parser.setExpensiveSearch(true);
        return parser;
    }

    @Override
    Map<UsersWrapper, Integer> fillPlayCounter(List<UsersWrapper> userList, String artist, String track, AlbumUserPlays fillWithUrl) throws LastFmException {
        Map<UsersWrapper, Integer> userMapPlays = new LinkedHashMap<>();
        UsersWrapper usersWrapper = userList.get(0);
        Track temp = lastFM.getTrackInfo(usersWrapper.getLastFMName(), artist, track);
        userMapPlays.put(usersWrapper, temp.getPlays());
        fillWithUrl.setAlbumUrl(temp.getImageUrl());

        fillWithUrl.setAlbum(temp.getName());
        fillWithUrl.setAlbumUrl(CommandUtil.getArtistImageUrl(getService(), artist, lastFM, discogsApi, spotify));
        userList.stream().skip(1).forEach(u -> {
            try {
                Track trackInfo = lastFM.getTrackInfo(u.getLastFMName(), artist, track);
                userMapPlays.put(u, trackInfo.getPlays());
            } catch (LastFmException ex) {
                Chuu.getLogger().warn(ex.getMessage(), ex);
            }
        });
        return userMapPlays;
    }

    @Override
    void doExtraThings(List<ReturnNowPlaying> list2, long id, long artistId, String album) {
        // Does nothing
    }

    @Override
    public String getDescription() {
        return "Get the list of people that have played a specific song on this server";
    }

    @Override
    public String getName() {
        return "Who Knows Song";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("wktrack", "whoknowstrack", "wkt");
    }
}
