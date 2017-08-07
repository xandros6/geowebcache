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
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.GeoWebCacheExtensions;
import org.geowebcache.GeoWebCacheUtils;
import org.geowebcache.conveyor.Conveyor;
import org.geowebcache.conveyor.Conveyor.CacheResult;
import org.geowebcache.filter.request.RequestFilterException;
import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.layer.BadTileException;
import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.service.HttpErrorCodeException;
import org.geowebcache.service.OWSException;
import org.geowebcache.stats.RuntimeStats;
import org.geowebcache.storage.DefaultStorageFinder;
import org.springframework.beans.factory.annotation.Autowired;
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
    public void getCapabilities(HttpServletRequest request, HttpServletResponse response) {
        manageCapabilitiesRequest(request, response);
    }

    @RequestMapping(value = { "/{layer}/{style}/{tileMatrixSet}/{tileMatrix}/{tileRow}/{tileCol}",
            "/{layer}/{tileMatrixSet}/{tileMatrix}/{tileRow}/{tileCol}" }, method = RequestMethod.GET, params = {
                    "format" })
    public void getTile(HttpServletRequest request, HttpServletResponse response,
            @PathVariable String layer, @PathVariable Optional<String> style,
            @PathVariable String tileMatrixSet, @PathVariable String tileMatrix,
            @PathVariable String tileRow, @PathVariable String tileCol,
            @RequestParam("format") String format, @RequestParam Map<String, String> params) {

        manageFeatureRequest(request, response, "gettile", layer, style, tileMatrixSet, tileMatrix,
                tileRow, tileCol, null, null, format, null, params);

    }

    @RequestMapping(value = {
            "/{layer}/{style}/{tileMatrixSet}/{tileMatrix}/{tileRow}/{tileCol}/{j}/{i}",
            "/{layer}/{tileMatrixSet}/{tileMatrix}/{tileRow}/{tileCol}/{j}/{i}" }, method = RequestMethod.GET, params = {
                    "format" })
    public void getFeatureInfo(HttpServletRequest request, HttpServletResponse response,
            @PathVariable String layer, @PathVariable Optional<String> style,
            @PathVariable String tileMatrixSet, @PathVariable String tileMatrix,
            @PathVariable String tileRow, @PathVariable String tileCol, @PathVariable String j,
            @PathVariable String i, @RequestParam("format") String format,
            @RequestParam Map<String, String> params) {

        manageFeatureRequest(request, response, "getfeatureinfo", layer, style, tileMatrixSet,
                tileMatrix, tileRow, tileCol, j, i, null, format, params);
    }

    private void manageCapabilitiesRequest(HttpServletRequest request,
            HttpServletResponse response) {
        manageFeatureRequest(request, response, "getcapabilities", null, Optional.empty(), null,
                null, null, null, null, null, null, null, null);
    }

    private void manageFeatureRequest(HttpServletRequest request, HttpServletResponse response,
            String type, String layer, Optional<String> style, String tileMatrixSet,
            String tileMatrix, String tileRow, String tileCol, String j, String i, String format,
            String infoformat, Map<String, String> params) {
        try {
            Conveyor conv = null;

            List<WMTSService> services = GeoWebCacheExtensions.extensions(WMTSService.class);

            Map<String, String> values = new HashMap<String, String>();
            if (layer != null)
                values.put("layer", layer);
            if (type != null)
                values.put("request", type);
            if (style.isPresent())
                values.put("style", style.get());
            if (tileMatrixSet != null)
                values.put("tilematrixset", tileMatrixSet);
            if (tileMatrix != null)
                values.put("tilematrix", tileMatrix);
            if (tileRow != null)
                values.put("tilerow", tileRow);
            if (tileCol != null)
                values.put("tilecol", tileCol);
            if (format != null)
                values.put("format", format);
            if (infoformat != null)
                values.put("infoformat", infoformat);
            if (j != null)
                values.put("j", j);
            if (i != null)
                values.put("i", i);

            // if (!services.isEmpty()) {
            WMTSService service = services.get(0);
            conv = service.getConveyor(request, response, values);

            final String layerName = conv.getLayerId();
            if (layerName != null && !tileLayerDispatcher.getTileLayer(layerName).isEnabled()) {
                throw new OWSException(400, "InvalidParameterValue", "LAYERS",
                        "Layer '" + layerName + "' is disabled");
            }

            // Check where this should be dispatched
            if (conv.reqHandler == Conveyor.RequestHandler.SERVICE) {
                // A3 The service object takes it from here
                service.handleRequest(conv);
            } else {
                GeoWebCacheUtils.writeTile(conv, layerName, tileLayerDispatcher,
                        defaultStorageFinder, runtimeStats);
            }

            // }

        } catch (HttpErrorCodeException e) {
            GeoWebCacheUtils.writeFixedResponse(response, e.getErrorCode(), "text/plain",
                    new ByteArrayResource(e.getMessage().getBytes()), CacheResult.OTHER,
                    runtimeStats);
        } catch (RequestFilterException e) {
            RequestFilterException reqE = (RequestFilterException) e;
            reqE.setHttpInfoHeader(response);
            GeoWebCacheUtils.writeFixedResponse(response, reqE.getResponseCode(),
                    reqE.getContentType(), reqE.getResponse(), CacheResult.OTHER, runtimeStats);
        } catch (OWSException e) {
            OWSException owsE = (OWSException) e;
            GeoWebCacheUtils.writeFixedResponse(response, owsE.getResponseCode(),
                    owsE.getContentType(), owsE.getResponse(), CacheResult.OTHER, runtimeStats);
        } catch (Exception e) {
            if (!(e instanceof BadTileException) || log.isDebugEnabled()) {
                log.error(e.getMessage() + " " + request.getRequestURL().toString());
            }
            GeoWebCacheUtils.writeErrorAsXML(response, 400, e.getMessage(), runtimeStats);
            if (!(e instanceof GeoWebCacheException) || log.isDebugEnabled()) {
                log.error(e.getMessage(), e);
            }
        }
    }

}
