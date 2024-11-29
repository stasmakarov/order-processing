package com.company.orderprocessing.nominatim;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.locationtech.jts.geom.Point;

@Service
public class GeoCodingService {
    private final RestTemplate restTemplate;

    public GeoCodingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Point verifyAddress(String address) {
        String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search?q={address}&format=json&polygon_kml=1&addressdetails=1";

        ResponseEntity<NominatimResponse[]> response = restTemplate.getForEntity(NOMINATIM_URL,
                NominatimResponse[].class,
                address);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            NominatimResponse[] results = response.getBody();
            if (results.length > 0) {
                double latitude = results[0].latitude();
                double longitude = results[0].longitude();
                return createPoint(latitude, longitude);
            }
        }
        return null;
    }

    private Point createPoint(double latitude, double longitude) {
        // Use JTS or any other library you prefer to create a Point
        GeometryFactory geometryFactory = new GeometryFactory();
        return geometryFactory.createPoint(new Coordinate(longitude, latitude));
    }
}