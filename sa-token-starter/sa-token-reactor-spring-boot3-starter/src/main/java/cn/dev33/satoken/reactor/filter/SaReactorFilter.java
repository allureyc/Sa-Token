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
package cn.dev33.satoken.reactor.filter;

import cn.dev33.satoken.exception.BackResultException;
import cn.dev33.satoken.exception.SaTokenException;
import cn.dev33.satoken.exception.StopMatchException;
import cn.dev33.satoken.filter.SaFilter;
import cn.dev33.satoken.filter.SaFilterAuthStrategy;
import cn.dev33.satoken.filter.SaFilterErrorStrategy;
import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;
import cn.dev33.satoken.reactor.util.SaReactorOperateUtil;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.util.SaTokenConsts;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Reactor 全局鉴权过滤器
 * <p>
 *     默认优先级为 -100，尽量保证在其它过滤器之前执行
 * </p>
 *
 * @author click33
 * @since 1.34.0
 */
@Order(SaTokenConsts.ASSEMBLY_ORDER)
public class SaReactorFilter implements SaFilter, WebFilter {

	// ------------------------ 设置此过滤器 拦截 & 放行 的路由 

	/**
	 * 拦截路由 
	 */
	public List<String> includeList = new ArrayList<>();

	/**
	 * 放行路由 
	 */
	public List<String> excludeList = new ArrayList<>();

	@Override
	public SaReactorFilter addInclude(String... paths) {
		includeList.addAll(Arrays.asList(paths));
		return this;
	}

	@Override
	public SaReactorFilter addExclude(String... paths) {
		excludeList.addAll(Arrays.asList(paths));
		return this;
	}

	@Override
	public SaReactorFilter setIncludeList(List<String> pathList) {
		includeList = pathList;
		return this;
	}

	@Override
	public SaReactorFilter setExcludeList(List<String> pathList) {
		excludeList = pathList;
		return this;
	}


	// ------------------------ 钩子函数
	
	/**
	 * 认证函数：每次请求执行 
	 */
	public SaFilterAuthStrategy auth = r -> {};

	/**
	 * 异常处理函数：每次[认证函数]发生异常时执行此函数
	 */
	public SaFilterErrorStrategy error = e -> {
		throw new SaTokenException(e);
	};

	/**
	 * 前置函数：在每次[认证函数]之前执行
	 *      <b>注意点：前置认证函数将不受 includeList 与 excludeList 的限制，所有路由的请求都会进入 beforeAuth</b>
	 */
	public SaFilterAuthStrategy beforeAuth = r -> {};

	@Override
	public SaReactorFilter setAuth(SaFilterAuthStrategy auth) {
		this.auth = auth;
		return this;
	}

	@Override
	public SaReactorFilter setError(SaFilterErrorStrategy error) {
		this.error = error;
		return this;
	}

	@Override
	public SaReactorFilter setBeforeAuth(SaFilterAuthStrategy beforeAuth) {
		this.beforeAuth = beforeAuth;
		return this;
	}

	// ------------------------ filter

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

		// ---------- 全局认证处理
		try {
			SaReactorSyncHolder.setContext(exchange);
			beforeAuth.run(null);
			SaRouter.match(includeList).notMatch(excludeList).check(r -> auth.run(null));
		}
		catch (StopMatchException ignored) {}
		catch (BackResultException e) {
			return SaReactorOperateUtil.writeResult(exchange, e.getMessage());
		}
		catch (Throwable e) {
			return SaReactorOperateUtil.writeResult(exchange, String.valueOf(error.run(e)));
		}
		finally {
			SaReactorSyncHolder.clearContext();
		}

		return chain.filter(exchange);
	}
}
