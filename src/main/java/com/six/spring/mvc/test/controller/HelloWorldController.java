/*
 * 日期：2021年08月13日
 *
 * 版权所有：浙江浙大网新众合轨道交通工程有限公司
 */

package com.six.spring.mvc.test.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.six.spring.mvc.framework.annotation.SixAutowired;
import com.six.spring.mvc.framework.annotation.SixController;
import com.six.spring.mvc.framework.annotation.SixRequestMpping;
import com.six.spring.mvc.framework.annotation.SixRequestParam;
import com.six.spring.mvc.test.service.IHelloWorldService;

/**
 * Ticket:
 *
 * @author: weishuojun
 */
@SixController
@SixRequestMpping("/hello")
public class HelloWorldController {

	@SixAutowired
	private IHelloWorldService helloWorldService;

	@SixRequestMpping("/world")
	public void getHelloWorld(HttpServletRequest req, HttpServletResponse resp, @SixRequestParam("name") String name) {
		String helloWorld = helloWorldService.getHelloWorld(name);

		try {
			resp.getWriter().write(helloWorld);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
