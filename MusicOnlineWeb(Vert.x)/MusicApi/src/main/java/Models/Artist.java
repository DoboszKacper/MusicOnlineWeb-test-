package Models;

import java.util.List;

public class Artist {
    private int id;
    private String name;
    private String albumsList;

    public String getName() {
        return name;
    }

    public Artist(String name, String albumsList) {
        this.name = name;
        this.albumsList = albumsList;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Artist() {
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlbumsList() {
        return albumsList;
    }

    public void setAlbumsList(String albumsList) {
        this.albumsList = albumsList;
    }
}
