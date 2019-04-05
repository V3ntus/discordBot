package main.Commands;

import DAO.DaoImplementation;
import DAO.Entities.LastFMData;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.management.InstanceNotFoundException;
import java.text.ParseException;
import java.util.List;

public abstract class MyCommandDbAccess extends MyCommand {
	private DaoImplementation dao;

	public MyCommandDbAccess(DaoImplementation dao) {
		this.dao = dao;
	}

	public String getLastFmUsername1input(String[] message, Long id, MessageReceivedEvent event) throws ParseException {
		String username;
		try {
			if ((message.length > 1) || (message.length == 0)) {
				username = this.dao.findShow(id).getName();
			} else {
				//Caso con @ y sin @
				List<User> list = event.getMessage().getMentionedUsers();
				username = message[0];
				if (!list.isEmpty()) {
					LastFMData data = this.dao.findShow((list.get(0).getIdLong()));
					username = data.getName();
				}
				if (username.startsWith("@")) {
					event.getChannel().sendMessage("Trolled xD").queue();
				}
			}
		} catch (InstanceNotFoundException e) {
			throw new ParseException("a", 1);
		}
		return username;
	}

	public void userNotOnDB(MessageReceivedEvent event) {

		System.out.println("Problemo");
		event.getChannel().sendMessage("User doesnt have an account set").queue();


	}


	public DaoImplementation getDao() {
		return dao;
	}
}