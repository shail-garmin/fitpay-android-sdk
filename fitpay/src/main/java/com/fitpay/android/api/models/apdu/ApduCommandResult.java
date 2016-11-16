package com.fitpay.android.api.models.apdu;

/**
 * Single apdu command execution result
 */
public class ApduCommandResult {

    private String commandId;
    private String responseCode;
    private String responseData;
    private transient boolean continueOnFailure;

    private ApduCommandResult() {
    }

    public String getCommandId() {
        return commandId;
    }

    public String getResponseData() {
        return responseData;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public boolean canContinueOnFailure() {
        return continueOnFailure;
    }

    public static class Builder {
        private String commandId;
        private String responseCode;
        private String responseData;
        private boolean continueOnFailure;

        public Builder() {
        }

        public ApduCommandResult build() {
            ApduCommandResult result = new ApduCommandResult();
            result.commandId = this.commandId;
            result.responseCode = this.responseCode;
            result.responseData = this.responseData;
            result.continueOnFailure = this.continueOnFailure;
            return result;
        }

        public Builder setCommandId(String commandId) {
            this.commandId = commandId;
            return this;
        }

        public Builder setResponseCode(String responseCode) {
            this.responseCode = responseCode;
            return this;
        }

        public Builder setResponseData(String responseData) {
            this.responseData = responseData;
            return this;
        }

        public Builder setContinueOnFailure(boolean continueOnFailure) {
            this.continueOnFailure = continueOnFailure;
            return this;
        }
    }
}
