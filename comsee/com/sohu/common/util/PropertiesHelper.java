package com.sohu.common.util;

import java.util.Properties;

public final class PropertiesHelper {
	
	public static boolean getBoolean(String property, Properties properties) {
		return Boolean.valueOf( properties.getProperty(property) ).booleanValue();
	}
	
	public static boolean getBoolean(String property, Properties properties, boolean defaultValue) {
		String setting = properties.getProperty(property);
		return (setting==null) ? defaultValue : Boolean.valueOf(setting).booleanValue();
	}
	
	public static int getInt(String property, Properties properties, int defaultValue) {
		String propValue = properties.getProperty(property);
		return (propValue==null) ? defaultValue : Integer.parseInt(propValue);
	}
	
	public static String getString(String property, Properties properties, String defaultValue) {
		String propValue = properties.getProperty(property);
		return (propValue==null) ? defaultValue : propValue;
	}
	
	public static Integer getInteger(String property, Properties properties) {
		String propValue = properties.getProperty(property);
		return (propValue==null) ? null : Integer.valueOf(propValue);
	}
	
	
	private PropertiesHelper() {}
}






