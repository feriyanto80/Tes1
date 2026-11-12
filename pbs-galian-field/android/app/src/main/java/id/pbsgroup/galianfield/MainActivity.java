package id.pbsgroup.galianfield;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.location.LocationManager;
import android.location.LocationListener;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.content.ClipData;
import androidx.core.content.FileProvider;
import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import org.json.JSONObject;
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
  private static final int REQUEST_NATIVE = 4104;
  private LocationManager gpsManager;
  private LocationListener gpsListener;
  private Handler gpsHandler = new Handler(Looper.getMainLooper());
  private Runnable gpsTimeout;
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
    settings.setUserAgentString(settings.getUserAgentString()+" PBSGalianAndroid/1.2");

    web.setWebViewClient(new WebViewClient() {
      @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        Uri uri = request.getUrl();
        String destination = uri.toString();
        if ("pbsfield".equals(uri.getScheme()) && "gps".equals(uri.getHost())) { requestNativeGPS(); return true; }
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
          boolean photo = false;
          for(String type : params.getAcceptTypes()) if(type != null && (type.contains("image") || type.equals("*/*"))) photo = true;
          if(!photo){
            startActivityForResult(Intent.createChooser(params.createIntent(), "Pilih dokumen"), REQUEST_FILE);
            return true;
          }
          if(params.isCaptureEnabled()){
            Intent camera = createCameraIntent();
            if(camera!=null){ startActivityForResult(camera,REQUEST_FILE); return true; }
          }
          Intent picker = new Intent(Intent.ACTION_GET_CONTENT);
          picker.setType("image/*");
          picker.addCategory(Intent.CATEGORY_OPENABLE);
          picker.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,params.getMode()==FileChooserParams.MODE_OPEN_MULTIPLE);
          Intent chooser = Intent.createChooser(picker,"Pilih foto / kamera");
          Intent camera = createCameraIntent();
          if(camera!=null) chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS,new Intent[]{camera});
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


  private Intent createCameraIntent() {
    Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    if(i.resolveActivity(getPackageManager())==null)return null;
    try{
      File folder=getExternalCacheDir()!=null?getExternalCacheDir():getCacheDir();
      File photo=new File(folder,"PBS_GALIAN_"+System.currentTimeMillis()+".jpg");
      cameraUri=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",photo);
      i.putExtra(MediaStore.EXTRA_OUTPUT,cameraUri);
      i.setClipData(ClipData.newRawUri("PBS Camera",cameraUri));
      i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
      return i;
    }catch(Exception e){cameraUri=null;return null;}
  }
  private boolean hasLocation(){
    return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED ||
           checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;
  }
  private void requestNativeGPS() {
    if(!hasLocation()){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},REQUEST_NATIVE);return;}
    getNativeGPS();
  }
  private void sendGpsError(String message){
    web.evaluateJavascript("window.PBSNativeGPSResult&&window.PBSNativeGPSResult({ok:false,error:"+JSONObject.quote(message)+"});",null);
  }
  private void stopNativeGPS(){
    if(gpsTimeout!=null){gpsHandler.removeCallbacks(gpsTimeout);gpsTimeout=null;}
    if(gpsManager!=null&&gpsListener!=null){try{gpsManager.removeUpdates(gpsListener);}catch(SecurityException ignored){}gpsListener=null;}
  }
  private void sendGps(Location loc){
    stopNativeGPS();
    if(loc==null){sendGpsError("GPS belum mendapatkan lokasi. Aktifkan lokasi presisi dan ulangi.");return;}
    String json=String.format(Locale.US,"{ok:true,lat:%.8f,lng:%.8f,accuracy:%.2f}",loc.getLatitude(),loc.getLongitude(),loc.hasAccuracy()?loc.getAccuracy():9999.0f);
    web.evaluateJavascript("window.PBSNativeGPSResult&&window.PBSNativeGPSResult("+json+");",null);
  }
  private void getNativeGPS(){
    if(!hasLocation()){sendGpsError("Izin lokasi ditolak. Buka Settings > Apps > PBS Galian Field > Permissions > Location.");return;}
    stopNativeGPS();
    gpsManager=(LocationManager)getSystemService(LOCATION_SERVICE);
    if(gpsManager==null){sendGpsError("Sensor lokasi Android tidak tersedia.");return;}
    try{
      String provider=gpsManager.isProviderEnabled(LocationManager.GPS_PROVIDER)?LocationManager.GPS_PROVIDER:
                      (gpsManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)?LocationManager.NETWORK_PROVIDER:null);
      if(provider==null){sendGpsError("GPS/lokasi HP belum aktif.");return;}
      final Location last=gpsManager.getLastKnownLocation(provider);
      gpsListener=new LocationListener(){
        @Override public void onLocationChanged(Location l){sendGps(l);}
        @Override public void onStatusChanged(String p,int s,Bundle e){}
        @Override public void onProviderEnabled(String p){}
        @Override public void onProviderDisabled(String p){sendGpsError("GPS dimatikan.");stopNativeGPS();}
      };
      gpsManager.requestLocationUpdates(provider,0L,0f,gpsListener,Looper.getMainLooper());
      gpsTimeout=()->{stopNativeGPS();if(last!=null&&(System.currentTimeMillis()-last.getTime())<120000)sendGps(last);else sendGpsError("GPS timeout 25 detik. Coba di ruang terbuka.");};
      gpsHandler.postDelayed(gpsTimeout,25000);
    }catch(SecurityException e){sendGpsError("Izin GPS tidak diberikan.");}
    catch(Exception e){sendGpsError("Layanan GPS tidak siap.");}
  }

  @Override protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (request == REQUEST_FILE && fileCallback != null) {
      ArrayList<Uri> picked=new ArrayList<>();
      if(result==RESULT_OK){
        if(data!=null && data.getClipData()!=null){
          ClipData clips=data.getClipData();
          for(int n=0;n<clips.getItemCount();n++)picked.add(clips.getItemAt(n).getUri());
        }else if(data!=null&&data.getData()!=null) picked.add(data.getData());
        else if(cameraUri!=null) picked.add(cameraUri);
      }
      Uri[] chosen=picked.isEmpty()?null:picked.toArray(new Uri[0]);
      cameraUri=null;
      fileCallback.onReceiveValue(chosen);
      fileCallback = null;
    }
  }

  @Override public void onRequestPermissionsResult(int request, String[] perms, int[] grants) {
    super.onRequestPermissionsResult(request, perms, grants);
    if (request == REQUEST_NATIVE) getNativeGPS();
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
    stopNativeGPS();
    web.destroy();
    super.onDestroy();
  }
}
