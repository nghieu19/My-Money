package com.example.mymoney.chatbot;

import android.content.Context;
import android.util.Log;

import com.example.mymoney.database.AppDatabase;
import com.example.mymoney.database.entity.Category;
import com.example.mymoney.database.entity.SavingGoal;
import com.example.mymoney.database.entity.Transaction;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChatbotService {
    private static final String TAG = "ChatbotService";
    // OpenRouter configuration
    private static final String OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1/";
    private static final String API_TOKEN = "sk-or-v1";
    private static final String MODEL = "deepseek/deepseek-chat-v3.1:free";
    
    private OpenRouterApiService apiService;
    private AppDatabase database;
    private Context context;

    public ChatbotService(Context context) {
        this.context = context;
        this.database = AppDatabase.getInstance(context);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(OPENROUTER_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        this.apiService = retrofit.create(OpenRouterApiService.class);
    }

    public void generateFinancialAdvice(int userId, int walletId, String userMessage, ChatbotCallback callback) {
        Log.d(TAG, "Starting financial advice generation for user: " + userId + ", wallet: " + walletId);

        // Analyze user's financial data in background
        new Thread(() -> {
            try {
                String financialAnalysis = analyzeUserFinancialData(userId, walletId);
                
                Log.d(TAG, "Financial analysis: " + financialAnalysis);

                // Create OpenRouter request with chat format
                OpenRouterRequest request = new OpenRouterRequest(MODEL);
                request.setTemperature(0.7);
                request.setMax_tokens(500);
                
                // System message to set context
                request.addMessage("system", 
                    "Bạn là trợ lý tài chính cá nhân chuyên nghiệp. " +
                    "Hãy đưa ra lời khuyên ngắn gọn, thực tế và hữu ích bằng tiếng Việt. " +
                    "Trả lời trong 3-4 câu, tập trung vào hành động cụ thể.");
                
                // User message with financial data
                String userPrompt = "Dữ liệu tài chính:\n" + financialAnalysis + "\n\nCâu hỏi: " + userMessage;
                request.addMessage("user", userPrompt);

                Call<OpenRouterResponse> call = apiService.generateResponse(
                    "Bearer " + API_TOKEN,
                    "https://github.com/notnbhd/mymoney", // Your app URL
                    "MyMoney App", // Your app name
                    request
                );

                call.enqueue(new Callback<OpenRouterResponse>() {
                    @Override
                    public void onResponse(Call<OpenRouterResponse> call, Response<OpenRouterResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Log.d(TAG, "API Response successful");
                            String generatedText = response.body().getGeneratedText();
                            
                            if (generatedText != null && !generatedText.isEmpty()) {
                                String cleanedResponse = cleanGeneratedText(generatedText);
                                callback.onSuccess(cleanedResponse);
                            } else {
                                Log.w(TAG, "Empty response from API, using local advice");
                                callback.onSuccess(generateLocalFinancialAdvice(userId, walletId, userMessage, financialAnalysis));
                            }
                        } else {
                            Log.e(TAG, "API Error: " + response.code() + " - " + response.message());
                            // Try to read error body
                            try {
                                String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                                Log.e(TAG, "Error body: " + errorBody);
                            } catch (Exception e) {
                                Log.e(TAG, "Error reading error body", e);
                            }
                            callback.onSuccess(generateLocalFinancialAdvice(userId, walletId, userMessage, financialAnalysis));
                        }
                    }

                    @Override
                    public void onFailure(Call<OpenRouterResponse> call, Throwable t) {
                        Log.e(TAG, "API Failure: " + t.getMessage(), t);
                        callback.onSuccess(generateLocalFinancialAdvice(userId, walletId, userMessage, financialAnalysis));
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error in financial analysis", e);
                callback.onError("Đã xảy ra lỗi khi phân tích dữ liệu tài chính");
            }
        }).start();
    }

    private String analyzeUserFinancialData(int userId, int walletId) {
        StringBuilder analysis = new StringBuilder();

        // Get current month date range
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long monthStartTimestamp = calendar.getTimeInMillis();
        long currentTimestamp = System.currentTimeMillis();

        // Get transactions for current month (wallet-specific)
        List<Transaction> monthlyTransactions = database.transactionDao()
            .getTransactionsByWalletAndDateRange(walletId, monthStartTimestamp, currentTimestamp);

        // Calculate totals
        double totalExpenses = 0;
        double totalIncome = 0;
        Map<Integer, Double> categoryExpenses = new HashMap<>();

        for (Transaction transaction : monthlyTransactions) {
            if ("expense".equals(transaction.getType())) {
                totalExpenses += transaction.getAmount();
                categoryExpenses.put(
                    transaction.getCategoryId(),
                    categoryExpenses.getOrDefault(transaction.getCategoryId(), 0.0) + transaction.getAmount()
                );
            } else if ("income".equals(transaction.getType())) {
                totalIncome += transaction.getAmount();
            }
        }

        // Build analysis
        analysis.append("📊 Tháng này (Ví hiện tại):\n");
        analysis.append(String.format("Thu nhập: %.0f VNĐ\n", totalIncome));
        analysis.append(String.format("Chi tiêu: %.0f VNĐ\n", totalExpenses));
        analysis.append(String.format("Tiết kiệm: %.0f VNĐ\n", totalIncome - totalExpenses));

        // Top spending categories
        if (!categoryExpenses.isEmpty()) {
            analysis.append("\n💰 Chi tiêu theo danh mục:\n");
            categoryExpenses.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(3)
                .forEach(entry -> {
                    Category category = database.categoryDao().getCategoryById(entry.getKey());
                    if (category != null) {
                        analysis.append(String.format("- %s: %.0f VNĐ\n", 
                            category.getName(), entry.getValue()));
                    }
                });
        }

        return analysis.toString();
    }

    private String cleanGeneratedText(String generatedText) {
        if (generatedText == null) return "";
        
        // OpenRouter/DeepSeek returns clean text, just trim
        return generatedText.trim();
    }

    private String generateLocalFinancialAdvice(int userId, int walletId, String userMessage, String financialAnalysis) {
        StringBuilder advice = new StringBuilder();
        
        advice.append(financialAnalysis);
        advice.append("\n\n💡 Lời khuyên:\n");

        String lowerMessage = userMessage.toLowerCase();

        if (lowerMessage.contains("chi tiêu") || lowerMessage.contains("tiêu")) {
            advice.append("• Theo dõi chi tiêu hàng ngày để kiểm soát tốt hơn\n");
            advice.append("• Ưu tiên các khoản chi tiêu cần thiết\n");
            advice.append("• Cân nhắc giảm chi tiêu không cần thiết");
        } else if (lowerMessage.contains("tiết kiệm") || lowerMessage.contains("save")) {
            advice.append("• Đặt mục tiêu tiết kiệm cụ thể và khả thi\n");
            advice.append("• Tự động chuyển tiền tiết kiệm mỗi tháng\n");
            advice.append("• Áp dụng quy tắc 50/30/20: 50% nhu cầu, 30% mong muốn, 20% tiết kiệm");
        } else if (lowerMessage.contains("thu nhập") || lowerMessage.contains("income")) {
            advice.append("• Đa dạng hóa nguồn thu nhập nếu có thể\n");
            advice.append("• Đầu tư vào kỹ năng để tăng thu nhập\n");
            advice.append("• Cân bằng giữa thu nhập và chi tiêu");
        } else {
            advice.append("• Theo dõi tài chính đều đặn để có cái nhìn tổng quan\n");
            advice.append("• Cân bằng giữa chi tiêu và tiết kiệm\n");
            advice.append("• Đặt mục tiêu tài chính rõ ràng và đo lường được");
        }

        return advice.toString();
    }

    public interface ChatbotCallback {
        void onSuccess(String response);
        void onError(String error);
    }
}
