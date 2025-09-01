package com.company.orderprocessing.configuration;


import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WebinarConfig {

    @Value("${webinar.region}")
    private String region;

    @PostConstruct
    public void validateRegion() {
        if (!"GL".equals(region) && !"RU".equals(region)) {
            throw new IllegalArgumentException("Invalid region value: " + region + ". Allowed values are 'GL' or 'RU'.");
        }
    }

    @Value("${webinar.moscow.minLat}")
    private double moscowMinLat;

    @Value("${webinar.moscow.maxLat}")
    private double moscowMaxLat;

    @Value("${webinar.moscow.minLon}")
    private double moscowMinLon;

    @Value("${webinar.moscow.maxLon}")
    private double moscowMaxLon;

    @Value("${webinar.london.minLat}")
    private double londonMinLat;

    @Value("${webinar.london.maxLat}")
    private double londonMaxLat;

    @Value("${webinar.london.minLon}")
    private double londonMinLon;

    @Value("${webinar.london.maxLon}")
    private double londonMaxLon;

    public String getRegion() {
        return region;
    }

    public double getMinLat() {
        return "RU".equals(region) ? moscowMinLat : londonMinLat;
    }

    public double getMaxLat() {
        return "RU".equals(region) ? moscowMaxLat : londonMaxLat;
    }

    public double getMinLon() {
        return "RU".equals(region) ? moscowMinLon : londonMinLon;
    }

    public double getMaxLon() {
        return "RU".equals(region) ? moscowMaxLon : londonMaxLon;
    }
}