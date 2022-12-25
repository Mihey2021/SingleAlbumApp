package ru.netology.singlealbumapp.repository

import android.app.Application
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import ru.netology.singlealbumapp.BuildConfig
import ru.netology.singlealbumapp.dto.Album
import ru.netology.singlealbumapp.dto.Track
import ru.netology.singlealbumapp.dto.errors.HttpError
import java.io.IOException
import javax.inject.Inject

private const val BASE_URL = BuildConfig.BASE_URL
private const val PREFS_KEY = "LikedTracks"

class SongRepositoryImpl @Inject constructor(
    application: Application,
    private val httpClient: OkHttpClient,
) : SongRepository {

    private val prefs = application.getSharedPreferences("likedTracks", Context.MODE_PRIVATE)
    private val type = TypeToken.getParameterized(List::class.java, Track::class.java).type
    private val gson = Gson()

    override fun getAlbum(callback: SongRepository.Callback<Album>) {
        val request = Request.Builder()
            .url("${BASE_URL}album.json")
            .build()
        try {
            httpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    callback.onError(HttpError(e.message.toString()))
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val body =
                            response.body?.string()
                                ?: throw HttpError("${response.code}: ${response.message}")
                        val gson = Gson()
                        val album = gson.fromJson(body, Album::class.java)
                            .also { album ->
                                album.tracks.map {
                                    //it.copy(albumTitle = album.title)
                                    it.albumTitle = album.title
                                }
                            }
                        callback.onSuccess(album)
                    } else {
                        callback.onError(HttpError("${response.code}: ${response.message}"))
                    }
                }
            })
        }
        catch (e: Exception) {
            callback.onError(e)
        }
    }

    override fun saveTrackSettings(trackList: List<Track>) {
        with(prefs.edit()) {
            putString(PREFS_KEY, gson.toJson(trackList))
            apply()
        }
    }

    override fun getTrackSettings(): List<Long> {
        val likedTracksIds = prefs.getString(PREFS_KEY, null)
        return if (likedTracksIds == null) emptyList()
        else (gson.fromJson<List<Track>?>(
            likedTracksIds,
            type
        ).map { it.id })
    }
}