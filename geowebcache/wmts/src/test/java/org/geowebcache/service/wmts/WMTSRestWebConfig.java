/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @author Sandro Salari, GeoSolutions S.A.S., Copyright 2017
 */
package org.geowebcache.service.wmts;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geowebcache.GeoWebCacheExtensions;
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.SRS;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.mime.MimeType;
import org.geowebcache.stats.RuntimeStats;
import org.geowebcache.storage.DefaultStorageFinder;
import org.geowebcache.storage.StorageBroker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

@Configuration
@ComponentScan({ "org.geowebcache.service.wmts" })
@EnableWebMvc
@Profile("test")
public class WMTSRestWebConfig extends WebMvcConfigurationSupport {

    GridSetBroker broker = new GridSetBroker(true, true);

    @Bean
    public DefaultStorageFinder defaultStorageFinder() {
        return mock(DefaultStorageFinder.class);
    }

    @Bean
    public RuntimeStats runtimeStats() {
        return mock(RuntimeStats.class);
    }

    @Bean
    public TileLayerDispatcher tileLayerDispatcher() throws Exception {
        TileLayerDispatcher tld = mock(TileLayerDispatcher.class);
        List<String> gridSetNames = Arrays.asList("GlobalCRS84Pixel", "GlobalCRS84Scale",
                "EPSG:4326");
        String layerName = "mockLayer";
        TileLayer tileLayer = mock(TileLayer.class);
        when(tld.getTileLayer(eq(layerName))).thenReturn(tileLayer);
        when(tileLayer.getName()).thenReturn(layerName);
        when(tileLayer.isEnabled()).thenReturn(true);
        when(tileLayer.isAdvertised()).thenReturn(true);

        final MimeType mimeType1 = MimeType.createFromFormat("image/png");
        final MimeType mimeType2 = MimeType.createFromFormat("image/jpeg");
        when(tileLayer.getMimeTypes()).thenReturn(Arrays.asList(mimeType1, mimeType2));

        final MimeType infoMimeType1 = MimeType.createFromFormat("text/plain");
        final MimeType infoMimeType2 = MimeType.createFromFormat("text/html");
        final MimeType infoMimeType3 = MimeType.createFromFormat("application/vnd.ogc.gml");
        when(tileLayer.getInfoMimeTypes())
                .thenReturn(Arrays.asList(infoMimeType1, infoMimeType2, infoMimeType3));
        Map<String, GridSubset> subsets = new HashMap<String, GridSubset>();
        Map<SRS, List<GridSubset>> bySrs = new HashMap<SRS, List<GridSubset>>();

        for (String gsetName : gridSetNames) {
            GridSet gridSet = broker.get(gsetName);
            XMLGridSubset xmlGridSubset = new XMLGridSubset();
            String gridSetName = gridSet.getName();
            xmlGridSubset.setGridSetName(gridSetName);
            GridSubset gridSubSet = xmlGridSubset.getGridSubSet(broker);
            subsets.put(gsetName, gridSubSet);

            List<GridSubset> list = bySrs.get(gridSet.getSrs());
            if (list == null) {
                list = new ArrayList<GridSubset>();
                bySrs.put(gridSet.getSrs(), list);
            }
            list.add(gridSubSet);

            when(tileLayer.getGridSubset(eq(gsetName))).thenReturn(gridSubSet);

        }

        for (SRS srs : bySrs.keySet()) {
            List<GridSubset> list = bySrs.get(srs);
            when(tileLayer.getGridSubsetsForSRS(eq(srs))).thenReturn(list);

        }
        when(tileLayer.getGridSubsets()).thenReturn(subsets.keySet());

        when(tileLayer.getParameterFilters()).thenReturn(Collections.<ParameterFilter> emptyList());

        when(tld.getLayerList()).thenReturn(Arrays.asList(tileLayer));

        return tld;
    }

    @Bean
    public GeoWebCacheExtensions geoWebCacheExtensions() {
        return new GeoWebCacheExtensions();
    }

    @Bean
    public WMTSService wmtsService() throws Exception {
        return new WMTSService(mock(StorageBroker.class), tileLayerDispatcher(), broker,
                mock(RuntimeStats.class));
        // return mock(WMTSService.class);
    }

    /*
     * @Bean public TileLayerDispatcher tileLayerDispatcher() throws GeoWebCacheException {
     * TileLayerDispatcher tileLayerDispatcher = mock(TileLayerDispatcher.class); TileLayer
     * tileLayer = mock(TileLayer.class);
     * when(tileLayer.getBlobStoreId()).thenReturn("mbtiles-store");
     * when(tileLayerDispatcher.getTileLayer("europe")).thenReturn(tileLayer); return
     * tileLayerDispatcher; }
     */

}
