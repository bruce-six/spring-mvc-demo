/*
 * 日期：2021年08月13日
 *
 * 版权所有：浙江浙大网新众合轨道交通工程有限公司
 */

package com.six.spring.mvc.test.service;

import com.six.spring.mvc.framework.annotation.SixService;

/**
 * Ticket:
 *
 * @author: weishuojun
 */
@SixService
public class HelloWorldService implements IHelloWorldService {
	public String getHelloWorld(String name) {
		return name + "! This is Hello World!";
	}
}
