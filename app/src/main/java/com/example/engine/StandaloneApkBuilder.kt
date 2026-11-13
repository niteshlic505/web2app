package com.example.engine

import android.content.Context
import com.example.data.model.AppProject
import com.example.data.model.TargetDeviceProfile
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Date
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object StandaloneApkBuilder {

    private const val TEMPLATE_ASSET_NAME = "template_webapp.apk"
    private const val PLACEHOLDER_PACKAGE = "com.w2a.template.placeholder.pkg"
    private const val PLACEHOLDER_APP_NAME = "W2A_APP_NAME_PLACEHOLDER"
    private const val PLACEHOLDER_VERSION_NAME = "1.0.0"

    private const val RES_ATTR_VERSION_CODE = 0x0101021b
    private val APK_SIG_BLOCK_MAGIC = "APK Sig Block 42".toByteArray(Charsets.US_ASCII)
    private const val APK_SIGNATURE_SCHEME_V2_BLOCK_ID = 0x7109871a
    private const val SIGNATURE_RSA_PKCS1_V1_5_WITH_SHA256 = 0x0103
    private const val CHUNK_SIZE_BYTES = 1024 * 1024 // 1 MB

    fun sanitizePackageName(rawPackage: String, projectId: String): String {
        val cleaned = rawPackage.lowercase()
            .replace(Regex("[^a-z0-9._]"), "")
            .split(".")
            .filter { it.isNotBlank() }
            .map { seg ->
                val fixed = if (seg.first().isLetter()) seg else "app$seg"
                fixed
            }
        return if (cleaned.size >= 2) {
            val joined = cleaned.joinToString(".")
            if (joined == "com.aistudio.website2app.studio") {
                "com.w2a.generated.${projectId.lowercase().replace(Regex("[^a-z0-9]"), "").ifBlank { "app" }}"
            } else {
                joined
            }
        } else {
            val suffix = projectId.lowercase().replace(Regex("[^a-z0-9]"), "").ifBlank { "webapp" }
            "com.w2a.app.$suffix"
        }
    }

    fun buildStandaloneApk(
        context: Context,
        project: AppProject,
        targetProfile: TargetDeviceProfile,
        outputFile: File
    ): Long {
        val templateBytes = context.assets.open(TEMPLATE_ASSET_NAME).use { it.readBytes() }
        val keyStoreDir = File(context.filesDir, "w2a_signer").apply { mkdirs() }
        return buildFromTemplateBytes(
            templateBytes = templateBytes,
            project = project,
            targetProfile = targetProfile,
            keyStoreDir = keyStoreDir,
            outputFile = outputFile
        )
    }

    fun buildFromTemplateBytes(
        templateBytes: ByteArray,
        project: AppProject,
        targetProfile: TargetDeviceProfile,
        keyStoreDir: File,
        outputFile: File
    ): Long {
        val safePackage = sanitizePackageName(project.packageName, project.id)
        val safeAppName = project.name.trim().ifBlank { "Web App" }.take(30)
        val safeVersionName = project.versionName.trim().ifBlank { "1.0" }
        val safeVersionCode = project.versionCode.coerceAtLeast(1)

        // 1. Read template entries
        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(templateBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && !entry.name.startsWith("META-INF/")) {
                    entries[entry.name] = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        // 2. Patch binary AndroidManifest.xml
        val rawManifest = entries["AndroidManifest.xml"]
            ?: error("Template APK missing AndroidManifest.xml")
        entries["AndroidManifest.xml"] = patchAndroidManifestAxml(
            rawAxml = rawManifest,
            newPackageName = safePackage,
            newAppName = safeAppName,
            newVersionCode = safeVersionCode,
            newVersionName = safeVersionName
        )

        // 3. Inject user's webapp_config.json
        entries["assets/webapp_config.json"] = buildConfigJson(project, targetProfile)

        // 4. Write aligned unsigned APK (resources.arsc STORED & 4-byte aligned)
        val unsignedApkBytes = writeAlignedZip(entries)

        // 5. Sign with APK Signature Scheme v2
        val (privateKey, certDer, publicKey) = getOrCreateSigningIdentity(keyStoreDir)
        val signedApkBytes = signApkV2(unsignedApkBytes, privateKey, certDer, publicKey)

        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { it.write(signedApkBytes) }
        return outputFile.length()
    }

    private fun buildConfigJson(project: AppProject, targetProfile: TargetDeviceProfile): ByteArray {
        val navArray = runCatching { JSONArray(project.navItemsJson) }.getOrDefault(JSONArray())
        val obj = JSONObject().apply {
            put("url", project.url)
            put("appName", project.name.ifBlank { "Web App" })
            put("primaryColor", project.primaryColorHex)
            put("accentColor", project.accentColorHex)
            put("splashTitle", project.splashTitle.ifBlank { project.name })
            put("splashSubtitle", project.splashSubtitle.ifBlank { "Powered by Website2App Studio" })
            put("splashDelayMs", project.splashDelayMs)
            put("deviceProfile", targetProfile.name.lowercase())
            put("navStyle", project.navStyle.name)
            put("navItems", navArray)
        }
        return obj.toString().toByteArray(Charsets.UTF_8)
    }

    /**
     * Patches Android Binary XML (AXML) string pool + android:versionCode attribute.
     */
    internal fun patchAndroidManifestAxml(
        rawAxml: ByteArray,
        newPackageName: String,
        newAppName: String,
        newVersionCode: Int,
        newVersionName: String
    ): ByteArray {
        val buf = ByteBuffer.wrap(rawAxml).order(ByteOrder.LITTLE_ENDIAN)
        val fileType = buf.getShort(0).toInt() and 0xFFFF
        val fileHdrSize = buf.getShort(2).toInt() and 0xFFFF
        require(fileType == 0x0003) { "Invalid AXML header type: $fileType" }

        val spStart = fileHdrSize
        val spType = buf.getShort(spStart).toInt() and 0xFFFF
        val spHdrSize = buf.getShort(spStart + 2).toInt() and 0xFFFF
        val spSize = buf.getInt(spStart + 4)
        require(spType == 0x0001) { "Expected StringPool chunk at $spStart" }

        val stringCount = buf.getInt(spStart + 8)
        val styleCount = buf.getInt(spStart + 12)
        val flags = buf.getInt(spStart + 16)
        val stringsStartRel = buf.getInt(spStart + 20)
        val stylesStartRel = buf.getInt(spStart + 24)
        val isUtf8 = (flags and 0x0100) != 0

        val stringOffsets = IntArray(stringCount) { i ->
            buf.getInt(spStart + spHdrSize + i * 4)
        }
        val stringsDataAbsStart = spStart + stringsStartRel

        // Decode strings
        val decodedStrings = ArrayList<String>(stringCount)
        for (i in 0 until stringCount) {
            val sPos = stringsDataAbsStart + stringOffsets[i]
            val decoded = if (isUtf8) {
                readUtf8PoolString(rawAxml, sPos)
            } else {
                readUtf16PoolString(rawAxml, sPos)
            }
            val replaced = when (decoded) {
                PLACEHOLDER_PACKAGE -> newPackageName
                PLACEHOLDER_APP_NAME -> newAppName
                PLACEHOLDER_VERSION_NAME -> newVersionName
                else -> decoded
            }
            decodedStrings.add(replaced)
        }

        // Preserve any trailing styles block if present (usually 0 for manifests)
        val stylesBytes = if (styleCount > 0 && stylesStartRel > 0) {
            val stylesAbs = spStart + stylesStartRel
            rawAxml.copyOfRange(stylesAbs, spStart + spSize)
        } else {
            ByteArray(0)
        }

        // Re-encode string pool
        val encodedStringsStream = ByteArrayOutputStream()
        val newOffsets = IntArray(stringCount)
        for (i in 0 until stringCount) {
            newOffsets[i] = encodedStringsStream.size()
            val str = decodedStrings[i]
            if (isUtf8) {
                writeUtf8PoolString(encodedStringsStream, str)
            } else {
                writeUtf16PoolString(encodedStringsStream, str)
            }
        }
        while (encodedStringsStream.size() % 4 != 0) {
            encodedStringsStream.write(0)
        }
        val newStringsBytes = encodedStringsStream.toByteArray()

        val newHeaderAndIndexSize = spHdrSize + (stringCount * 4) + (styleCount * 4)
        val newStylesStartRel = if (stylesBytes.isNotEmpty()) newHeaderAndIndexSize + newStringsBytes.size else 0
        val newSpSize = newHeaderAndIndexSize + newStringsBytes.size + stylesBytes.size

        val newSpBuf = ByteBuffer.allocate(newSpSize).order(ByteOrder.LITTLE_ENDIAN)
        newSpBuf.putShort(0x0001.toShort())
        newSpBuf.putShort(spHdrSize.toShort())
        newSpBuf.putInt(newSpSize)
        newSpBuf.putInt(stringCount)
        newSpBuf.putInt(styleCount)
        newSpBuf.putInt(flags)
        newSpBuf.putInt(newHeaderAndIndexSize)
        newSpBuf.putInt(newStylesStartRel)
        for (off in newOffsets) {
            newSpBuf.putInt(off)
        }
        // Copy style index offsets if any
        if (styleCount > 0) {
            for (i in 0 until styleCount) {
                newSpBuf.putInt(buf.getInt(spStart + spHdrSize + stringCount * 4 + i * 4))
            }
        }
        newSpBuf.put(newStringsBytes)
        if (stylesBytes.isNotEmpty()) {
            newSpBuf.put(stylesBytes)
        }

        // Copy remainder of AXML after StringPool and patch android:versionCode
        val restBytes = rawAxml.copyOfRange(spStart + spSize, rawAxml.size)
        patchVersionCodeInXmlChunks(restBytes, newVersionCode)

        val totalNewSize = fileHdrSize + newSpSize + restBytes.size
        val out = ByteArrayOutputStream(totalNewSize)
        val topHeader = ByteBuffer.allocate(fileHdrSize).order(ByteOrder.LITTLE_ENDIAN)
        topHeader.put(rawAxml, 0, fileHdrSize)
        topHeader.putInt(4, totalNewSize)
        out.write(topHeader.array())
        out.write(newSpBuf.array())
        out.write(restBytes)
        return out.toByteArray()
    }

    private fun patchVersionCodeInXmlChunks(chunks: ByteArray, newVersionCode: Int) {
        val buf = ByteBuffer.wrap(chunks).order(ByteOrder.LITTLE_ENDIAN)
        var pos = 0
        var versionCodeAttrNameIdx = -1
        while (pos + 8 <= chunks.size) {
            val type = buf.getShort(pos).toInt() and 0xFFFF
            val size = buf.getInt(pos + 4)
            if (size <= 0 || pos + size > chunks.size) break

            if (type == 0x0180) { // ResXMLTree_resourceMap
                val count = (size - 8) / 4
                for (i in 0 until count) {
                    val resId = buf.getInt(pos + 8 + i * 4)
                    if (resId == RES_ATTR_VERSION_CODE) {
                        versionCodeAttrNameIdx = i
                        break
                    }
                }
            } else if (type == 0x0102 && versionCodeAttrNameIdx >= 0) { // START_ELEMENT (<manifest>)
                val attrStart = buf.getShort(pos + 24).toInt() and 0xFFFF
                val attrSize = buf.getShort(pos + 26).toInt() and 0xFFFF
                val attrCount = buf.getShort(pos + 28).toInt() and 0xFFFF
                for (i in 0 until attrCount) {
                    val aOff = pos + 16 + attrStart + i * attrSize
                    if (aOff + 20 > chunks.size) break
                    val nameIdx = buf.getInt(aOff + 4)
                    if (nameIdx == versionCodeAttrNameIdx) {
                        val valType = buf.get(aOff + 15).toInt() and 0xFF
                        if (valType == 0x10 || valType == 0x11) {
                            buf.putInt(aOff + 16, newVersionCode)
                        }
                        return
                    }
                }
            }
            pos += size
        }
    }

    private fun readUtf16PoolString(data: ByteArray, offset: Int): String {
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        var pos = offset
        var len = buf.getShort(pos).toInt() and 0xFFFF
        pos += 2
        if ((len and 0x8000) != 0) {
            val low = buf.getShort(pos).toInt() and 0xFFFF
            len = ((len and 0x7FFF) shl 16) or low
            pos += 2
        }
        return String(data, pos, len * 2, Charsets.UTF_16LE)
    }

    private fun writeUtf16PoolString(out: ByteArrayOutputStream, str: String) {
        val bytes = str.toByteArray(Charsets.UTF_16LE)
        val charLen = str.length
        if (charLen > 0x7FFF) {
            val hi = (charLen ushr 16) or 0x8000
            val lo = charLen and 0xFFFF
            out.write(hi and 0xFF)
            out.write((hi ushr 8) and 0xFF)
            out.write(lo and 0xFF)
            out.write((lo ushr 8) and 0xFF)
        } else {
            out.write(charLen and 0xFF)
            out.write((charLen ushr 8) and 0xFF)
        }
        out.write(bytes)
        out.write(0)
        out.write(0)
    }

    private fun readUtf8PoolString(data: ByteArray, offset: Int): String {
        var pos = offset
        val u16Len = data[pos++].toInt() and 0xFF
        if ((u16Len and 0x80) != 0) pos++
        var byteLen = data[pos++].toInt() and 0xFF
        if ((byteLen and 0x80) != 0) {
            byteLen = ((byteLen and 0x7F) shl 8) or (data[pos++].toInt() and 0xFF)
        }
        return String(data, pos, byteLen, Charsets.UTF_8)
    }

    private fun writeUtf8PoolString(out: ByteArrayOutputStream, str: String) {
        val u16Len = str.length
        val bytes = str.toByteArray(Charsets.UTF_8)
        val byteLen = bytes.size
        if (u16Len >= 0x80) {
            out.write(((u16Len ushr 8) and 0x7F) or 0x80)
            out.write(u16Len and 0xFF)
        } else {
            out.write(u16Len)
        }
        if (byteLen >= 0x80) {
            out.write(((byteLen ushr 8) and 0x7F) or 0x80)
            out.write(byteLen and 0xFF)
        } else {
            out.write(byteLen)
        }
        out.write(bytes)
        out.write(0)
    }

    /**
     * Writes an APK ZIP archive where `resources.arsc` and image resources are STORED and 4-byte aligned.
     */
    private fun writeAlignedZip(entries: Map<String, ByteArray>): ByteArray {
        return writeAlignedZipStrictOrder(entries)
    }

    private fun writeAlignedZipStrictOrder(entries: Map<String, ByteArray>): ByteArray {
        val baos = ByteArrayOutputStream()
        val storedKeys = entries.keys.filter {
            it == "resources.arsc" || it.endsWith(".png") || it.endsWith(".webp")
        }.sortedWith(compareBy<String> { if (it == "resources.arsc") 0 else 1 }.thenBy { it })

        val deflatedKeys = entries.keys.filter { it !in storedKeys }.sorted()

        var currentOffset = 0
        ZipOutputStream(baos).use { zos ->
            for (name in storedKeys) {
                val data = entries.getValue(name)
                val entry = ZipEntry(name).apply {
                    method = ZipEntry.STORED
                    size = data.size.toLong()
                    compressedSize = data.size.toLong()
                    crc = CRC32().apply { update(data) }.value
                }
                val nameLen = name.toByteArray(Charsets.UTF_8).size
                val baseHeaderLen = 30 + nameLen
                val pad = (4 - ((currentOffset + baseHeaderLen) % 4)) % 4
                if (pad > 0) {
                    entry.extra = ByteArray(pad)
                }
                zos.putNextEntry(entry)
                zos.write(data)
                zos.closeEntry()
                currentOffset += baseHeaderLen + pad + data.size
            }

            for (name in deflatedKeys) {
                val data = entries.getValue(name)
                val entry = ZipEntry(name).apply {
                    method = ZipEntry.DEFLATED
                }
                zos.putNextEntry(entry)
                zos.write(data)
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    // ==================== PURE-KOTLIN APK SIGNATURE SCHEME V2 ====================

    private fun signApkV2(
        unsignedApk: ByteArray,
        privateKey: PrivateKey,
        certDer: ByteArray,
        publicKey: PublicKey
    ): ByteArray {
        val eocdOffset = findEocdOffset(unsignedApk)
        require(eocdOffset >= 0) { "Invalid ZIP: EOCD not found" }

        val eocdBuf = ByteBuffer.wrap(unsignedApk, eocdOffset, unsignedApk.size - eocdOffset)
            .slice()
            .order(ByteOrder.LITTLE_ENDIAN)
        val cdSize = eocdBuf.getInt(12)
        val cdOffset = eocdBuf.getInt(16)
        require(cdOffset >= 0 && cdSize >= 0 && cdOffset + cdSize == eocdOffset) {
            "Invalid ZIP CD bounds: cdOffset=$cdOffset cdSize=$cdSize eocdOffset=$eocdOffset"
        }

        val section1 = unsignedApk.copyOfRange(0, cdOffset)
        val section3 = unsignedApk.copyOfRange(cdOffset, eocdOffset)
        val section4 = unsignedApk.copyOfRange(eocdOffset, unsignedApk.size)

        // Compute 1MB chunked SHA-256 content digest
        val contentDigest = computeV2ContentDigest(section1, section3, section4)

        // Build SignedData
        val digestEntry = lengthPrefixed(
            int32LE(SIGNATURE_RSA_PKCS1_V1_5_WITH_SHA256) + lengthPrefixed(contentDigest)
        )
        val digestsSeq = lengthPrefixed(digestEntry)
        val certsSeq = lengthPrefixed(lengthPrefixed(certDer))
        val attrsSeq = lengthPrefixed(ByteArray(0))
        val signedData = digestsSeq + certsSeq + attrsSeq

        // Sign SignedData
        val sigEngine = Signature.getInstance("SHA256withRSA")
        sigEngine.initSign(privateKey)
        sigEngine.update(signedData)
        val signatureBytes = sigEngine.sign()

        val sigEntry = lengthPrefixed(
            int32LE(SIGNATURE_RSA_PKCS1_V1_5_WITH_SHA256) + lengthPrefixed(signatureBytes)
        )
        val sigsSeq = lengthPrefixed(sigEntry)
        val pubKeyBytes = publicKey.encoded

        val signerBlock = lengthPrefixed(signedData) + sigsSeq + lengthPrefixed(pubKeyBytes)
        val signersSeq = lengthPrefixed(lengthPrefixed(signerBlock))

        // Wrap into APK Signing Block
        val pairValue = signersSeq
        val pairLen = 4L + pairValue.size.toLong()
        // Total size excluding initial uint64: 8 (pairLen) + 4 (id) + value.size + 8 (size2) + 16 (magic)
        val sizeOfBlock = 8L + pairLen + 8L + 16L
        val totalSigningBlockSize = (8L + sizeOfBlock).toInt()

        val blk = ByteBuffer.allocate(totalSigningBlockSize).order(ByteOrder.LITTLE_ENDIAN)
        blk.putLong(sizeOfBlock)
        blk.putLong(pairLen)
        blk.putInt(APK_SIGNATURE_SCHEME_V2_BLOCK_ID)
        blk.put(pairValue)
        blk.putLong(sizeOfBlock)
        blk.put(APK_SIG_BLOCK_MAGIC)
        val signingBlockBytes = blk.array()

        // Patch EOCD centralDirectoryOffset to point after the APK Signing Block
        val patchedEocd = section4.copyOf()
        ByteBuffer.wrap(patchedEocd).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(16, cdOffset + signingBlockBytes.size)

        val finalOut = ByteArrayOutputStream(unsignedApk.size + signingBlockBytes.size)
        finalOut.write(section1)
        finalOut.write(signingBlockBytes)
        finalOut.write(section3)
        finalOut.write(patchedEocd)
        return finalOut.toByteArray()
    }

    private fun computeV2ContentDigest(vararg sections: ByteArray): ByteArray {
        val chunkDigests = mutableListOf<ByteArray>()
        val sha = MessageDigest.getInstance("SHA-256")
        val prefix = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN)

        for (sec in sections) {
            var offset = 0
            while (offset < sec.size) {
                val len = minOf(CHUNK_SIZE_BYTES, sec.size - offset)
                sha.reset()
                prefix.clear()
                prefix.put(0xA5.toByte())
                prefix.putInt(len)
                sha.update(prefix.array())
                sha.update(sec, offset, len)
                chunkDigests.add(sha.digest())
                offset += len
            }
        }

        sha.reset()
        prefix.clear()
        prefix.put(0x5A.toByte())
        prefix.putInt(chunkDigests.size)
        sha.update(prefix.array())
        for (cd in chunkDigests) {
            sha.update(cd)
        }
        return sha.digest()
    }

    private fun findEocdOffset(zip: ByteArray): Int {
        val minOffset = maxOf(0, zip.size - 65536 - 22)
        for (i in zip.size - 22 downTo minOffset) {
            if (zip[i] == 0x50.toByte() &&
                zip[i + 1] == 0x4B.toByte() &&
                zip[i + 2] == 0x05.toByte() &&
                zip[i + 3] == 0x06.toByte()
            ) {
                val commentLen = (zip[i + 20].toInt() and 0xFF) or ((zip[i + 21].toInt() and 0xFF) shl 8)
                if (i + 22 + commentLen == zip.size) {
                    return i
                }
            }
        }
        return -1
    }

    private fun lengthPrefixed(data: ByteArray): ByteArray {
        val out = ByteArray(4 + data.size)
        ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN).putInt(0, data.size)
        System.arraycopy(data, 0, out, 4, data.size)
        return out
    }

    private fun int32LE(value: Int): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0, value).array()
    }

    // ==================== PERSISTENT PER-DEVICE RSA-2048 X.509 SIGNER ====================

    private fun getOrCreateSigningIdentity(dir: File): Triple<PrivateKey, ByteArray, PublicKey> {
        val privFile = File(dir, "w2a_key.pk8")
        val pubFile = File(dir, "w2a_pub.der")
        val certFile = File(dir, "w2a_cert.der")

        if (privFile.exists() && pubFile.exists() && certFile.exists()) {
            return runCatching {
                val kf = KeyFactory.getInstance("RSA")
                val priv = kf.generatePrivate(PKCS8EncodedKeySpec(privFile.readBytes()))
                val pub = kf.generatePublic(X509EncodedKeySpec(pubFile.readBytes()))
                val cert = certFile.readBytes()
                Triple(priv, cert, pub)
            }.getOrElse {
                generateAndPersistIdentity(privFile, pubFile, certFile)
            }
        }
        return generateAndPersistIdentity(privFile, pubFile, certFile)
    }

    private fun generateAndPersistIdentity(
        privFile: File,
        pubFile: File,
        certFile: File
    ): Triple<PrivateKey, ByteArray, PublicKey> {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048, SecureRandom())
        val pair = kpg.generateKeyPair()
        val certDer = generateSelfSignedX509Cert(pair.public, pair.private)

        privFile.writeBytes(pair.private.encoded)
        pubFile.writeBytes(pair.public.encoded)
        certFile.writeBytes(certDer)
        return Triple(pair.private, certDer, pair.public)
    }

    /**
     * Generates a minimal, RFC 5280 compliant DER-encoded X.509 v3 self-signed certificate.
     */
    private fun generateSelfSignedX509Cert(publicKey: PublicKey, privateKey: PrivateKey): ByteArray {
        // OIDs
        val oidSha256WithRsa = derOid(byteArrayOf(0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x01, 0x0B)) // 1.2.840.113549.1.1.11
        val oidCommonName = derOid(byteArrayOf(0x55, 0x04, 0x03)) // 2.5.4.3
        val oidOrg = derOid(byteArrayOf(0x55, 0x04, 0x0A))        // 2.5.4.10
        val oidCountry = derOid(byteArrayOf(0x55, 0x04, 0x06))    // 2.5.4.6

        val sigAlgId = derSeq(oidSha256WithRsa + byteArrayOf(0x05, 0x00)) // SEQUENCE { OID, NULL }

        val rdnC = derSet(derSeq(oidCountry + derPrintableString("IN")))
        val rdnO = derSet(derSeq(oidOrg + derUtf8String("Website2App Bharat Studio")))
        val rdnCn = derSet(derSeq(oidCommonName + derUtf8String("Website2App Studio Publisher")))
        val nameSeq = derSeq(rdnC + rdnO + rdnCn)

        val nowMs = System.currentTimeMillis() - 86_400_000L // backdate 1 day for clock skew safety
        val notBefore = derGeneralizedTime(Date(nowMs))
        val notAfter = derGeneralizedTime(Date(nowMs + 25L * 365L * 86_400_000L))
        val validity = derSeq(notBefore + notAfter)

        val versionTag = derTag(0xA0, derInteger(BigInteger.valueOf(2))) // v3 (2)
        val serial = derInteger(BigInteger(64, SecureRandom()).abs().add(BigInteger.ONE))
        val spki = publicKey.encoded // already SubjectPublicKeyInfo DER SEQUENCE

        val tbsCert = derSeq(
            versionTag +
            serial +
            sigAlgId +
            nameSeq +
            validity +
            nameSeq +
            spki
        )

        val sigEngine = Signature.getInstance("SHA256withRSA")
        sigEngine.initSign(privateKey)
        sigEngine.update(tbsCert)
        val sigBytes = sigEngine.sign()

        // BIT STRING with 0 unused bits
        val bitString = derTag(0x03, byteArrayOf(0x00) + sigBytes)
        return derSeq(tbsCert + sigAlgId + bitString)
    }

    private fun derLength(len: Int): ByteArray {
        return when {
            len < 128 -> byteArrayOf(len.toByte())
            len <= 0xFF -> byteArrayOf(0x81.toByte(), len.toByte())
            len <= 0xFFFF -> byteArrayOf(0x82.toByte(), (len ushr 8).toByte(), len.toByte())
            else -> byteArrayOf(0x83.toByte(), (len ushr 16).toByte(), (len ushr 8).toByte(), len.toByte())
        }
    }

    private fun derTag(tag: Int, content: ByteArray): ByteArray {
        val lenBytes = derLength(content.size)
        return byteArrayOf(tag.toByte()) + lenBytes + content
    }

    private fun derSeq(content: ByteArray) = derTag(0x30, content)
    private fun derSet(content: ByteArray) = derTag(0x31, content)
    private fun derOid(encodedOidBytes: ByteArray) = derTag(0x06, encodedOidBytes)
    private fun derInteger(value: BigInteger) = derTag(0x02, value.toByteArray())
    private fun derUtf8String(s: String) = derTag(0x0C, s.toByteArray(Charsets.UTF_8))
    private fun derPrintableString(s: String) = derTag(0x13, s.toByteArray(Charsets.US_ASCII))

    private fun derGeneralizedTime(date: Date): ByteArray {
        val fmt = java.text.SimpleDateFormat("yyyyMMddHHmmss'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        return derTag(0x18, fmt.format(date).toByteArray(Charsets.US_ASCII))
    }
}
