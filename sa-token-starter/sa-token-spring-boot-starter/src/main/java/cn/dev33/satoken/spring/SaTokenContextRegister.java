/*
 * Copyright 2020-2099 sa-token.cc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.dev33.satoken.spring;

import cn.dev33.satoken.filter.SaFirewallCheckFilterForServlet;
import cn.dev33.satoken.filter.SaTokenContextFilterForServlet;
import cn.dev33.satoken.filter.SaTokenCorsFilterForServlet;
import cn.dev33.satoken.spring.pathmatch.SaPatternsRequestConditionHolder;
import cn.dev33.satoken.strategy.SaStrategy;
import org.springframework.context.annotation.Bean;

/**
 * 注册 Sa-Token 框架所需要的 Bean
 *
 * @author click33
 * @since 1.34.0
 */
public class SaTokenContextRegister {

	public SaTokenContextRegister() {
		// 重写路由匹配算法
		SaStrategy.instance.routeMatcher = (pattern, path) -> {
			return SaPatternsRequestConditionHolder.match(pattern, path);
		};
	}

	/**
	 * 上下文过滤器
	 *
	 * @return /
	 */
	@Bean
	public SaTokenContextFilterForServlet saTokenContextFilterForServlet() {
		return new SaTokenContextFilterForServlet();
	}

	/**
	 * CORS 跨域策略过滤器
	 *
	 * @return /
	 */
	@Bean
	public SaTokenCorsFilterForServlet saTokenCorsFilterForServlet() {
		return new SaTokenCorsFilterForServlet();
	}

	/**
	 * 防火墙过滤器
	 *
	 * @return /
	 */
	@Bean
	public SaFirewallCheckFilterForServlet saFirewallCheckFilterForServlet() {
		return new SaFirewallCheckFilterForServlet();
	}

}
