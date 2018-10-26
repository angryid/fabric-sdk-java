package com.ule.merchant.chaincode.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

/**
 * 读取properties文件
 */
public class PropertyUtil {
	private static Logger log = LogManager.getLogger(PropertyUtil.class);

	private static Map<String, Properties> cache = new HashMap<String, Properties>();

	public static Properties get(String configFileName) {
		if (cache.get(configFileName) != null) {
			return cache.get(configFileName);
		}
		InputStream is = PropertyUtil.class.getResourceAsStream(configFileName);
		Properties config = new Properties();
		try {
			config.load(is);
			cache.put(configFileName, config);
		} catch (IOException e) {
			log.error("load property failed, configFileName:" + configFileName, e);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				log.error(e);
			}
		}
		return config;
	}

	public static String getStringValue(String configFileName, String key) {
		Properties p = get(configFileName);
		String value = null;
		if (p != null) {
			value = (String) p.get(key);
		}
		return value;
	}

	public static List<String> getListValue(String configFileName, String key) {
		List<String> listVal = new ArrayList<String>();

		String strVal = getStringValue(configFileName, key);
		if (strVal == null || strVal.isEmpty()) return listVal;

		String[] arrayVal = strVal.split(",");
		for (String str : arrayVal) {
			str = str.trim();
			if (!str.isEmpty()) {
				listVal.add(str);
			}
		}

		return listVal;
	}

	public static String[] getArrayValue(String configFileName, String key) {
		List<String> listVal = getListValue(configFileName, key);
		return listVal.isEmpty() ? null : listVal.toArray(new String[listVal.size()]);
	}

	public static BigDecimal getBigDecimalValue(String configFileName, String key) {
		String strVal = getStringValue(configFileName, key);
		BigDecimal bdVal = null;
		if (strVal != null && !strVal.isEmpty()) {
			try {
				bdVal = new BigDecimal(strVal);
			} catch (Exception e) {
				log.error("getBigDecimalValue failed, [" + strVal + "] is not a number!");
			}
		}
		return bdVal;
	}

	public static Integer getIntValue(String configFileName, String key) {
		BigDecimal bdVal = getBigDecimalValue(configFileName, key);
		return bdVal == null ? null : bdVal.intValue();
	}

	public static Long getLongValue(String configFileName, String key) {
		BigDecimal bdVal = getBigDecimalValue(configFileName, key);
		return bdVal == null ? null : bdVal.longValue();
	}

	public static Double getDoubleValue(String configFileName, String key) {
		BigDecimal bdVal = getBigDecimalValue(configFileName, key);
		return bdVal == null ? null : bdVal.doubleValue();
	}
}
