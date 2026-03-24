package com.rumisystem.rumi_smtp;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hash {
	public static String sha3_256(byte[] data) {
		try {
			MessageDigest mesd = MessageDigest.getInstance("SHA3-256");
			byte[] hash = mesd.digest(data);
			return to_hex(hash);
		} catch (NoSuchAlgorithmException ex) {
			//起こり得ないです。
			return null;
		}
	}

	private static String to_hex(byte[] data) {
		StringBuilder sb = new StringBuilder();
		for (byte b:data) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
}
