package ru.netology.singlealbumapp.dto

data class Album(
    val id: Long,
    val title: String,
    val subtitle: String,
    val artist: String,
    val published: String,
    val genre: String,
    val tracks: MutableList<Track>,
)

