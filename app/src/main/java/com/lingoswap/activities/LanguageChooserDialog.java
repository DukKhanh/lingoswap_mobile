package com.lingoswap.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.lingoswap.R;

public class LanguageChooserDialog extends BottomSheetDialogFragment {

    private String selectedLanguage = null;
    private View selectedView = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_language_chooser, container, false);

        Button btnClose = view.findViewById(R.id.btnClose);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnStartMatching = view.findViewById(R.id.btnStartMatching);

        // Language options
        int[] langIds = {
            R.id.langEnglish, R.id.langVietnamese, R.id.langJapanese,
            R.id.langKorean, R.id.langChinese, R.id.langFrench,
            R.id.langGerman, R.id.langSpanish
        };
        String[] langCodes = {"English", "Tiếng Việt", "日本語", "한국어", "中文", "Français", "Deutsch", "Español"};

        for (int i = 0; i < langIds.length; i++) {
            final String lang = langCodes[i];
            LinearLayout option = view.findViewById(langIds[i]);
            option.setOnClickListener(v -> selectLanguage(v, lang));
        }

        // Pre-select English
        LinearLayout englishOption = view.findViewById(R.id.langEnglish);
        selectLanguage(englishOption, "English");

        if (btnClose != null) btnClose.setOnClickListener(v -> dismiss());
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dismiss());

        if (btnStartMatching != null) {
            btnStartMatching.setOnClickListener(v -> {
                if (selectedLanguage == null) {
                    Toast.makeText(getContext(), "Please select a language", Toast.LENGTH_SHORT).show();
                    return;
                }
                dismiss();
                Intent intent = new Intent(getContext(), MatchingActivity.class);
                intent.putExtra("language", selectedLanguage);
                startActivity(intent);
            });
        }

        return view;
    }

    private void selectLanguage(View view, String language) {
        // Deselect previous
        if (selectedView != null) {
            selectedView.setBackgroundResource(R.drawable.bg_lang_option);
            updateTextColor(selectedView, false);
        }
        // Select new
        selectedView = view;
        selectedLanguage = language;
        view.setBackgroundResource(R.drawable.bg_lang_option_selected);
        updateTextColor(view, true);
    }

    private void updateTextColor(View view, boolean isSelected) {
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View child = vg.getChildAt(i);
                if (child instanceof TextView) {
                    TextView tv = (TextView) child;
                    if (isSelected) {
                        tv.setTextColor(ContextCompat.getColor(getContext(), R.color.blue));
                        // Dòng thứ 2 thường là tên ngôn ngữ, ta cho bold lên khi được chọn
                        if (i == 1) tv.setTypeface(null, android.graphics.Typeface.BOLD);
                    } else {
                        tv.setTextColor(ContextCompat.getColor(getContext(), R.color.text_dark));
                        if (i == 1) tv.setTypeface(null, android.graphics.Typeface.NORMAL);
                    }
                }
            }
        }
    }
}
