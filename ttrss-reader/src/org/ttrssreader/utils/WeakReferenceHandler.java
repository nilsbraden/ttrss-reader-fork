package org.ttrssreader.utils;

import java.lang.ref.WeakReference;
import android.os.Handler;
import android.os.Message;

/**
 * A handler which keeps a weak reference to a fragment. According to
 * Android's lint, references to Handlers can be kept around for a long
 * time - longer than Fragments for example. So we should use handlers
 * that don't have strong references to the things they are handling for.
 * 
 * You can use this class to more or less forget about that requirement.
 * Unfortunately you can have anonymous static inner classes, so it is a
 * little more verbose.
 * 
 * Example use:
 * 
 * private static class MsgHandler extends WeakReferenceHandler<MyFragment>
 * {
 * public MsgHandler(MyFragment fragment) { super(fragment); }
 * 
 * @Override
 *           public void handleMessage(MyFragment fragment, Message msg)
 *           {
 *           fragment.doStuff(msg.arg1);
 *           }
 *           }
 * 
 *           // ...
 *           MsgHandler handler = new MsgHandler(this);
 */
public abstract class WeakReferenceHandler<T> extends Handler {
    private WeakReference<T> mReference;
    
    public WeakReferenceHandler(T reference) {
        mReference = new WeakReference<T>(reference);
    }
    
    @Override
    public void handleMessage(Message msg) {
        if (mReference.get() == null)
            return;
        handleMessage(mReference.get(), msg);
    }
    
    protected abstract void handleMessage(T reference, Message msg);
}
