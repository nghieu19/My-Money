package com.example.mymoney.chatbot;

public class HuggingFaceRequest {
    private String inputs;
    private Parameters parameters;
    private Options options;

    public HuggingFaceRequest(String inputs) {
        this.inputs = inputs;
        this.parameters = new Parameters();
        this.options = new Options();
    }

    public String getInputs() {
        return inputs;
    }

    public void setInputs(String inputs) {
        this.inputs = inputs;
    }

    public Parameters getParameters() {
        return parameters;
    }

    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }

    public Options getOptions() {
        return options;
    }

    public void setOptions(Options options) {
        this.options = options;
    }

    public static class Parameters {
        private int max_new_tokens = 150;
        private double temperature = 0.7;
        private double top_p = 0.95;
        private boolean return_full_text = false;

        public int getMax_new_tokens() {
            return max_new_tokens;
        }

        public void setMax_new_tokens(int max_new_tokens) {
            this.max_new_tokens = max_new_tokens;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public double getTop_p() {
            return top_p;
        }

        public void setTop_p(double top_p) {
            this.top_p = top_p;
        }

        public boolean isReturn_full_text() {
            return return_full_text;
        }

        public void setReturn_full_text(boolean return_full_text) {
            this.return_full_text = return_full_text;
        }
    }

    public static class Options {
        private boolean use_cache = true;
        private boolean wait_for_model = true;

        public boolean isUse_cache() {
            return use_cache;
        }

        public void setUse_cache(boolean use_cache) {
            this.use_cache = use_cache;
        }

        public boolean isWait_for_model() {
            return wait_for_model;
        }

        public void setWait_for_model(boolean wait_for_model) {
            this.wait_for_model = wait_for_model;
        }
    }
}