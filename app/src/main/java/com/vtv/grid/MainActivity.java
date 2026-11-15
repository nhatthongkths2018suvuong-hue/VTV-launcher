package com.vtv.grid;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private WebView web;
    private boolean resumed = false;
    private boolean homePending = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Khung chứa nền đen; WebView luôn giữ tỉ lệ 16:9 và nằm giữa (điện thoại màn hình dài sẽ có viền đen, không bị cắt)
        final FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);
        web = new WebView(this);
        web.setBackgroundColor(0xFF000000);
        root.addView(web, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
        setContentView(root);
        root.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int l, int t, int r, int b,
                                       int ol, int ot, int or, int ob) {
                fitWeb(r - l, b - t);
            }
        });

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSupportZoom(false);

        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        web.addJavascriptInterface(new Bridge(), "AndroidBridge");
        web.loadUrl("file:///android_asset/index.html");
        web.requestFocus();
    }

    private void fitWeb(int w, int h) {
        if (w <= 0 || h <= 0) return;
        int tw = w;
        int th = w * 9 / 16;
        if (th > h) {
            th = h;
            tw = h * 16 / 9;
        }
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) web.getLayoutParams();
        if (lp.width != tw || lp.height != th) {
            lp.width = tw;
            lp.height = th;
            lp.gravity = Gravity.CENTER;
            web.setLayoutParams(lp);
        }
    }

    /** File channels.txt nằm ở: Android/data/com.vtv.grid/files/channels.txt trong bộ nhớ trong của box. */
    private File channelFile() {
        File dir = getExternalFilesDir(null);
        if (dir == null) dir = getFilesDir();
        return new File(dir, "channels.txt");
    }

    /** Lần chạy đầu chưa có file: chép file mẫu ra để người dùng thấy đúng thư mục cần đặt file. */
    private void ensureSampleFile() {
        File f = channelFile();
        if (f.exists()) return;
        try {
            InputStream in = getAssets().open("channels.txt");
            OutputStream out = new FileOutputStream(f);
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            out.close();
            in.close();
        } catch (Exception ignored) {
        }
    }

    private class Bridge {
        @JavascriptInterface
        public String readChannels() {
            try {
                ensureSampleFile();
                InputStream in = new java.io.FileInputStream(channelFile());
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
                in.close();
                return bo.toString("UTF-8");
            } catch (Exception e) {
                return "";
            }
        }

        @JavascriptInterface
        public String getPath() {
            return channelFile().getAbsolutePath();
        }

        @JavascriptInterface
        public void exit() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            });
        }

        /** true nếu app này đang là launcher mặc định (khi đó phím Back ở màn hình chọn kênh không thoát app). */
        @JavascriptInterface
        public boolean isDefaultHome() {
            try {
                Intent h = new Intent(Intent.ACTION_MAIN);
                h.addCategory(Intent.CATEGORY_HOME);
                ResolveInfo ri = getPackageManager().resolveActivity(h, PackageManager.MATCH_DEFAULT_ONLY);
                return ri != null && ri.activityInfo != null && getPackageName().equals(ri.activityInfo.packageName);
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public void openSettings() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    } catch (Exception e) {
                        android.widget.Toast.makeText(MainActivity.this, "Không mở được Cài đặt", android.widget.Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        @JavascriptInterface
        public void openApps() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startActivity(new Intent(MainActivity.this, AppsActivity.class));
                }
            });
        }

        /** Mở danh sách ứng dụng ở chế độ chọn "ứng dụng nhanh". */
        @JavascriptInterface
        public void pickQuick() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startActivity(new Intent(MainActivity.this, AppsActivity.class).putExtra(AppsActivity.EXTRA_PICK, true));
                }
            });
        }

        /** Trả về JSON {"label":"..."} của ứng dụng nhanh, hoặc chuỗi rỗng nếu chưa chọn. */
        @JavascriptInterface
        public String getQuickApp() {
            try {
                String[] q = AppsActivity.getQuick(MainActivity.this);
                if (q == null) return "";
                return new org.json.JSONObject().put("label", AppsActivity.quickLabel(MainActivity.this, q)).toString();
            } catch (Exception e) {
                return "";
            }
        }

        @JavascriptInterface
        public String getQuickIcon() {
            String[] q = AppsActivity.getQuick(MainActivity.this);
            return q == null ? "" : AppsActivity.quickIconData(MainActivity.this, q);
        }

        @JavascriptInterface
        public void launchQuick() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String[] q = AppsActivity.getQuick(MainActivity.this);
                    if (q != null) AppsActivity.launch(MainActivity.this, q[0], q[1]);
                }
            });
        }
    }

    private void js(String code) {
        web.evaluateJavascript(code, null);
    }

    // Phím Back và CH+/CH- được xử lý ở đây rồi chuyển vào trang web; các phím khác (mũi tên, OK, số) WebView tự nhận.
    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.KEYCODE_BACK) {
            if (e.getAction() == KeyEvent.ACTION_UP) js("window.nativeBack&&window.nativeBack()");
            return true;
        }
        if (k == KeyEvent.KEYCODE_CHANNEL_UP || k == KeyEvent.KEYCODE_CHANNEL_DOWN) {
            if (e.getAction() == KeyEvent.ACTION_DOWN) {
                js("window.nativeZap&&window.nativeZap(" + (k == KeyEvent.KEYCODE_CHANNEL_UP ? 1 : -1) + ")");
            }
            return true;
        }
        return super.dispatchKeyEvent(e);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        resumed = false;
        web.onPause();
    }

    /** Bấm phím Home (khi app là launcher): phát lại kênh VTV1. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && Intent.ACTION_MAIN.equals(intent.getAction())
                && intent.hasCategory(Intent.CATEGORY_HOME)) {
            if (resumed) {
                js("window.nativeHome&&window.nativeHome()");
            } else {
                homePending = true; // đang ở nền: chờ onResume rồi mới chạy
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        web.onResume();
        js("window.nativeRefresh&&window.nativeRefresh()");
        if (homePending) {
            homePending = false;
            js("window.nativeHome&&window.nativeHome()");
        }
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.removeJavascriptInterface("AndroidBridge");
            web.destroy();
        }
        super.onDestroy();
    }
}
