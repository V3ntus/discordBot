package main.commands;

import dao.DaoImplementation;
import main.parsers.OnlyUsernameParser;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.management.InstanceNotFoundException;
import java.util.Collections;
import java.util.List;

public class TotalArtistNumberCommand extends ConcurrentCommand {
	public TotalArtistNumberCommand(DaoImplementation dao) {
		super(dao);
		this.parser = new OnlyUsernameParser(dao);
	}

	@Override
	public String getDescription() {
		return ("Artists count of user ");
	}

	@Override
	public List<String> getAliases() {
		return Collections.singletonList("scrobbled");
	}

	@Override
	protected void onCommand(MessageReceivedEvent e) {
		String[] returned = parser.parse(e);
		if (returned == null) {
			return;
		}
		String lastfm = returned[0];
		String username;
		try {
			long who = getDao().getDiscordIdFromLastfm(lastfm, e.getGuild().getIdLong());
			username = getUserString(who, e, lastfm);
		} catch (InstanceNotFoundException ex) {
			parser.sendError(parser.getErrorMessage(1), e);
			return;
		}
		int plays = getDao().getUserArtistCount(lastfm);
		sendMessageQueue(e, "**" + username + "** has scrobbled  **" + plays + "** " + "  different artists");

	}

	@Override
	public String getName() {
		return "Artist count ";
	}
}
