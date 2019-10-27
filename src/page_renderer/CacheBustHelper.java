package page_renderer;

import java.io.IOException;
import java.io.InputStream;

public class CacheBustHelper {
    public static void readAllBytes(InputStream is) throws IOException {
        while (is.read() != -1) {}
    }
}
