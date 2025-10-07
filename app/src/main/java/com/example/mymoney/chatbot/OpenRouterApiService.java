package com.example.mymoney.chatbot;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface OpenRouterApiService {
    @POST("chat/completions")
    Call<OpenRouterResponse> generateResponse(
        @Header("Authorization") String authorization,
        @Header("HTTP-Referer") String referer,
        @Header("X-Title") String title,
        @Body OpenRouterRequest request
    );
}
