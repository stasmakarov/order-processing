package com.company.orderprocessing.nominatim;

import com.company.orderprocessing.configuration.WebinarConfig;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GeoCodingService {
    private static final Logger log = LoggerFactory.getLogger(GeoCodingService.class);

    private final RestTemplate restTemplate;
    private final WebinarConfig webinarConfig;

    public GeoCodingService(RestTemplate restTemplate, WebinarConfig webinarConfig) {
        this.restTemplate = restTemplate;
        this.webinarConfig = webinarConfig;
    }

    /**
     * Geocode address with Nominatim and verify that the point lies within configured bounds.
     * Returns a JTS Point (SRID=4326) if inside bounds, otherwise returns null.
     */
    public Point verifyAddress(String address) {
        // Use limit=1 to reduce payload
        final String NOMINATIM_URL =
                "https://nominatim.openstreetmap.org/search?q={address}&format=json&limit=1&addressdetails=1";

        ResponseEntity<NominatimResponse[]> response = restTemplate.getForEntity(
                NOMINATIM_URL, NominatimResponse[].class, address);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            log.warn("Nominatim response is empty or not 2xx for address: {}", address);
            return null;
        }

        NominatimResponse[] results = response.getBody();
        if (results.length == 0) {
            log.info("No geocode results for: {}", address);
            return null;
        }

        double lat = results[0].latitude();
        double lon = results[0].longitude();

        // Bounds check from WebinarConfig (expects lon in [minLon,maxLon], lat in [minLat,maxLat])
        if (!isWithinBounds(lat, lon)) {
            log.info("Address outside bounds: lat={}, lon={} (allowed: lat[{}..{}], lon[{}..{}])",
                    lat, lon,
                    webinarConfig.getMinLat(), webinarConfig.getMaxLat(),
                    webinarConfig.getMinLon(), webinarConfig.getMaxLon());
            return null; // or throw IllegalArgumentException if that fits better
        }

        // Create Point with SRID=4326 (x=lon, y=lat)
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);
        return gf.createPoint(new Coordinate(lon, lat));
    }

    // --- Helpers ---

    /**
     * Check if the given lat/lon is inside configured rectangle (inclusive).
     */
    private boolean isWithinBounds(double lat, double lon) {
        double minLat = webinarConfig.getMinLat();
        double maxLat = webinarConfig.getMaxLat();
        double minLon = webinarConfig.getMinLon();
        double maxLon = webinarConfig.getMaxLon();

        // Guard against swapped config
        if (minLat > maxLat) { double t = minLat; minLat = maxLat; maxLat = t; }
        if (minLon > maxLon) { double t = minLon; minLon = maxLon; maxLon = t; }

        // Inclusive bounds
        return lat >= minLat && lat <= maxLat && lon >= minLon && lon <= maxLon;
    }
}
