package com.example.virtualpetkmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform