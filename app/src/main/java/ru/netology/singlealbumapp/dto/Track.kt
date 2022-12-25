package ru.netology.singlealbumapp.dto

data class Track(
    val id: Long,
    val file: String,
    var albumTitle: String,
    var isPlayed: Boolean = false,
    var isLiked: Boolean = false,
    var initInPlayer: Boolean = false,
)