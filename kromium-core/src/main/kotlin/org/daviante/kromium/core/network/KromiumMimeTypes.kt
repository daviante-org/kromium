package org.daviante.kromium.core.network

import java.net.URLConnection
import java.util.HashMap
import java.util.Locale

/**
 * Fast, zero-dependency MIME type resolver for modern web and application assets.
 */
object KromiumMimeTypes {
    private const val DEFAULT_MIME_TYPE = "application/octet-stream"

    private val MIME_MAP: Map<String, String> = HashMap<String, String>().apply {
        // Web Core
        put("html", "text/html; charset=utf-8")
        put("htm", "text/html; charset=utf-8")
        put("css", "text/css; charset=utf-8")
        put("js", "application/javascript; charset=utf-8")
        put("mjs", "application/javascript; charset=utf-8")
        put("wasm", "application/wasm")
        put("json", "application/json; charset=utf-8")
        put("map", "application/json; charset=utf-8")

        // Text & Data
        put("txt", "text/plain; charset=utf-8")
        put("csv", "text/csv; charset=utf-8")
        put("xml", "application/xml; charset=utf-8")
        put("md", "text/markdown; charset=utf-8")
        put("yaml", "application/yaml; charset=utf-8")
        put("yml", "application/yaml; charset=utf-8")

        // Images
        put("svg", "image/svg+xml")
        put("png", "image/png")
        put("jpg", "image/jpeg")
        put("jpeg", "image/jpeg")
        put("gif", "image/gif")
        put("webp", "image/webp")
        put("avif", "image/avif")
        put("ico", "image/x-icon")
        put("bmp", "image/bmp")
        put("tiff", "image/tiff")
        put("tif", "image/tiff")

        // Fonts
        put("woff", "font/woff")
        put("woff2", "font/woff2")
        put("ttf", "font/ttf")
        put("otf", "font/otf")
        put("eot", "application/vnd.ms-fontobject")

        // Audio
        put("mp3", "audio/mpeg")
        put("wav", "audio/wav")
        put("ogg", "audio/ogg")
        put("flac", "audio/flac")
        put("aac", "audio/aac")
        put("m4a", "audio/mp4")

        // Video
        put("mp4", "video/mp4")
        put("webm", "video/webm")
        put("ogv", "video/ogg")
        put("mov", "video/quicktime")
        put("mkv", "video/x-matroska")

        // Documents & Archives
        put("pdf", "application/pdf")
        put("zip", "application/zip")
        put("tar", "application/x-tar")
        put("gz", "application/gzip")
        put("7z", "application/x-7z-compressed")
    }

    /**
     * Resolves the MIME type for a given file name, relative path, or URL.
     *
     * @param path The file name, path, or URL (e.g. "index.html", "/static/bundle.js").
     * @param fallback The fallback MIME type if the extension is unrecognized. Defaults to "application/octet-stream".
     * @return The resolved MIME type.
     */
    @JvmStatic
    @JvmOverloads
    fun lookup(path: String, fallback: String = DEFAULT_MIME_TYPE): String {
        val extension = getExtension(path)
        if (extension.isEmpty()) {
            return fallback
        }

        return MIME_MAP[extension]
            ?: URLConnection.guessContentTypeFromName(path)
            ?: fallback
    }

    /**
     * Checks if a filename or path has a known static file extension.
     */
    @JvmStatic
    fun hasExtension(path: String): Boolean = getExtension(path).isNotEmpty()

    /**
     * Extracts the file extension (without the dot) from a path string in lowercase.
     */
    @JvmStatic
    fun getExtension(path: String): String {
        val cleanPath = path.substringBefore('?').substringBefore('#')
        val lastSlash = cleanPath.lastIndexOfAny(charArrayOf('/', '\\'))
        val fileName = if (lastSlash >= 0) cleanPath.substring(lastSlash + 1) else cleanPath

        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot in 1 until fileName.length - 1) {
            fileName.substring(lastDot + 1).lowercase(Locale.ROOT)
        } else {
            ""
        }
    }
}
