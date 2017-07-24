package org.geowebcache.service.wmts;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.geowebcache.layer.TileLayer;
import org.geowebcache.mime.MimeType;

public final class WMTSUtils {

    private WMTSUtils() {
    }

    protected static List<String> getLayerFormats(TileLayer layer) throws IOException {
        return layer.getMimeTypes().stream().map(MimeType::getFormat).collect(Collectors.toList());
    }
}
