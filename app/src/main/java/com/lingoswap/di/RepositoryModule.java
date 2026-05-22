package com.lingoswap.di;

import com.lingoswap.data.repository.AuthRepositoryImpl;
import com.lingoswap.data.repository.ChatRepositoryImpl;
import com.lingoswap.data.repository.FriendRepositoryImpl;
import com.lingoswap.data.repository.UserRepositoryImpl;
import com.lingoswap.domain.repository.AuthRepository;
import com.lingoswap.domain.repository.ChatRepository;
import com.lingoswap.domain.repository.FriendRepository;
import com.lingoswap.domain.repository.UserRepository;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public abstract class RepositoryModule {

    @Binds @Singleton
    public abstract AuthRepository bindAuthRepository(AuthRepositoryImpl impl);

    @Binds @Singleton
    public abstract UserRepository bindUserRepository(UserRepositoryImpl impl);

    @Binds @Singleton
    public abstract ChatRepository bindChatRepository(ChatRepositoryImpl impl);

    @Binds @Singleton
    public abstract FriendRepository bindFriendRepository(FriendRepositoryImpl impl);
}
