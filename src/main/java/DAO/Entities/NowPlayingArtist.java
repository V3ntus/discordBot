package DAO.Entities;

public class NowPlayingArtist {
	private String artistName;
	private String mbid;
	private boolean nowPlaying;
	private String albumName;
	private String songName;
	private String url;
	private String username;

	public NowPlayingArtist(String artistName, String mbid, boolean nowPlaying, String albumName, String songName, String url, String username) {
		this.artistName = artistName;
		this.mbid = mbid;
		this.nowPlaying = nowPlaying;
		this.albumName = albumName;
		this.songName = songName;
		this.url = url;
		this.username = username;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getArtistName() {
		return artistName;
	}

	public void setArtistName(String artistName) {
		this.artistName = artistName;
	}

	public String getMbid() {
		return mbid;
	}

	public void setMbid(String mbid) {
		this.mbid = mbid;
	}

	public boolean isNowPlaying() {
		return nowPlaying;
	}

	public void setNowPlaying(boolean nowPlaying) {
		this.nowPlaying = nowPlaying;
	}

	public String getAlbumName() {
		return albumName;
	}

	public void setAlbumName(String albumName) {
		this.albumName = albumName;
	}

	public String getSongName() {
		return songName;
	}

	public void setSongName(String songName) {
		this.songName = songName;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		NowPlayingArtist that = (NowPlayingArtist) o;

		if (nowPlaying != that.nowPlaying) return false;
		if (artistName != null ? !artistName.equals(that.artistName) : that.artistName != null) return false;
		if (mbid != null ? !mbid.equals(that.mbid) : that.mbid != null) return false;
		if (albumName != null ? !albumName.equals(that.albumName) : that.albumName != null) return false;
		if (songName != null ? !songName.equals(that.songName) : that.songName != null) return false;
		return url != null ? url.equals(that.url) : that.url == null;
	}

	@Override
	public int hashCode() {
		int result = artistName != null ? artistName.hashCode() : 0;
		result = 31 * result + (mbid != null ? mbid.hashCode() : 0);
		result = 31 * result + (nowPlaying ? 1 : 0);
		result = 31 * result + (albumName != null ? albumName.hashCode() : 0);
		result = 31 * result + (songName != null ? songName.hashCode() : 0);
		result = 31 * result + (url != null ? url.hashCode() : 0);
		return result;
	}
}