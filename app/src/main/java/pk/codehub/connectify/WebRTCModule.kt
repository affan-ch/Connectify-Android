package pk.codehub.connectify

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WebRTCModule {
    @Provides
    @Singleton
    fun provideWebRTCViewModel(application: Application): WebRTCViewModel {
        return WebRTCViewModel(application)
    }
}
