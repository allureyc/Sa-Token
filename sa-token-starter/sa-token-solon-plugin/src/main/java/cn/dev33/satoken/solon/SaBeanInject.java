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
package cn.dev33.satoken.solon;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.annotation.handler.SaAnnotationHandlerInterface;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.second.SaTokenSecondContextCreator;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.httpauth.basic.SaHttpBasicTemplate;
import cn.dev33.satoken.httpauth.basic.SaHttpBasicUtil;
import cn.dev33.satoken.httpauth.digest.SaHttpDigestTemplate;
import cn.dev33.satoken.httpauth.digest.SaHttpDigestUtil;
import cn.dev33.satoken.json.SaJsonTemplate;
import cn.dev33.satoken.listener.SaTokenEventCenter;
import cn.dev33.satoken.listener.SaTokenListener;
import cn.dev33.satoken.log.SaLog;
import cn.dev33.satoken.plugin.SaTokenPluginHolder;
import cn.dev33.satoken.same.SaSameTemplate;
import cn.dev33.satoken.serializer.SaSerializerTemplate;
import cn.dev33.satoken.sign.SaSignTemplate;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.strategy.SaAnnotationStrategy;
import cn.dev33.satoken.temp.SaTempInterface;
import org.noear.solon.annotation.*;

import java.util.List;

/**
 * 注入 Sa-Token 所需要的 Bean //todo: 再去插件添加：context.beanMake(SaBeanInject.class);
 * 
 * @author click33
 * @since 1.34.0
 */
@Configuration
public class SaBeanInject {

	/**
	 * 组件注入 
	 * <p> 为确保 Log 组件正常打印，必须将 SaLog 和 SaTokenConfig 率先初始化 </p> 
	 * 
	 * @param log log 对象
	 * @param saTokenConfig 配置对象
	 */
	public SaBeanInject(
			@Inject(required = false) SaLog log,
			@Inject(required = false) SaTokenConfig saTokenConfig,
			@Inject(required = false) SaTokenPluginHolder pluginHolder
			){
		if(log != null) {
			SaManager.setLog(log);
		}

		if(saTokenConfig != null) {
			SaManager.setConfig(saTokenConfig);
		}

		// 初始化 Sa-Token SPI 插件
		if (pluginHolder == null) {
			pluginHolder = SaTokenPluginHolder.instance;
		}
		pluginHolder.init();
		SaTokenPluginHolder.instance = pluginHolder;
	}
	
	/**
	 * 注入持久化Bean
	 * 
	 * @param saTokenDao SaTokenDao对象 
	 */
	@Condition(onBean = SaTokenDao.class)
	@Bean
	public void setSaTokenDao(SaTokenDao saTokenDao) {
		SaManager.setSaTokenDao(saTokenDao);
	}

	/**
	 * 注入权限认证Bean
	 * 
	 * @param stpInterface StpInterface对象 
	 */
	@Condition(onBean = StpInterface.class)
	@Bean
	public void setStpInterface(StpInterface stpInterface) {
		SaManager.setStpInterface(stpInterface);
	}

	/**
	 * 注入上下文Bean
	 * 
	 * @param saTokenContext SaTokenContext对象 
	 */
	@Condition(onBean = SaTokenContext.class)
	@Bean
	public void setSaTokenContext(SaTokenContext saTokenContext) {
		SaManager.setSaTokenContext(saTokenContext);
	}

	/**
	 * 注入二级上下文Bean
	 * 
	 * @param saTokenSecondContextCreator 二级上下文创建器 
	 */
	@Condition(onBean = SaTokenSecondContextCreator.class)
	@Bean
	public void setSaTokenContext(SaTokenSecondContextCreator saTokenSecondContextCreator) {
		SaManager.setSaTokenSecondContext(saTokenSecondContextCreator.create());
	}

	/**
	 * 注入侦听器Bean
	 * 
	 * @param listenerList 侦听器集合 
	 */
	@Bean
	public void setSaTokenListener(List<SaTokenListener> listenerList) {
		SaTokenEventCenter.registerListenerList(listenerList);
	}

	/**
	 * 注入自定义注解处理器
	 *
	 * @param handlerList 自定义注解处理器集合
	 */
	@Bean
	public void setSaAnnotationHandler(List<SaAnnotationHandlerInterface<?>> handlerList) {
		for (SaAnnotationHandlerInterface<?> handler : handlerList) {
			SaAnnotationStrategy.instance.registerAnnotationHandler(handler);
		}
	}

	/**
	 * 注入临时令牌验证模块 Bean
	 * 
	 * @param saTemp saTemp对象 
	 */
	@Condition(onBean = SaTempInterface.class)
	@Bean
	public void setSaTemp(SaTempInterface saTemp) {
		SaManager.setSaTemp(saTemp);
	}

	/**
	 * 注入 Same-Token 模块 Bean
	 * 
	 * @param saSameTemplate saSameTemplate对象 
	 */
	@Condition(onBean = SaSameTemplate.class)
	@Bean
	public void setSaIdTemplate(SaSameTemplate saSameTemplate) {
		SaManager.setSaSameTemplate(saSameTemplate);
	}

	/**
	 * 注入 Sa-Token Http Basic 认证模块 
	 * 
	 * @param saBasicTemplate saBasicTemplate对象 
	 */
	@Condition(onBean = SaHttpBasicTemplate.class)
	@Bean
	public void setSaHttpBasicTemplate(SaHttpBasicTemplate saBasicTemplate) {
		SaHttpBasicUtil.saHttpBasicTemplate = saBasicTemplate;
	}

	/**
	 * 注入 Sa-Token Http Digest 认证模块
	 *
	 * @param saHttpDigestTemplate saHttpDigestTemplate 对象
	 */
	@Condition(onBean = SaHttpDigestTemplate.class)
	@Bean
	public void setSaHttpDigestTemplate(SaHttpDigestTemplate saHttpDigestTemplate) {
		SaHttpDigestUtil.saHttpDigestTemplate = saHttpDigestTemplate;
	}

	/**
	 * 注入自定义的 JSON 转换器 Bean 
	 * 
	 * @param saJsonTemplate JSON 转换器 
	 */
	@Condition(onBean = SaJsonTemplate.class)
	@Bean
	public void setSaJsonTemplate(SaJsonTemplate saJsonTemplate) {
		SaManager.setSaJsonTemplate(saJsonTemplate);
	}

	/**
	 * 注入自定义的序列化器 Bean
	 *
	 * @param saSerializerTemplate 序列化器
	 */
	@Condition(onBean = SaSerializerTemplate.class)
	@Bean
	public void setSaSerializerTemplate(SaSerializerTemplate saSerializerTemplate) {
		SaManager.setSaSerializerTemplate(saSerializerTemplate);
	}

	/**
	 * 注入自定义的 参数签名 Bean 
	 * 
	 * @param saSignTemplate 参数签名 Bean 
	 */
	@Condition(onBean = SaSignTemplate.class)
	@Bean
	public void setSaSignTemplate(SaSignTemplate saSignTemplate) {
		SaManager.setSaSignTemplate(saSignTemplate);
	}

	/**
	 * 注入自定义的 StpLogic 
	 * @param stpLogic / 
	 */
	@Condition(onBean = StpLogic.class)
	@Bean
	public void setStpLogic(StpLogic stpLogic) {
		StpUtil.setStpLogic(stpLogic);
	}
	
	/**
	 * 利用自动注入特性，获取Spring框架内部使用的路由匹配器
	 * 
	 * @param pathMatcher 要设置的 pathMatcher
	 */
//	@Condition(onBean = StpLogic.class) //SaPathMatcherHolder 这个类可能是新的未刷到（后面补上）
//	@Bean
//	public void setPathMatcher(PathMatcher pathMatcher) {
//		SaPathMatcherHolder.setPathMatcher(pathMatcher);
//	}

}
