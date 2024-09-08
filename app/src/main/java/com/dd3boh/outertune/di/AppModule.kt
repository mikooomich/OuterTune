package com.dd3boh.outertune.di

import android.content.Context
import android.os.Environment
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.dd3boh.outertune.db.InternalDatabase
import com.dd3boh.outertune.db.MusicDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton


@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadCache

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): MusicDatabase =
        InternalDatabase.newInstance(context)

    @Singleton
    @Provides
    fun provideDatabaseProvider(@ApplicationContext context: Context): DatabaseProvider =
        StandaloneDatabaseProvider(context)

    @Singleton
    @Provides
    @DownloadCache
    fun provideDownloadCache(@ApplicationContext context: Context, databaseProvider: DatabaseProvider): SimpleCache {
//        val constructor = {
//            SimpleCache(context.filesDir.resolve("download"), NoOpCacheEvictor(), databaseProvider)
//        }

        val constructor = { cacheDir: File ->
            SimpleCache(cacheDir, NoOpCacheEvictor(), databaseProvider)
        }

        // Determine whether to use external or internal storage for caching
        val cacheDir = getExternalDownloadCacheDir(context)

        println("WTF cachedeir " + cacheDir.path)
        // Release any existing cache before returning
        constructor(cacheDir).release()

//        constructor().release()
        return constructor(cacheDir)
    }

    // Helper function to get external download cache directory
    private fun getExternalDownloadCacheDir(context: Context): File {
        return if (true) {
            // Use MediaStore API on Android 10 and above (Scoped storage)
            println("wtf uifweuif ${context.getExternalFilesDirs("Test it").get(0)}")
            File(context.getExternalFilesDir("Test it"), "folder")

            context.getExternalFilesDirs("Test it").forEach {
                println("wtf ---> " + it)
            }
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "folder").apply {
                if (!exists()) {
                    mkdirs()
                }
            }


        } else {
            // Use legacy method for Android 9 and below
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "folder").apply {
                if (!exists()) {
                    mkdirs()
                }
            }
        }
    }


}
