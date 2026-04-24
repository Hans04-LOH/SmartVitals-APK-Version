package com.example.smartvitals;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.GenerativeBackend;

public class ScanFragment extends Fragment {

    private ObjectAnimator scanLineAnimator;
    private ObjectAnimator innerPulseAnimator;
    private ObjectAnimator outerPulseAnimator;

    private ImageView ivScanCirclePreview;
    private ImageView ivScanCenterIcon;
    private View viewScanLine;
    private TextView tvScanResult;
    private ProgressBar progressScan;
    private AppCompatButton btnTakePhoto;
    private AppCompatButton btnAnalyze;

    private Bitmap capturedBitmap;
    private GenerativeModelFutures model;

    private final ActivityResultLauncher<Void> takePicturePreviewLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    capturedBitmap = bitmap;

                    ivScanCirclePreview.setImageBitmap(bitmap);
                    ivScanCirclePreview.setVisibility(View.VISIBLE);

                    ivScanCenterIcon.setVisibility(View.GONE);

                    hideScanLineAfterPhoto();

                    tvScanResult.setText("Photo captured. Tap Analyze with AI.");
                    btnAnalyze.setEnabled(true);
                } else {
                    tvScanResult.setText("No photo captured.");
                }
            });

    public ScanFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View scanOuterRing = view.findViewById(R.id.scanOuterRing);
        View scanInnerRing = view.findViewById(R.id.scanInnerRing);
        viewScanLine = view.findViewById(R.id.viewScanLine);

        ivScanCirclePreview = view.findViewById(R.id.ivScanCirclePreview);
        ivScanCenterIcon = view.findViewById(R.id.ivScanCenterIcon);
        tvScanResult = view.findViewById(R.id.tvScanResult);
        progressScan = view.findViewById(R.id.progressScan);
        btnTakePhoto = view.findViewById(R.id.btnTakePhoto);
        btnAnalyze = view.findViewById(R.id.btnAnalyze);

        btnAnalyze.setEnabled(false);
        progressScan.setVisibility(View.GONE);

        setupAnimations(scanOuterRing, scanInnerRing, viewScanLine);
        setupGeminiModel();
        setupClicks();
    }

    private void setupAnimations(View scanOuterRing, View scanInnerRing, View scanLine) {
        float travelDistance = 62f * getResources().getDisplayMetrics().density;

        scanLineAnimator = ObjectAnimator.ofFloat(scanLine, "translationY", -travelDistance, travelDistance);
        scanLineAnimator.setDuration(1800);
        scanLineAnimator.setRepeatCount(ValueAnimator.INFINITE);
        scanLineAnimator.setRepeatMode(ValueAnimator.REVERSE);
        scanLineAnimator.setInterpolator(new LinearInterpolator());
        scanLineAnimator.start();

        innerPulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                scanInnerRing,
                PropertyValuesHolder.ofFloat(View.ALPHA, 0.72f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0.98f, 1.02f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.98f, 1.02f)
        );
        innerPulseAnimator.setDuration(1000);
        innerPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        innerPulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        innerPulseAnimator.start();

        outerPulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                scanOuterRing,
                PropertyValuesHolder.ofFloat(View.ALPHA, 0.55f, 0.9f),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0.985f, 1.015f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.985f, 1.015f)
        );
        outerPulseAnimator.setDuration(1300);
        outerPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        outerPulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        outerPulseAnimator.start();
    }

    private void setupGeminiModel() {
        GenerativeModel ai = FirebaseAI.getInstance(GenerativeBackend.googleAI())
                .generativeModel("gemini-2.5-flash");

        model = GenerativeModelFutures.from(ai);
    }

    private void setupClicks() {
        btnTakePhoto.setOnClickListener(v -> takePicturePreviewLauncher.launch(null));

        btnAnalyze.setOnClickListener(v -> {
            if (capturedBitmap == null) {
                tvScanResult.setText("Please take a photo first.");
                return;
            }
            analyzeImageWithGemini(capturedBitmap);
        });
    }

    private void analyzeImageWithGemini(Bitmap bitmap) {
        setLoading(true);

        String prompt =
                "Read the text in this image and transform it into a clean, professional health guide.\n\n" +
                        "Style:\n" +
                        "- Minimalist\n" +
                        "- High readability\n" +
                        "- Brief and direct\n" +
                        "- Plenty of white space\n" +
                        "- No long paragraphs\n\n" +
                        "Formatting:\n" +
                        "- Bold headings\n" +
                        "- Short bullet points\n" +
                        "- Functional emojis as visual anchors only:\n" +
                        "  ✅ Action\n" +
                        "  ⚠️ Warning\n" +
                        "  💡 Tip\n" +
                        "  🩺 Symptoms\n\n" +
                        "Rules:\n" +
                        "- Preserve the original meaning\n" +
                        "- Remove repetition and filler words\n" +
                        "- Make the most important points instantly scannable\n" +
                        "- Do not invent extra health advice\n" +
                        "- Do not present the content as a confirmed diagnosis\n\n" +
                        "Structure:\n" +
                        "1. Title\n" +
                        "2. 🩺 Key Signs\n" +
                        "3. ✅ What To Do\n" +
                        "4. ⚠️ Warnings\n" +
                        "5. 💡 Quick Tips\n" +
                        "6. When to Seek Professional Care";

        Content content = new Content.Builder()
                .addImage(bitmap)
                .addText(prompt)
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String text = result.getText();

                if (text == null || text.trim().isEmpty()) {
                    tvScanResult.setText("No result returned.");
                } else {
                    tvScanResult.setText(text);
                }

                setLoading(false);
            }

            @Override
            public void onFailure(Throwable t) {
                tvScanResult.setText("Analysis failed: " + t.getMessage());
                setLoading(false);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void hideScanLineAfterPhoto() {
        if (scanLineAnimator != null) {
            scanLineAnimator.cancel();
        }

        if (viewScanLine != null) {
            viewScanLine.setVisibility(View.GONE);
        }
    }

    private void setLoading(boolean isLoading) {
        progressScan.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnTakePhoto.setEnabled(!isLoading);
        btnAnalyze.setEnabled(!isLoading && capturedBitmap != null);
    }

    @Override
    public void onDestroyView() {
        if (scanLineAnimator != null) {
            scanLineAnimator.cancel();
        }
        if (innerPulseAnimator != null) {
            innerPulseAnimator.cancel();
        }
        if (outerPulseAnimator != null) {
            outerPulseAnimator.cancel();
        }
        super.onDestroyView();
    }
}