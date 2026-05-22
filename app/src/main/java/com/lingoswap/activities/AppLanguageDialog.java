package com.lingoswap.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.lingoswap.R;
import com.lingoswap.presentation.base.BaseActivity;
import com.lingoswap.utils.LocaleManager;

/**
 * AppLanguageDialog – Bottom sheet để người dùng chọn ngôn ngữ GIAO DIỆN app.
 *
 * Khác với LanguageChooserDialog (chọn ngôn ngữ luyện tập → MatchingActivity),
 * dialog này gọi LocaleManager.setLocale() và recreate() Activity ngay lập tức.
 *
 * Cách dùng từ bất kỳ Activity nào kế thừa BaseActivity:
 *   new AppLanguageDialog().show(getSupportFragmentManager(), "app_lang");
 */
public class AppLanguageDialog extends BottomSheetDialogFragment {

    private String selectedCode = null;
    private View selectedView   = null;

    // Danh sách ngôn ngữ giao diện hỗ trợ
    private static final String[] LANG_CODES = {
        LocaleManager.LANG_EN,
        LocaleManager.LANG_VI,
        LocaleManager.LANG_JA,
        LocaleManager.LANG_KO,
        LocaleManager.LANG_ZH,
        LocaleManager.LANG_FR,
        LocaleManager.LANG_DE,
        LocaleManager.LANG_ES
    };

    // R.id tương ứng 1-1 với LANG_CODES – dùng lại layout dialog_language_chooser
    private static final int[] LANG_IDS = {
        R.id.langEnglish,
        R.id.langVietnamese,
        R.id.langJapanese,
        R.id.langKorean,
        R.id.langChinese,
        R.id.langFrench,
        R.id.langGerman,
        R.id.langSpanish
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.dialog_language_chooser, container, false);

        // Lấy ngôn ngữ hiện tại để pre-select đúng option
        String currentLang = getCurrentAppLang();

        for (int i = 0; i < LANG_IDS.length; i++) {
            final String code = LANG_CODES[i];
            LinearLayout option = view.findViewById(LANG_IDS[i]);
            if (option == null) continue;

            option.setOnClickListener(v -> selectLanguage(v, code));

            // Pre-select ngôn ngữ đang dùng
            if (code.equals(currentLang)) {
                selectLanguage(option, code);
            }
        }

        // Thay label nút confirm thành "Apply" thay vì "Start Matching"
        Button btnConfirm = view.findViewById(R.id.btnStartMatching);
        if (btnConfirm != null) {
            btnConfirm.setText(R.string.apply); // thêm string "apply" = "Apply" / "Áp dụng"
            btnConfirm.setOnClickListener(v -> applyLanguage());
        }

        Button btnClose  = view.findViewById(R.id.btnClose);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        if (btnClose  != null) btnClose.setOnClickListener(v -> dismiss());
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dismiss());

        return view;
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private String getCurrentAppLang() {
        if (getActivity() instanceof BaseActivity) {
            return ((BaseActivity<?>) getActivity()).getCurrentLanguage();
        }
        return LocaleManager.LANG_EN;
    }

    private void applyLanguage() {
        if (selectedCode == null) return;
        dismiss();
        if (getActivity() instanceof BaseActivity) {
            // changeLanguage() đã có sẵn trong BaseActivity:
            // gọi localeManager.setLocale() + recreate()
            ((BaseActivity<?>) getActivity()).changeLanguage(selectedCode);
        }
    }

    private void selectLanguage(View view, String langCode) {
        // Bỏ highlight option cũ
        if (selectedView != null) {
            selectedView.setBackgroundResource(R.drawable.bg_lang_option);
            updateTextColor(selectedView, false);
        }
        // Highlight option mới
        selectedView = view;
        selectedCode = langCode;
        view.setBackgroundResource(R.drawable.bg_lang_option_selected);
        updateTextColor(view, true);
    }

    private void updateTextColor(View view, boolean isSelected) {
        if (!(view instanceof ViewGroup)) return;
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
