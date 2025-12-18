package com.edgecloudrecorder.mearitaskerplugin;

import android.text.TextUtils;

import com.meari.sdk.bean.CameraInfo;
import com.meari.sdk.json.BaseJSONObject;

import org.json.JSONException;

public class CommonUtils {
    // Quality preference constants
    public static final int QUALITY_AUTO = 0;
    public static final int QUALITY_HD = 1;
    public static final int QUALITY_SD = 2;
    public static final int QUALITY_LOW = 3;

    public static String getDefaultStreamId(CameraInfo cameraInfo) {
        return getDefaultStreamId(cameraInfo, QUALITY_AUTO);
    }

    public static String getDefaultStreamId(CameraInfo cameraInfo, int qualityPreference) {
        String streamId = "1";
        if (cameraInfo.getVst() == 1) {
            streamId = "0";
        } else {
            if (!TextUtils.isEmpty(cameraInfo.getBps2())) {
                try {
                    BaseJSONObject object = new BaseJSONObject(cameraInfo.getBps2());
                    // Try to apply quality preference
                    switch (qualityPreference) {
                        case QUALITY_HD:
                            // Try HD streams first: higher numbers = higher quality
                            if (object.has("3")) {
                                streamId = "103";
                            } else if (object.has("2")) {
                                streamId = "102";
                            } else if (object.has("1")) {
                                streamId = "101";
                            } else if (object.has("0")) {
                                streamId = "100";
                            }
                            break;
                        case QUALITY_SD:
                            // Try SD streams: middle quality
                            if (object.has("1")) {
                                streamId = "101";
                            } else if (object.has("2")) {
                                streamId = "102";
                            } else if (object.has("0")) {
                                streamId = "100";
                            } else if (object.has("3")) {
                                streamId = "103";
                            }
                            break;
                        case QUALITY_LOW:
                            // Try low quality streams: lower numbers = lower quality
                            if (object.has("0")) {
                                streamId = "100";
                            } else if (object.has("1")) {
                                streamId = "101";
                            } else if (object.has("2")) {
                                streamId = "102";
                            } else if (object.has("3")) {
                                streamId = "103";
                            }
                            break;
                        default: // QUALITY_AUTO
                            // Auto: first available (usually lowest for bandwidth)
                            if (object.has("0")) {
                                streamId = "100";
                            } else if (object.has("1")) {
                                streamId = "101";
                            } else if (object.has("2")) {
                                streamId = "102";
                            } else if (object.has("3")) {
                                streamId = "103";
                            }
                            break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (cameraInfo.getBps() == 0 || cameraInfo.getBps() == -1) {
                streamId = "0";
            } else {
                streamId = "1";
            }
        }
        return streamId;
    }

}
