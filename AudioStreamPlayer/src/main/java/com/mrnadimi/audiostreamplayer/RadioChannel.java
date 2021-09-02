package com.mrnadimi.audiostreamplayer;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.Objects;

/**
 * Developer: Mohamad Nadimi
 * Company: Saghe
 * Website: https://www.mrnadimi.com
 * Created on 04 July 2021
 * <p>
 * Description: Objecti az channel bude ke besyar mohem ast
 */
public class RadioChannel implements Parcelable {

    @SerializedName("name")
    public final String name;
    @SerializedName("stream")
    public final String url;
    //In data ha baraye estefade shoma ijad shode ast
    public String country;
    public String desc;
    public Date lastPlayTime;

    public RadioChannel(String name, String url) {
        this.name = name;
        this.url = url;
        country = "";
        desc = "";
    }

    public void setCountry(String country) {
        if (country == null)return;;
        this.country = country;
    }

    public void setDesc(String desc) {
        if (desc == null)return;
        this.desc = desc;
    }

    protected RadioChannel(Parcel in) {
        name = in.readString();
        country = in.readString();
        url = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(country);
        dest.writeString(url);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<RadioChannel> CREATOR = new Creator<RadioChannel>() {
        @Override
        public RadioChannel createFromParcel(Parcel in) {
            return new RadioChannel(in);
        }

        @Override
        public RadioChannel[] newArray(int size) {
            return new RadioChannel[size];
        }
    };


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RadioChannel that = (RadioChannel) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(url, that.url);
    }
}
