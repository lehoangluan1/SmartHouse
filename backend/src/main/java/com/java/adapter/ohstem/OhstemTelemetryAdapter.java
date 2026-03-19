package com.java.adapter.ohstem;

import java.util.Map;

public class OhstemTelemetryAdapter {

  public OhstemTelemetryDto adapt(Map<String, Object> raw) {
    String deviceKey = str(raw.getOrDefault("deviceKey", "yolobit-01"));

    Double temp = dbl(raw.get("temp"));

    Double humidity = dbl(raw.get("humidity"));
    Integer shine = integer(raw.get("shine"));
    Boolean someone = bool(raw.get("someone"));

    return new OhstemTelemetryDto(deviceKey, temp, humidity, shine, someone);
  }

  private static String str(Object v) { return v == null ? null : String.valueOf(v).trim(); }
  private static Double dbl(Object v) {
    if (v == null) return null;
    if (v instanceof Number n) return n.doubleValue();
    try { return Double.valueOf(String.valueOf(v).trim()); } catch (NumberFormatException e) { return null; }
  }
  private static Integer integer(Object v) {
    if (v == null) return null;
    if (v instanceof Number n) return n.intValue();
    try { return Integer.valueOf(String.valueOf(v).trim()); } catch (NumberFormatException e) { return null; }
  }
  private static Boolean bool(Object v) {
    if (v == null) return null;
    if (v instanceof Boolean b) return b;
    String s = String.valueOf(v).trim().toLowerCase();
    if (s.equals("1") || s.equals("true")) return true;
    if (s.equals("0") || s.equals("false")) return false;
    return null;
  }
}