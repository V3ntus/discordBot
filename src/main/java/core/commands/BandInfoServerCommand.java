package core.commands;

import core.exceptions.LastFmException;
import core.imagerenderer.BandRendered;
import core.parsers.params.ArtistParameters;
import dao.ChuuService;
import dao.entities.AlbumUserPlays;
import dao.entities.ArtistAlbums;
import dao.entities.ScrobbledArtist;
import dao.entities.WrapperReturnNowPlaying;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.image.BufferedImage;
import java.util.List;

public class BandInfoServerCommand extends BandInfoCommand {
    public BandInfoServerCommand(ChuuService dao) {
        super(dao);
        this.respondInPrivate = false;
    }

    @Override
    protected CommandCategory getCategory() {
        return CommandCategory.SERVER_STATS;
    }

    @Override
    public String getDescription() {
        return "Like artist command but for all the users in the server";
    }

    @Override
    public List<String> getAliases() {
        return List.of("serverartist", "sa");
    }

    @Override
    public String getName() {
        return "Specific Artist Overview in a Server";
    }

    @Override
    void bandLogic(ArtistParameters ap) {


        boolean b = ap.hasOptional("--list");
        boolean b1 = ap.hasOptional("--pie");
        int limit = b || b1 ? Integer.MAX_VALUE : 4;
        ScrobbledArtist who = ap.getScrobbledArtist();
        List<AlbumUserPlays> userTopArtistAlbums = getService().getServerTopArtistAlbums(limit, who.getArtistId(), ap.getE().getGuild().getIdLong());
        MessageReceivedEvent e = ap.getE();

        WrapperReturnNowPlaying np = getService().whoKnows(who.getArtistId(), e.getGuild().getIdLong(), 5);
        np.getReturnNowPlayings().forEach(element ->
                element.setDiscordName(CommandUtil.getUserInfoNotStripped(e, element.getDiscordId()).getUsername())
        );
        BufferedImage logo = CommandUtil.getLogo(getService(), e);
        ArtistAlbums ai = new ArtistAlbums(who.getArtist(), userTopArtistAlbums);

        if (b) {
            doList(ap, ai);
            return;
        }
        if (b1) {
            doPie(ap, np, ai, logo);
            return;
        }
        long plays = getService().getServerArtistPlays(ap.getE().getGuild().getIdLong(), who.getArtistId());
        doImage(ap, np, ai, Math.toIntExact(plays), logo);
    }

    void doImage(ArtistParameters ap, WrapperReturnNowPlaying np, ArtistAlbums ai, int plays, BufferedImage logo) {
        BufferedImage returnedImage = BandRendered
                .makeBandImage(np, ai, plays, logo, ap.getE().getGuild().getName());
        sendImage(returnedImage, ap.getE());
    }
}
