package top.niunaijun.blackbox.fake.service;

import android.content.Context;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import black.android.os.BRServiceManager;
import black.com.android.internal.telephony.BRITelephonyStub;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.entity.location.BCell;
import top.niunaijun.blackbox.fake.frameworks.BLocationManager;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Md5Utils;


public class ITelephonyManagerProxy extends BinderInvocationStub {
    public static final String TAG = "ITelephonyManagerProxy";
    
    private static String sStableImei = null;
    private static String sStableImsi = null;

    public ITelephonyManagerProxy() {
        super(BRServiceManager.get().getService(Context.TELEPHONY_SERVICE));
    }
    
    private static String getStableImei() {
        if (sStableImei == null) {
            try {
                String packageName = BActivityThread.getAppPackageName();
                int userId = BActivityThread.getUserId();
                String source = BlackBoxCore.getHostPkg() + "_imei_" + userId + "_" + packageName;
                String md5 = Md5Utils.md5(source);
                // 生成有效的15位IMEI
                sStableImei = generateValidImei(md5);
            } catch (Exception e) {
                Log.w(TAG, "Failed to generate IMEI, using fallback", e);
                sStableImei = "352315053488619";
            }
            Log.d(TAG, "Generated stable IMEI: " + sStableImei);
        }
        return sStableImei;
    }
    
    private static String getStableImsi() {
        if (sStableImsi == null) {
            try {
                String packageName = BActivityThread.getAppPackageName();
                int userId = BActivityThread.getUserId();
                String source = BlackBoxCore.getHostPkg() + "_imsi_" + userId + "_" + packageName;
                String md5 = Md5Utils.md5(source);
                // 生成有效的15位IMSI
                sStableImsi = generateValidImsi(md5);
            } catch (Exception e) {
                Log.w(TAG, "Failed to generate IMSI, using fallback", e);
                sStableImsi = "460001234567890";
            }
            Log.d(TAG, "Generated stable IMSI: " + sStableImsi);
        }
        return sStableImsi;
    }
    
    private static String generateValidImei(String md5) {
        StringBuilder imei = new StringBuilder();
        // 取前14位作为IMEI的前14位
        for (int i = 0; i < 14 && i < md5.length(); i++) {
            char c = md5.charAt(i);
            if (Character.isDigit(c)) {
                imei.append(c);
            } else {
                // 将字母转为数字
                imei.append((c % 10));
            }
        }
        // 补全到14位
        while (imei.length() < 14) {
            imei.append('0');
        }
        // 计算Luhn算法校验位
        imei.append(computeLuhnCheckDigit(imei.toString()));
        return imei.toString();
    }
    
    private static String generateValidImsi(String md5) {
        StringBuilder imsi = new StringBuilder();
        // IMSI: MCC(3) + MNC(2-3) + MSIN(10-11)
        imsi.append("460"); // 中国MCC
        imsi.append("00"); // 默认MNC
        // 生成MSIN部分
        for (int i = 0; i < 10 && i < md5.length(); i++) {
            char c = md5.charAt(i);
            if (Character.isDigit(c)) {
                imsi.append(c);
            } else {
                imsi.append((c % 10));
            }
        }
        while (imsi.length() < 15) {
            imsi.append('0');
        }
        return imsi.toString();
    }
    
    private static char computeLuhnCheckDigit(String number) {
        int sum = 0;
        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = number.charAt(i) - '0';
            if ((number.length() - i) % 2 == 0) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
        }
        int checkDigit = (10 - (sum % 10)) % 10;
        return (char) ('0' + checkDigit);
    }

    @Override
    protected Object getWho() {
        IBinder telephony = BRServiceManager.get().getService(Context.TELEPHONY_SERVICE);
        return BRITelephonyStub.get().asInterface(telephony);
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("getDeviceId")
    public static class GetDeviceId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Log.d(TAG, "getDeviceId called");
            return getStableImei();
        }
    }

    @ProxyMethod("getImeiForSlot")
    public static class getImeiForSlot extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Log.d(TAG, "getImeiForSlot called");
            return getStableImei();
        }
    }

    @ProxyMethod("getMeidForSlot")
    public static class GetMeidForSlot extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Log.d(TAG, "getMeidForSlot called");
            return getStableImei();
        }
    }

    @ProxyMethod("isUserDataEnabled")
    public static class IsUserDataEnabled extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return true;
        }
    }


    @ProxyMethod("getLine1NumberForDisplay")
    public static class getLine1NumberForDisplay extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return null;
        }
    }

    @ProxyMethod("getSubscriberId")
    public static class GetSubscriberId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Log.d(TAG, "getSubscriberId called");
            return getStableImsi();
        }
    }

    @ProxyMethod("getDeviceIdWithFeature")
    public static class GetDeviceIdWithFeature extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Log.d(TAG, "getDeviceIdWithFeature called");
            return getStableImei();
        }
    }

    @ProxyMethod("getCellLocation")
    public static class GetCellLocation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Log.d(TAG, "getCellLocation");
            if (BLocationManager.isFakeLocationEnable()) {
                BCell cell = BLocationManager.get().getCell(BActivityThread.getUserId(), BActivityThread.getAppPackageName());
                if (cell != null) {
                    
                    return null;
                }
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getAllCellInfo")
    public static class GetAllCellInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (BLocationManager.isFakeLocationEnable()) {
                List<BCell> cell = BLocationManager.get().getAllCell(BActivityThread.getUserId(), BActivityThread.getAppPackageName());
                
                return cell;
            }
            try {
                return method.invoke(who, args);
            } catch (Throwable e) {
                return null;
            }
        }
    }

    @ProxyMethod("getNetworkOperator")
    public static class GetNetworkOperator extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Log.d(TAG, "getNetworkOperator");
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getNetworkTypeForSubscriber")
    public static class GetNetworkTypeForSubscriber extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Throwable e) {
                return 0;
            }
        }
    }

    @ProxyMethod("getNeighboringCellInfo")
    public static class GetNeighboringCellInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Log.d(TAG, "getNeighboringCellInfo");
            if (BLocationManager.isFakeLocationEnable()) {
                List<BCell> cell = BLocationManager.get().getNeighboringCell(BActivityThread.getUserId(), BActivityThread.getAppPackageName());
                
                return null;
            }
            return method.invoke(who, args);
        }
    }
}
