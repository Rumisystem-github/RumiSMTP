package com.rumisystem.rumi_smtp.MODULE;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

public class DNSResolve {
	public static List<String> GetDNS(String Domain, String Type) throws NamingException {
		//環境設定
		Hashtable<String, String> ENV = new Hashtable<String, String>();
		ENV.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");

		DirContext DCT = new InitialDirContext(ENV);
		Attributes ATTRS = DCT.getAttributes(Domain, new String[] {Type});
		Attribute ATTR = ATTRS.get(Type);
		if (ATTR != null) {
			List<String> RETURN = new ArrayList<String>();
			for (int I = 0; I < ATTR.size(); I++) {
				RETURN.add((String) ATTR.get(I));
			}
			return RETURN;
		} else {
			return new ArrayList<String>();
		}
	}
}
