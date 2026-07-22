package com.chemecador.passwordmemory.di

import com.chemecador.passwordmemory.security.CryptoManager
import com.chemecador.passwordmemory.security.KeystoreCryptoManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.security.SecureRandom
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindCryptoManager(impl: KeystoreCryptoManager): CryptoManager

    companion object {

        /** Shared so that every salt, IV and generated password comes from the same CSPRNG. */
        @Provides
        fun provideSecureRandom(): SecureRandom = SecureRandom()
    }
}
