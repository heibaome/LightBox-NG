package top.niunaijun.blackbox.fake.service;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.webkit.WebView;
import android.webkit.WebViewDatabase;
import android.webkit.WebSettings;

import java.io.File;
import java.lang.reflect.Method;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.app.BActivityThread;


public class WebViewProxy extends ClassInvocationStub {
    public static final String TAG = "WebViewProxy";

    public WebViewProxy() {
        super();
    }

    
    @Override
    protected Object getWho() {
        return null; 
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    
    @ProxyMethod("<init>")
    public static class Constructor extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "WebView: Constructor called");
            Context context = null;
            try {
                if (args != null && args.length > 0 && args[0] instanceof Context) {
                    context = (Context) args[0];
                } else {
                    context = BlackBoxCore.getContext();
                }

                // Ensure WebView data directory exists using the default app_webview/ path
                // This matches the setup in BActivityThread.handleBindApplication()
                if (context != null) {
                    String webViewDir = context.getApplicationInfo().dataDir + "/app_webview";
                    File dataDir = new File(webViewDir);
                    if (!dataDir.exists()) {
                        dataDir.mkdirs();
                        new File(dataDir, "cache").mkdirs();
                        new File(dataDir, "cookies").mkdirs();
                        Slog.d(TAG, "WebView: Created app_webview directory: " + webViewDir);
                    }
                }

                // Call original constructor
                Object result = method.invoke(who, args);

                if (result instanceof WebView) {
                    WebView webView = (WebView) result;
                    configureWebView(webView);
                }

                return result;
            } catch (Exception e) {
                Slog.w(TAG, "WebView: Constructor failed, attempting fallback", e);
                return createFallbackWebView(context);
            }
        }
        
        private void configureWebView(WebView webView) {
            try {
                WebSettings settings = webView.getSettings();
                if (settings != null) {
                    
                    settings.setJavaScriptEnabled(true);
                    
                    settings.setDomStorageEnabled(true);
                    
                    settings.setDatabaseEnabled(true);
                    
                    settings.setCacheMode(WebSettings.LOAD_DEFAULT);

                    
                    try {
                        
                        Method setAppCacheEnabled = settings.getClass().getMethod("setAppCacheEnabled", boolean.class);
                        setAppCacheEnabled.invoke(settings, true);

                        if (webView.getContext() != null) {
                            Method setAppCachePath = settings.getClass().getMethod("setAppCachePath", String.class);
                            setAppCachePath.invoke(settings, webView.getContext().getCacheDir().getAbsolutePath());
                        }
                    } catch (Throwable e) {
                        
                        Slog.w(TAG, "WebView: AppCache not supported: " + e.getMessage());
                    }

                    
                    settings.setBlockNetworkLoads(false);
                    settings.setBlockNetworkImage(false);

                    
                    settings.setAllowFileAccess(true);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        settings.setAllowFileAccessFromFileURLs(true);
                        settings.setAllowUniversalAccessFromFileURLs(true);
                    }

                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                    }

                    
                    String userAgent = settings.getUserAgentString();
                    if (userAgent != null && !userAgent.contains("BlackBox")) {
                        settings.setUserAgentString(userAgent + " BlackBox");
                    }

                    
                    try {
                        webView.setNetworkAvailable(true);
                    } catch (Exception e) {
                        
                    }

                    
                    settings.setAllowContentAccess(true);

                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                         settings.setSafeBrowsingEnabled(false);
                    }

                    Slog.d(TAG, "WebView: Configured successfully with network access enabled");
                }
            } catch (Exception e) {
                Slog.w(TAG, "WebView: Failed to configure settings", e);
            }
        }
        
        private WebView createFallbackWebView(Context context) {
            try {
                if (context != null) {
                    
                    WebView webView = new WebView(context);
                    WebSettings settings = webView.getSettings();
                    if (settings != null) {
                        settings.setJavaScriptEnabled(true);
                        settings.setDomStorageEnabled(true);
                    }
                    Slog.d(TAG, "WebView: Created fallback WebView");
                    return webView;
                }
            } catch (Exception e) {
                Slog.e(TAG, "WebView: Failed to create fallback WebView", e);
            }
            return null;
        }
    }

    
    @ProxyMethod("setDataDirectorySuffix")
    public static class SetDataDirectorySuffix extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0) {
                Slog.d(TAG, "WebView: BLOCKED setDataDirectorySuffix(\"" + args[0]
                    + "\") — suffix is process-wide and would conflict across virtual apps");
            }
            // Silently swallow: we use the default app_webview/ directory.
            // setDataDirectorySuffix() is process-wide, can only be called once,
            // and would break isolation between virtual apps and the host.
            return null;
        }
    }

    
    @ProxyMethod("getDataDirectory")
    public static class GetDataDirectory extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Let the original method return whatever path Chromium decides.
                // In virtual app processes, ApplicationInfo.dataDir is redirected
                // by BEnvironment/PackageManagerCompat, so this returns the correct
                // per-virtual-app path automatically.
                Object result = method.invoke(who, args);
                // Ensure that path exists so Chromium doesn't get ERR_CACHE_MISS.
                if (result != null) {
                    File dir = new File(result.toString());
                    if (!dir.exists()) {
                        dir.mkdirs();
                        Slog.d(TAG, "WebView: Created default data dir: " + result);
                    }
                }
                return result;
            } catch (Exception e) {
                Slog.w(TAG, "WebView: getDataDirectory failed", e);
                return method.invoke(who, args);
            }
        }
    }

    
    @ProxyMethod("getInstance")
    public static class GetWebViewDatabaseInstance extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "WebView: getInstance called for WebViewDatabase");
            try {
                // Ensure WebView data directory exists using the default app_webview/ path
                Context context = BlackBoxCore.getContext();
                if (context != null) {
                    String webViewDir = context.getApplicationInfo().dataDir + "/app_webview";
                    File dataDir = new File(webViewDir);
                    if (!dataDir.exists()) {
                        dataDir.mkdirs();
                        new File(dataDir, "cache").mkdirs();
                        new File(dataDir, "cookies").mkdirs();
                    }
                    System.setProperty("webview.database.path", webViewDir);
                    Slog.d(TAG, "WebView: Set database path: " + webViewDir);
                }
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "WebView: Failed to get WebViewDatabase instance", e);
                return null;
            }
        }
    }

    
    @ProxyMethod("loadUrl")
    public static class LoadUrl extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0) {
                String url = (String) args[0];
                Slog.d(TAG, "WebView: loadUrl called with: " + url);
                
                
                if (url != null && url.startsWith("file://")) {
                    
                    Slog.d(TAG, "WebView: Handling file URL: " + url);
                }
            }
            
            return method.invoke(who, args);
        }
    }
}
