package id.pbsgroup.pbsmobile;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.net.http.SslError;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String HOME_URL =
        "https://script.google.com/macros/s/AKfycbwfQLhAVyjEPjXXTU_pLbAqWXVImers_huvebEgBwUs4EkxObeA8bedcydLLgDDDagJ/exec?utm_source=chatgpt.com";
    private static final int REQUEST_FILES = 1201;
    private static final int REQUEST_GPS = 1202;
    private static final int REQUEST_STORAGE = 1203;

    private WebView browser;
    private ProgressBar loading;
    private LinearLayout errorView;
    private ValueCallback<Uri[]> fileCallback;
    private Uri cameraUri;
    private GeolocationPermissions.Callback locationCallback;
    private String locationOrigin;
    private String downloadUrl, downloadDisposition, downloadMime;

    private int dp(float n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    @Override public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().setStatusBarColor(Color.rgb(13,24,43));
        getWindow().setNavigationBarColor(Color.rgb(13,24,43));
        buildLayout();
        setupWebView();
        if (bundle != null) {
            String pending = bundle.getString("cameraUri");
            if (pending != null) cameraUri = Uri.parse(pending);
            browser.restoreState(bundle);
            if (browser.getUrl() == null) browser.loadUrl(HOME_URL);
        } else browser.loadUrl(HOME_URL);
    }

    private void buildLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        setContentView(root);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12),dp(5),dp(8),dp(5));
        header.setBackgroundColor(Color.rgb(13,24,43));
        root.addView(header,new LinearLayout.LayoutParams(-1,dp(56)));

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_pbs);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        header.addView(icon,new LinearLayout.LayoutParams(dp(40),dp(40)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(dp(10),0,0,0);
        header.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        TextView title = new TextView(this);
        title.setText("PBS Mobile");
        title.setTextSize(17);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        labels.addView(title);
        TextView sub = new TextView(this);
        sub.setText("PT Panca Baswara Sakti");
        sub.setTextSize(10);
        sub.setTextColor(Color.rgb(169,197,218));
        labels.addView(sub);

        TextView reload = new TextView(this);
        reload.setText("\u27f3");
        reload.setContentDescription("Muat ulang");
        reload.setGravity(Gravity.CENTER);
        reload.setTextSize(29);
        reload.setTextColor(Color.WHITE);
        reload.setOnClickListener(v -> browser.reload());
        header.addView(reload,new LinearLayout.LayoutParams(dp(48),dp(46)));

        loading = new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        loading.setMax(100);
        loading.setVisibility(View.INVISIBLE);
        root.addView(loading,new LinearLayout.LayoutParams(-1,dp(3)));

        FrameLayout frame = new FrameLayout(this);
        root.addView(frame,new LinearLayout.LayoutParams(-1,0,1));
        browser = new WebView(this);
        frame.addView(browser,new FrameLayout.LayoutParams(-1,-1));

        errorView = new LinearLayout(this);
        errorView.setOrientation(LinearLayout.VERTICAL);
        errorView.setGravity(Gravity.CENTER);
        errorView.setBackgroundColor(Color.WHITE);
        errorView.setPadding(dp(20),dp(20),dp(20),dp(20));
        TextView msg = new TextView(this);
        msg.setText("Halaman tidak dapat dibuka.\nPeriksa koneksi internet atau akses Apps Script.");
        msg.setTextSize(16);
        msg.setTextColor(Color.rgb(37,52,72));
        msg.setGravity(Gravity.CENTER);
        errorView.addView(msg);
        TextView retry = new TextView(this);
        retry.setText("COBA LAGI");
        retry.setGravity(Gravity.CENTER);
        retry.setPadding(dp(16),dp(20),dp(16),dp(20));
        retry.setTextColor(Color.rgb(0,140,190));
        retry.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        retry.setOnClickListener(v -> {
            errorView.setVisibility(View.GONE);
            browser.loadUrl(HOME_URL);
        });
        errorView.addView(retry);
        frame.addView(errorView,new FrameLayout.LayoutParams(-1,-1));
        errorView.setVisibility(View.GONE);
    }

    private boolean isTrustedGoogleAppsScript(Uri uri) {
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme())) return false;
        String host = uri.getHost();
        if (host == null) return false;
        host = host.toLowerCase(Locale.US);
        return host.equals("script.google.com") ||
            host.equals("script.googleusercontent.com") ||
            host.endsWith(".script.googleusercontent.com");
    }

    private void openExternal(Uri uri) {
        try {
            Intent intent;
            if ("intent".equalsIgnoreCase(uri.getScheme())) {
                intent = Intent.parseUri(uri.toString(),Intent.URI_INTENT_SCHEME);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setComponent(null);
            } else {
                intent = new Intent(Intent.ACTION_VIEW,uri);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
            }
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this,"Tautan tidak bisa dibuka",Toast.LENGTH_SHORT).show();
        }
    }

    private void setupWebView() {
        WebSettings settings = browser.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setGeolocationEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        if (Build.VERSION.SDK_INT >= 26) settings.setSafeBrowsingEnabled(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(browser,true);

        browser.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                Uri uri = request.getUrl();
                if (isTrustedGoogleAppsScript(uri)) return false;
                openExternal(uri);
                return true;
            }
            @Override public void onPageStarted(WebView view,String url,android.graphics.Bitmap icon) {
                errorView.setVisibility(View.GONE);
                loading.setVisibility(View.VISIBLE);
            }
            @Override public void onPageFinished(WebView view,String url) {
                loading.setVisibility(View.INVISIBLE);
            }
            @Override public void onReceivedError(WebView view,WebResourceRequest request,WebResourceError error) {
                if (request.isForMainFrame()) {
                    loading.setVisibility(View.INVISIBLE);
                    errorView.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onReceivedHttpError(WebView view,WebResourceRequest request,WebResourceResponse response) {
                if (request.isForMainFrame() && response.getStatusCode()>=500) {
                    loading.setVisibility(View.INVISIBLE);
                    errorView.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onReceivedSslError(WebView view,SslErrorHandler handler,SslError error) {
                handler.cancel();
                errorView.setVisibility(View.VISIBLE);
            }
        });

        browser.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view,int progress) {
                loading.setProgress(progress);
                if (progress>=100) loading.setVisibility(View.INVISIBLE);
            }
            @Override public boolean onShowFileChooser(WebView view,ValueCallback<Uri[]> callback,FileChooserParams params) {
                if (fileCallback!=null) fileCallback.onReceiveValue(null);
                fileCallback=callback;
                cameraUri=null;
                try {
                    Intent filePicker = params.createIntent();
                    filePicker.addCategory(Intent.CATEGORY_OPENABLE);
                    Intent chooser = Intent.createChooser(filePicker,"Pilih foto atau berkas");
                    if (Build.VERSION.SDK_INT>=29 && acceptsImages(params)) {
                        Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        if (camera.resolveActivity(getPackageManager())!=null) {
                            ContentValues values = new ContentValues();
                            values.put(MediaStore.Images.Media.DISPLAY_NAME,
                                "PBS_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date())+".jpg");
                            values.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");
                            values.put(MediaStore.Images.Media.RELATIVE_PATH,
                                Environment.DIRECTORY_PICTURES+"/PBS Mobile");
                            cameraUri=getContentResolver().insert(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
                            if (cameraUri!=null) {
                                camera.putExtra(MediaStore.EXTRA_OUTPUT,cameraUri);
                                camera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS,new Intent[]{camera});
                            }
                        }
                    }
                    startActivityForResult(chooser,REQUEST_FILES);
                    return true;
                } catch(Exception e) {
                    fileCallback.onReceiveValue(null);
                    fileCallback=null;
                    cleanCamera();
                    Toast.makeText(MainActivity.this,"Pemilih file tidak tersedia",Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
            @Override public void onGeolocationPermissionsShowPrompt(String origin,
                GeolocationPermissions.Callback callback) {
                if (!isTrustedGoogleAppsScript(Uri.parse(origin))) {
                    callback.invoke(origin,false,false);
                    return;
                }
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==
                    PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==
                    PackageManager.PERMISSION_GRANTED) {
                    callback.invoke(origin,true,false);
                } else {
                    locationCallback=callback;
                    locationOrigin=origin;
                    requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    },REQUEST_GPS);
                }
            }
        });

        browser.setDownloadListener((url,userAgent,disposition,mime,length)->{
            if (!url.startsWith("https://") && !url.startsWith("http://")) {
                Toast.makeText(this,"Tipe unduhan belum didukung",Toast.LENGTH_LONG).show();
                return;
            }
            if (Build.VERSION.SDK_INT<=28 &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!=
                PackageManager.PERMISSION_GRANTED) {
                downloadUrl=url; downloadDisposition=disposition; downloadMime=mime;
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_STORAGE);
            } else startDownload(url,disposition,mime);
        });
    }

    private boolean acceptsImages(WebChromeClient.FileChooserParams params) {
        String[] accepts=params.getAcceptTypes();
        if (accepts==null || accepts.length==0) return true;
        for(String accept:accepts) {
            if (accept!=null && (accept.isEmpty() || accept.equals("*/*") ||
                accept.startsWith("image/") || accept.equals(".jpg") ||
                accept.equals(".png") || accept.equals(".jpeg"))) return true;
        }
        return false;
    }

    private void startDownload(String url,String disposition,String mime) {
        try {
            String name=URLUtil.guessFileName(url,disposition,mime);
            DownloadManager.Request request=new DownloadManager.Request(Uri.parse(url));
            request.setTitle(name);
            request.setDescription("PBS Mobile download");
            if(mime!=null&&!mime.isEmpty()) request.setMimeType(mime);
            request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,name);
            String cookie=CookieManager.getInstance().getCookie(url);
            if(cookie!=null) request.addRequestHeader("Cookie",cookie);
            request.addRequestHeader("User-Agent",browser.getSettings().getUserAgentString());
            ((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(request);
            Toast.makeText(this,"File disimpan di Download",Toast.LENGTH_SHORT).show();
        } catch(Exception e) {
            Toast.makeText(this,"Unduhan gagal; mencoba browser",Toast.LENGTH_LONG).show();
            openExternal(Uri.parse(url));
        }
    }

    private void cleanCamera() {
        if(cameraUri!=null) {
            try {getContentResolver().delete(cameraUri,null,null);}
            catch(Exception ignored) {}
            cameraUri=null;
        }
    }

    @Override protected void onActivityResult(int code,int resultCode,Intent data) {
        super.onActivityResult(code,resultCode,data);
        if(code!=REQUEST_FILES) return;
        Uri[] result=null;
        boolean photoTaken=false;
        if(resultCode==RESULT_OK) {
            if(data!=null) result=WebChromeClient.FileChooserParams.parseResult(resultCode,data);
            else if(cameraUri!=null) {result=new Uri[]{cameraUri};photoTaken=true;}
        }
        if(fileCallback!=null) {fileCallback.onReceiveValue(result);fileCallback=null;}
        if(!photoTaken) cleanCamera();
        cameraUri=null;
    }

    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(code,permissions,results);
        if(code==REQUEST_GPS) {
            boolean approved=false;
            for(int result:results) if(result==PackageManager.PERMISSION_GRANTED) approved=true;
            if(locationCallback!=null) locationCallback.invoke(locationOrigin,approved,false);
            locationCallback=null;locationOrigin=null;
        } else if(code==REQUEST_STORAGE) {
            if(results.length>0 && results[0]==PackageManager.PERMISSION_GRANTED &&
                downloadUrl!=null) startDownload(downloadUrl,downloadDisposition,downloadMime);
            else Toast.makeText(this,"Izin penyimpanan ditolak",Toast.LENGTH_SHORT).show();
            downloadUrl=null;downloadDisposition=null;downloadMime=null;
        }
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        if(cameraUri!=null) state.putString("cameraUri",cameraUri.toString());
        browser.saveState(state);
        super.onSaveInstanceState(state);
    }

    @Override public void onBackPressed() {
        if(errorView.getVisibility()==View.VISIBLE) {
            errorView.setVisibility(View.GONE);
            browser.loadUrl(HOME_URL);
        } else if(browser.canGoBack()) browser.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if(fileCallback!=null) {fileCallback.onReceiveValue(null);fileCallback=null;}
        if(locationCallback!=null) {
            locationCallback.invoke(locationOrigin,false,false);
            locationCallback=null;
        }
        if(browser!=null) {browser.stopLoading();browser.destroy();}
        super.onDestroy();
    }
}
