/*
 * 日期：2021年08月13日
 *
 * 版权所有：浙江浙大网新众合轨道交通工程有限公司
 */

package com.six.spring.mvc.framework.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.six.spring.mvc.framework.annotation.SixAutowired;
import com.six.spring.mvc.framework.annotation.SixController;
import com.six.spring.mvc.framework.annotation.SixRequestMpping;
import com.six.spring.mvc.framework.annotation.SixService;

/**
 * Ticket: 控制器
 *
 * @author: weishuojun
 */
public class SixDispatchServlet extends HttpServlet {
	private Properties configuration = new Properties();
	private Map<String, Object> ioc = new ConcurrentHashMap<String, Object>();
	private Map<String, Method> handlerMapping = new ConcurrentHashMap<String, Method>();
	private List<String> beanNames = new ArrayList<String>();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			doDispatch(req, resp);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	private void doDispatch(HttpServletRequest req, HttpServletResponse resp)
			throws IOException, InvocationTargetException, IllegalAccessException {
		//总路径
		String reqRequestURI = req.getRequestURI();
		//相对路径
		String contextPath = req.getContextPath();

		reqRequestURI.replaceAll(contextPath, "").replaceAll("/+", "/");

		if (!handlerMapping.containsKey(reqRequestURI)) {
			resp.getWriter().write("404 NOT Found!");
		}

		Method method = handlerMapping.get(reqRequestURI);
		//method属于哪个类
		Class<?> declaringClass = method.getDeclaringClass();
		String beanName = toLowerFirstCase(declaringClass.getSimpleName());
		Map<String, String[]> parameterMap = req.getParameterMap();
		method.invoke(ioc.get(beanName), req, resp, parameterMap.get("name")[0]);

	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		//1、加载配置文件
		doInitConfig(config.getInitParameter("configuration"));
		//2、扫码相关类
		doScanner(configuration.getProperty("scanner"));
		//3、实例化
		doInstance();
		//4、依赖注入
		doAutowired();
		//5、初始化HandlerMapping
		doInitHandlerMapping();
	}

	private void doInitHandlerMapping() {
		if (ioc.isEmpty()) {
			return;
		}

		for (Map.Entry<String, Object> entry : ioc.entrySet()) {
			Class<?> aClass = entry.getValue().getClass();
			if (!aClass.isAnnotationPresent(SixController.class)) {
				continue;
			}

			String url = "";
			if (aClass.isAnnotationPresent(SixRequestMpping.class)) {
				SixRequestMpping annotation1 = aClass.getAnnotation(SixRequestMpping.class);
				url = annotation1.value();
			}
			Method[] methods = aClass.getMethods();
			//获取当前方法类名路径

			for (Method method : methods) {
				//方法上有注解
				if (!method.isAnnotationPresent(SixRequestMpping.class)) {
					continue;
				}

				SixRequestMpping annotation = method.getAnnotation(SixRequestMpping.class);

				//获取当前方法的路径
				String value = "/" + url + "/" + annotation.value();
				handlerMapping.put(value.replaceAll("/+", "/"), method);
			}
		}
	}

	private void doAutowired() {
		if (ioc.isEmpty()) {
			return;
		}

		for (Map.Entry<String, Object> entry : ioc.entrySet()) {
			//获取所有属性
			Field[] declaredFields = entry.getValue().getClass().getDeclaredFields();
			for (Field declaredField : declaredFields) {
				if (!declaredField.isAnnotationPresent(SixAutowired.class)) {
					continue;
				}
				SixAutowired annotation = declaredField.getAnnotation(SixAutowired.class);
				String beanName = annotation.value();
				//如果没有给别名，则用默认名称
				if ("".equals(beanName)) {
					beanName = declaredField.getType().getName();
				}
				//暴力访问
				declaredField.setAccessible(true);
				//从IOC中取出对应的实例，赋给当前属性
				try {
					declaredField.set(entry.getValue(), ioc.get(beanName));
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private void doInstance() {
		if (beanNames.isEmpty()) {
			return;
		}
		try {
			for (String beanName : beanNames) {
				Class<?> bean = Class.forName(beanName);
				//判断是否有注解
				if (bean.isAnnotationPresent(SixController.class)) {
					Object instance = bean.newInstance();
					String className = toLowerFirstCase(bean.getSimpleName());
					ioc.put(className, instance);
				} else if (bean.isAnnotationPresent(SixService.class)) {
					//默认类名小写
					String className = toLowerFirstCase(bean.getSimpleName());
					SixService annotation = bean.getAnnotation(SixService.class);
					//自定义命名
					if (!"".equals(annotation.value())) {
						className = annotation.value();
					}
					Object instance = bean.newInstance();
					ioc.put(className, instance);
					//根据类型注入
					Class<?>[] interfaces = bean.getInterfaces();
					for (Class<?> anInterface : interfaces) {
						if (ioc.containsKey(anInterface.getName())) {
							throw new Exception("The beanName '" + anInterface.getName() + "' is already exit!");
						}
						ioc.put(anInterface.getName(), instance);
					}
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String toLowerFirstCase(String className) {
		char[] chars = className.toCharArray();
		chars[0] += 32;
		return String.valueOf(chars);
	}

	private void doScanner(String path) {
		URL url = this.getClass().getClassLoader().getResource("/" + path.replaceAll("\\.", "/"));
		if (null == url) {
			return;
		}
		File file = new File(url.getFile());
		File[] files = file.listFiles();

		if (null == files) {
			return;
		}
		for (File file1 : files) {
			if (file1.isDirectory()) {
				doScanner(path + "." + file1.getName());
			}
			//只加载.class文件
			if (!file1.getName().endsWith(".class")) {
				continue;
			}
			beanNames.add(path + "." + file1.getName().replace(".class", ""));
		}

	}

	private void doInitConfig(String config) {
		InputStream in = this.getClass().getClassLoader().getResourceAsStream(config);
		try {
			configuration.load(in);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (null != in) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}
}
