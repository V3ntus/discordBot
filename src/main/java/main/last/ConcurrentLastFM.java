package main.last;

import DAO.Entities.ArtistData;
import DAO.Entities.NowPlayingArtist;
import DAO.Entities.UrlCapsule;
import DAO.Entities.UserInfo;
import main.Exceptions.LastFMNoPlaysException;
import main.Exceptions.LastFMServiceException;
import main.Exceptions.LastFmUserNotFoundException;
import main.ImageRenderer.CollageMaker;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;


public class ConcurrentLastFM {//implements LastFMService {
	private static final String API_KEY = "&api_key=fdd31e327054d877dc77c85d3fd5cdf8";
	private static final String BASE = "http://ws.audioscrobbler.com/2.0/";
	private static final String GET_ALBUMS = "?method=user.gettopalbums&user=";
	private static final String GET_LIBRARY = "?method=library.getartists&user=";
	private static final String GET_USER = "?method=user.getinfo&user=";
	private static final String ending = "&format=json";
	private static final String GET_NOW_PLAYINH = "?method=user.getrecenttracks&limit=1&user=";
	private static final String GET_ALL = "?method=user.getrecenttracks&limit=200&user=";

	private static final String GET_ARTIST = "?method=user.gettopartists&user=";
	private static final String GET_TRACKS = "?method=user.getartisttracks&user=";
	private static final String GET_CORRECTION = "?method=artist.getcorrection&artist=";

	//@Override
	public static NowPlayingArtist getNowPlayingInfo(String user) throws LastFMServiceException, LastFMNoPlaysException {
		HttpClient client = new HttpClient();
		String url = BASE + GET_NOW_PLAYINH + user + API_KEY + ending;
		HttpMethodBase method = createMethod(url);
		try {
			int statusCode = client.executeMethod(method);
			if (statusCode != HttpStatus.SC_OK) {
				System.err.println("Method failed: " + method.getStatusLine());
				throw new LastFMServiceException("Error in the service: " + method.getStatusLine());
			}
			byte[] responseBody = method.getResponseBody();
			JSONObject obj = new JSONObject(new String(responseBody));
			obj = obj.getJSONObject("recenttracks");
			JSONObject attrObj = obj.getJSONObject("@attr");
			if (attrObj.getInt("total") == 0) {
				throw new LastFMNoPlaysException(user);
			}
			boolean nowPlayin;


			JSONObject tracltObj = obj.getJSONArray("track").getJSONObject(0);

			try {
				nowPlayin = tracltObj.getJSONObject("@attr").getBoolean("nowplaying");
			} catch (JSONException e) {
				nowPlayin = false;
			}
			JSONObject artistObj = tracltObj.getJSONObject("artist");
			String artistname = artistObj.getString("#text");
			String mbid = artistObj.getString("mbid");

			String albumName = tracltObj.getJSONObject("album").getString("#text");
			String songName = tracltObj.getString("name");
			String image_url = tracltObj.getJSONArray("image").getJSONObject(2).getString("#text");

			return new NowPlayingArtist(artistname, mbid, nowPlayin, albumName, songName, image_url);
		} catch (IOException e) {
			throw new LastFMServiceException("Error in the service: " + method.getStatusLine());
		}

	}

	public static TimestampWrapper<LinkedList<ArtistData>> getWhole(String user, int timestampQuery) throws LastFMServiceException, LastFMNoPlaysException {
		HttpClient client = new HttpClient();
		List<NowPlayingArtist> list = new ArrayList<>();
		String url = BASE + GET_ALL + user + API_KEY + ending;

		if (timestampQuery != 0)
			url += "&from=" + (timestampQuery + 1);

		int timestamp = 0;
		int page = 1;
		int pages = 2;
		while (page <= pages) {

			String urlPage = url + "&page=" + page;
			HttpMethodBase method = createMethod(urlPage);


			try {

				int statusCode = client.executeMethod(method);
				if (statusCode != HttpStatus.SC_OK) {
					System.err.println("Method failed: " + method.getStatusLine());
					throw new LastFMServiceException("Error in the service: " + method.getStatusLine());
				}
				byte[] responseBody = method.getResponseBody();
				JSONObject obj = new JSONObject(new String(responseBody));
				obj = obj.getJSONObject("recenttracks");
				JSONObject attrObj = obj.getJSONObject("@attr");
				System.out.println("Plays " + attrObj.getInt("total"));

				if (attrObj.getInt("total") == 0) {
					throw new LastFMNoPlaysException(user);
				}

				if (page++ == 1) {
					pages = obj.getJSONObject("@attr").getInt("totalPages");
					timestamp = obj.getJSONArray("track").getJSONObject(0).getJSONObject("date").getInt("uts");
				}


				JSONArray arr = obj.getJSONArray("track");
				for (int i = 0; i < arr.length(); i++) {
					JSONObject tracltObj = arr.getJSONObject(i);
					JSONObject artistObj = tracltObj.getJSONObject("artist");
					String artistname = artistObj.getString("#text");

					String albumName = tracltObj.getJSONObject("album").getString("#text");
					String songName = tracltObj.getString("name");
					JSONArray images = tracltObj.getJSONArray("image");
					String image_url = images.getJSONObject(images.length() - 1).getString("#text");
					list.add(new NowPlayingArtist(artistname, "", false, albumName, songName, image_url));
				}

			} catch (IOException e) {
				throw new LastFMServiceException("Error in the service: " + method.getStatusLine());
			} catch (JSONException e) {
				throw new LastFMNoPlaysException("a");
			}
		}
		Map<String, Long> a = list.stream().collect(Collectors.groupingBy(NowPlayingArtist::getArtistName, Collectors.counting()));
		return new TimestampWrapper<>(
				a.entrySet().stream().map(
						entry -> {
							String artist = entry.getKey();
							String tempUrl;
							Optional<NowPlayingArtist> r = list.stream().filter(t -> t.getArtistName().equals(artist)).findAny();
							tempUrl = r.map(NowPlayingArtist::getUrl).orElse(null);
							return new
									ArtistData(entry.getKey(), entry.getValue().intValue(), tempUrl);
						})
						.collect(Collectors.toCollection(LinkedList::new)), timestamp);


	}

	//@Override
	public static List<UserInfo> getUserInfo(List<String> lastFmNames) throws LastFMServiceException {
		HttpClient client = new HttpClient();
		List<UserInfo> returnList = new ArrayList<>();

		try {

			for (String lastFmName : lastFmNames) {
				String url = BASE + GET_USER + lastFmName + API_KEY + ending;
				HttpMethodBase method = createMethod(url);
				int statusCode = client.executeMethod(method);
				if (statusCode != HttpStatus.SC_OK) {
					System.err.println("Method failed: " + method.getStatusLine());
					throw new LastFMServiceException("Error in the service: " + method.getStatusLine());
				}
				byte[] responseBody = method.getResponseBody();
				JSONObject obj = new JSONObject(new String(responseBody));
				obj = obj.getJSONObject("user");
				JSONArray image = obj.getJSONArray("image");
				JSONObject bigImage = image.getJSONObject(2);
				String image2 = bigImage.getString("#text");
				int playcount = obj.getInt("playcount");
				returnList.add(new UserInfo(playcount, image2, lastFmName));

			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Execute the method.


		// Read the response body.


		return returnList;

	}

	//@Override
	public static LinkedList<ArtistData> getLibrary(String User) throws LastFMServiceException, LastFMNoPlaysException {
		String url = BASE + GET_LIBRARY + User + API_KEY + ending;
		int page = 1;
		int pages = 1;
		HttpClient client = new HttpClient();
		url += "&limit=500";

		LinkedList<ArtistData> linkedlist = new LinkedList<>();
		Map<String, Integer> map = new HashMap<>();
		while (page <= pages) {

			String urlPage = url + "&page=" + page;
			HttpMethodBase method = createMethod(urlPage);


			try {

				// Execute the method.
				int statusCode = client.executeMethod(method);

				if (statusCode != HttpStatus.SC_OK) {
					System.err.println("Method failed: " + method.getStatusLine());
					throw new LastFMServiceException("Error in the service: " + method.getStatusLine());
				}

				// Read the response body.
				byte[] responseBody = method.getResponseBody();
				JSONObject obj = new JSONObject(new String(responseBody));
				obj = obj.getJSONObject("artists");
				if (page++ == 1) {
					pages = obj.getJSONObject("@attr").getInt("totalPages");

				}

				JSONArray arr = obj.getJSONArray("artist");
				for (int i = 0; i < arr.length(); i++) {
					JSONObject artistObj = arr.getJSONObject(i);
					String mbid = artistObj.getString("name");

					int count = artistObj.getInt("playcount");
					JSONArray image = artistObj.getJSONArray("image");

					JSONObject bigImage = image.getJSONObject(image.length() - 1);

					linkedlist.add(new ArtistData(mbid, count, bigImage.getString("#text")));
				}
			} catch (JSONException e) {
				throw new LastFMNoPlaysException(e.getMessage());

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return linkedlist;
	}

	public static byte[] getUserList(String userName, String weekly, int x, int y, boolean isAlbum) throws
			LastFMServiceException, LastFmUserNotFoundException {

		String apiMethod;
		String leadingObject;
		String arrayObject;
		if (isAlbum) {
			apiMethod = GET_ALBUMS;
			leadingObject = "topalbums";
			arrayObject = "album";
		} else {
			apiMethod = GET_ARTIST;
			leadingObject = "topartists";
			arrayObject = "artist";
		}
		String url = BASE + apiMethod + userName + API_KEY + ending + "&period=" + weekly;
		BlockingQueue<UrlCapsule> queue = new LinkedBlockingQueue<>();
		HttpClient client = new HttpClient();

		int requestedSize = x * y;
		int size = 0;
		int page = 1;

		if (requestedSize > 150)
			url += "&limit=500";

		while (size < requestedSize) {

			String urlPage = url + "&page=" + page;
			HttpMethodBase method = createMethod(urlPage);

			++page;
			System.out.println(page + " :page             size: " + size);
			try {

				// Execute the method.
				int statusCode = client.executeMethod(method);

				if (statusCode != HttpStatus.SC_OK) {
					if (statusCode == HttpStatus.SC_NOT_FOUND)
						throw new LastFmUserNotFoundException(userName);

					throw new LastFMServiceException("Error in the service: " + method.getStatusLine());
				}

				// Read the response body.
				byte[] responseBody = method.getResponseBody();
				JSONObject obj = new JSONObject(new String(responseBody));
				obj = obj.getJSONObject(leadingObject);
				int limit = obj.getJSONObject("@attr").getInt("total");
				if (limit == size)
					break;
				JSONArray arr = obj.getJSONArray(arrayObject);
				for (int i = 0; i < arr.length() && size < requestedSize; i++) {
					JSONObject albumObj = arr.getJSONObject(i);
					if (isAlbum)
						queue.add(parseAlbum(albumObj, size));
					else
						queue.add(parseArtist(albumObj, size));

					++size;
				}


			} catch (HttpException e) {
				System.err.println("Fatal protocol violation: " + e.getMessage());
				e.printStackTrace();
			} catch (JSONException | IOException e) {
				e.printStackTrace();
			} finally {
				// Release the connection.
				method.releaseConnection();
			}
		}
		byte[] img;
		//

		int minx = (int) Math.ceil((double) size / x);
		int miny = (int) Math.ceil((double) size / y);

		if (minx == 1)
			x = size;
		BufferedImage image = CollageMaker.generateCollageThreaded(x, minx, queue);
		ByteArrayOutputStream b = new ByteArrayOutputStream();

		try {
			ImageIO.write(image, "png", b);
		} catch (IOException e) {
			e.printStackTrace();
		}

		img = b.toByteArray();
		// Deal with the response.
		// Use caution: ensure correct character encoding and is not binary data

		return img;
	}

	private static UrlCapsule parseAlbum(JSONObject albumObj, int size) {
		JSONObject artistObj = albumObj.getJSONObject("artist");
		String albumName = albumObj.getString("name");
		String artistName = artistObj.getString("name");
		JSONArray image = albumObj.getJSONArray("image");
		JSONObject bigImage = image.getJSONObject(3);
		return new UrlCapsule(bigImage.getString("#text"), size, albumName, artistName);
	}

	private static UrlCapsule parseArtist(JSONObject artistObj, int size) {
		String artistName = artistObj.getString("name");
		JSONArray image = artistObj.getJSONArray("image");
		JSONObject bigImage = image.getJSONObject(3);
		return new UrlCapsule(bigImage.getString("#text"), size, "", artistName);
	}

	private static HttpMethodBase createMethod(String url) {
		GetMethod method = new GetMethod(url);
		method.setRequestHeader(new Header("User-Agent", "IshDiscordBot"));
		return method;

	}

	public static String getCorrection(String artistToCorrect) {
		HttpClient client = new HttpClient();
		try {


			String url = BASE + GET_CORRECTION + URLEncoder.encode(artistToCorrect, "UTF-8") + API_KEY + ending;
			HttpMethodBase method = createMethod(url);
			int statusCode = client.executeMethod(method);
			if (statusCode != HttpStatus.SC_OK) {
				System.err.println("Method failed: " + method.getStatusLine());
				return artistToCorrect;
			}
			byte[] responseBody = method.getResponseBody();
			JSONObject obj = new JSONObject(new String(responseBody));
			obj = obj.getJSONObject("corrections");
			JSONObject artistObj = obj.getJSONObject("correction").getJSONObject("artist");


			return artistObj.getString("name");

		} catch (Exception e) {
			e.printStackTrace();
			return artistToCorrect;

		}

	}

	public static int getPlaysAlbum_Artist(String username, boolean isAlbum, String artist, String queriedString) throws
			LastFmUserNotFoundException {

		HttpClient client = new HttpClient();

		String url = BASE + GET_TRACKS + username + "&artist=" + artist.replaceAll(" ", "+") + API_KEY + ending + "&size=500";
		List<UserInfo> returnList = new ArrayList<>();
		int counter = 0;
		int page = 1;
		int limit = 50;
		int queryCounter = 0;

		while (true) {

			String urlPage = url + "&page=" + page;
			++page;

			HttpMethodBase method = createMethod(urlPage);

			System.out.println(page + " :page             size: ");
			try {

				// Execute the method.
				int statusCode = client.executeMethod(method);

				if (statusCode != HttpStatus.SC_OK) {
					if (statusCode == HttpStatus.SC_NOT_FOUND)
						throw new LastFmUserNotFoundException(username);
					throw new LastFMServiceException("Error in the service: " + method.getStatusLine());
				}

				// Read the response body.
				byte[] responseBody = method.getResponseBody();
				JSONObject obj = new JSONObject(new String(responseBody));
				obj = obj.getJSONObject("artisttracks");


				JSONArray arr = obj.getJSONArray("track");
				int pageCounter = 0;
				for (int i = 0; i < arr.length(); i++) {
					JSONObject albumObj = arr.getJSONObject(i);
					if (!albumObj.has("date"))
						continue;

					if (isAlbum) {
						if (albumObj.getJSONObject("album").getString("#text").equalsIgnoreCase(queriedString))
							++queryCounter;
					} else if (albumObj.getString("name").equalsIgnoreCase(queriedString))
						++queryCounter;

					++pageCounter;
				}
				if (pageCounter != 50)
					break;


			} catch (HttpException e) {
				System.err.println("Fatal protocol violation: " + e.getMessage());
				e.printStackTrace();

			} catch (JSONException e) {
				return 0;
			} catch (LastFMServiceException | IOException e) {
				e.printStackTrace();

			}
		}
		return queryCounter;
	}

}


