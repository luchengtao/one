package com.nsn.web.lte.ningbo.sys.service;

import com.nsn.web.lte.aop.Cache;

public class TestService {
	@Cache(region = "1d", key = "sss")
	public String test(String name) {
		System.out.println("test service!");
		return "test service cache " + name;
	}
}
