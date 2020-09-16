package advisor;

public class PlayList extends SpotifyObject{
    private final String name;
    private final String url;

    public PlayList(String name, String url) {
        this.name = name;
        this.url = url;
    }

    @Override
    public String toString() {
        return name + "\n" +
                url + "\n";
    }
}
