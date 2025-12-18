package com.edgecloudrecorder.mearitaskerplugin.recorder;

import android.content.Context;
import com.meari.sdk.MeariUser;
import com.meari.sdk.bean.UserInfo;
import com.meari.sdk.callback.ILoginCallback;

/**
 * Handles automatic login to Meari/CloudEdge account
 */
public class AutoLoginManager {
    private static final String TAG = "AutoLoginManager";
    private final RecorderLogger logger = RecorderLogger.getInstance();
    private final RecorderConfig config;
    
    public interface LoginCallback {
        void onLoginSuccess();
        void onLoginFailed(String error);
    }
    
    public AutoLoginManager(Context context) {
        this.config = new RecorderConfig(context);
    }
    
    /**
     * Attempt automatic login with credentials from config
     */
    public void login(final LoginCallback callback) {
        if (!config.hasCredentials()) {
            String error = "No credentials configured. Set via ADB or system properties.";
            logger.error(TAG, error);
            callback.onLoginFailed(error);
            return;
        }
        
        String username = config.getUsername();
        String password = config.getPassword();
        String country = config.getCountry();
        String countryCode = config.getCountryCode();
        
        logger.info(TAG, "Attempting login with user: " + username + ", country: " + country + "/" + countryCode);
        
        MeariUser.getInstance().loginWithAccount(country, countryCode, username, password, new ILoginCallback() {
            @Override
            public void onSuccess(UserInfo userInfo) {
                logger.info(TAG, "Login successful! User ID: " + userInfo.getUserID());
                callback.onLoginSuccess();
            }
            
            @Override
            public void onError(int code, String error) {
                String errorMsg = "Login failed: [" + code + "] " + error;
                logger.error(TAG, errorMsg);
                callback.onLoginFailed(errorMsg);
            }
        });
    }
    
    /**
     * Check if user is already logged in
     */
    public boolean isLoggedIn() {
        try {
            return MeariUser.getInstance().isLogin();
        } catch (Exception e) {
            logger.error(TAG, "Error checking login status", e);
            return false;
        }
    }
}
