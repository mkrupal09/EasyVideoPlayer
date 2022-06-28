/*
 * Copyright (C) 2007-2008 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc.easyvideoplayer

import android.annotation.TargetApi
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.DatabaseUtils
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.*
import java.lang.Long
import java.text.DecimalFormat
import java.util.*

/**
 * This helper class if used for performing file related operations
 */
object FileUtils {
    var AUTHORITY = "com.dc.easyvideoplayer.fileprovider"

    var SECURE_GALLERY_FOLDER_PATH =
        Environment.getExternalStorageDirectory().path + "/" + "Pictotum" + "/"

    /**
     * TAG for log messages.
     */
    internal val TAG = "FileUtils"
    private val DEBUG = false // Set to true to enable logging


    val HIDDEN_PREFIX = "."

    /**
     * File and folder comparator. TODO Expose sorting option method
     *
     * @author paulburke
     */
    var sComparator: Comparator<File> = Comparator { f1, f2 ->
        // Sort alphabetically by lower case, which is much cleaner
        f1.name.toLowerCase().compareTo(f2.name.toLowerCase())
    }

    /**
     * File (not directories) filter.
     *
     * @author paulburke
     */
    var sFileFilter: FileFilter = FileFilter { file ->
        val fileName = file.name
        // Return files only (not directories) and skip hidden files
        file.isFile && !fileName.startsWith(HIDDEN_PREFIX)
    }

    /**
     * Folder (directories) filter.
     *
     * @author paulburke
     */
    var sDirFilter: FileFilter = FileFilter { file ->
        val fileName = file.name
        // Return directories only and skip hidden directories
        file.isDirectory && !fileName.startsWith(HIDDEN_PREFIX)
    }

    /**
     * Gets the extension of a file name, like ".png" or ".jpg".
     *
     * @param uri
     * @return Extension including the dot("."); "" if there is no extension;
     * null if uri was null.
     */
    fun getExtension(uri: String?): String? {
        if (uri == null) {
            return null
        }

        val dot = uri.lastIndexOf(".")
        return if (dot >= 0) {
            uri.substring(dot)
        } else {
            // No extension.
            ""
        }
    }

    /**
     * Function to check uri is local or not
     * @return Whether the URI is a local one.
     */
    fun isLocal(url: String?): Boolean {
        return if (url != null && !url.startsWith("http://") && !url.startsWith("https://")) {
            true
        } else false
    }

    /**
     * Function to check if it is media url or not
     * @return True if Uri is a MediaStore Uri.
     * @author paulburke
     */
    fun isMediaUri(uri: Uri?): Boolean {
        return "media".equals(uri!!.authority, ignoreCase = true)
    }

    /**
     * Convert File into Uri.
     *
     * @param file
     * @return uri
     */
    fun getUri(file: File?): Uri? {
        return if (file != null) {
            Uri.fromFile(file)
        } else null
    }

    /**
     * Returns the path only (without file name).
     *
     * @param file
     * @return
     */
    fun getPathWithoutFilename(file: File?): File? {
        if (file != null) {
            if (file.isDirectory) {
                // no file to be split off. Return everything
                return file
            } else {
                val filename = file.name
                val filepath = file.absolutePath

                // Construct path without file name.
                var pathwithoutname = filepath.substring(0, filepath.length - filename.length)
                if (pathwithoutname.endsWith("/")) {
                    pathwithoutname = pathwithoutname.substring(0, pathwithoutname.length - 1)
                }
                return File(pathwithoutname)
            }
        }
        return null
    }

    /**
     * Function to get mime type
     * @return The MIME type for the given file.
     */
    fun getMimeType(file: File): String? {
        val extension = getExtension(file.name)
        return if (extension!!.length > 0) MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(extension.substring(1)) else "application/octet-stream"
    }

    /**
     * Function to get mime type
     * @param context [Context]
     * @param uri [Uri]
     * @return The MIME type for the give Uri.
     */
    fun getMimeType(context: Context, uri: Uri): String? {
        val file = File(getPath(context, uri)!!)
        return getMimeType(file)
    }

    /**
     * Function to check if it is local storage document or not
     * @param uri The Uri to check.
     * @return true if it is local storage document
     */
    fun isLocalStorageDocument(uri: Uri): Boolean {
        return AUTHORITY == uri.authority
    }

    /**
     * Function to check if it is external storage document or not
     * @param uri The Uri to check.
     * @return true if it is external storage document
     */
    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * Function to check if it is download document or not
     * @param uri The Uri to check.
     * @return true if it is download document
     */
    fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * Function to check if it is media document or not
     * @param uri The Uri to check.
     * @return true if it is media document
     */
    fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * Function to check if it is google photos document or not
     * @param uri The Uri to check.
     * @return true if it is google photos document
     */
    fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     * @author paulburke
     */
    fun getDataColumn(
        context: Context,
        uri: Uri?,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {

        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)

        try {
            cursor =
                context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                if (DEBUG) DatabaseUtils.dumpCursor(cursor)

                val column_index = cursor.getColumnIndexOrThrow(column)
                val data = cursor.getString(column_index)
                if (data != null) {
                    return data
                }
                return downloadAndSaveImage(uri, context)
            }
        } finally {
            if (cursor != null) cursor.close()
        }
        return null
    }

    private fun downloadAndSaveImage(
        uri: Uri,
        context: Context
    ): String? {
        var inputStream: InputStream? = null
        var filePath: String? = null
        if (uri.authority != null) {
            try {
                inputStream = context.contentResolver.openInputStream(uri) // context needed
                val photoFile: File? = createTemporalFileFrom(inputStream, context)
                filePath = photoFile?.path
            } catch (e: FileNotFoundException) {
                // log
            } catch (e: IOException) {
                // log
            } finally {
                try {
                    inputStream?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return filePath
    }

    @Throws(IOException::class)
    private fun createTemporalFileFrom(
        inputStream: InputStream?,
        context: Context
    ): File? {
        var targetFile: File? = null
        if (inputStream != null) {
            var read: Int
            val buffer = ByteArray(8 * 1024)
            targetFile = createTemporalFile(context)
            val outputStream: OutputStream = FileOutputStream(targetFile)
            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
            }
            outputStream.flush()
            try {
                outputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return targetFile
    }

    private fun createTemporalFile(context: Context): File? {
        return File(
            context.externalCacheDir,
            java.lang.String.format("%s.jpg", UUID.randomUUID())
        ) // context needed
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
            /**
             * Get a file path from a Uri. This will get the the path for Storage Access
             * Framework Documents, as well as the _data field for the MediaStore and
             * other file-based ContentProviders.<br></br>
             * <br></br>
             * Callers should check whether the path is local before assuming it
             * represents a local file.
             *
             * @param context The context.
             * @param uri     The Uri to query.
             * @author paulburke
             * @see .isLocal
             * @see .getFile
             */
    fun getPath(context: Context, uri: Uri): String? {

        if (DEBUG) Log.e(
            TAG + " File -",
            "Authority: " + uri.authority + ", Fragment: " + uri.fragment + ", Port: " + uri.port + ", Query: " + uri.query + ", Scheme: " + uri.scheme + ", Host: " + uri.host + ", Segments: " + uri.pathSegments.toString()
        )

        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
        var filePath = ""

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // LocalStorageProvider
            if (isLocalStorageDocument(uri)) {
                // The path is the id
                return DocumentsContract.getDocumentId(uri)
            } else if (isExternalStorageDocument(
                    uri
                )
            ) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }else {
                    if (Build.VERSION.SDK_INT > 20) {
                        //getExternalMediaDirs() added in API 21
                        if (context.externalMediaDirs.size > 1) {
                            filePath = context.externalMediaDirs[1].absolutePath
                            filePath = filePath.substring(0, filePath.indexOf("Android")) + split[1]
                        }
                    }else{
                        filePath = "/storage/" + type + "/" + split[1]
                    }
                    return filePath
                }

                // TODO handle non-primary volumes
            } else if (isDownloadsDocument(uri)) {

                val id = DocumentsContract.getDocumentId(uri)
                if (!TextUtils.isEmpty(id)) {
                    if (id.startsWith("raw:")) {
                        return id.replaceFirst("raw:", "");
                    }
                }
                val contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"),
                    Long.valueOf(id)
                )

                return getDataColumn(
                    context,
                    contentUri,
                    null,
                    null
                )
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                var contentUri: Uri? = null
                if ("image" == type) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])

                return getDataColumn(
                    context,
                    contentUri,
                    selection,
                    selectionArgs
                )
            }// MediaProvider
            // DownloadsProvider
            // ExternalStorageProvider
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {

            // Return the remote address
            return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(
                context,
                uri,
                null,
                null
            )

        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }// File
        // MediaStore (and general)

        return null
    }

    /**
     * Convert Uri into File, if possible.
     *
     * @return file A local file that the Uri was pointing to, or null if the
     * Uri is unsupported or pointed to a remote resource.
     * @author paulburke
     * @see .getPath
     */
    fun getFile(context: Context, uri: Uri?): File? {
        if (uri != null) {
            val path = getPath(context, uri)
            if (path != null && isLocal(path)) {
                return File(path)
            }
        }
        return null
    }

    /**
     * Get the file size in a human-readable string.
     *
     * @param size
     * @return
     * @author paulburke
     */
    fun getReadableFileSize(size: Int): String {
        val BYTES_IN_KILOBYTES = 1024
        val dec = DecimalFormat("###.#")
        val KILOBYTES = " KB"
        val MEGABYTES = " MB"
        val GIGABYTES = " GB"
        var fileSize = 0f
        var suffix = KILOBYTES

        if (size > BYTES_IN_KILOBYTES) {
            fileSize = (size / BYTES_IN_KILOBYTES).toFloat()
            if (fileSize > BYTES_IN_KILOBYTES) {
                fileSize = fileSize / BYTES_IN_KILOBYTES
                if (fileSize > BYTES_IN_KILOBYTES) {
                    fileSize = fileSize / BYTES_IN_KILOBYTES
                    suffix = GIGABYTES
                } else {
                    suffix = MEGABYTES
                }
            }
        }
        return (dec.format(fileSize.toDouble()) + suffix).toString()
    }

    /**
     * Attempt to retrieve the thumbnail of given File from the MediaStore. This
     * should not be called on the UI thread.
     *
     * @param context
     * @param file
     * @return
     * @author paulburke
     */
    fun getThumbnail(context: Context, file: File): Bitmap? {
        return getThumbnail(
            context,
            getUri(file)!!,
            getMimeType(file)
        )
    }

    /**
     * Attempt to retrieve the thumbnail of given Uri from the MediaStore. This
     * should not be called on the UI thread.
     *
     * @param context
     * @param uri
     * @param mimeType
     * @return
     * @author paulburke
     */
    @JvmOverloads
    fun getThumbnail(
        context: Context, uri: Uri, mimeType: String? = getMimeType(
            context,
            uri
        )
    ): Bitmap? {
        if (DEBUG) Log.e(TAG, "Attempting to get thumbnail")

        if (!isMediaUri(uri)) {
            Log.e(TAG, "You can only retrieve thumbnails for images and videos.")
            return null
        }

        var bm: Bitmap? = null
        val resolver = context.contentResolver
        var cursor: Cursor? = null
        try {
            cursor = resolver.query(uri, null, null, null, null)
            if (cursor!!.moveToFirst()) {
                val id = cursor.getInt(0)
                if (DEBUG) Log.e(
                    TAG, "Got thumb ID: " + id
                )

                if (mimeType != null && mimeType.contains("video")) {
                    bm = MediaStore.Video.Thumbnails.getThumbnail(
                        resolver,
                        id.toLong(),
                        MediaStore.Video.Thumbnails.MINI_KIND,
                        null
                    )
                } else if (mimeType != null && mimeType.contains("image")) {
                    bm = MediaStore.Images.Thumbnails.getThumbnail(
                        resolver,
                        id.toLong(),
                        MediaStore.Images.Thumbnails.MINI_KIND,
                        null
                    )
                }
            }
        } catch (e: Exception) {
            if (DEBUG) Log.e(
                TAG, "getThumbnail", e
            )
        } finally {
            if (cursor != null) cursor.close()
        }
        return bm
    }

    /**
     * Get the Intent for selecting content to be used in an Intent Chooser.
     *
     * @return The intent for opening a file with Intent.createChooser()
     * @author paulburke
     */
    fun createGetContentIntent(): Intent {
        // Implicitly allow the user to select a particular kind of data
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        // The MIME data type filter
        intent.type = "*/*"
        // Only return URIs that can be opened with ContentResolver
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        return intent
    }

    /**
     * Function to convert bitmap to file
     * @param bitmap [Bitmap]
     * @return file [File]
     */
    fun convertBitmapToFile(bitmap: Bitmap): File? {
        try {
            //AppLogger.e("PostPhoto8", Calendar.getInstance().getTime() + "");
            val current = System.currentTimeMillis()
            val dir = File(SECURE_GALLERY_FOLDER_PATH)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            if (dir.exists()) {
                // AppLogger.e("PostPhoto9", Calendar.getInstance().getTime() + "");
                val file = File(SECURE_GALLERY_FOLDER_PATH + current + ".png")
                if (file.exists()) file.delete()
                file.createNewFile()
                val fOut: FileOutputStream
                //  AppLogger.e("PostPhoto10", Calendar.getInstance().getTime() + "");
                fOut = FileOutputStream(file)
                // AppLogger.e("PostPhoto11", Calendar.getInstance().getTime() + "");
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut)
                // AppLogger.e("PostPhoto12", Calendar.getInstance().getTime() + "");
                fOut.flush()
                fOut.close()
                // AppLogger.e("PostPhoto13", Calendar.getInstance().getTime() + "");
                return file
            } else {
                return null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

    }

    /**
     * Function to delete app directory
     */
    fun deleteAppDirectory() {
        val dir = File(SECURE_GALLERY_FOLDER_PATH)
        if (dir.exists()) {
            if (dir.isDirectory) {
                val children = dir.list() ?: return
                for (i in children.indices) {
                    File(dir, children[i]).delete()
                }
            }
        }
    }

}//private constructor to enforce Singleton pattern
/**
 * Attempt to retrieve the thumbnail of given Uri from the MediaStore. This
 * should not be called on the UI thread.
 *
 * @param context
 * @param uri
 * @return
 * @author paulburke
 */