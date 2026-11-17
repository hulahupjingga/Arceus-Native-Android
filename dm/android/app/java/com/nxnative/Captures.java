// The Switch's Capture button (Arceus-Native-Android #56, #64): the engine saves the game's frame as a PNG in the app's captures folder (nvn_vk.c capture_now,
// android_main.c front_capture_saved); this moves it into the phone's gallery, Pictures/<app name>/, as a JPEG (as the console's album keeps them), and says so.
// Android 10+ adds to the gallery through MediaStore with no storage permission. status: 1 saved (path), 0 the engine could not save it, -1 the game does not
// allow screenshots here (as on the console: its own nn::oe::DisableScreenShot)
package com.nxnative;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;
import java.io.File;
import java.io.OutputStream;

final class Captures {
    private Captures() { }

    static void publish(Activity a, String path, int status) {
        String msg;
        if (status < 0) msg = T.t(a, "The game doesn't allow photos here.");
        else if (status == 0 || path == null || path.isEmpty()) msg = T.t(a, "The photo could not be saved.");
        else msg = save(a, new File(path)) ? T.t(a, "Photo saved to Pictures/%s.", album(a)) : T.t(a, "The photo could not be saved.");
        final String m = msg;
        a.runOnUiThread(() -> Toast.makeText(a, m, Toast.LENGTH_SHORT).show());
    }

    private static String album(Activity a) {
        CharSequence l = a.getApplicationInfo().loadLabel(a.getPackageManager());
        String s = l != null ? l.toString().replaceAll("[/\\\\:*?\"<>|]", "").trim() : "";
        return s.isEmpty() ? "Captures" : s;
    }

    private static boolean save(Activity a, File png) {
        Bitmap b = BitmapFactory.decodeFile(png.getPath());
        if (b == null) { android.util.Log.e("nxnative", "photo: cannot read " + png); return false; }
        String name = png.getName().replaceAll("\\.png$", ".jpg");
        ContentValues v = new ContentValues();
        v.put(MediaStore.Images.Media.DISPLAY_NAME, name);
        v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        v.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + album(a));
        v.put(MediaStore.Images.Media.IS_PENDING, 1);
        ContentResolver r = a.getContentResolver();
        Uri u = null;
        try {
            u = r.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), v);
            if (u == null) return false;
            try (OutputStream o = r.openOutputStream(u)) { if (o == null || !b.compress(Bitmap.CompressFormat.JPEG, 95, o)) throw new java.io.IOException("compress"); }
            v.clear(); v.put(MediaStore.Images.Media.IS_PENDING, 0); r.update(u, v, null, null);
            android.util.Log.i("nxnative", "photo: saved to the gallery as " + name);
            if (!png.delete()) png.deleteOnExit();   // (the engine's PNG: the gallery keeps the JPEG; on a failure the PNG stays in the app's captures folder)
            return true;
        } catch (Exception e) {
            android.util.Log.e("nxnative", "photo: not saved to the gallery: " + e);
            if (u != null) try { r.delete(u, null, null); } catch (Exception ignored) { }
            return false;
        } finally {
            b.recycle();
        }
    }
}
