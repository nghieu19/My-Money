package com.example.mymoney.chatbot;

import com.google.gson.annotations.SerializedName;

// HuggingFace API returns a direct array of objects with "generated_text" field
public class HuggingFaceResponse extends java.util.ArrayList<HuggingFaceResponse.ResponseItem> {

    public static class ResponseItem {
        @SerializedName("generated_text")
        private String generated_text;

        public String getGenerated_text() {
            return generated_text;
        }

        public void setGenerated_text(String generated_text) {
            this.generated_text = generated_text;
        }
    }

    // Convenience method to get first response
    public String getGeneratedText() {
        if (!this.isEmpty() && this.get(0) != null) {
            return this.get(0).getGenerated_text();
        }
        return null;
    }
}
