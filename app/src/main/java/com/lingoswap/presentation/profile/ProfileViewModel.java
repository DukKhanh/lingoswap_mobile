package com.lingoswap.presentation.profile;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lingoswap.data.model.ApiResponse;
import com.lingoswap.data.model.User;
import com.lingoswap.data.repository.RepositoryCallback;
import com.lingoswap.domain.repository.AuthRepository;
import com.lingoswap.domain.repository.UserRepository;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ProfileViewModel — nạp & cập nhật hồ sơ thật:
 *  GET /api/users/me, PUT /api/users/me, PATCH /api/auth/password, POST /api/auth/logout.
 */
@HiltViewModel
public class ProfileViewModel extends ViewModel {

    private final UserRepository userRepository;
    private final AuthRepository authRepository;

    public final MutableLiveData<User>    profile        = new MutableLiveData<>();
    public final MutableLiveData<Boolean> isLoading      = new MutableLiveData<>(false);
    public final MutableLiveData<String>  error          = new MutableLiveData<>();
    public final MutableLiveData<String>  successMessage = new MutableLiveData<>();
    public final MutableLiveData<Boolean> loggedOut      = new MutableLiveData<>(false);
    public final MutableLiveData<String>  avatarUrl      = new MutableLiveData<>();

    @Inject
    public ProfileViewModel(UserRepository userRepository, AuthRepository authRepository) {
        this.userRepository = userRepository;
        this.authRepository = authRepository;
    }

    public void loadProfile() {
        isLoading.setValue(true);
        userRepository.getProfile(new RepositoryCallback<User>() {
            @Override public void onSuccess(User data) {
                profile.postValue(data);
                isLoading.postValue(false);
            }
            @Override public void onError(String message) {
                error.postValue(message);
                isLoading.postValue(false);
            }
        });
    }

    /** PUT /api/users/me — cập nhật fullName, bio, theme. */
    public void updateProfile(String fullName, String bio, String theme) {
        isLoading.setValue(true);
        Map<String, Object> profileMap = new HashMap<>();
        profileMap.put("fullName", fullName);
        profileMap.put("bio", bio);

        Map<String, Object> body = new HashMap<>();
        body.put("profile", profileMap);
        if (theme != null) {
            Map<String, Object> settings = new HashMap<>();
            settings.put("theme", theme);
            body.put("settings", settings);
        }

        userRepository.updateProfile(body, new RepositoryCallback<Map<String, Object>>() {
            @Override public void onSuccess(Map<String, Object> data) {
                Object msg = data != null ? data.get("message") : null;
                successMessage.postValue(msg != null ? msg.toString() : "Cập nhật hồ sơ thành công");
                isLoading.postValue(false);
            }
            @Override public void onError(String message) {
                error.postValue(message);
                isLoading.postValue(false);
            }
        });
    }

    public void changePassword(String currentPassword, String newPassword) {
        isLoading.setValue(true);
        authRepository.changePassword(currentPassword, newPassword,
                new RepositoryCallback<ApiResponse>() {
                    @Override public void onSuccess(ApiResponse data) {
                        successMessage.postValue(
                                data != null && data.message != null ? data.message : "Đổi mật khẩu thành công");
                        isLoading.postValue(false);
                    }
                    @Override public void onError(String message) {
                        error.postValue(message);
                        isLoading.postValue(false);
                    }
                });
    }

    /** PUT /api/users/me/avatar (multipart field "avatar"). */
    public void uploadAvatar(okhttp3.MultipartBody.Part avatar) {
        isLoading.setValue(true);
        userRepository.uploadAvatar(avatar, new RepositoryCallback<Map<String, String>>() {
            @Override public void onSuccess(Map<String, String> data) {
                isLoading.postValue(false);
                String url = data != null ? data.get("avatarUrl") : null;
                avatarUrl.postValue(url);
                successMessage.postValue("Cập nhật avatar thành công");
            }
            @Override public void onError(String message) {
                isLoading.postValue(false);
                error.postValue(message);
            }
        });
    }

    public void logout() {
        authRepository.logout(new RepositoryCallback<Void>() {
            @Override public void onSuccess(Void data) { loggedOut.postValue(true); }
            @Override public void onError(String message) { loggedOut.postValue(true); }
        });
    }
}
