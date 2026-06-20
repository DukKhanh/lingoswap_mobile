package com.lingoswap.activities;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.lingoswap.R;
import com.lingoswap.data.local.UserPreferences;
import com.lingoswap.data.model.User;
import com.lingoswap.databinding.ActivityProfileBinding;
import com.lingoswap.presentation.base.BaseActivity;
import com.lingoswap.presentation.profile.ProfileViewModel;
import com.lingoswap.utils.LocaleManager;
import com.lingoswap.utils.ThemeManager;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

@AndroidEntryPoint
public class ProfileActivity extends BaseActivity<ActivityProfileBinding> {

    @Inject UserPreferences userPreferences;
    @Inject com.lingoswap.utils.SocketManager socketManager;
    @Inject com.lingoswap.utils.HeartbeatManager heartbeatManager;

    private ProfileViewModel viewModel;
    private String pendingTheme; // theme đang chọn trên UI (light/dark) để gửi khi save

    private final ActivityResultLauncher<String> avatarPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) uploadAvatar(uri);
            });

    @Override
    protected ActivityProfileBinding inflateBinding(LayoutInflater inflater) {
        return ActivityProfileBinding.inflate(inflater);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        String[] countries = getResources().getStringArray(R.array.countries_array);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, countries);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCountry.setAdapter(adapter);

        bindCachedProfile();

        pendingTheme = isDarkMode() ? "dark" : "light";
        updateAppearanceUI();
        updateLanguageButtonLabel();

        binding.tvBackHome.setOnClickListener(v -> finish());

        binding.btnSaveProfile.setOnClickListener(v -> {
            String name     = binding.etFullName.getText().toString().trim();
            String newEmail = binding.etEmail.getText().toString().trim();
            String newBio   = binding.etBio.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                binding.etFullName.setError(getString(R.string.full_name) + " empty");
                binding.etFullName.requestFocus();
                return;
            }
            if (!TextUtils.isEmpty(newEmail) && !Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                binding.etEmail.setError("Email invalid");
                binding.etEmail.requestFocus();
                return;
            }
            viewModel.updateProfile(name, newBio, pendingTheme);
        });

        binding.btnUpdatePassword.setOnClickListener(v -> {
            String curPw     = binding.etCurrentPw.getText().toString();
            String newPw     = binding.etNewPw.getText().toString();
            String confirmPw = binding.etConfirmNewPw.getText().toString();

            if (TextUtils.isEmpty(curPw)) {
                binding.etCurrentPw.setError(getString(R.string.current_password) + " empty");
                binding.etCurrentPw.requestFocus();
                return;
            }
            if (newPw.length() < 8) {
                binding.etNewPw.setError("Min 8 chars");
                binding.etNewPw.requestFocus();
                return;
            }
            if (!newPw.equals(confirmPw)) {
                binding.etConfirmNewPw.setError("Not match");
                binding.etConfirmNewPw.requestFocus();
                return;
            }
            viewModel.changePassword(curPw, newPw);
        });

        binding.appearLight.setOnClickListener(v -> {
            pendingTheme = "light";
            themeManager.setTheme(ThemeManager.LIGHT);
            recreate();
        });
        binding.appearDark.setOnClickListener(v -> {
            pendingTheme = "dark";
            themeManager.setTheme(ThemeManager.DARK);
            recreate();
        });

        binding.btnChangeLanguage.setOnClickListener(this::showLanguageMenu);

        binding.btnSaveSettings.setOnClickListener(v -> {
            String name = binding.etFullName.getText().toString().trim();
            String bio  = binding.etBio.getText().toString().trim();
            if (TextUtils.isEmpty(name)) name = userPreferences.getFullName();
            viewModel.updateProfile(name, bio, pendingTheme);
        });

        binding.flAvatar.setOnClickListener(v -> avatarPicker.launch("image/*"));

        binding.navHome.setOnClickListener(v -> goTo(com.lingoswap.activities.HomeActivity.class));
        binding.navFriends.setOnClickListener(v -> goTo(com.lingoswap.presentation.friends.FriendsActivity.class));
        binding.navMatch.setOnClickListener(v -> goTo(com.lingoswap.activities.HomeActivity.class));
        binding.navChat.setOnClickListener(v -> goTo(com.lingoswap.presentation.chat.ConversationListActivity.class));
        binding.navProfile.setOnClickListener(v -> { /* đang ở Profile */ });

        binding.btnLogout.setOnClickListener(v -> confirmLogout());
    }

    private void goTo(Class<?> target) {
        Intent intent = new Intent(this, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        finish();
    }

    @Override
    protected void observeViewModel() {
        viewModel.profile.observe(this, this::bindProfile);
        viewModel.successMessage.observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
        viewModel.error.observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        });
        viewModel.loggedOut.observe(this, done -> {
            if (Boolean.TRUE.equals(done)) navigateToSignIn();
        });
        viewModel.avatarUrl.observe(this, url -> {
            if (!TextUtils.isEmpty(url)) loadAvatar(url);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadProfile();
    }

    private void bindCachedProfile() {
        String fullName = userPreferences.getFullName();
        String email    = userPreferences.getEmail();
        binding.etFullName.setText(fullName);
        binding.etEmail.setText(email);
        binding.tvHeroName.setText(TextUtils.isEmpty(fullName) ? "User" : fullName);
        binding.tvHeroEmail.setText(email);
        binding.tvHeroInitial.setText(initialOf(fullName));
        binding.spinnerCountry.setSelection(getCountryIndex(userPreferences.getCountry(), 0));
    }

    private void bindProfile(User user) {
        if (user == null) return;

        String fullName = user.getProfile() != null ? user.getProfile().getFullName() : "";
        String bio      = user.getProfile() != null ? user.getProfile().getBio() : "";
        String country  = user.getProfile() != null ? user.getProfile().getCountry() : "";
        String email    = user.getEmail();

        if (fullName == null) fullName = "";
        if (email == null) email = "";

        binding.etFullName.setText(fullName);
        binding.etBio.setText(bio == null ? "" : bio);
        binding.etEmail.setText(email);
        binding.tvHeroName.setText(fullName.isEmpty() ? "User" : fullName);
        binding.tvHeroEmail.setText(email);
        binding.tvHeroInitial.setText(initialOf(fullName));
        binding.spinnerCountry.setSelection(getCountryIndex(country, 0));

        if (user.getStats() != null) {
            binding.tvHeroStreak.setText(getString(R.string.home_streak_value, user.getStats().getStreak()));
            binding.tvHeroSessions.setText(String.valueOf(user.getStats().getTotalSessions()));
        }

        if (user.getProfile() != null && !TextUtils.isEmpty(user.getProfile().getAvatar())) {
            loadAvatar(user.getProfile().getAvatar());
        }

        if (user.getSettings() != null && user.getSettings().getTheme() != null) {
            pendingTheme = user.getSettings().getTheme();
        }

        userPreferences.saveProfile(fullName, bio, country);
    }

    private String initialOf(String name) {
        return TextUtils.isEmpty(name) ? "U" : String.valueOf(name.charAt(0)).toUpperCase();
    }

    private void loadAvatar(String url) {
        binding.ivAvatar.setVisibility(View.VISIBLE);
        binding.tvHeroInitial.setVisibility(View.GONE);
        Glide.with(this).load(com.lingoswap.utils.ImageUtils.normalizeAvatar(url))
                .circleCrop().into(binding.ivAvatar);
    }

    /** Đọc ảnh từ Uri → multipart field "avatar" → PUT /api/users/me/avatar. */
    private void uploadAvatar(Uri uri) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            if (in == null) {
                Toast.makeText(this, "Không đọc được ảnh đã chọn", Toast.LENGTH_SHORT).show();
                return;
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int n;
            while ((n = in.read(chunk)) != -1) buffer.write(chunk, 0, n);
            in.close();
            byte[] bytes = buffer.toByteArray();

            String mime = getContentResolver().getType(uri);
            if (mime == null) mime = "image/*";
            RequestBody body = RequestBody.create(MediaType.parse(mime), bytes);
            MultipartBody.Part part = MultipartBody.Part.createFormData("avatar", "avatar.jpg", body);

            binding.ivAvatar.setVisibility(View.VISIBLE);
            binding.tvHeroInitial.setVisibility(View.GONE);
            Glide.with(this).load(uri).circleCrop().into(binding.ivAvatar);

            viewModel.uploadAvatar(part);
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi xử lý ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.profile_logout_confirm_title)
                .setMessage(R.string.profile_logout_confirm_msg)
                .setPositiveButton(R.string.profile_btn_logout, (d, w) -> viewModel.logout())
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void navigateToSignIn() {
        // Ngắt socket + dừng heartbeat để không giữ phiên của tài khoản vừa đăng xuất.
        heartbeatManager.stop();
        socketManager.disconnect();

        Intent intent = new Intent(this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private int getCountryIndex(String countryCode, int fallback) {
        if (TextUtils.isEmpty(countryCode)) return fallback;
        String[] countries = getResources().getStringArray(R.array.countries_array);
        String lower = countryCode.toLowerCase();
        String keyword;
        switch (lower) {
            case "vi": case "vn": keyword = "Vietnam";       break;
            case "us": case "en": keyword = "United States"; break;
            case "jp": case "ja": keyword = "Japan";         break;
            case "kr": case "ko": keyword = "Korea";         break;
            case "fr":            keyword = "France";        break;
            case "de":            keyword = "Germany";       break;
            case "es":            keyword = "Spain";         break;
            default:              return fallback;
        }
        for (int i = 0; i < countries.length; i++) {
            if (countries[i].contains(keyword)) return i;
        }
        return fallback;
    }

    private void updateAppearanceUI() {
        boolean dark = isDarkMode();
        binding.appearLight.setBackgroundResource(dark ? 0 : R.drawable.bg_lang_option_selected);
        binding.appearDark.setBackgroundResource(dark ? R.drawable.bg_lang_option_selected : 0);
        binding.frameLightCheck.setVisibility(dark ? View.GONE  : View.VISIBLE);
        binding.frameDarkCheck.setVisibility(dark ? View.VISIBLE : View.GONE);
    }

    private void updateLanguageButtonLabel() {
        String langName = getLangDisplayName(getCurrentLanguage());
        binding.btnChangeLanguage.setText(langName);
    }

    private String getLangDisplayName(String code) {
        return LocaleManager.LANG_VI.equals(code) ? "Tiếng Việt" : "English";
    }
}
