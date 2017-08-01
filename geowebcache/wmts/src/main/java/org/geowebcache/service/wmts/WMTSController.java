/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * @author Sandro Salari, GeoSolutions S.A.S., Copyright 2017
 * 
 */

package org.geowebcache.service.wmts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geowebcache.GeoWebCacheExtensions;
import org.geowebcache.GeoWebCacheUtils;
import org.geowebcache.conveyor.Conveyor;
import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.service.OWSException;
import org.geowebcache.stats.RuntimeStats;
import org.geowebcache.storage.DefaultStorageFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("**" + WMTSService.SERVICE_WMTS)
public class WMTSController {

    private static Log log = LogFactory.getLog(WMTSController.class);

    @Autowired
    private TileLayerDispatcher tileLayerDispatcher;

    @Autowired
    private DefaultStorageFinder defaultStorageFinder;

    @Autowired
    private RuntimeStats runtimeStats;

    @RequestMapping(value = "/WMTSCapabilities.xml", method = RequestMethod.GET)
    public String getCapabilities() {
        return "";
    }

    @RequestMapping(value = "/{layer}/{style}/{tileMatrixSet}/{tileMatrix}/{tileRow}/{tileCol}", method = RequestMethod.GET, params = {
            "format" })
    public void getTile(HttpServletRequest request, HttpServletResponse response,
            @PathVariable String layer, @PathVariable String style,
            @PathVariable String tileMatrixSet, @PathVariable String tileMatrix,
            @PathVariable String tileRow, @PathVariable String tileCol,
            @RequestParam("format") String format, @RequestParam Map<String, String> params)
            throws Exception {
        Conveyor conv = null;

        List<WMTSService> services = GeoWebCacheExtensions.extensions(WMTSService.class);

        Map<String, String> values = new HashMap<String, String>();
        values.put("layer", layer);
        values.put("request", "gettile");
        values.put("style", style);
        values.put("format", format);
        values.put("tilematrixset", tileMatrixSet);
        values.put("tilematrix", tileMatrix);
        values.put("tilerow", tileRow);
        values.put("tilecol", tileCol);

        if (!services.isEmpty()) {
            WMTSService service = services.get(0);
            conv = service.getConveyor(request, response, values);

            // 2) Find out what layer will be used and how
            final String layerName = conv.getLayerId();
            if (layerName != null && !tileLayerDispatcher.getTileLayer(layerName).isEnabled()) {
                throw new OWSException(400, "InvalidParameterValue", "LAYERS",
                        "Layer '" + layerName + "' is disabled");
            }

            GeoWebCacheUtils.writeTile(conv, layerName, tileLayerDispatcher, defaultStorageFinder,
                    runtimeStats);
        }

    }

    @RequestMapping(value = "/{layer}/{style}/{tileMatrixSet}/{tileMatrix}/{tileRow}/{tileCol}/{J}/{I}", method = RequestMethod.GET, params = {
            "format" })
    public String getFeatureInfo(@PathVariable String layer, @PathVariable String style,
            @PathVariable String tileMatrixSet, @PathVariable String tileMatrix,
            @PathVariable String tileRow, @PathVariable String tileCol, @PathVariable String j,
            @PathVariable String i, @RequestParam Map<String, String> params) {
        return "";
    }
}
