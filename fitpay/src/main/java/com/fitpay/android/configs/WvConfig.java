package com.fitpay.android.configs;

import android.util.Base64;

import com.fitpay.android.utils.FPLog;
import com.fitpay.android.utils.StringUtils;
import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;

/**
 * Encoded wv config
 */
class WvConfig {

    private HashMap<String, Object> data = new HashMap<>();

    @Override
    public String toString() {
        return new Gson().toJson(data);
    }

    public String getEncodedString() {
        byte[] bytesToEncode = toString().getBytes(StandardCharsets.UTF_8);
        return Base64.encodeToString(bytesToEncode, Base64.URL_SAFE);
    }

    public static class Builder{
        private HashMap<String, Object> data = new HashMap<>();

        public Builder(){
            data.put("language", Locale.getDefault().getLanguage()); //ISO 639
        }

        public Builder accountExist(boolean value) {
            data.put("account", value);
            return this;
        }

        public Builder demoMode(boolean value) {
            data.put("demoMode", value);
            return this;
        }

        public Builder demoCardGroup(String demoCardGroup) {
            data.put("demoCardGroup", demoCardGroup);
            return this;
        }

        public Builder email(String email) {
            data.put("userEmail", email);
            return this;
        }

        public Builder clientId(String clientId) {
            data.put("clientId", clientId);
            return this;
        }

        public <T extends WvPaymentDeviceInfo> Builder paymentDevice(T paymentDevice) {
            data.put("paymentDevice", paymentDevice);
            return this;
        }

        public Builder redirectUri(String redirectUri) {
            data.put("redirectUri", redirectUri);
            return this;
        }

        public Builder setCSSUrl(String cssUrl) {
            data.put("themeOverrideCssUrl", cssUrl);
            return this;
        }

        public Builder useWebCardScanner(boolean value) {
            data.put("useWebCardScanner", value);
            return this;
        }

        public Builder setBaseLanguageUrl(String baseLanguageUrl){
            data.put("baseLangUrl", baseLanguageUrl);
            return this;
        }

        public Builder setAccessToken(String accessToken){
            data.put("accessToken", accessToken);
            return this;
        }

        public Builder setLanguage(String language){
            if(StringUtils.isEmpty(language)){
                FPLog.w("Language can't be null. Using default");
            } else {
                data.put("language", language);
            }
            return this;
        }

        public Builder addKeyValue(String key, Object value) {
            data.put(key, value);
            return this;
        }

        public WvConfig build() {
            WvConfig wvConfig = new WvConfig();
            wvConfig.data = data;
            return wvConfig;
        }
    }
}