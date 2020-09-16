package advisor;

import java.util.List;

public class Album extends SpotifyObject{
    private final String name;
    private final List<String> artists;
    private final String url;

    public Album(String name, List<String> artists, String url) {
        this.name = name;
        this.artists = artists;
        this.url = url;
    }

    @Override
    public String toString() {
        return name + "\n" +
                artists + "\n" +
                url + "\n";
    }
}
