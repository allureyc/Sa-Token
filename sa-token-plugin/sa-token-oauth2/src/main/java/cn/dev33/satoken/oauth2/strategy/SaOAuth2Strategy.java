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
package cn.dev33.satoken.oauth2.strategy;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.fun.SaParamFunction;
import cn.dev33.satoken.fun.SaTwoParamFunction;
import cn.dev33.satoken.oauth2.SaOAuth2Manager;
import cn.dev33.satoken.oauth2.config.SaOAuth2ServerConfig;
import cn.dev33.satoken.oauth2.consts.GrantType;
import cn.dev33.satoken.oauth2.consts.SaOAuth2Consts;
import cn.dev33.satoken.oauth2.data.model.loader.SaClientModel;
import cn.dev33.satoken.oauth2.data.model.request.ClientIdAndSecretModel;
import cn.dev33.satoken.oauth2.error.SaOAuth2ErrorCode;
import cn.dev33.satoken.oauth2.exception.SaOAuth2Exception;
import cn.dev33.satoken.oauth2.function.SaOAuth2ConfirmViewFunction;
import cn.dev33.satoken.oauth2.function.SaOAuth2DoLoginHandleFunction;
import cn.dev33.satoken.oauth2.function.SaOAuth2NotLoginViewFunction;
import cn.dev33.satoken.oauth2.function.strategy.*;
import cn.dev33.satoken.oauth2.granttype.handler.AuthorizationCodeGrantTypeHandler;
import cn.dev33.satoken.oauth2.granttype.handler.PasswordGrantTypeHandler;
import cn.dev33.satoken.oauth2.granttype.handler.RefreshTokenGrantTypeHandler;
import cn.dev33.satoken.oauth2.granttype.handler.SaOAuth2GrantTypeHandlerInterface;
import cn.dev33.satoken.oauth2.scope.CommonScope;
import cn.dev33.satoken.oauth2.scope.handler.*;
import cn.dev33.satoken.util.SaFoxUtil;
import cn.dev33.satoken.util.SaResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sa-Token OAuth2 相关策略
 *
 * @author click33
 * @since 1.39.0
 */
public final class SaOAuth2Strategy {

	private SaOAuth2Strategy() {
		registerDefaultScopeHandler();
		registerDefaultGrantTypeHandler();
	}

	/**
	 * 全局单例引用
	 */
	public static final SaOAuth2Strategy instance = new SaOAuth2Strategy();


	// ------------------ 权限处理器 ------------------

	/**
	 * Scope 权限处理器集合
	 */
	public Map<String, SaOAuth2ScopeHandlerInterface> scopeHandlerMap = new LinkedHashMap<>();

	/**
	 * 注册所有默认的 Scope 权限处理器
	 */
	public void registerDefaultScopeHandler() {
		scopeHandlerMap.put(CommonScope.OPENID, new OpenIdScopeHandler());
		scopeHandlerMap.put(CommonScope.UNIONID, new UnionIdScopeHandler());
		scopeHandlerMap.put(CommonScope.USERID, new UserIdScopeHandler());
		scopeHandlerMap.put(CommonScope.OIDC, new OidcScopeHandler());
	}

	/**
	 * 注册一个权限处理器
	 */
	public void registerScopeHandler(SaOAuth2ScopeHandlerInterface handler) {
		scopeHandlerMap.put(handler.getHandlerScope(), handler);
		SaManager.getLog().info("自定义 SCOPE [{}] (处理器: {})", handler.getHandlerScope(), handler.getClass().getCanonicalName());
	}

	/**
	 * 移除一个权限处理器
	 */
	public void removeScopeHandler(String scope) {
		scopeHandlerMap.remove(scope);
	}

	/**
	 * 根据 scope 信息对一个 AccessTokenModel 进行加工处理
	 */
	public SaOAuth2ScopeWorkAccessTokenFunction workAccessTokenByScope = (at) -> {
		if(at.scopes != null && !at.scopes.isEmpty()) {
			for (String scope : at.scopes) {
				SaOAuth2ScopeHandlerInterface handler = scopeHandlerMap.get(scope);
				if(handler != null) {
					handler.workAccessToken(at);
				}
			}
		}
		SaOAuth2ScopeHandlerInterface finallyWorkScopeHandler = scopeHandlerMap.get(SaOAuth2Consts._FINALLY_WORK_SCOPE);
		if(finallyWorkScopeHandler != null) {
			finallyWorkScopeHandler.workAccessToken(at);
		}
	};

	/**
	 * 当使用 RefreshToken 刷新 AccessToken 时，根据 scope 信息对一个 AccessTokenModel 进行加工处理
	 */
	public SaOAuth2ScopeWorkAccessTokenFunction refreshAccessTokenWorkByScope = (at) -> {
		if(at.scopes != null && !at.scopes.isEmpty()) {
			for (String scope : at.scopes) {
				SaOAuth2ScopeHandlerInterface handler = scopeHandlerMap.get(scope);
				if(handler != null && handler.refreshAccessTokenIsWork()) {
					handler.workAccessToken(at);
				}
			}
		}
		SaOAuth2ScopeHandlerInterface finallyWorkScopeHandler = scopeHandlerMap.get(SaOAuth2Consts._FINALLY_WORK_SCOPE);
		if(finallyWorkScopeHandler != null && finallyWorkScopeHandler.refreshAccessTokenIsWork()) {
			finallyWorkScopeHandler.workAccessToken(at);
		}
	};

	/**
	 * 根据 scope 信息对一个 ClientTokenModel 进行加工处理
	 */
	public SaOAuth2ScopeWorkClientTokenFunction workClientTokenByScope = (ct) -> {
		if(ct.scopes != null && !ct.scopes.isEmpty()) {
			for (String scope : ct.scopes) {
				SaOAuth2ScopeHandlerInterface handler = scopeHandlerMap.get(scope);
				if(handler != null) {
					handler.workClientToken(ct);
				}
			}
		}
		SaOAuth2ScopeHandlerInterface finallyWorkScopeHandler = scopeHandlerMap.get(SaOAuth2Consts._FINALLY_WORK_SCOPE);
		if(finallyWorkScopeHandler != null) {
			finallyWorkScopeHandler.workClientToken(ct);
		}
	};


	// ------------------ grant_type 处理器 ------------------

	/**
	 * grant_type 处理器集合
	 */
	public Map<String, SaOAuth2GrantTypeHandlerInterface> grantTypeHandlerMap = new LinkedHashMap<>();

	/**
	 * 注册所有默认的 grant_type 处理器
	 */
	public void registerDefaultGrantTypeHandler() {
		grantTypeHandlerMap.put(GrantType.authorization_code, new AuthorizationCodeGrantTypeHandler());
		grantTypeHandlerMap.put(GrantType.password, new PasswordGrantTypeHandler());
		grantTypeHandlerMap.put(GrantType.refresh_token, new RefreshTokenGrantTypeHandler());
	}

	/**
	 * 注册一个 grant_type 处理器
	 */
	public void registerGrantTypeHandler(SaOAuth2GrantTypeHandlerInterface handler) {
		grantTypeHandlerMap.put(handler.getHandlerGrantType(), handler);
		SaManager.getLog().info("自定义 GRANT_TYPE [{}] (处理器: {})", handler.getHandlerGrantType(), handler.getClass().getCanonicalName());
	}

	/**
	 * 移除一个 grant_type 处理器
	 */
	public void removeGrantTypeHandler(String scope) {
		scopeHandlerMap.remove(scope);
	}

	/**
	 * 根据 grantType 构造一个 AccessTokenModel
	 */
	public SaOAuth2GrantTypeAuthFunction grantTypeAuth = (req) -> {
		// 先校验提供的 grant_type 是否有效
		String grantType = req.getParamNotNull(SaOAuth2Consts.Param.grant_type);
		SaOAuth2GrantTypeHandlerInterface grantTypeHandler = grantTypeHandlerMap.get(grantType);
		if(grantTypeHandler == null) {
			throw new SaOAuth2Exception("无效 grant_type: " + grantType).setCode(SaOAuth2ErrorCode.CODE_30126);
		}

		// 针对 authorization_code 与 password 两种特殊 grant_type，需要判断全局是否开启
		SaOAuth2ServerConfig config = SaOAuth2Manager.getServerConfig();
		if(grantType.equals(GrantType.authorization_code) && !config.getEnableAuthorizationCode() ) {
			throw new SaOAuth2Exception("系统未开放的 grant_type: " + grantType).setCode(SaOAuth2ErrorCode.CODE_30126);
		}
		if(grantType.equals(GrantType.password) && !config.getEnablePassword() ) {
			throw new SaOAuth2Exception("系统未开放的 grant_type: " + grantType).setCode(SaOAuth2ErrorCode.CODE_30126);
		}

		// 校验 clientSecret 和 scope
		ClientIdAndSecretModel clientIdAndSecretModel = SaOAuth2Manager.getDataResolver().readClientIdAndSecret(req);
		List<String> scopes = SaOAuth2Manager.getDataConverter().convertScopeStringToList(req.getParam(SaOAuth2Consts.Param.scope));
		SaClientModel clientModel = SaOAuth2Manager.getTemplate().checkClientSecretAndScope(clientIdAndSecretModel.getClientId(), clientIdAndSecretModel.getClientSecret(), scopes);

		// 检测应用是否开启此 grantType
		if(!clientModel.getAllowGrantTypes().contains(grantType)) {
			throw new SaOAuth2Exception("应用未开放的 grant_type: " + grantType).setCode(SaOAuth2ErrorCode.CODE_30141);
		}

		// 调用 处理器构建 Access-Token
		return grantTypeHandler.getAccessToken(req, clientIdAndSecretModel.getClientId(), scopes);
	};


	// ------------------ 凭证创建 ------------------

	/**
	 * 创建一个 code value
	 */
	public SaOAuth2CreateCodeValueFunction createCodeValue = (clientId, loginId, scopes) -> {
		return SaFoxUtil.getRandomString(60);
	};

	/**
	 * 创建一个 AccessToken value
	 */
	public SaOAuth2CreateAccessTokenValueFunction createAccessToken = (clientId, loginId, scopes) -> {
		return SaFoxUtil.getRandomString(60);
	};

	/**
	 * 创建一个 RefreshToken value
	 */
	public SaOAuth2CreateRefreshTokenValueFunction createRefreshToken = (clientId, loginId, scopes) -> {
		return SaFoxUtil.getRandomString(60);
	};

	/**
	 * 创建一个 ClientToken value
	 */
	public SaOAuth2CreateClientTokenValueFunction createClientToken = (clientId, scopes) -> {
		return SaFoxUtil.getRandomString(60);
	};


	// ------------------ 认证流程回调 ------------------

	/**
	 * OAuth-Server端：未登录时返回的View
	 */
	public SaOAuth2NotLoginViewFunction notLoginView = () -> "当前会话在 OAuth-Server 认证中心尚未登录";

	/**
	 * OAuth-Server端：确认授权时返回的View
	 */
	public SaOAuth2ConfirmViewFunction confirmView = (clientId, scopes) -> "本次操作需要用户授权";

	/**
	 * OAuth-Server端：登录函数
	 */
	public SaOAuth2DoLoginHandleFunction doLoginHandle = (name, pwd) -> SaResult.error();

	/**
	 * OAuth-Server端：用户在授权指定 client 前的检查，如果检查不通过，请直接抛出异常
	 */
	public SaTwoParamFunction<Object, String> userAuthorizeClientCheck = (loginId, clientId) -> {

	};


	// ------------------ 其它 ------------------

	/**
	 * 在创建 SaClientModel 时，设置其默认字段
	 */
	public SaParamFunction<SaClientModel> setSaClientModelDefaultFields = (clientModel) -> {
		SaOAuth2ServerConfig config = SaOAuth2Manager.getServerConfig();
		clientModel.accessTokenTimeout = config.getAccessTokenTimeout();
		clientModel.refreshTokenTimeout = config.getRefreshTokenTimeout();
		clientModel.clientTokenTimeout = config.getClientTokenTimeout();
		clientModel.maxAccessTokenCount = config.getMaxAccessTokenCount();
		clientModel.maxRefreshTokenCount = config.getMaxRefreshTokenCount();
		clientModel.maxClientTokenCount = config.getMaxClientTokenCount();
		clientModel.isNewRefresh = config.getIsNewRefresh();
	};

}
