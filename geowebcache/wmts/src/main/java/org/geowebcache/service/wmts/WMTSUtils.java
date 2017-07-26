package org.geowebcache.service.wmts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.mime.MimeType;

public final class WMTSUtils {

    private WMTSUtils() {
    }

    protected static List<String> getLayerFormats(TileLayer layer) throws IOException {
        return layer.getMimeTypes().stream().map(MimeType::getFormat).collect(Collectors.toList());
    }

    public static List<String> getInfoFormats(TileLayer layer) {
        return layer.getInfoMimeTypes().stream().map(MimeType::getFormat)
                .collect(Collectors.toList());
    }

    public static List<ParameterFilter> getLayerDimensions(List<ParameterFilter> filters) {
        List<ParameterFilter> dimensions = new ArrayList<ParameterFilter>(0);
        if (filters != null) {
            dimensions = filters.stream()
                    .filter(filter -> !"STYLES".equalsIgnoreCase(filter.getKey())
                            && filter.getLegalValues() != null)
                    .collect(Collectors.toList());
        }
        return dimensions;
    }

}
