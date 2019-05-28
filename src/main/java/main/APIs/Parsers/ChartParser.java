package main.APIs.Parsers;

import DAO.DaoImplementation;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

public class ChartParser extends DaoParser {
	public ChartParser(MessageReceivedEvent e, DaoImplementation dao) {
		super(e, dao);
	}

	private String getTimeFromChar(String timeFrame) {
		if (timeFrame.startsWith("y"))
			return "12month";
		if (timeFrame.startsWith("t"))
			return "3month";
		if (timeFrame.startsWith("m"))
			return "1month";
		if (timeFrame.startsWith("a"))
			return "overall";
		return "7day";
	}

	@Override
	public String[] parse() {
		String timeFrame = "w";
		String discordName;
		String x = "5";
		String y = "5";

		String pattern = "\\d+[xX]\\d+";
		String[] message = getSubMessage(e.getMessage());


		boolean flag = true;
		String[] message1 = Arrays.stream(message).filter(s -> !s.equals("--artist")).toArray(String[]::new);
		if (message1.length != message.length) {
			message = message1;
			flag = false;
		}
		if (message.length > 3) {
			sendError(getErrorMessage(1));
			return null;
		}

		Stream<String> firstStream = Arrays.stream(message).filter(s -> s.matches(pattern));
		Optional<String> opt = firstStream.filter(s -> s.matches(pattern)).findAny();
		if (opt.isPresent()) {
			x = (opt.get().split("[xX]")[0]);
			y = opt.get().split("[xX]")[1];
			message = Arrays.stream(message).filter(s -> !s.equals(opt.get())).toArray(String[]::new);

		}

		Stream<String> secondStream = Arrays.stream(message).filter(s -> s.length() == 1 && s.matches("[ytmwao]"));
		Optional<String> opt2 = secondStream.findAny();
		if (opt2.isPresent()) {
			timeFrame = opt2.get();
			message = Arrays.stream(message).filter(s -> !s.equals(opt2.get())).toArray(String[]::new);

		}

		discordName = getLastFmUsername1input(message, e.getAuthor().getIdLong(), e);
		if (discordName == null) {
			sendError(getErrorMessage(2));
			return null;
		}

		timeFrame = getTimeFromChar(timeFrame);
		return new String[]{x, y, discordName, timeFrame, Boolean.toString(flag)};
	}

	@Override
	public void setUpErrorMessages() {
		try {
			errorMessages.put(1, "You Introduced too many words");
			errorMessages.put(2, "User not on database");
			errorMessages.put(3, "Not a valid lastfm username");
			errorMessages.put(4, "Internal Server Error, try again later");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public String getErrorMessage(int code) {
		return errorMessages.get(code);
	}

}