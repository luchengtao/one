package com.nsn.web.lte.ningbo.sys.service;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class Cookies implements CookieJar {
	private static List<Cookie> cookies;

	@Override
	public void saveFromResponse(HttpUrl httpUrl, List<Cookie> cks) {
		this.cookies = cks;
	}

	@Override
	public List<Cookie> loadForRequest(HttpUrl httpUrl) {
		if (null != cookies) {
			return cookies;
		} else {
			return new ArrayList<Cookie>();
		}
	}

	public static void resetCookies() {
		cookies = null;
	}
}
