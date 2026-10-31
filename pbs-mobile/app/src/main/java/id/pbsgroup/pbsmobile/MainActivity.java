package id.pbsgroup.pbsmobile;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.LocationManager;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.drawable.GradientDrawable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    // BALAP is opened using HTTPS when possible; the explicit HTTP option
    // is available only if the legacy deployment lacks TLS.
    private static final String BALAP_HTTPS = "https://balap.pbsgroup.id/";
    private static final String BALAP_HTTP = "http://balap.pbsgroup.id/";
    private static final String SURVEY_URL = "https://basl.pbsgroup.id/";
    private static final int HOME = 0, BALAP = 1, SURVEY = 2;
    private static final int REQUEST_FILE = 1201, REQUEST_GPS = 1202, REQUEST_STORAGE = 1203;

    private int selected = HOME;
    private LinearLayout root, header, bottom, homeView;
    private TextView title, subtitle, gpsButton, refreshButton;
    private ImageView logo;
    private FrameLayout body;
    private ProgressBar progress;
    private final WebView[] pages = new WebView[3];
    private final boolean[] started = new boolean[3];
    private final TextView[] navigationButtons = new TextView[3];
    private boolean balapUsingHttp = false;
    private View errorOverlay;
    private ValueCallback<Uri[]> fileCallback;
    private Uri cameraUri;
    private GeolocationPermissions.Callback locationCallback;
    private String locationOrigin;
    private String queuedDownloadUrl, queuedDisposition, queuedMime;

    private static final int NAVY = Color.rgb(9, 27, 53);
    private static final int BLUE = Color.rgb(23, 99, 205);
    private static final int GREEN = Color.rgb(0, 147, 115);
    private static final int TEXT = Color.rgb(22, 37, 62);
    private static final int MUTED = Color.rgb(107, 124, 146);
    private static final int BG = Color.rgb(244, 247, 251);

    private int dp(float val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable shape(int color, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(color);
        t.setTextSize(size);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        createLayout();
        if (state != null) {
            int restored = state.getInt("selected", HOME);
            balapUsingHttp = state.getBoolean("balapHttp", false);
            String camera = state.getString("cameraUri");
            if (camera != null) cameraUri = Uri.parse(camera);
            for (int id = BALAP; id <= SURVEY; id++) {
                Bundle saved = state.getBundle("webState" + id);
                if (saved != null) {
                    WebView w = getOrCreateWebView(id);
                    w.restoreState(saved);
                    started[id] = w.getUrl() != null;
                }
            }
            showModule(restored, false);
        } else {
            showModule(HOME, false);
        }
    }

    private void createLayout() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        setContentView(root);

        header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(NAVY);
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(58)));

        logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_pbs);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        header.addView(logo, new LinearLayout.LayoutParams(dp(42), dp(42)));
        logo.setContentDescription("Logo PT Panca Baswara Sakti");
        logo.setOnClickListener(v -> showModule(HOME, false));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(dp(9),0,0,0);
        header.addView(labels, new LinearLayout.LayoutParams(0,-2,1));
        title = text("PBS Balap", 18, Color.WHITE, true);
        title.setSingleLine();
        labels.addView(title);
        subtitle = text("Digital Forms", 11, Color.rgb(173,198,224), false);
        labels.addView(subtitle);

        gpsButton = text("GPS", 13, Color.WHITE, true);
        gpsButton.setGravity(Gravity.CENTER);
        gpsButton.setContentDescription("Periksa GPS Android");
        gpsButton.setOnClickListener(v -> showGpsDialog());
        header.addView(gpsButton, new LinearLayout.LayoutParams(dp(46),dp(46)));

        refreshButton = text("\u27f3", 30, Color.WHITE, false);
        refreshButton.setGravity(Gravity.CENTER);
        refreshButton.setContentDescription("Muat ulang form yang aktif");
        refreshButton.setOnClickListener(v -> {
            if (selected != HOME && pages[selected] != null) pages[selected].reload();
        });
        header.addView(refreshButton, new LinearLayout.LayoutParams(dp(46),dp(46)));

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setVisibility(View.GONE);
        root.addView(progress, new LinearLayout.LayoutParams(-1,dp(3)));

        body = new FrameLayout(this);
        body.setBackgroundColor(BG);
        root.addView(body, new LinearLayout.LayoutParams(-1,0,1));
        buildHomePage();

        bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.CENTER);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setPadding(dp(6),dp(4),dp(6),dp(5));
        bottom.setBackgroundColor(Color.WHITE);
        root.addView(bottom, new LinearLayout.LayoutParams(-1,dp(63)));

        String[] items = {"⌂\nBeranda", "▤\nBALAP", "▤\nBA Survey"};
        for (int i=0;i<3;i++) {
            final int module=i;
            TextView item = text(items[i],12,MUTED,false);
            item.setGravity(Gravity.CENTER);
            item.setBackground(shape(Color.WHITE,12));
            item.setOnClickListener(v -> showModule(module,false));
            LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(0,-1,1);
            layout.setMargins(dp(2),0,dp(2),0);
            bottom.addView(item, layout);
            navigationButtons[i]=item;
        }
        applyResponsiveHeader();
    }

    private void buildHomePage() {
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        scroll.setClipToPadding(false);
        homeView=new LinearLayout(this);
        homeView.setPadding(dp(16),dp(20),dp(16),dp(24));
        homeView.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(homeView, new ScrollView.LayoutParams(-1,-2));
        body.addView(scroll, new FrameLayout.LayoutParams(-1,-1));
        scroll.setTag("HOME_VIEW");

        LinearLayout heading=new LinearLayout(this);
        heading.setPadding(dp(20),dp(18),dp(20),dp(18));
        heading.setOrientation(LinearLayout.VERTICAL);
        heading.setGravity(Gravity.CENTER);
        heading.setBackground(shape(Color.WHITE,20));
        ImageView headingIcon=new ImageView(this);
        headingIcon.setImageResource(R.drawable.ic_pbs);
        headingIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        heading.addView(headingIcon,new LinearLayout.LayoutParams(dp(95),dp(95)));
        TextView appName=text("PBS Balap",25,NAVY,true);
        appName.setGravity(Gravity.CENTER);
        heading.addView(appName);
        TextView tagline=text("Satu Aplikasi • Dua Form Digital",13,MUTED,false);
        tagline.setGravity(Gravity.CENTER);
        heading.addView(tagline);
        homeView.addView(heading,new LinearLayout.LayoutParams(-1,-2));

        TextView section=text("Formulir Digital FTTH",16,TEXT,true);
        LinearLayout.LayoutParams sectionLp=new LinearLayout.LayoutParams(-1,-2);
        sectionLp.topMargin=dp(22);
        sectionLp.bottomMargin=dp(10);
        homeView.addView(section,sectionLp);

        addHomeModule("▤","BALAP Digital",
            "Berita Acara Lapangan Project FTTH",
            "Laporan pekerjaan lapangan, lokasi, dan dokumentasi.",
            BLUE, BALAP);
        addHomeModule("▤","BA Survey Digital",
            "Berita Acara Survey Project FTTH",
            "Data survei, desa, PIC, dan administrasi lapangan.",
            GREEN, SURVEY);

        TextView note=text("Form tersambung langsung ke website PBS. Data tetap diproses oleh sistem BALAP dan BASL masing-masing.",12,MUTED,false);
        note.setPadding(dp(8),dp(12),dp(8),dp(12));
        homeView.addView(note,new LinearLayout.LayoutParams(-1,-2));
    }

    private void addHomeModule(String glyph, String name, String description,
                               String details, int accent, int module) {
        LinearLayout card=new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(15),dp(17),dp(12),dp(17));
        card.setBackground(shape(Color.WHITE,18));
        LinearLayout.LayoutParams cardLp=new LinearLayout.LayoutParams(-1,-2);
        cardLp.bottomMargin=dp(12);
        homeView.addView(card,cardLp);
        card.setElevation(dp(2));
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> showModule(module,false));

        TextView ico=text(glyph,30,accent,true);
        ico.setGravity(Gravity.CENTER);
        ico.setBackground(shape(module==BALAP?Color.rgb(226,238,255):Color.rgb(220,249,237),13));
        card.addView(ico,new LinearLayout.LayoutParams(dp(54),dp(60)));

        LinearLayout content=new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12),0,dp(4),0);
        card.addView(content,new LinearLayout.LayoutParams(0,-2,1));
        content.addView(text(name,17,NAVY,true));
        TextView d=text(description,12,MUTED,false);
        d.setPadding(0,dp(3),0,dp(5));
        content.addView(d);
        TextView info=text(details,11,MUTED,false);
        content.addView(info);
        TextView arrow=text("›",28,accent,true);
        arrow.setGravity(Gravity.CENTER);
        card.addView(arrow,new LinearLayout.LayoutParams(dp(22),dp(46)));
    }

    private WebView getOrCreateWebView(int module) {
        if (pages[module] != null) return pages[module];
        WebView page = new WebView(this);
        page.setBackgroundColor(Color.WHITE);
        page.setVisibility(View.GONE);
        configure(page, module);
        body.addView(page,new FrameLayout.LayoutParams(-1,-1));
        pages[module] = page;
        return page;
    }

    private void showModule(int module, boolean forceReload) {
        if (module < HOME || module > SURVEY) module=HOME;
        selected=module;
        for (int i=0;i<=SURVEY;i++) {
            TextView button=navigationButtons[i];
            if(button!=null) {
                boolean active=i==module;
                button.setTextColor(active?BLUE:MUTED);
                button.setTypeface(Typeface.DEFAULT,active?Typeface.BOLD:Typeface.NORMAL);
                button.setBackground(shape(active?Color.rgb(233,243,255):Color.WHITE,12));
            }
        }
        refreshButton.setVisibility(module==HOME?View.GONE:View.VISIBLE);
        subtitle.setText(module==HOME?"Digital Forms":module==BALAP?"BALAP Digital":"BA Survey Digital");

        for (int i=0;i<body.getChildCount();i++) body.getChildAt(i).setVisibility(View.GONE);
        hideError();
        if (module==HOME) {
            body.getChildAt(0).setVisibility(View.VISIBLE);
            progress.setVisibility(View.GONE);
        } else {
            WebView page=getOrCreateWebView(module);
            page.setVisibility(View.VISIBLE);
            if (!started[module] || forceReload) {
                started[module]=true;
                page.loadUrl(module==BALAP?(balapUsingHttp?BALAP_HTTP:BALAP_HTTPS):SURVEY_URL);
            }
            progress.setVisibility(page.getProgress()<100?View.VISIBLE:View.GONE);
        }
        applyResponsiveHeader();
    }

    private void configure(WebView view, int module) {
        WebSettings settings=view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setTextZoom(100);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(true);
        if (Build.VERSION.SDK_INT>=26) settings.setSafeBrowsingEnabled(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(view,true);

        view.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView w,WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                Uri uri=request.getUrl();
                if (internalUri(uri)) return false;
                external(uri);
                return true;
            }

            @Override public void onPageStarted(WebView w,String url,android.graphics.Bitmap favicon) {
                if(selected==module) {
                    hideError();
                    progress.setProgress(0);
                    progress.setVisibility(View.VISIBLE);
                }
            }

            @Override public void onPageFinished(WebView w,String url) {
                if(selected==module) progress.setVisibility(View.GONE);
                Uri uri=Uri.parse(url);
                String host=uri.getHost()==null?"":uri.getHost().toLowerCase(Locale.US);
                if(host.equals("balap.pbsgroup.id") || host.equals("basl.pbsgroup.id")) {
                    // Restrained style fix on our own top-level pages only;
                    // no access to private cross-domain iframe content.
                    w.evaluateJavascript(
                        "(function(){if(!document.head)return;" +
                        "if(!document.querySelector('meta[name=viewport]'))" +
                        "{var m=document.createElement('meta');m.name='viewport';" +
                        "m.content='width=device-width,initial-scale=1';document.head.appendChild(m);}" +
                        "if(!document.getElementById('pbs-unified-mobile'))" +
                        "{var s=document.createElement('style');s.id='pbs-unified-mobile';" +
                        "s.textContent='img,video{max-width:100%;height:auto;}' +" +
                        "'input,select,textarea{max-width:100%;box-sizing:border-box;}';" +
                        "document.head.appendChild(s);}})();",null);
                }
            }

            @Override public void onReceivedError(WebView w,WebResourceRequest request,WebResourceError error) {
                if(!request.isForMainFrame())return;
                if(selected!=module)return;
                showError(module, "Halaman gagal dimuat. Periksa jaringan dan akses website PBS.",module==BALAP&&!balapUsingHttp);
            }

            @Override public void onReceivedHttpError(WebView w,WebResourceRequest request,WebResourceResponse response) {
                if(request.isForMainFrame() && response.getStatusCode()>=500 && selected==module)
                    showError(module,"Server PBS sedang mengalami gangguan ("+response.getStatusCode()+").",false);
            }

            @Override public void onReceivedSslError(WebView w,SslErrorHandler handler,SslError err) {
                handler.cancel();
                if(selected==module) showError(module,"Sertifikat HTTPS tidak valid. Demi keamanan koneksi tidak dilanjutkan.",false);
            }
        });

        view.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView w,int p) {
                if(selected==module){
                    progress.setProgress(p);
                    progress.setVisibility(p>=100?View.GONE:View.VISIBLE);
                }
            }

            @Override public boolean onShowFileChooser(WebView w,
                 ValueCallback<Uri[]> callback, FileChooserParams params) {
                if(fileCallback!=null) fileCallback.onReceiveValue(null);
                fileCallback=callback;
                cameraUri=null;
                try{
                    Intent picker=params.createIntent();
                    picker.addCategory(Intent.CATEGORY_OPENABLE);
                    Intent chooser=Intent.createChooser(picker,"Pilih foto atau file");
                    if(Build.VERSION.SDK_INT>=29 && acceptsImages(params)){
                        Intent camera=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        if(camera.resolveActivity(getPackageManager())!=null){
                            ContentValues values=new ContentValues();
                            values.put(MediaStore.Images.Media.DISPLAY_NAME,
                                "PBS_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date())+".jpg");
                            values.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");
                            values.put(MediaStore.Images.Media.RELATIVE_PATH,Environment.DIRECTORY_PICTURES+"/PBS Balap");
                            cameraUri=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
                            if(cameraUri!=null){
                                camera.putExtra(MediaStore.EXTRA_OUTPUT,cameraUri);
                                camera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS,new Intent[]{camera});
                            }
                        }
                    }
                    startActivityForResult(chooser,REQUEST_FILE);
                    return true;
                }catch(Exception e){
                    if(fileCallback!=null)fileCallback.onReceiveValue(null);
                    fileCallback=null;
                    deleteUnusedCamera();
                    Toast.makeText(MainActivity.this,"Pemilih file tidak tersedia",Toast.LENGTH_SHORT).show();
                    return true;
                }
            }

            @Override public void onGeolocationPermissionsShowPrompt(
                String origin,GeolocationPermissions.Callback callback) {
                if(!trustedGPS(Uri.parse(origin))){
                    callback.invoke(origin,false,false);
                    return;
                }
                if(hasLocationPermission()) callback.invoke(origin,true,false);
                else {
                    if(locationCallback!=null) locationCallback.invoke(locationOrigin,false,false);
                    locationCallback=callback;
                    locationOrigin=origin;
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION},REQUEST_GPS);
                }
            }
        });

        view.setDownloadListener((url,userAgent,disposition,mime,length)->{
            Uri uri=Uri.parse(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) &&
               !("http".equalsIgnoreCase(uri.getScheme()) &&
                   "balap.pbsgroup.id".equalsIgnoreCase(uri.getHost()))) {
                Toast.makeText(this,"Link unduhan tidak didukung",Toast.LENGTH_SHORT).show();
                return;
            }
            if(Build.VERSION.SDK_INT<=28 &&
               checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
                queuedDownloadUrl=url;
                queuedDisposition=disposition;
                queuedMime=mime;
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_STORAGE);
            } else download(url,disposition,mime,view);
        });
    }

    private void showError(int module,String message,boolean offerHttp) {
        if(selected!=module)return;
        hideError();
        progress.setVisibility(View.GONE);
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(22),dp(30),dp(22),dp(30));
        box.setBackgroundColor(Color.WHITE);
        TextView msg=text(message,16,TEXT,false);
        msg.setGravity(Gravity.CENTER);
        box.addView(msg,new LinearLayout.LayoutParams(-1,-2));
        TextView retry=text("COBA LAGI",15,BLUE,true);
        retry.setGravity(Gravity.CENTER);
        retry.setPadding(dp(20),dp(20),dp(20),dp(16));
        retry.setOnClickListener(v->{hideError(); pages[module].reload();});
        box.addView(retry,new LinearLayout.LayoutParams(-1,-2));
        if(offerHttp){
            TextView fallback=text("Buka BALAP versi HTTP (tidak terenkripsi)",13,Color.rgb(162,76,20),true);
            fallback.setGravity(Gravity.CENTER);
            fallback.setPadding(dp(5),dp(12),dp(5),dp(12));
            fallback.setOnClickListener(v->{
                new AlertDialog.Builder(this)
                    .setTitle("Koneksi tidak terenkripsi")
                    .setMessage("Alamat HTTP tidak melindungi data saat dikirim. Gunakan hanya jika server BALAP belum mendukung HTTPS dan Anda memahami risikonya.")
                    .setNegativeButton("Batal",null)
                    .setPositiveButton("Lanjutkan", (dialog,which)->{
                        balapUsingHttp=true;
                        hideError();
                        pages[BALAP].loadUrl(BALAP_HTTP);
                    }).show();
            });
            box.addView(fallback,new LinearLayout.LayoutParams(-1,-2));
        }
        errorOverlay=box;
        body.addView(box,new FrameLayout.LayoutParams(-1,-1));
    }

    private void hideError() {
        if(errorOverlay!=null) {
            body.removeView(errorOverlay);
            errorOverlay=null;
        }
    }

    private boolean internalUri(Uri uri) {
        if(uri==null)return false;
        String scheme=uri.getScheme(),host=uri.getHost();
        if(scheme==null || host==null)return false;
        host=host.toLowerCase(Locale.US);
        if("https".equalsIgnoreCase(scheme)){
            return host.equals("balap.pbsgroup.id") || host.equals("basl.pbsgroup.id")
                || host.equals("script.google.com") || host.equals("script.googleusercontent.com")
                || host.endsWith(".script.googleusercontent.com")
                || host.endsWith("-script.googleusercontent.com");
        }
        return "http".equalsIgnoreCase(scheme)&&host.equals("balap.pbsgroup.id");
    }

    private boolean trustedGPS(Uri uri) {
        if(uri==null || !"https".equalsIgnoreCase(uri.getScheme()))return false;
        String host=uri.getHost();
        if(host==null)return false;
        host=host.toLowerCase(Locale.US);
        return host.equals("balap.pbsgroup.id") || host.equals("basl.pbsgroup.id")
            || host.equals("script.google.com") || host.equals("script.googleusercontent.com")
            || host.endsWith(".script.googleusercontent.com")
            || host.endsWith("-script.googleusercontent.com");
    }

    private boolean hasLocationPermission(){
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
            || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;
    }

    private void showGpsDialog(){
        if(!hasLocationPermission()){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION},REQUEST_GPS);
            return;
        }
        LocationManager mgr=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
        boolean gps=false,network=false;
        try{
            if(mgr!=null){
                gps=mgr.isProviderEnabled(LocationManager.GPS_PROVIDER);
                network=mgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            }
        }catch(Exception ignored){}
        String msg="Izin aplikasi: AKTIF\nGPS: "+(gps?"AKTIF":"TIDAK AKTIF")
            +"\nLokasi jaringan: "+(network?"AKTIF":"TIDAK AKTIF")
            +"\n\nGPS di dalam iframe Google Apps Script masih membutuhkan delegasi izin dari website.";
        new AlertDialog.Builder(this).setTitle("GPS PBS Balap")
            .setMessage(msg).setPositiveButton("OK",null)
            .setNeutralButton("Pengaturan lokasi",(dialog,which)->{
                try{startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));}
                catch(Exception ignored){}
            }).show();
    }

    private void external(Uri uri){
        try{
            Intent intent;
            if("intent".equalsIgnoreCase(uri.getScheme())){
                intent=Intent.parseUri(uri.toString(),Intent.URI_INTENT_SCHEME);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setComponent(null);
            }else{
                intent=new Intent(Intent.ACTION_VIEW,uri);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
            }
            startActivity(intent);
        }catch(Exception e){
            Toast.makeText(this,"Tautan tidak dapat dibuka",Toast.LENGTH_SHORT).show();
        }
    }

    private boolean acceptsImages(WebChromeClient.FileChooserParams params){
        String[] types=params.getAcceptTypes();
        if(types==null||types.length==0)return true;
        for(String t:types){
            if(t!=null && (t.isEmpty()||t.equals("*/*")||t.startsWith("image/")
                ||t.equals(".jpeg")||t.equals(".jpg")||t.equals(".png")))return true;
        }
        return false;
    }

    private void download(String url,String disposition,String mime,WebView source){
        try{
            String filename=URLUtil.guessFileName(url,disposition,mime);
            DownloadManager.Request req=new DownloadManager.Request(Uri.parse(url));
            req.setTitle(filename);
            if(mime!=null&&!mime.isEmpty())req.setMimeType(mime);
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,filename);
            String cookie=CookieManager.getInstance().getCookie(url);
            if(cookie!=null)req.addRequestHeader("Cookie",cookie);
            req.addRequestHeader("User-Agent",source.getSettings().getUserAgentString());
            ((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(req);
            Toast.makeText(this,"Unduhan dimulai",Toast.LENGTH_SHORT).show();
        }catch(Exception e){
            Toast.makeText(this,"Unduhan gagal",Toast.LENGTH_LONG).show();
        }
    }

    private void deleteUnusedCamera(){
        if(cameraUri!=null){
            try{getContentResolver().delete(cameraUri,null,null);}
            catch(Exception ignored){}
            cameraUri=null;
        }
    }

    @Override protected void onActivityResult(int request,int resultCode,Intent data){
        super.onActivityResult(request,resultCode,data);
        if(request!=REQUEST_FILE)return;
        Uri[] uris=null;
        boolean cameraUsed=false;
        if(resultCode==RESULT_OK){
            if(data!=null)uris=WebChromeClient.FileChooserParams.parseResult(resultCode,data);
            else if(cameraUri!=null){uris=new Uri[]{cameraUri};cameraUsed=true;}
        }
        if(fileCallback!=null)fileCallback.onReceiveValue(uris);
        fileCallback=null;
        if(!cameraUsed)deleteUnusedCamera();
        cameraUri=null;
    }

    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){
        super.onRequestPermissionsResult(request,permissions,grants);
        if(request==REQUEST_GPS){
            boolean ok=false;
            for(int g:grants)if(g==PackageManager.PERMISSION_GRANTED)ok=true;
            boolean fromWeb=locationCallback!=null;
            if(locationCallback!=null)locationCallback.invoke(locationOrigin,ok,false);
            locationCallback=null;locationOrigin=null;
            if(!fromWeb && ok)showGpsDialog();
            else if(!fromWeb)Toast.makeText(this,"Izin lokasi ditolak",Toast.LENGTH_SHORT).show();
        }else if(request==REQUEST_STORAGE){
            if(grants.length>0 && grants[0]==PackageManager.PERMISSION_GRANTED && queuedDownloadUrl!=null)
                download(queuedDownloadUrl,queuedDisposition,queuedMime,
                    pages[selected]!=null?pages[selected]:pages[BALAP]);
            else Toast.makeText(this,"Izin penyimpanan ditolak",Toast.LENGTH_SHORT).show();
            queuedDownloadUrl=null;queuedDisposition=null;queuedMime=null;
        }
    }

    private void applyResponsiveHeader(){
        if(header==null||logo==null)return;
        Configuration cfg=getResources().getConfiguration();
        boolean small=cfg.screenWidthDp<360;
        boolean landscape=cfg.orientation==Configuration.ORIENTATION_LANDSCAPE;
        ViewGroup.LayoutParams hp=header.getLayoutParams();
        if(hp!=null){
            hp.height=dp(landscape?48:small?52:58);
            header.setLayoutParams(hp);
        }
        header.setPadding(dp(small?8:14),dp(3),dp(8),dp(3));
        logo.getLayoutParams().width=dp(small?34:40);
        logo.getLayoutParams().height=dp(small?34:40);
        logo.requestLayout();
        title.setTextSize(small?15:18);
        subtitle.setVisibility(small||landscape?View.GONE:View.VISIBLE);
    }

    @Override public void onConfigurationChanged(Configuration cfg){
        super.onConfigurationChanged(cfg);
        applyResponsiveHeader();
    }

    @Override protected void onSaveInstanceState(Bundle state){
        state.putInt("selected",selected);
        state.putBoolean("balapHttp",balapUsingHttp);
        if(cameraUri!=null)state.putString("cameraUri",cameraUri.toString());
        for(int id=BALAP;id<=SURVEY;id++){
            if(pages[id]!=null){
                Bundle data=new Bundle();
                pages[id].saveState(data);
                state.putBundle("webState"+id,data);
            }
        }
        super.onSaveInstanceState(state);
    }

    @Override public void onBackPressed(){
        if(errorOverlay!=null){hideError();showModule(HOME,false);}
        else if(selected!=HOME){
            WebView page=pages[selected];
            if(page!=null&&page.canGoBack())page.goBack();
            else showModule(HOME,false);
        }else super.onBackPressed();
    }

    @Override protected void onDestroy(){
        if(fileCallback!=null)fileCallback.onReceiveValue(null);
        if(locationCallback!=null)locationCallback.invoke(locationOrigin,false,false);
        for(int id=BALAP;id<=SURVEY;id++){
            if(pages[id]!=null){
                pages[id].stopLoading();
                pages[id].destroy();
            }
        }
        super.onDestroy();
    }
}
