package com.example.mymoney.chatbot;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface HuggingFaceApiService {
    // Using Mistral-7B-Instruct for better financial advice
    @POST("models/mistralai/Mistral-7B-Instruct-v0.2")
    Call<HuggingFaceResponse> generateResponse(
            @Header("Authorization") String authorization,
            @Body HuggingFaceRequest request
    );
}