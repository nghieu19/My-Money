package com.example.mymoney.chatbot;

import java.util.ArrayList;
import java.util.List;

public class OpenRouterRequest {
    private String model;
    private List<Message> messages;
    private double temperature;
    private int max_tokens;

    public OpenRouterRequest(String model) {
        this.model = model;
        this.messages = new ArrayList<>();
        this.temperature = 0.7;
        this.max_tokens = 500;
    }

    public void addMessage(String role, String content) {
        messages.add(new Message(role, content));
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMax_tokens() {
        return max_tokens;
    }

    public void setMax_tokens(int max_tokens) {
        this.max_tokens = max_tokens;
    }

    public static class Message {
        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
