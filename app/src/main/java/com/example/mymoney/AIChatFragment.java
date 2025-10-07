package com.example.mymoney;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymoney.chatbot.ChatAdapter;
import com.example.mymoney.chatbot.ChatMessage;
import com.example.mymoney.chatbot.ChatbotService;

import java.util.HashMap;
import java.util.Map;

public class AIChatFragment extends Fragment {

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private EditText messageInput;
    private ImageView sendButton;
    private ChatbotService chatbotService;
    private LinearLayout suggestedQuestion1, suggestedQuestion2;
    private TextView suggestedText1, suggestedText2;

    // 🔹 Static cache to preserve chat history per wallet
    private static Map<String, ChatAdapter> chatHistoryCache = new HashMap<>();
    private int currentUserId = -1;
    private int currentWalletId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_ai_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        currentUserId = MainActivity.getCurrentUserId();
        currentWalletId = MainActivity.getSelectedWalletId();
        
        // Initialize views
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        messageInput = view.findViewById(R.id.message_input);
        sendButton = view.findViewById(R.id.send_button);
        
        // Setup RecyclerView with cached or new adapter
        setupChatAdapter();
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        chatRecyclerView.setAdapter(chatAdapter);
        
        // Initialize chatbot service
        chatbotService = new ChatbotService(requireContext());
        
        // Setup suggested questions
        setupSuggestedQuestions(view);
        
        // Setup send button
        sendButton.setOnClickListener(v -> sendMessage());
        
        // Add welcome message if this is a new chat
        if (chatAdapter.getItemCount() == 0) {
            addWelcomeMessage();
        } else {
            // Scroll to bottom if restoring chat
            scrollToBottom();
        }
    }
    
    /**
     * Setup chat adapter - either restore from cache or create new one
     */
    private void setupChatAdapter() {
        String cacheKey = getCacheKey();
        
        if (chatHistoryCache.containsKey(cacheKey)) {
            // Restore existing chat history
            chatAdapter = chatHistoryCache.get(cacheKey);
        } else {
            // Create new chat adapter
            chatAdapter = new ChatAdapter();
            chatHistoryCache.put(cacheKey, chatAdapter);
        }
    }
    
    /**
     * Generate unique cache key for user+wallet combination
     */
    private String getCacheKey() {
        return "user_" + currentUserId + "_wallet_" + currentWalletId;
    }
    
    /**
     * Public method to clear chat history for current wallet
     */
    public void clearChatHistory() {
        String cacheKey = getCacheKey();
        chatHistoryCache.remove(cacheKey);
        if (chatAdapter != null) {
            chatAdapter.clearMessages();
            addWelcomeMessage();
        }
    }
    
    /**
     * Static method to clear all chat history (for logout, etc.)
     */
    public static void clearAllChatHistory() {
        chatHistoryCache.clear();
    }

    
    private void setupSuggestedQuestions(View view) {
        suggestedQuestion1 = view.findViewById(R.id.suggested_question_1);
        suggestedQuestion2 = view.findViewById(R.id.suggested_question_2);
        
        if (suggestedQuestion1 != null) {
            suggestedQuestion1.setOnClickListener(v -> {
                messageInput.setText("Tôi nên chi tiêu như thế nào?");
                sendMessage();
            });
        }
        
        if (suggestedQuestion2 != null) {
            suggestedQuestion2.setOnClickListener(v -> {
                messageInput.setText("Nhận xét chi tiêu tháng qua của tôi");
                sendMessage();
            });
        }
    }
    
    private void addWelcomeMessage() {
        ChatMessage welcomeMessage = new ChatMessage(
            "Xin chào! 👋 Tôi là trợ lý tài chính của bạn.\n\n" +
            "Tôi có thể giúp bạn:\n" +
            "• Phân tích chi tiêu\n" +
            "• Lời khuyên tiết kiệm\n" +
            "• Đánh giá tình hình tài chính\n\n" +
            "Hãy hỏi tôi bất cứ điều gì về tài chính của bạn!",
            false
        );
        chatAdapter.addMessage(welcomeMessage);
        scrollToBottom();
    }
    
    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        
        if (message.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập tin nhắn", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Add user message
        ChatMessage userMessage = new ChatMessage(message, true);
        chatAdapter.addMessage(userMessage);
        scrollToBottom();
        
        // Clear input
        messageInput.setText("");
        
        // Add loading indicator
        ChatMessage loadingMessage = new ChatMessage(true);
        chatAdapter.addMessage(loadingMessage);
        scrollToBottom();
        
        // Get AI response (wallet-specific)
        int userId = MainActivity.getCurrentUserId();
        int walletId = MainActivity.getSelectedWalletId(); // 🔹 Pass wallet ID
        
        chatbotService.generateFinancialAdvice(userId, walletId, message, new ChatbotService.ChatbotCallback() {
            @Override
            public void onSuccess(String response) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Remove loading indicator
                        chatAdapter.removeLastMessage();
                        
                        // Add bot response
                        ChatMessage botMessage = new ChatMessage(response, false);
                        chatAdapter.addMessage(botMessage);
                        scrollToBottom();
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Remove loading indicator
                        chatAdapter.removeLastMessage();
                        
                        // Add error message
                        ChatMessage errorMessage = new ChatMessage(
                            "Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại sau.",
                            false
                        );
                        chatAdapter.addMessage(errorMessage);
                        scrollToBottom();
                        
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    private void scrollToBottom() {
        if (chatAdapter.getItemCount() > 0) {
            chatRecyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
        }
    }
}

