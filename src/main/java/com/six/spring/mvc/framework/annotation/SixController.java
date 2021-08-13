/*
 * 日期：2021年08月13日
 *
 * 版权所有：浙江浙大网新众合轨道交通工程有限公司
 */

package com.six.spring.mvc.framework.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ticket:
 *
 * @author: weishuojun
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SixController {
	String value() default "";
}
