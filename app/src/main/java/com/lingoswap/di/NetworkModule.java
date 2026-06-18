package com.lingoswap.di;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lingoswap.data.api.AuthApiService;
import com.lingoswap.data.api.AuthInterceptor;
import com.lingoswap.data.api.ChatApiService;
import com.lingoswap.data.api.FriendApiService;
import com.lingoswap.data.api.MatchApiService;
import com.lingoswap.data.api.NotificationApiService;
import com.lingoswap.data.api.ReportApiService;
import com.lingoswap.data.api.PersistentCookieJar;
import com.lingoswap.data.api.UserApiService;
import com.lingoswap.data.local.UserPreferences;
import com.lingoswap.data.model.Message;
import com.lingoswap.data.remote.TimestampDeserializer;

import java.util.concurrent.TimeUnit;

import javax.inject.Provider;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    public static final String BASE_URL = "http://10.0.2.2:5000/";

    @Provides @Singleton
    public AuthInterceptor provideAuthInterceptor(UserPreferences prefs) {
        return new AuthInterceptor(prefs);
    }

    @Provides @Singleton
    public PersistentCookieJar provideCookieJar() {
        return PersistentCookieJar.getInstance();
    }

    @Provides @Singleton
    public OkHttpClient provideOkHttpClient(
            AuthInterceptor authInterceptor,
            PersistentCookieJar cookieJar
    ) {
        // Chỉ dùng AuthInterceptor cho việc gắn token + refresh 401 (1 đường duy nhất,
        // tránh đua refresh với Authenticator).
        return new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .cookieJar(cookieJar)
                .addInterceptor(new HttpLoggingInterceptor()
                        .setLevel(HttpLoggingInterceptor.Level.BODY))
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Provides @Singleton
    public Gson provideGson() {
        return new GsonBuilder()
                .registerTypeAdapter(Message.TimestampField.class, new TimestampDeserializer())
                .create();
    }

    @Provides @Singleton
    public Retrofit provideRetrofit(OkHttpClient client, Gson gson) {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    @Provides @Singleton public AuthApiService provideAuthApi(Retrofit r) { return r.create(AuthApiService.class); }
    @Provides @Singleton public UserApiService provideUserApi(Retrofit r) { return r.create(UserApiService.class); }
    @Provides @Singleton public ChatApiService provideChatApi(Retrofit r) { return r.create(ChatApiService.class); }
    @Provides @Singleton public FriendApiService provideFriendApi(Retrofit r) { return r.create(FriendApiService.class); }
    @Provides @Singleton public NotificationApiService provideNotifApi(Retrofit r) { return r.create(NotificationApiService.class); }
    @Provides @Singleton public MatchApiService provideMatchApi(Retrofit r) { return r.create(MatchApiService.class); }
    @Provides @Singleton public ReportApiService provideReportApi(Retrofit r) { return r.create(ReportApiService.class); }
}
