package com.webscare.volumenormalizer

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform