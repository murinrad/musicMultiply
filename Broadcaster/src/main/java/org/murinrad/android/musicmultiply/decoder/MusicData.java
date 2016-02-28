package org.murinrad.android.musicmultiply.decoder;

import android.content.ContentResolver;
import android.net.Uri;

/**
 * Created by Radovan Murin on 2/28/2016.
 */
public class MusicData {
    private String songName,songArtist;
    private int duration = -1; //in seconds

    private MusicData() {
        songName = "Unknown song name";
        songArtist = "Unknown artist";
    }


    public String getSongName() {
        return songName;
    }

    public String getSongArtist() {
        return songArtist;
    }

    public int getDuration() {
        return duration;
    }



    public static class Builder {
        MusicData item = new MusicData();

        public Builder withName(String name) {
            item.songName = name;
            return this;
        }

        public Builder withArtist(String artist) {
            item.songArtist = artist;
            return this;
        }

        public  Builder withLength(int lengthInSeconds) {
            item.duration = lengthInSeconds;
            return this;
        }

        public MusicData build() {
            return item;
        }


    }

}
