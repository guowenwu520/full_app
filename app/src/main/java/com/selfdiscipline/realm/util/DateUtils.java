package com.selfdiscipline.realm.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DateUtils {
    private static final SimpleDateFormat DATE = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat DATE_TIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    public static String today() { return DATE.format(new Date()); }
    public static String now() { return DATE_TIME.format(new Date()); }
    public static boolean isValidDate(String s) { try { DATE.setLenient(false); DATE.parse(s); return true; } catch (Exception e) { return false; } }
    public static boolean isValidTime(String s) { if (s == null || !s.matches("\\d{2}:\\d{2}")) return false; int h=Integer.parseInt(s.substring(0,2)); int m=Integer.parseInt(s.substring(3,5)); return h>=0 && h<24 && m>=0 && m<60; }
    public static boolean sleepPassed(String sleep, String wake) { return isValidTime(sleep) && isValidTime(wake) && minutes(sleep) <= 23*60+30 && minutes(wake) <= 8*60+30; }
    public static int minutes(String time) { return Integer.parseInt(time.substring(0,2))*60 + Integer.parseInt(time.substring(3,5)); }
    public static int maxStreak(List<String> dates) { Set<String> set = new HashSet<>(dates); int best=0; for (String d : set) { int c=1; String next=shift(d,1); while(set.contains(next)){c++; next=shift(next,1);} if(c>best) best=c; } return best; }
    public static int currentStreak(List<String> dates) { Set<String> set = new HashSet<>(dates); String d=today(); if(!set.contains(d)) d=shift(d,-1); int c=0; while(set.contains(d)){c++; d=shift(d,-1);} return c; }
    public static String shift(String date, int days) { try { Calendar c=Calendar.getInstance(); c.setTime(DATE.parse(date)); c.add(Calendar.DATE, days); return DATE.format(c.getTime()); } catch(Exception e){ return date; } }
}
