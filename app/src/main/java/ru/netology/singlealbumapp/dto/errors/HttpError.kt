package ru.netology.singlealbumapp.dto.errors

class HttpError(errorMessage: String): RuntimeException(errorMessage)