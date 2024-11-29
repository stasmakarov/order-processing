package com.company.orderprocessing.nominatim;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NominatimResponse(
        @JsonProperty("lat") double latitude,
        @JsonProperty("lon") double longitude,
        @JsonProperty("display_name") String displayName) {
}