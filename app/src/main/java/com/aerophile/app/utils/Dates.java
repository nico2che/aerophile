package com.aerophile.app.utils;

import android.util.Log;

import org.androidannotations.annotations.EBean;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Date;

@EBean
public class Dates {

    public static String dateToReadable(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        if(date == null)
            return null;
        return dateFormat.format(date);
    }

    public static Date stringToDate(String date) {
        if(date == null)
            return null;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        try {
            return format.parse(date);
        } catch (ParseException e) {
            Log.d("AEROBUG", "Date non parsable");
        }
        format = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        try {
            return format.parse(date);
        } catch (ParseException e) {
            Log.d("AEROBUG", "Date non parsable");
        }
        return new Date(Long.valueOf(date));
    }

    private Date getStartOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DATE);
        calendar.set(year, month, day, 0, 0, 0);
        return calendar.getTime();
    }

    private Date getEndOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DATE);
        calendar.set(year, month, day, 23, 59, 59);
        return calendar.getTime();
    }
}
