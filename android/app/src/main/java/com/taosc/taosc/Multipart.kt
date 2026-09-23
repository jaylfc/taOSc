package com.taosc.taosc

class MultipartBody(val boundary: String) {
    private val parts = mutableListOf<Part>()

    private class Part(
        val name: String,
        val filename: String?,
        val contentType: String?,
        val body: ByteArray
    )

    fun addField(name: String, value: String) {
        parts.add(Part(escapeName(name), null, null, value.toByteArray(Charsets.UTF_8)))
    }

    fun addFile(name: String, filename: String, contentType: String, bytes: ByteArray) {
        parts.add(Part(escapeName(name), escapeFilename(filename), contentType, bytes))
    }

    fun contentType(): String {
        return "multipart/form-data; boundary=$boundary"
    }

    fun toByteArray(): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val crlf = "\r\n".toByteArray(Charsets.UTF_8)
        val dashBoundary = "--$boundary".toByteArray(Charsets.UTF_8)
        val dashBoundaryDash = "--$boundary--".toByteArray(Charsets.UTF_8)

        for (part in parts) {
            out.write(dashBoundary)
            out.write(crlf)
            out.write("Content-Disposition: form-data; name=\"${part.name}\"".toByteArray(Charsets.UTF_8))
            if (part.filename != null) {
                out.write("; filename=\"${part.filename}\"".toByteArray(Charsets.UTF_8))
            }
            out.write(crlf)
            if (part.contentType != null) {
                out.write("Content-Type: ${part.contentType}".toByteArray(Charsets.UTF_8))
                out.write(crlf)
            }
            out.write(crlf)
            out.write(part.body)
            out.write(crlf)
        }
        out.write(dashBoundaryDash)
        out.write(crlf)
        return out.toByteArray()
    }

    private fun escapeName(name: String): String {
        return name.replace("\"", "'").replace("\r", "").replace("\n", "")
    }

    private fun escapeFilename(filename: String): String {
        return filename.replace("\"", "'").replace("\r", "").replace("\n", "")
    }
}
