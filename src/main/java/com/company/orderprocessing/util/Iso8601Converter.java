package com.company.orderprocessing.util;

public class Iso8601Converter {

    public static String convertSecondsToDuration(int totalSeconds) {
        // Calculate minutes and remaining seconds
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        // Build the ISO 8601 duration string
        StringBuilder isoDuration = new StringBuilder("PT");
        if (minutes > 0) {
            isoDuration.append(minutes).append("M");
        }
        if (seconds > 0 || minutes == 0) { // Always include seconds if no minutes
            isoDuration.append(seconds).append("S");
        }

        return isoDuration.toString();
    }
}

