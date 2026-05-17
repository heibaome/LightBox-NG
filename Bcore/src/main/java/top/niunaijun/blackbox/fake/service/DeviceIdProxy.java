package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;


public class DeviceIdProxy extends ClassInvocationStub {
    public static final String TAG = "DeviceIdProxy";
    
    private static String sMockDeviceId = null;
    private static String sMockImei = null;

    public DeviceIdProxy() {
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

    
    private static String getStableDeviceId() {
        if (sMockDeviceId == null) {
            try {
                String packageName = top.niunaijun.blackbox.app.BActivityThread.getAppPackageName();
                int userId = top.niunaijun.blackbox.app.BActivityThread.getUserId();
                String source = top.niunaijun.blackbox.BlackBoxCore.getHostPkg() + "_device_" + userId + "_" + packageName;
                String md5 = top.niunaijun.blackbox.utils.Md5Utils.md5(source);
                // 生成有效的15位IMEI格式
                sMockDeviceId = generateValidImei(md5);
            } catch (Exception e) {
                Slog.w(TAG, "DeviceId: Failed to generate stable ID, using fallback", e);
                sMockDeviceId = "352315053488619";
            }
            Slog.d(TAG, "DeviceId: Generated stable device ID: " + sMockDeviceId);
        }
        return sMockDeviceId;
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
    
    @ProxyMethod("getDeviceId")
    public static class GetDeviceId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Object result = method.invoke(who, args);
                if (result != null && !"0".equals(result.toString()) && !"".equals(result.toString())) {
                    return result;
                }
            } catch (Exception e) {
                Slog.w(TAG, "GetDeviceId: Original method failed", e);
            }
            Slog.d(TAG, "GetDeviceId: Returning stable device ID");
            return getStableDeviceId();
        }
    }

    
    @ProxyMethod("setDeviceId")
    public static class SetDeviceId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                if (who == null) {
                    Slog.w(TAG, "SetDeviceId called on null object, ignoring");
                    return null;
                }
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "SetDeviceId error, ignoring: " + e.getMessage());
                return null;
            }
        }
    }

    
    @ProxyMethod("isValidDeviceId")
    public static class IsValidDeviceId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                if (who == null) {
                    Slog.w(TAG, "IsValidDeviceId called on null object, returning true");
                    return true;
                }
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "IsValidDeviceId error, returning true: " + e.getMessage());
                return true;
            }
        }
    }

    
    @ProxyMethod("generateDeviceId")
    public static class GenerateDeviceId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "GenerateDeviceId: Returning stable device ID");
            return getStableDeviceId();
        }
    }

    
    @ProxyMethod("storeDeviceId")
    public static class StoreDeviceId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                if (who == null) {
                    Slog.w(TAG, "StoreDeviceId called on null object, ignoring");
                    return null;
                }
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "StoreDeviceId error, ignoring: " + e.getMessage());
                return null;
            }
        }
    }

    
    @ProxyMethod("retrieveDeviceId")
    public static class RetrieveDeviceId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "RetrieveDeviceId: Returning stable device ID");
            return getStableDeviceId();
        }
    }
}
