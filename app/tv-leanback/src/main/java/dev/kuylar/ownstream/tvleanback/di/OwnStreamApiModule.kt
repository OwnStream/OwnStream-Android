package dev.kuylar.ownstream.tvleanback.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.kuylar.ownstream.tvleanback.BuildConfig
import dev.kuylar.ownstream.api.OwnStreamApiClient

@Module
@InstallIn(ActivityComponent::class, FragmentComponent::class)
object OwnStreamApiModule {
	@Provides
	fun provideOwnStreamApiClient(
		@ApplicationContext context: Context
	): OwnStreamApiClient {
		val sp = context.getSharedPreferences("main", Context.MODE_PRIVATE)
		val token = sp.getString("token", null)
		val client = OwnStreamApiClient(
			sp.getString("host", "https://invalid")!!,
			"OwnStream-Android/${BuildConfig.VERSION_NAME} (Tv-Leanback)"
		)
		token?.let { client.setAuth(it) }
		client.setLocale("en_US")
		return client
	}
}
