package com.hyperpixelacity.app.core.media
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Clip(val uri:Uri,val name:String,val duration:Long)
class Recordings(private val context:Context) {
 suspend fun list():List<Clip> = withContext(Dispatchers.IO) {
  val out=mutableListOf<Clip>()
  context.contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
   arrayOf(MediaStore.Video.Media._ID,MediaStore.Video.Media.DISPLAY_NAME,MediaStore.Video.Media.DURATION),
   "${MediaStore.Video.Media.RELATIVE_PATH} = ? AND ${MediaStore.Video.Media.OWNER_PACKAGE_NAME} = ?",
   arrayOf("Movies/Hyperpixelacity/",context.packageName),"${MediaStore.Video.Media.DATE_ADDED} DESC")?.use { c ->
    while(c.moveToNext()) out+=Clip(ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,c.getLong(0)),c.getString(1),c.getLong(2))
   };out
 }
 suspend fun delete(uri:Uri) = withContext(Dispatchers.IO) { context.contentResolver.delete(uri,null,null) }
 suspend fun rename(uri:Uri,title:String) = withContext(Dispatchers.IO) {
  val safe=title.trim().replace(Regex("[^\\p{L}\\p{N} _-]"),"").take(60).ifBlank { "Hyperpixelacity" }
  context.contentResolver.update(uri,ContentValues().apply { put(MediaStore.Video.Media.DISPLAY_NAME,"$safe.mp4") },null,null)
 }
 fun share(uri:Uri) { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type="video/mp4";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) },"Share your creation")) }
}
