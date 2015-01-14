package org.thecosmicfrog.luasataglance;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class Serializer {

    private static final String LOG_TAG = Serializer.class.getSimpleName();

    public static byte[] serialize(Object obj) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);

            oos.writeObject(obj);
            oos.flush();
            oos.close();
            baos.close();

            return baos.toByteArray();
        } catch (IOException ioe) {
            Log.e(LOG_TAG, ioe.getMessage());
        }

        return null;
    }

    public static Object deserialize(byte[] bytes) {
        try {
            ByteArrayInputStream b = new ByteArrayInputStream(bytes);
            ObjectInputStream o = new ObjectInputStream(b);

            o.close();
            b.close();

            return o.readObject();
        } catch (IOException ioe) {
            Log.e(LOG_TAG, "Exception:", ioe);
        } catch (ClassNotFoundException cnfe) {
            Log.e(LOG_TAG, "Exception:", cnfe);
        }

        return null;
    }
}