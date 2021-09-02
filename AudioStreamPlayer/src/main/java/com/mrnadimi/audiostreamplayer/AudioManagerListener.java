package com.mrnadimi.audiostreamplayer;

/**
 * Developer: Mohamad Nadimi
 * Company: Saghe
 * Website: https://www.mrnadimi.com
 * Created on 06 July 2021
 * <p>
 * Description: Method ha dar in class vazeh hastand
 */
public interface AudioManagerListener {

    public void onLoading(boolean progress);
    public void onPlaying();
    public void onStoping();
    public void onBuffering();
    public void onError();
}
