package com.example.virtualpetkmp.util

import java.io.File

actual fun carpetaDatosApp(): String =
    File(System.getProperty("user.home"), ".virtualpet").apply { mkdirs() }.absolutePath
