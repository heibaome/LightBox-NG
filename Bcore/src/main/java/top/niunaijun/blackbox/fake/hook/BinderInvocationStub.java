package top.niunaijun.blackbox.fake.hook;

import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import top.niunaijun.blackbox.utils.Slog;

import java.io.FileDescriptor;
import java.util.Map;

import black.android.os.BRServiceManager;


public abstract class BinderInvocationStub extends ClassInvocationStub implements IBinder {
    private IBinder mBaseBinder;

    public BinderInvocationStub(IBinder baseBinder) {
        mBaseBinder = baseBinder;
    }

    @Override
    protected void onBindMethod() {
    }

    @Nullable
    @Override
    public String getInterfaceDescriptor() throws RemoteException {
        return mBaseBinder.getInterfaceDescriptor();
    }

    @Override
    public boolean pingBinder() {
        return mBaseBinder.pingBinder();
    }

    @Override
    public boolean isBinderAlive() {
        return mBaseBinder.isBinderAlive();
    }

    @Nullable
    @Override
    public IInterface queryLocalInterface(@NonNull String descriptor) {
        return (IInterface) getProxyInvocation();
    }

    @Override
    public void dump(@NonNull FileDescriptor fd, @Nullable String[] args) throws RemoteException {
        mBaseBinder.dump(fd, args);
    }

    @Override
    public void dumpAsync(@NonNull FileDescriptor fd, @Nullable String[] args) throws RemoteException {
        mBaseBinder.dumpAsync(fd, args);
    }

    @Override
    public boolean transact(int code, @NonNull Parcel data, @Nullable Parcel reply, int flags) throws RemoteException {
        return mBaseBinder.transact(code, data, reply, flags);
    }

    @Override
    public void linkToDeath(@NonNull DeathRecipient recipient, int flags) throws RemoteException {
        mBaseBinder.linkToDeath(recipient, flags);
    }

    @Override
    public boolean unlinkToDeath(@NonNull DeathRecipient recipient, int flags) {
        return mBaseBinder.unlinkToDeath(recipient, flags);
    }


    protected void replaceSystemService(String name) {
        try {
            Map<String, IBinder> services = BRServiceManager.get().sCache();
            if (services != null) {
                services.put(name, this);
                Slog.d("BinderInvocationStub", "replaceSystemService: " + name + " OK (sCache)");
                return;
            }
        } catch (Throwable t) {
            Slog.w("BinderInvocationStub", "replaceSystemService sCache failed: " + t.getMessage());
        }
        
        // Fallback for Android 14+ where sCache may be inaccessible.
        // Try addService as a direct registration path.
        try {
            BRServiceManager.get().addService(name, this);
            Slog.d("BinderInvocationStub", "replaceSystemService: " + name + " OK (addService fallback)");
        } catch (Throwable t) {
            Slog.e("BinderInvocationStub", "replaceSystemService: " + name + " FAILED — " + t.getMessage() +
                   " — network hooks will NOT intercept system service calls!");
        }
    }
}
