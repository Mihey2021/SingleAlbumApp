package ru.netology.singlealbumapp.repository

import ru.netology.singlealbumapp.dto.Album
import ru.netology.singlealbumapp.dto.Track

interface SongRepository {
    fun getAlbum(callback: Callback<Album>)
    fun saveTrackSettings(trackList: List<Track>)
    fun getTrackSettings(): List<Long>
    interface Callback<T> {
        fun onSuccess(result: T) {}
        fun onError(e: Exception) {}
    }
}