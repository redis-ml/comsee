package com.sohu.common.dictz;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.zip.Adler32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class Dictz {

	/**
	 * @param args
	 * @throws UnsupportedEncodingException
	 * @throws DataFormatException
	 * @throws IOException
	 */
	public static void main(String[] args) throws DataFormatException {
		// new Dictz().test();
		byte[] input = "abcdefg1234567".getBytes();
		System.out.println(new String(input));
		byte[] compress = new byte[100];
		Deflater compresser = new Deflater();
		// byte[] dict = freadall("D://adler-f8854915.dictz");
		int offset = 5;
		compresser.setInput(input);
		// compresser.setInput(input, offset, input.length-offset);
		// compresser.setDictionary(dict);
		compresser.finish();
		int compressedDataLength = compresser.deflate(compress);
		System.out.println(compressedDataLength);

		// Inflater decompresser = new Inflater();
		// // byte[] dict = freadall(filename);
		// byte[] out = new byte[100];
		// decompresser.setInput(compress, 0, compressedDataLength);
		// int resultLength = decompresser.inflate(out);
		// if(decompresser.needsDictionary())
		// decompresser.setDictionary(dict);
		// resultLength = decompresser.inflate(out);
		// decompresser.end();
		// System.out.println(new String(out));

		byte[] output = uncompress(compress, 0, compressedDataLength);
		System.out.println(new String(output));
	}

	public static final long DICTZ_NEGATIVE_TTL_DEFAULT = 10;

	public static long parse_env_as_long(String envname, long defaultvalue) {
		String envvalue = System.getenv(envname);
		if (envvalue == null)
			return defaultvalue;
		try {
			long v = Long.parseLong(envvalue);
			return v;
		} catch (NumberFormatException e) {
			return defaultvalue;
		}
	}

	public static final long g_dictz_negative_ttl = parse_env_as_long(
			"DICTZ_NEGATIVE_TTL", DICTZ_NEGATIVE_TTL_DEFAULT);

	public static final int DICTZ_MAP_SIZE = 1019;
	// adler32 -> {dict, dictLen}
	static HashMap<Integer, byte[]> g_dict_map = new HashMap<Integer, byte[]>(
			DICTZ_MAP_SIZE);
	// adler32 -> last failture timestamp
	static HashMap<Long, Long> g_dict_negative_map = new HashMap<Long, Long>(
			DICTZ_MAP_SIZE);

	public static byte[] freadall(String filename) {
		byte[] bytes = null;
		try {
			InputStream is = new FileInputStream(filename);
			int size = is.available();
			// System.out.println(size);
			bytes = new byte[size];
			is.read(bytes);
			is.close();
		} catch (IOException e) {
			return null;
		}
		return bytes;
	}

	public static byte[] loadDictionaryByAdler32(long adler) {
		String patterns[] = { "./adler-%08x.dictz",
				"/usr/share/ssplatform/dictz/adler-%08x.dictz",
				"/search/adler-%08x.dictz", };
		boolean ok = false;
		byte[] dict = null;
		if ((dict = loadDictionaryByAdler32_i(adler)) != null)
			return dict;
		Long last_failure_time = g_dict_negative_map.get(adler);
		if (last_failure_time != null
				&& last_failure_time + g_dictz_negative_ttl >= System
						.currentTimeMillis() / 1000)
			return null;
		for (String pattern : patterns) {
			String filename = String.format(pattern, (int) adler);
			byte[] bytes = null;
			if ((bytes = freadall(filename)) != null) {
				Adler32 adler32 = new Adler32();
				adler32.update(bytes);
				int adler2 = (int) adler32.getValue();
				if (adler == adler2) {
					registerDictionary(bytes, bytes.length);
					ok = true;
				} else if (bytes.length > 1) {
					adler32.reset();
					adler32.update(bytes, 0, bytes.length - 1);
					long adler3 = adler32.getValue();
					if (adler == adler3) {
						registerDictionary(bytes, bytes.length - 1);
						ok = true;
					}
				}
			}
			if (ok)
				break;
		}
		if (ok)
			return loadDictionaryByAdler32_i(adler);
		else {
			g_dict_negative_map.put(adler, System.currentTimeMillis() / 1000);
			return null;
		}
	}

	public static byte[] loadDictionaryByAdler32_i(long adler) {
		return g_dict_map.get((int) adler);
	}

	public static void registerDictionary(byte[] dictionary, int length) {
		Adler32 adler32 = new Adler32();
		adler32.update(dictionary, 0, length);
		int adler = (int) adler32.getValue();
		g_dict_map.put(adler, dictionary);
	}

	/*
	 * return byte array uncompressed
	 */
	public static byte[] uncompress(byte[] input, int offset, int length) {
		byte[] result = null;
		Inflater decompresser = new Inflater();
		decompresser.setInput(input, offset, input.length - offset);
		int resultLength = 0;
		byte[] out = new byte[input.length];
		try {
			resultLength = decompresser.inflate(out);
			if (resultLength == 0 && decompresser.needsDictionary()) {
				int adler = decompresser.getAdler();
				byte[] dict = loadDictionaryByAdler32(adler);
				decompresser.setDictionary(dict);
				resultLength = decompresser.inflate(out);
			}
			result = new byte[resultLength];
			System.arraycopy(out, 0, result, 0, resultLength);
		} catch (DataFormatException e) {
			return null;
		}
		return result;
	}

}
