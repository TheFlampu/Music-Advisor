package advisor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class Client {
    private HttpClient client = HttpClient.newBuilder().build();
    private Scanner scanner = new Scanner(System.in);

    private boolean exit = false;

    private String token = null;
    private volatile String code = null;

    private List<? extends List<? extends SpotifyObject>> pages;
    private int currentPage;

    private String access = "https://accounts.spotify.com";
    private String resource = "https://api.spotify.com";
    private int pageSize = 5;

    public void start() {
        while (!exit) {
            String action = scanner.nextLine();
            String category = "";

            if (action.equals("auth")) {
                auth();
                continue;
            } else if (token == null) {
                System.out.println("Please, provide access for application.");
                continue;
            } else if (action.contains("playlists")) {
                category = action.substring(10);
                action = "playlists";
            }

            switch (action) {
                case "new" : newReleases();
                break;
                case "featured" : featured();
                break;
                case "categories" : categories();
                break;
                case "playlists" : playlists(category);
                break;
                case "exit" : exit();
                break;
                case "next" : printNext();
                break;
                case "prev" : printPrevious();
                break;
            }
        }
    }

    private void newReleases() {
        JsonObject jo = sendGetRequest(URI.create(resource + "/v1/browse/new-releases"), token);

        List<Album> albums = getAlbums(jo);

        pages = getIterator(albums, pageSize);

        printNext();
    }

    private void featured() {
        JsonObject jo = sendGetRequest(URI.create(resource + "/v1/browse/featured-playlists"), token);

        List<PlayList> playLists = getPlayLists(jo);

        pages = getIterator(playLists, pageSize);

        printNext();
    }

    private void categories() {
        List<Category> categories = getCategories(resource, token);

        pages = getIterator(categories, pageSize);

        printNext();
    }

    private void playlists(String category) {
        List<Category> categories = getCategories(resource, token);

        for (Category current : categories) {
            if (current.getName().equals(category)) {
                category = current.getId();
                break;
            }
        }

        JsonObject jo = sendGetRequest(URI.create(resource + "/v1/browse/categories/" + category +  "/playlists"), token);

        if (!(jo.getAsJsonObject("error") == null)) {
            System.out.println(jo.getAsJsonObject("error").get("message").getAsString());
            return;
        }

        List<PlayList> playLists = getPlayLists(jo);

        pages = getIterator(playLists, pageSize);

        printNext();
    }

    private void auth() {
        Server server = new Server(8080);
        server.getCode(this);

        String clientId = "07e3f252dfef4ea8862a8d4cad3954af";
        System.out.println("use this link to request the access code:" + "\n" +
                "https://accounts.spotify.com/ru/authorize?" + "client_id=" + clientId + "&redirect_uri=http://localhost:8080&response_type=code" + "\n" +
                "waiting for code...");

        while (code == null) {
            Thread.onSpinWait();
        }

        server.stop();

        System.out.println("code received" + "\n" +
                "making http request for access_token...");

        String clientSecret = "74a22bb1d3d648b1bcd3841cfc13e43d";
        JsonObject jo = sendPostRequest(URI.create(access + "/api/token"), clientId, clientSecret, code);

        token = jo.get("access_token").getAsString();

        System.out.println("---SUCCESS---");

    }

    private void exit() {
        System.out.println("---GOODBYE!---");
        exit = true;
    }

    private List<Category> getCategories(String resource, String token) {
        JsonObject jo = sendGetRequest(URI.create(resource + "/v1/browse/categories"), token);

        List<Category> categories = new ArrayList<>();

        for (JsonElement item : jo.getAsJsonObject("categories").getAsJsonArray("items")) {
            String name = item.getAsJsonObject().get("name").getAsString();
            String id = item.getAsJsonObject().get("id").getAsString();
            categories.add(new Category(name, id));
        }

        return categories;
    }

    private JsonObject sendGetRequest(URI uri, String token) {
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .header("Authorization", "Bearer " + token)
                .build();

        return sendRequest(getRequest);
    }

    private JsonObject sendPostRequest(URI uri, String clientId, String clientSecret, String code) {
        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(uri)
                .POST(HttpRequest.BodyPublishers.ofString("client_id=" + clientId +
                        "&client_secret=" + clientSecret +
                        "&code=" + code +
                        "&grant_type=authorization_code" +
                        "&redirect_uri=http://localhost:8080"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();
        
        return sendRequest(postRequest);
    }

    private JsonObject sendRequest(HttpRequest httpRequest) {
        try {
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            return JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<PlayList> getPlayLists(JsonObject jo) {
        List<PlayList> playLists = new ArrayList<>();

        for (JsonElement item : jo.getAsJsonObject("playlists").getAsJsonArray("items")) {
            String name = item.getAsJsonObject().get("name").getAsString();
            String url = item.getAsJsonObject().getAsJsonObject("external_urls").get("spotify").getAsString();
            playLists.add(new PlayList(name, url));
        }

        return playLists;
    }

    private List<Album> getAlbums(JsonObject jo) {
        List<Album> albums = new ArrayList<>();

        for (JsonElement item : jo.getAsJsonObject("albums").getAsJsonArray("items")) {
            String name = item.getAsJsonObject().get("name").getAsString();
            String url = item.getAsJsonObject().getAsJsonObject("external_urls").get("spotify").getAsString();
            List<String> artists = new ArrayList<>();

            for (JsonElement artist : item.getAsJsonObject().getAsJsonArray("artists")) {
                artists.add(artist.getAsJsonObject().get("name").getAsString());
            }

            albums.add(new Album(name, artists, url));
        }

        return albums;
    }

    private <T> List<List<T>> getIterator(List<T> list, int size) {
        List<List<T>> pages = new ArrayList<>();
        currentPage = -1;

        for (int i = 0; i < list.size(); i += size) {
            List<T> page = list.subList(i, Math.min(list.size(), i + size));
            pages.add(page);
        }

        return pages;
    }

    private void printNext() {
        if (currentPage == pages.size() - 1) {
            System.out.println("No more pages.");
        } else {
            for (SpotifyObject el : pages.get(++currentPage)) {
                System.out.println(el);
            }
            System.out.println("---PAGE " + (currentPage + 1) + " OF " + pages.size() + "---");
        }
    }

    private void printPrevious() {
        if (currentPage == 0) {
            System.out.println("No more pages.");
        } else {
            for (SpotifyObject el : pages.get(--currentPage)) {
                System.out.println(el);
            }
            System.out.println("---PAGE " + (currentPage + 1) + " OF " + pages.size() + "---");
        }
    }

    public void setAccess(String access) {
        this.access = access;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public void setCode(String code) {
        this.code = code;
    }
}