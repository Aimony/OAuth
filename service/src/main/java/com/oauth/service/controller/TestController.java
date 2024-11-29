package com.oauth.service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Aimony
 * @date 2024/11/28 23:06
 * @description
 */


@RestController
public class TestController {
	@GetMapping("/test")
	public String hello() {
		return "hello world";
	}
}