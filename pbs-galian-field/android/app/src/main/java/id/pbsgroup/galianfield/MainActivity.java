package id.pbsgroup.galianfield;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.WindowManager;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

/** PBS Galian Field Android companion app. API and interface are served by the Node.js backend. */
public final class MainActivity extends Activity {
  private static final int REQUEST_FILE = 4102;
  private static final int REQUEST_LOCATION = 4103;
  private WebView web;
  private ValueCallback<Uri[]> fileCallback;
  private Uri cameraUri;
  private GeolocationPermissions.Callback locationCallback;
  private String locationOrigin;
  private String serverOrigin;

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().setStatusBarColor(0xFF0C2C4E);
    getWindow().setNavigationBarColor(0xFF0C2C4E);
    Uri server = Uri.parse(BuildConfig.APP_URL);
    serverOrigin = server.getScheme() + "://" + server.getAuthority();
    web = new WebView(this);
    setContentView(web);
    WebSettings settings = web.getSettings();
    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    settings.setDatabaseEnabled(true);
    settings.setGeolocationEnabled(true);
    settings.setAllowFileAccess(false);
    settings.setAllowContentAccess(true);
    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

    web.setWebViewClient(new WebViewClient() {
      @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        Uri uri = request.getUrl();
        String destination = uri.toString();
        if (destination.equals(serverOrigin) || destination.startsWith(serverOrigin + "/")) return false;
        if ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) {
          try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
          catch (Exception ignored) { Toast.makeText(MainActivity.this, "Tidak dapat membuka tautan", Toast.LENGTH_SHORT).show(); }
        }
        return true;
      }
    });
    web.setWebChromeClient(new WebChromeClient() {
      @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
        if (fileCallback != null) fileCallback.onReceiveValue(null);
        fileCallback = callback;
        try {
          Intent choose = params.createIntent();
          Intent chooser = Intent.createChooser(choose, "Pilih dokumen / kamera");
          boolean photo = false;
          for (String type : params.getAcceptTypes()) {
            if (type.contains("image") || type.equals("*/*")) photo = true;
          }
          if (photo) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "PBS_GALIAN_" + System.currentTimeMillis() + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            cameraUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (cameraUri != null) {
              Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
              if (camera.resolveActivity(getPackageManager()) != null) {
                camera.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
                camera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{camera});
              }
            }
          }
          startActivityForResult(chooser, REQUEST_FILE);
          return true;
        } catch (Exception ex) {
          fileCallback = null;
          Toast.makeText(MainActivity.this, "Pemilih file tidak tersedia", Toast.LENGTH_LONG).show();
          return false;
        }
      }

      @Override public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
        if (!serverOrigin.equals(origin)) { callback.invoke(origin, false, false); return; }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
          callback.invoke(origin, true, false);
        } else {
          locationCallback = callback;
          locationOrigin = origin;
          requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
        }
      }
    });
    if (state != null) web.restoreState(state); else web.loadUrl(BuildConfig.APP_URL);
  }

  @Override protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (request == REQUEST_FILE && fileCallback != null) {
      Uri[] chosen = WebChromeClient.FileChooserParams.parseResult(result, data);
      if (result == RESULT_OK && (chosen == null || chosen.length == 0) && cameraUri != null) chosen = new Uri[]{cameraUri};
      if (cameraUri != null && (chosen == null || chosen.length == 0 || !cameraUri.equals(chosen[0]))) {
        try { getContentResolver().delete(cameraUri, null, null); } catch (Exception ignored) {}
      }
      cameraUri = null;
      fileCallback.onReceiveValue(chosen);
      fileCallback = null;
    }
  }

  @Override public void onRequestPermissionsResult(int request, String[] perms, int[] grants) {
    super.onRequestPermissionsResult(request, perms, grants);
    if (request == REQUEST_LOCATION && locationCallback != null) {
      boolean allowed = false;
      for (int grant : grants) if (grant == PackageManager.PERMISSION_GRANTED) allowed = true;
      locationCallback.invoke(locationOrigin, allowed, false);
      locationCallback = null;
    }
  }

  @Override protected void onSaveInstanceState(Bundle state) { web.saveState(state); super.onSaveInstanceState(state); }
  @Override public void onBackPressed() { if (web.canGoBack()) web.goBack(); else super.onBackPressed(); }
  @Override protected void onDestroy() {
    if (fileCallback != null) { fileCallback.onReceiveValue(null); fileCallback = null; }
    web.destroy();
    super.onDestroy();
  }
}
