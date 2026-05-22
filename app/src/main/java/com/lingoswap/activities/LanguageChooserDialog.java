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
import com.lingoswap.utils.LocaleManager;

public class LanguageChooserDialog extends BottomSheetDialogFragment {

    public static LanguageChooserDialog newInstance() {
        return new LanguageChooserDialog();
    }

    private String selectedLanguageCode = null;
    private View selectedView = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_language_chooser, container, false);

        Button btnClose        = view.findViewById(R.id.btnClose);
        Button btnCancel       = view.findViewById(R.id.btnCancel);
        Button btnStartMatching = view.findViewById(R.id.btnStartMatching);

        int[] langIds = {
            R.id.langEnglish, R.id.langVietnamese, R.id.langJapanese,
            R.id.langKorean,  R.id.langChinese,    R.id.langFrench,
            R.id.langGerman,  R.id.langSpanish
        };
        String[] langCodes = {
            LocaleManager.LANG_EN,
            LocaleManager.LANG_VI,
            LocaleManager.LANG_JA,
            LocaleManager.LANG_KO,
            LocaleManager.LANG_ZH,
            LocaleManager.LANG_FR,
            LocaleManager.LANG_DE,
            LocaleManager.LANG_ES
        };

        for (int i = 0; i < langIds.length; i++) {
            final String code = langCodes[i];
            LinearLayout option = view.findViewById(langIds[i]);
            if (option != null) {
                option.setOnClickListener(v -> selectLanguage(v, code));
            }
        }

        // Pre-select English
        LinearLayout englishOption = view.findViewById(R.id.langEnglish);
        if (englishOption != null) {
            selectLanguage(englishOption, LocaleManager.LANG_EN);
        }

        if (btnClose  != null) btnClose.setOnClickListener(v -> dismiss());
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dismiss());

        if (btnStartMatching != null) {
            btnStartMatching.setOnClickListener(v -> {
                if (selectedLanguageCode == null) {
                    Toast.makeText(getContext(),
                        getString(R.string.lang_chooser_select_prompt), Toast.LENGTH_SHORT).show();
                    return;
                }
                dismiss();
                
                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).startMatching(selectedLanguageCode);
                } else {
                    Intent intent = new Intent(getContext(), MatchingActivity.class);
                    intent.putExtra("language", selectedLanguageCode);
                    startActivity(intent);
                }
            });
        }

        return view;
    }

    private void selectLanguage(View view, String langCode) {
        if (selectedView != null) {
            selectedView.setBackgroundResource(R.drawable.bg_lang_option);
            updateTextColor(selectedView, false);
        }
        selectedView        = view;
        selectedLanguageCode = langCode;
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
                        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue));
                        if (i == 1) tv.setTypeface(null, android.graphics.Typeface.BOLD);
                    } else {
                        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_dark));
                        if (i == 1) tv.setTypeface(null, android.graphics.Typeface.NORMAL);
                    }
                }
            }
        }
    }
}
