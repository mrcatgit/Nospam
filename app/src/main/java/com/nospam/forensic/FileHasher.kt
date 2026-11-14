package com.nospam.forensic

import java.io.File
import java.security.MessageDigest

/** Calcolo di SHA-256 su file, a blocchi, per sigillare le registrazioni audio. */
object FileHasher {
    fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(8192)
            while (true) {
                val read = input.read(buf)
                if (read <= 0) break
                md.update(buf, 0, read)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
