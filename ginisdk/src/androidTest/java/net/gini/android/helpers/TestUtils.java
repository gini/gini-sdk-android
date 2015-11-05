package net.gini.android.helpers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class TestUtils {

    public static byte[] createByteArray(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[4096];
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int read;
        while((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
    }

    private TestUtils() {
    }
}
