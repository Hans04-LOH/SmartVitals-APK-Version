package com.example.smartvitals;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AiChatActivity extends AppCompatActivity {

    // Emulator uses 10.0.2.2 to reach your computer localhost
    private static final String BACKEND_URL = "http://10.0.2.2:3000/analyzeHealthText";

    private EditText etUserMessage;
    private AppCompatButton btnSend;
    private LinearLayout chatContainer;
    private ScrollView scrollChat;
    private ImageView btnBack;

    private final OkHttpClient client = new OkHttpClient();
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        etUserMessage = findViewById(R.id.etUserMessage);
        btnSend = findViewById(R.id.btnSend);
        chatContainer = findViewById(R.id.chatContainer);
        scrollChat = findViewById(R.id.scrollChat);
        btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        addBotMessage("Hello, I’m your AI health assistant. Ask me about symptoms, healthy habits, or what to do next.");

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String userText = etUserMessage.getText().toString().trim();
        if (userText.isEmpty() || isLoading) return;

        addUserMessage(userText);
        etUserMessage.setText("");

        setLoading(true);
        addBotMessage("Thinking...");

        try {
            JSONObject json = new JSONObject();
            json.put("age", 0);
            json.put("heartRate", 0);
            json.put("spo2", 0);
            json.put("symptoms", userText);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(BACKEND_URL)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        removeLastBotIfThinking();
                        addBotMessage("Sorry, backend request failed: " + e.getMessage());
                        setLoading(false);
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    runOnUiThread(() -> {
                        removeLastBotIfThinking();

                        try {
                            if (!response.isSuccessful()) {
                                addBotMessage("Sorry, backend error: " + response.code());
                            } else {
                                JSONObject obj = new JSONObject(responseBody);
                                String reply = obj.optString("reply", "").trim();

                                if (reply.isEmpty()) {
                                    addBotMessage("Sorry, I could not generate a response.");
                                } else {
                                    addBotMessage(reply);
                                }
                            }
                        } catch (Exception e) {
                            addBotMessage("Sorry, response parse failed: " + e.getMessage());
                        }

                        setLoading(false);
                    });
                }
            });

        } catch (Exception e) {
            removeLastBotIfThinking();
            addBotMessage("Sorry, request build failed: " + e.getMessage());
            setLoading(false);
        }
    }

    private void setLoading(boolean loading) {
        isLoading = loading;
        btnSend.setEnabled(!loading);
        etUserMessage.setEnabled(!loading);
    }

    private void addUserMessage(String text) {
        TextView tv = buildBubble(text, true);
        chatContainer.addView(tv);
        scrollToBottom();
    }

    private void addBotMessage(String text) {
        TextView tv = buildBubble(text, false);
        tv.setTag("bot_message");
        chatContainer.addView(tv);
        scrollToBottom();
    }

    private void removeLastBotIfThinking() {
        int count = chatContainer.getChildCount();
        if (count <= 0) return;

        View last = chatContainer.getChildAt(count - 1);
        if (last instanceof TextView) {
            CharSequence text = ((TextView) last).getText();
            if ("Thinking...".contentEquals(text)) {
                chatContainer.removeView(last);
            }
        }
    }

    private TextView buildBubble(String text, boolean isUser) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(14f);
        tv.setPadding(24, 18, 24, 18);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = 16;
        lp.bottomMargin = 4;

        if (isUser) {
            lp.gravity = android.view.Gravity.END;
            tv.setBackgroundResource(R.drawable.bg_chat_user);
            tv.setTextColor(0xFFFFFFFF);
        } else {
            lp.gravity = android.view.Gravity.START;
            tv.setBackgroundResource(R.drawable.bg_chat_bot);
            tv.setTextColor(0xFF0F172A);
        }

        tv.setLayoutParams(lp);
        return tv;
    }

    private void scrollToBottom() {
        scrollChat.post(() -> scrollChat.fullScroll(View.FOCUS_DOWN));
    }
}