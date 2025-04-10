package com.pj.satoken;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.fun.strategy.SaCorsHandleFunction;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaHttpMethod;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.strategy.SaAnnotationStrategy;
import cn.dev33.satoken.util.SaResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;


/**
 * [Sa-Token 权限认证] 配置类 
 * @author click33
 *
 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {
	
	/**
	 * 注册 Sa-Token 拦截器打开注解鉴权功能  
	 */
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		// 注册 Sa-Token 拦截器打开注解鉴权功能 
		registry.addInterceptor(new SaInterceptor(handle -> {
			// SaManager.getLog().debug("----- 请求path={}  提交token={}", SaHolder.getRequest().getRequestPath(), StpUtil.getTokenValue());

			// 指定一条 match 规则
            SaRouter
                .match("/user/**")    // 拦截的 path 列表，可以写多个
                .notMatch("/user/doLogin", "/user/doLogin2")     // 排除掉的 path 列表，可以写多个
                .check(r -> StpUtil.checkLogin());        // 要执行的校验动作，可以写完整的 lambda 表达式

            // 权限校验 -- 不同模块认证不同权限
            SaRouter.match("/admin/**", r -> StpUtil.checkPermission("admin"));
            SaRouter.match("/goods/**", r -> StpUtil.checkPermission("goods"));
            SaRouter.match("/orders/**", r -> StpUtil.checkPermission("orders"));
            SaRouter.match("/notice/**", r -> StpUtil.checkPermission("notice"));
            SaRouter.match("/comment/**", r -> StpUtil.checkPermission("comment"));

			// 甚至你可以随意的写一个打印语句
			SaRouter.match("/router/print", r -> System.out.println("----啦啦啦----"));

			// 写一个完整的 lambda
			SaRouter.match("/router/print2", r -> {
				System.out.println("----啦啦啦2----");
				// ... 其它代码
			});

			/*
			 * 相关路由都定义在 com.pj.cases.use.RouterCheckController 中
			 */

		})).addPathPatterns("/**");
		
	}
	
	/**
     * 注册 [Sa-Token 全局过滤器] 
     */
    @Bean
    public SaServletFilter getSaServletFilter() {
        return new SaServletFilter()
        		
        		// 指定 [拦截路由] 与 [放行路由]
        		.addInclude("/**")// .addExclude("/favicon.ico")
        		
        		// 认证函数: 每次请求执行 
        		.setAuth(obj -> {
        			// System.out.println("---------- sa全局认证 " + SaHolder.getRequest().getRequestPath()); 
        			// SaManager.getLog().debug("----- 请求path={}  提交token={}", SaHolder.getRequest().getRequestPath(), StpUtil.getTokenValue());

                    // 权限校验 -- 不同模块认证不同权限 
        			//		这里你可以写和拦截器鉴权同样的代码，不同点在于：
        			// 		校验失败后不会进入全局异常组件，而是进入下面的 .setError 函数 
                    SaRouter.match("/admin/**", r -> StpUtil.checkPermission("admin"));
                    SaRouter.match("/goods/**", r -> StpUtil.checkPermission("goods"));
                    SaRouter.match("/orders/**", r -> StpUtil.checkPermission("orders"));
                    SaRouter.match("/notice/**", r -> StpUtil.checkPermission("notice"));
                    SaRouter.match("/comment/**", r -> StpUtil.checkPermission("comment"));
        		})
        		
        		// 异常处理函数：每次认证函数发生异常时执行此函数 
        		.setError(e -> {
        			System.out.println("---------- sa全局异常 ");
        			return SaResult.error(e.getMessage());
        		})
        		
        		// 前置函数：在每次认证函数之前执行（BeforeAuth 不受 includeList 与 excludeList 的限制，所有请求都会进入）
        		.setBeforeAuth(r -> {
        			// ---------- 设置一些安全响应头 ----------
        			SaHolder.getResponse()
        			// 服务器名称 
        			.setServer("sa-server")
        			// 是否可以在iframe显示视图： DENY=不可以 | SAMEORIGIN=同域下可以 | ALLOW-FROM uri=指定域名下可以 
        			.setHeader("X-Frame-Options", "SAMEORIGIN")
        			// 是否启用浏览器默认XSS防护： 0=禁用 | 1=启用 | 1; mode=block 启用, 并在检查到XSS攻击时，停止渲染页面
        			.setHeader("X-XSS-Protection", "1; mode=block")
        			// 禁用浏览器内容嗅探 
        			.setHeader("X-Content-Type-Options", "nosniff")
        			;
        		})
        		;
    }

	/**
	 * CORS 跨域处理
	 */
	@Bean
	public SaCorsHandleFunction corsHandle() {
		return (req, res, sto) -> {
			res.
					// 允许指定域访问跨域资源
							setHeader("Access-Control-Allow-Origin", "*")
					// 允许所有请求方式
					.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE")
					// 有效时间
					.setHeader("Access-Control-Max-Age", "3600")
					// 允许的header参数
					.setHeader("Access-Control-Allow-Headers", "*");

			// 如果是预检请求，则立即返回到前端
			SaRouter.match(SaHttpMethod.OPTIONS)
					.free(r -> System.out.println("--------OPTIONS预检请求，不做处理"))
					.back();
		};
	}

	/**
     * 重写 Sa-Token 框架内部算法策略 
     */
    @PostConstruct
    public void rewriteSaStrategy() {
    	// 重写Sa-Token的注解处理器，增加注解合并功能 
    	SaAnnotationStrategy.instance.getAnnotation = (element, annotationClass) -> {
    		return AnnotatedElementUtils.getMergedAnnotation(element, annotationClass);
    	};

		// 重写 SaCheckELRootMap 扩展函数，增加注解鉴权 EL 表达式可使用的根对象
		SaAnnotationStrategy.instance.checkELRootMapExtendFunction = rootMap -> {
			System.out.println("--------- 执行 SaCheckELRootMap 增强，目前已包含的的跟对象包括：" + rootMap.keySet());
			// 新增 stpUser 根对象，使之可以在表达式中通过 stpUser.checkLogin() 方式进行多账号体系鉴权
			rootMap.put("stpUser", StpUserUtil.getStpLogic());
		};
    }



}
