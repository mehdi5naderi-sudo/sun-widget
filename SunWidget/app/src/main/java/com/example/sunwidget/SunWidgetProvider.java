package com.example.sunwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

import java.util.Calendar;
import java.util.TimeZone;

public class SunWidgetProvider extends AppWidgetProvider {

    // ===== Edit these two lines for your own city =====
    private static final double LATITUDE = 35.6892;   // Tehran latitude
    private static final double LONGITUDE = 51.3890;  // Tehran longitude
    // ====================================================

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId);
        }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int widgetId) {
        Calendar now = Calendar.getInstance();
        double timezoneOffsetHours = now.get(Calendar.ZONE_OFFSET) / 3600000.0
                + (now.get(Calendar.DST_OFFSET) / 3600000.0);

        double[] times = calculateSunTimes(now, LATITUDE, LONGITUDE, timezoneOffsetHours);
        String sunrise = formatTime(times[0]);
        String sunset = formatTime(times[1]);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        views.setTextViewText(R.id.sunrise_text, "Sunrise: " + sunrise);
        views.setTextViewText(R.id.sunset_text, "Sunset: " + sunset);

        appWidgetManager.updateAppWidget(widgetId, views);
    }

    /**
     * NOAA solar calculation algorithm.
     * Returns {sunriseHourLocal, sunsetHourLocal} as decimal hours (0-24).
     */
    private double[] calculateSunTimes(Calendar cal, double lat, double lon, double tzOffsetHours) {
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);

        double julianDay = toJulianDay(year, month, day);
        double julianCentury = (julianDay - 2451545.0) / 36525.0;

        double geomMeanLongSun = (280.46646 + julianCentury * (36000.76983 + julianCentury * 0.0003032)) % 360.0;
        double geomMeanAnomSun = 357.52911 + julianCentury * (35999.05029 - 0.0001537 * julianCentury);
        double eccentEarthOrbit = 0.016708634 - julianCentury * (0.000042037 + 0.0000001267 * julianCentury);

        double sunEqOfCtr = Math.sin(Math.toRadians(geomMeanAnomSun)) * (1.914602 - julianCentury * (0.004817 + 0.000014 * julianCentury))
                + Math.sin(Math.toRadians(2 * geomMeanAnomSun)) * (0.019993 - 0.000101 * julianCentury)
                + Math.sin(Math.toRadians(3 * geomMeanAnomSun)) * 0.000289;

        double sunTrueLong = geomMeanLongSun + sunEqOfCtr;
        double sunAppLong = sunTrueLong - 0.00569 - 0.00478 * Math.sin(Math.toRadians(125.04 - 1934.136 * julianCentury));

        double meanObliqEcliptic = 23.0 + (26.0 + (21.448 - julianCentury * (46.815 + julianCentury * (0.00059 - julianCentury * 0.001813))) / 60.0) / 60.0;
        double obliqCorr = meanObliqEcliptic + 0.00256 * Math.cos(Math.toRadians(125.04 - 1934.136 * julianCentury));

        double sunDeclin = Math.toDegrees(Math.asin(Math.sin(Math.toRadians(obliqCorr)) * Math.sin(Math.toRadians(sunAppLong))));

        double varY = Math.tan(Math.toRadians(obliqCorr / 2)) * Math.tan(Math.toRadians(obliqCorr / 2));
        double eqOfTime = 4 * Math.toDegrees(
                varY * Math.sin(2 * Math.toRadians(geomMeanLongSun))
                        - 2 * eccentEarthOrbit * Math.sin(Math.toRadians(geomMeanAnomSun))
                        + 4 * eccentEarthOrbit * varY * Math.sin(Math.toRadians(geomMeanAnomSun)) * Math.cos(2 * Math.toRadians(geomMeanLongSun))
                        - 0.5 * varY * varY * Math.sin(4 * Math.toRadians(geomMeanLongSun))
                        - 1.25 * eccentEarthOrbit * eccentEarthOrbit * Math.sin(2 * Math.toRadians(geomMeanAnomSun))
        );

        double haSunrise = Math.toDegrees(Math.acos(
                (Math.cos(Math.toRadians(90.833)) / (Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(sunDeclin))))
                        - Math.tan(Math.toRadians(lat)) * Math.tan(Math.toRadians(sunDeclin))
        ));

        double solarNoonMinutes = 720 - 4 * lon - eqOfTime + tzOffsetHours * 60;
        double sunriseMinutes = solarNoonMinutes - haSunrise * 4;
        double sunsetMinutes = solarNoonMinutes + haSunrise * 4;

        double sunriseHour = (sunriseMinutes / 60.0 + 24) % 24;
        double sunsetHour = (sunsetMinutes / 60.0 + 24) % 24;

        return new double[]{sunriseHour, sunsetHour};
    }

    private double toJulianDay(int year, int month, int day) {
        if (month <= 2) {
            year -= 1;
            month += 12;
        }
        int a = year / 100;
        int b = 2 - a + a / 4;
        return Math.floor(365.25 * (year + 4716)) + Math.floor(30.6001 * (month + 1)) + day + b - 1524.5;
    }

    private String formatTime(double decimalHour) {
        int h = (int) decimalHour;
        int m = (int) Math.round((decimalHour - h) * 60);
        if (m == 60) {
            m = 0;
            h = (h + 1) % 24;
        }
        return String.format("%02d:%02d", h, m);
    }
}
