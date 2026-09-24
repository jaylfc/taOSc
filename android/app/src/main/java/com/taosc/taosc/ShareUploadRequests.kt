package com.taosc.taosc

class ShareUploadRequests(private val boundary: String) {
    fun libraryFile(baseUrl: String, filename: String, mimeType: String?, bytes: ByteArray, title: String?): UploadRequest {
        val multipart = MultipartBody(boundary)
        multipart.addFile("file", filename, mimeType ?: "application/octet-stream", bytes)
        if (!title.isNullOrBlank()) {
            multipart.addField("title", title)
        }
        val body = multipart.toByteArray()
        return UploadRequest("$baseUrl/api/library/ingest", multipart.contentType(), body)
    }

    fun libraryLink(baseUrl: String, url: String, title: String?): UploadRequest {
        val multipart = MultipartBody(boundary)
        multipart.addField("url", url)
        if (!title.isNullOrBlank()) {
            multipart.addField("title", title)
        }
        val body = multipart.toByteArray()
        return UploadRequest("$baseUrl/api/library/ingest", multipart.contentType(), body)
    }

    fun projectFile(baseUrl: String, slug: String, filename: String, mimeType: String?, bytes: ByteArray): UploadRequest {
        val multipart = MultipartBody(boundary)
        multipart.addFile("file", filename, mimeType ?: "application/octet-stream", bytes)
        val encodedSlug = encodeSlug(slug)
        val body = multipart.toByteArray()
        return UploadRequest("$baseUrl/api/projects/$encodedSlug/files/upload", multipart.contentType(), body)
    }

    private fun encodeSlug(slug: String): String {
        return slug.map { ch ->
            if (ch in 'a'..'z' || ch in 'A'..'Z' || ch in '0'..'9' || ch == '-' || ch == '_' || ch == '.') {
                ch.toString()
            } else {
                "%%%02X".format(ch.code)
            }
        }.joinToString("")
    }
}

class UploadRequest(
    val url: String,
    val contentType: String,
    val body: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as UploadRequest
        return url == other.url && contentType == other.contentType && body.contentEquals(other.body)
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + body.contentHashCode()
        return result
    }
}
