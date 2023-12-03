package com.driver;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

@Repository
public class SpotifyRepository {
    public HashMap<Artist, List<Album>> artistAlbumMap;
    public HashMap<Album, List<Song>> albumSongMap;
    public HashMap<Playlist, List<Song>> playlistSongMap;
    public HashMap<Playlist, List<User>> playlistListenerMap;
    public HashMap<User, Playlist> creatorPlaylistMap;
    public HashMap<User, List<Playlist>> userPlaylistMap;
    public HashMap<Song, List<User>> songLikeMap;

    public List<User> users;
    public List<Song> songs;
    public List<Playlist> playlists;
    public List<Album> albums;
    public List<Artist> artists;

    public SpotifyRepository(){
        //To avoid hitting APIs multiple times, initialize all the hashmaps here with some dummy data
        artistAlbumMap = new HashMap<>();
        albumSongMap = new HashMap<>();
        playlistSongMap = new HashMap<>();
        playlistListenerMap = new HashMap<>();
        creatorPlaylistMap = new HashMap<>();
        userPlaylistMap = new HashMap<>();
        songLikeMap = new HashMap<>();

        users = new ArrayList<>();
        songs = new ArrayList<>();
        playlists = new ArrayList<>();
        albums = new ArrayList<>();
        artists = new ArrayList<>();
    }

    public User createUser(String name, String mobile) {
        User user = new User(name, mobile);
        users.add(user);
        return user;
    }

    public Artist createArtist(String name) {
        Artist artist = new Artist(name);
        artists.add(artist);
        return artist;
    }

    public Album createAlbum(String title, String artistName) {
        Artist artist = findArtist(artistName);
        if (artist == null) {
            artist = createArtist(artistName);
        }

        Album album = new Album(title);
        albums.add(album);

        artistAlbumMap.computeIfAbsent(artist, k -> new ArrayList<>()).add(album);
        return album;
    }

    public Song createSong(String title, String albumName, int length) throws Exception {
        Album album = findAlbum(albumName);
        if (album == null) {
            throw new Exception("Album does not exist");
        }

        Song song = new Song(title, length);
        songs.add(song);

        albumSongMap.computeIfAbsent(album, k -> new ArrayList<>()).add(song);
        return song;
    }

    public Playlist createPlaylistOnLength(String mobile, String title, int length) throws Exception {
        User user = findUserByMobile(mobile);
        if (user == null) {
            throw new Exception("User does not exist");
        }

//        List<Song> selectedSongs = songs.stream().filter(song -> song.getLength() == length).toList();
//        Playlist playlist = new Playlist(title);
//        playlists.add(playlist);

        List<Song> selectedSongs = songs.stream()
                .filter(song -> song.getLength() == length)
                .collect(Collectors.toList());

        Playlist playlist = new Playlist(title);
        playlists.add(playlist);

        playlistSongMap.put(playlist, selectedSongs);
        playlistListenerMap.computeIfAbsent(playlist, k -> new ArrayList<>()).add(user);
        creatorPlaylistMap.put(user, playlist);
        userPlaylistMap.computeIfAbsent(user, k -> new ArrayList<>()).add(playlist);

        return playlist;
    }

    public Playlist createPlaylistOnName(String mobile, String title, List<String> songTitles) throws Exception {
        User user = findUserByMobile(mobile);
        if (user == null) {
            throw new Exception("User does not exist");
        }

        List<Song> selectedSongs = songs.stream()
                .filter(song -> songTitles.contains(song.getTitle()))
                .collect(Collectors.toList());

        Playlist playlist = new Playlist(title);
        playlists.add(playlist);

        playlistSongMap.put(playlist, selectedSongs);
        playlistListenerMap.computeIfAbsent(playlist, k -> new ArrayList<>()).add(user);
        creatorPlaylistMap.put(user, playlist);
        userPlaylistMap.computeIfAbsent(user, k -> new ArrayList<>()).add(playlist);

        return playlist;
    }

    public Playlist findPlaylist(String mobile, String playlistTitle) throws Exception {
        User user = findUserByMobile(mobile);
        if (user == null) {
            throw new Exception("User does not exist");
        }

        Playlist playlist = playlists.stream().filter(p -> p.getTitle().equals(playlistTitle)).findFirst().orElse(null);
        if (playlist == null) {
            throw new Exception("Playlist does not exist");
        }

        List<User> listeners = playlistListenerMap.getOrDefault(playlist, new ArrayList<>());
        if (!listeners.contains(user)) {
            listeners.add(user);
        }

        userPlaylistMap.computeIfAbsent(user, k -> new ArrayList<>()).add(playlist);

        return playlist;
    }

    public Song likeSong(String mobile, String songTitle) throws Exception {
        User user = findUserByMobile(mobile);
        if (user == null) {
            throw new Exception("User does not exist");
        }

        Song song = songs.stream().filter(s -> s.getTitle().equals(songTitle)).findFirst().orElse(null);
        if (song == null) {
            throw new Exception("Song does not exist");
        }

        List<User> likers = songLikeMap.computeIfAbsent(song, k -> new ArrayList<>());
        if (!likers.contains(user)) {
            likers.add(user);
            // Auto-like the corresponding artist
            Artist artist = findArtistBySong(song);
            if (artist != null) {
                artist.setLikes(artist.getLikes() + 1);
            }
        }

        return song;
    }

    public String mostPopularArtist() {
        Artist mostPopularArtist = artists.stream().max(Comparator.comparingInt(Artist::getLikes)).orElse(null);
        return (mostPopularArtist != null) ? mostPopularArtist.getName() : "";
    }

    public String mostPopularSong() {
        Song mostPopularSong = songs.stream().max(Comparator.comparingInt(song -> songLikeMap.getOrDefault(song, new ArrayList<>()).size())).orElse(null);
        return (mostPopularSong != null) ? mostPopularSong.getTitle() : "";
    }

    private Artist findArtistBySong(Song song) {
        return artistAlbumMap.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(album -> albumSongMap.get(album).contains(song)))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    private User findUserByMobile(String mobile) {
        return users.stream().filter(user -> user.getMobile().equals(mobile)).findFirst().orElse(null);
    }

    private Artist findArtist(String artistName) {
        return artists.stream().filter(artist -> artist.getName().equals(artistName)).findFirst().orElse(null);
    }

    private Album findAlbum(String albumName) {
        return albums.stream().filter(album -> album.getTitle().equals(albumName)).findFirst().orElse(null);
    }
}
