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
package cn.dev33.satoken.oauth2.dao;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.fun.SaParamFunction;
import cn.dev33.satoken.oauth2.SaOAuth2Manager;
import cn.dev33.satoken.oauth2.data.model.AccessTokenModel;
import cn.dev33.satoken.oauth2.data.model.ClientTokenModel;
import cn.dev33.satoken.oauth2.data.model.CodeModel;
import cn.dev33.satoken.oauth2.data.model.RefreshTokenModel;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.session.raw.SaRawSessionDelegator;
import cn.dev33.satoken.util.SaFoxUtil;

import java.util.*;

import static cn.dev33.satoken.oauth2.template.SaOAuth2Util.checkClientModel;

/**
 * Sa-Token OAuth2 数据持久层
 *
 * @author click33
 * @since 1.39.0
 */
public class SaOAuth2Dao {

	// ------------------- 索引操作公共代码

	/**
	 * Raw Session 读写委托
	 */
	public SaRawSessionDelegator oauth2RSD = new SaRawSessionDelegator("oauth2");

	/**
	 * 在 raw-session 中的保存 Access-Token 索引列表使用的 key
	 */
	public static final String ACCESS_TOKEN_MAP = "__HD_ACCESS_TOKEN_MAP";

	/**
	 * 在 raw-session 中的保存 Refresh-Token 索引列表使用的 key
	 */
	public static final String REFRESH_TOKEN_MAP = "__HD_REFRESH_TOKEN_MAP";

	/**
	 * 在 raw-session 中的保存 Client-Token 索引列表使用的 key
	 */
	public static final String CLIENT_TOKEN_MAP = "__HD_CLIENT_TOKEN_MAP";

	/**
	 * 获取 Access-Token 索引 RawSession
	 * @param clientId 应用 id
	 * @param loginId 账号 id
	 * @param isCreate 如果尚未创建，是否理解创建
	 * @return /
	 */
	protected SaSession getRawSessionByAccessToken(String clientId, Object loginId, boolean isCreate) {
		String value = splicingAccessTokenRSDValue(clientId, loginId);
		return oauth2RSD.getSessionById(value, isCreate);
	}

	/**
	 * 获取 refresh_token 索引 RawSession
	 * @param clientId 应用 id
	 * @param loginId 账号 id
	 * @param isCreate 如果尚未创建，是否理解创建
	 * @return /
	 */
	protected SaSession getRawSessionByRefreshToken(String clientId, Object loginId, boolean isCreate) {
		String value = splicingRefreshTokenRSDValue(clientId, loginId);
		return oauth2RSD.getSessionById(value, isCreate);
	}

	/**
	 * 获取 client_token 索引 RawSession
	 * @param clientId 应用 id
	 * @param isCreate 如果尚未创建，是否理解创建
	 * @return /
	 */
	protected SaSession getRawSessionByClientToken(String clientId, boolean isCreate) {
		String value = splicingClientTokenRSDValue(clientId);
		return oauth2RSD.getSessionById(value, isCreate);
	}

	/**
	 * 在 RawSession 上添加 token 索引
	 *
	 * @param session 待操作的 RawSession
	 * @param tokenIndexMapSaveKey 在 session 上保存 token 索引列表使用的 key
	 * @param token 待添加的 token
	 * @param timeout 添加的 token 其过期时间
	 * @param maxTokenCount 允许的最多 token 数量，超出的将被删除 (-1=不限制)
	 * @param removeFun 执行删除 token 的函数
	 */
	protected void addTokenIndex(SaSession session, String tokenIndexMapSaveKey, String token, long timeout, int maxTokenCount, SaParamFunction<String> removeFun) {
		Map<String, Long> tokenIndexMap = session.get(tokenIndexMapSaveKey, this::newTokenIndexMap);
		if(! tokenIndexMap.containsKey(token)) {
			// 添加
			tokenIndexMap.put(token, ttlToExpireTime(timeout));
			// 剔除过期的
			tokenIndexMap = removeExpiredIndex(tokenIndexMap);
			// 删掉溢出的
			tokenIndexMap = removeOverflowIndex(tokenIndexMap, maxTokenCount, removeFun);
			// 保存
			session.set(tokenIndexMapSaveKey, tokenIndexMap);
			// 更新 TTL
			long maxTtl = getMaxTtl(tokenIndexMap.values());
			if(maxTtl != 0) {
				session.updateTimeout(maxTtl);
			}
		}
	}

	/**
	 * 在 RawSession 上删除 token 索引
	 * @param session 待操作的 RawSession
	 * @param tokenIndexMapSaveKey 在 session 上保存 token 索引列表使用的 key
	 * @param token 待删除的 token
	 */
	protected void deleteTokenIndex(SaSession session, String tokenIndexMapSaveKey, String token) {
		Map<String, Long> tokenIndexMap = session.get(tokenIndexMapSaveKey, this::newTokenIndexMap);
		tokenIndexMap.remove(token);
		// 如果删除后还有记录，就再次保存
		if( ! tokenIndexMap.isEmpty()) {
			session.set(tokenIndexMapSaveKey, tokenIndexMap);
		} else {
			// 没有的话就直接注销此 RawSession
			session.logout();
		}
	}

	/**
	 * 剔除已过期的 token 索引
	 *
	 * @param tokenIndexMap token 索引列表
	 * @return 调整后的索引列表
	 */
	protected Map<String, Long> removeExpiredIndex(Map<String, Long> tokenIndexMap) {
		Map<String, Long>  newTokenList = newTokenIndexMap();
		for (Map.Entry<String, Long> entry : tokenIndexMap.entrySet()) {
			long ttl = expireTimeToTtl(entry.getValue());
			if(ttl != SaTokenDao.NOT_VALUE_EXPIRE) {
				newTokenList.put(entry.getKey(), entry.getValue());
			}
		}
		return newTokenList;
	}

	/**
	 * 将 token 索引列表中溢出的部分删除（按照插入顺序先进先出，不考虑每个剩余 token 剩余有效期）
	 *
	 * @param tokenIndexMap token 索引列表(key=token, value=token过期时间)（传入的 Map 必须是有序的）
	 * @param maxTokenCount 允许的最多 token 数量，超出的将被删除 (-1=不限制)
	 * @param removeFun 执行删除 token 的函数
	 * @return 调整后的索引列表
	 */
	protected Map<String, Long> removeOverflowIndex(Map<String, Long> tokenIndexMap, int maxTokenCount, SaParamFunction<String> removeFun) {

		// 如果当前数量未超过限制，直接返回
		if (tokenIndexMap.size() <= maxTokenCount || maxTokenCount == SaTokenDao.NEVER_EXPIRE) {
			return tokenIndexMap;
		}

		// 创建新的索引 Map 副本
		Map<String, Long> newTokenIndexMap = newTokenIndexMap();

		// 溢出数量
		int overflowCount = tokenIndexMap.size() - maxTokenCount;

		// 已删除 Token 数量
		int removedCount = 0;

		// 遍历原 Map 的所有条目
		for (Map.Entry<String, Long> entry : tokenIndexMap.entrySet()) {
			String token = entry.getKey();
			if (removedCount < overflowCount) {
				// 溢出部分：执行删除回调，但不添加到新 Map
				removeFun.run(token);
				removedCount++;
			} else {
				// 未溢出部分：添加到新 Map 副本
				newTokenIndexMap.put(token, entry.getValue());
			}
		}

		// 返回索引 Map 副本
		return newTokenIndexMap;
	}

	/**
	 * 获取 Token 列表
	 *
	 * @param session 待操作的 RawSession
	 * @param tokenIndexMapSaveKey 在 session 上保存 token 索引列表使用的 key
	 * @return /
	 */
	protected List<String> getTokenValueList(SaSession session, String tokenIndexMapSaveKey) {
		if(session == null) {
			return new ArrayList<>();
		}

		// 根据 ttl 值过滤一遍
		Map<String, Long> tokenIndexMap = session.get(tokenIndexMapSaveKey, this::newTokenIndexMap);
		Map<String, Long> newTokenIndexMap = removeExpiredIndex(tokenIndexMap);

		// 如果调整后集合长度归零了，说明 token 已全部过期，直接注销此 RawSession
		if(newTokenIndexMap.isEmpty()) {
			session.logout();
			return new ArrayList<>();
		}

		// 没有归零，但是长度变小了，说明有过期的 token，需要重写写入一遍
		if(tokenIndexMap.size() > newTokenIndexMap.size()) {
			session.set(tokenIndexMapSaveKey, newTokenIndexMap);
		}

		// 转 List 返回
		return new ArrayList<>(newTokenIndexMap.keySet());
	}


	// ------------------- code 操作

	/**
	 * 持久化：Code-Model
	 * @param c .
	 */
	public void saveCode(CodeModel c) {
		if(c == null) {
			return;
		}
		getSaTokenDao().setObject(splicingCodeSaveKey(c.code), c, SaOAuth2Manager.getServerConfig().getCodeTimeout());
	}

	/**
	 * 删除：Code
	 * @param code 值
	 */
	public void deleteCode(String code) {
		if(code != null) {
			getSaTokenDao().deleteObject(splicingCodeSaveKey(code));
		}
	}

	/**
	 * 获取：Code Model
	 * @param code .
	 * @return .
	 */
	public CodeModel getCode(String code) {
		if(code == null) {
			return null;
		}
		return (CodeModel)getSaTokenDao().getObject(splicingCodeSaveKey(code));
	}


	// ------------------- code 索引

	/**
	 * 持久化：Code-索引
	 * @param c .
	 */
	public void saveCodeIndex(CodeModel c) {
		if(c == null) {
			return;
		}
		getSaTokenDao().set(splicingCodeIndexKey(c.clientId, c.loginId), c.code, SaOAuth2Manager.getServerConfig().getCodeTimeout());
	}

	/**
	 * 删除：Code索引
	 * @param clientId 应用id
	 * @param loginId 账号id
	 */
	public void deleteCodeIndex(String clientId, Object loginId) {
		getSaTokenDao().delete(splicingCodeIndexKey(clientId, loginId));
	}

	/**
	 * 获取：Code Value
	 * @param clientId 应用id
	 * @param loginId 账号id
	 * @return .
	 */
	public String getCodeValue(String clientId, Object loginId) {
		return getSaTokenDao().get(splicingCodeIndexKey(clientId, loginId));
	}


	// ------------------- access_token Model

	/**
	 * 持久化：AccessToken-Model
	 * @param at .
	 */
	public void saveAccessToken(AccessTokenModel at) {
		if(at == null) {
			return;
		}
		getSaTokenDao().setObject(splicingAccessTokenSaveKey(at.accessToken), at, at.getExpiresIn());
	}

	/**
	 * 删除：Access-Token
	 * @param accessToken 值
	 */
	public void deleteAccessToken(String accessToken) {
		if(accessToken != null) {
			getSaTokenDao().deleteObject(splicingAccessTokenSaveKey(accessToken));
		}
	}

	/**
	 * 获取：Access-Token Model
	 * @param accessToken .
	 * @return .
	 */
	public AccessTokenModel getAccessToken(String accessToken) {
		if(accessToken == null) {
			return null;
		}
		return (AccessTokenModel)getSaTokenDao().getObject(splicingAccessTokenSaveKey(accessToken));
	}


	// ------------------- access_token 索引

	/**
	 * 持久化：Access-Token 索引
	 *
	 * @param at /
	 * @param maxAccessTokenCount 允许的最多 Access-Token 数量，超出的将被删除 (-1=不限制)
	 */
	public void saveAccessTokenIndex(AccessTokenModel at, int maxAccessTokenCount) {
		if(at == null) {
			return;
		}
		SaSession session = getRawSessionByAccessToken(at.clientId, at.loginId, true);
		addTokenIndex(session, ACCESS_TOKEN_MAP, at.accessToken, at.getExpiresIn(), maxAccessTokenCount, this::deleteAccessToken);
	}

	/**
	 * 删除：Access-Token 在 RawSession 上的单个索引数据
	 *
	 * @param clientId 应用 id
	 * @param loginId 账号id
	 * @param accessToken 值
	 */
	public void deleteAccessTokenIndexBySingleData(String clientId, Object loginId, String accessToken) {
		SaSession session = getRawSessionByAccessToken(clientId, loginId, false);
		if(session == null) {
			return;
		}
		deleteTokenIndex(session, ACCESS_TOKEN_MAP, accessToken);
	}

	/**
	 * 删除：Access-Token 索引整体
	 * @param clientId 应用id
	 * @param loginId 账号id
	 */
	public void deleteAccessTokenIndex(String clientId, Object loginId) {
		oauth2RSD.deleteSessionById(splicingAccessTokenRSDValue(clientId, loginId));
	}

	/**
	 * 获取 Access-Token 列表：此应用下 对 某个用户 签发的所有 Access-token
	 *
	 * @param clientId 应用id
	 * @param loginId 账号id
	 * @return /
	 */
	public List<String> getAccessTokenValueList(String clientId, Object loginId) {
		SaSession session = getRawSessionByAccessToken(clientId, loginId, false);
		return getTokenValueList(session, ACCESS_TOKEN_MAP);
	}


	// ------------------- refresh_token Model

	/**
	 * 持久化：RefreshToken-Model
	 * @param rt .
	 */
	public void saveRefreshToken(RefreshTokenModel rt) {
		if(rt == null) {
			return;
		}
		getSaTokenDao().setObject(splicingRefreshTokenSaveKey(rt.refreshToken), rt, rt.getExpiresIn());
	}

	/**
	 * 删除：Refresh-Token
	 * @param refreshToken 值
	 */
	public void deleteRefreshToken(String refreshToken) {
		if(refreshToken != null) {
			getSaTokenDao().deleteObject(splicingRefreshTokenSaveKey(refreshToken));
		}
	}

	/**
	 * 获取：Refresh-Token Model
	 * @param refreshToken .
	 * @return .
	 */
	public RefreshTokenModel getRefreshToken(String refreshToken) {
		if(refreshToken == null) {
			return null;
		}
		return (RefreshTokenModel)getSaTokenDao().getObject(splicingRefreshTokenSaveKey(refreshToken));
	}


	// ------------------- refresh_token 索引

	/**
	 * 持久化：Refresh-Token 索引
	 *
	 * @param rt /
	 * @param maxRefreshTokenCount 允许的最多 Refresh-Token 数量，超出的将被删除 (-1=不限制)
	 */
	public void saveRefreshTokenIndex(RefreshTokenModel rt, int maxRefreshTokenCount) {
		if(rt == null) {
			return;
		}
		SaSession session = getRawSessionByRefreshToken(rt.clientId, rt.loginId, true);
		addTokenIndex(session, REFRESH_TOKEN_MAP, rt.refreshToken, rt.getExpiresIn(), maxRefreshTokenCount, this::deleteRefreshToken);
	}

	/**
	 * 删除：Refresh-Token 在 RawSession 上的单个索引数据
	 *
	 * @param clientId 应用 id
	 * @param loginId 账号id
	 * @param refreshToken 值
	 */
	public void deleteRefreshTokenIndexBySingleData(String clientId, Object loginId, String refreshToken) {
		SaSession session = getRawSessionByRefreshToken(clientId, loginId, false);
		if(session == null) {
			return;
		}
		deleteTokenIndex(session, REFRESH_TOKEN_MAP, refreshToken);
	}

	/**
	 * 删除：Refresh-Token 索引整体
	 * @param clientId 应用id
	 * @param loginId 账号id
	 */
	public void deleteRefreshTokenIndex(String clientId, Object loginId) {
		oauth2RSD.deleteSessionById(splicingRefreshTokenRSDValue(clientId, loginId));
	}

	/**
	 * 获取 Refresh-Token 列表：此应用下 对 某个用户 签发的所有 Refresh-token
	 *
	 * @param clientId 应用id
	 * @param loginId 账号id
	 * @return /
	 */
	public List<String> getRefreshTokenValueList(String clientId, Object loginId) {
		SaSession session = getRawSessionByRefreshToken(clientId, loginId, false);
		return getTokenValueList(session, REFRESH_TOKEN_MAP);
	}


	// ------------------- client_token Model

	/**
	 * 持久化：ClientToken-Model
	 * @param ct .
	 */
	public void saveClientToken(ClientTokenModel ct) {
		if(ct == null) {
			return;
		}
		getSaTokenDao().setObject(splicingClientTokenSaveKey(ct.clientToken), ct, ct.getExpiresIn());
	}

	/**
	 * 删除：Client-Token
	 * @param clientToken 值
	 */
	public void deleteClientToken(String clientToken) {
		if(clientToken != null) {
			getSaTokenDao().deleteObject(splicingClientTokenSaveKey(clientToken));
		}
	}

	/**
	 * 获取：Client-Token Model
	 * @param clientToken .
	 * @return .
	 */
	public ClientTokenModel getClientToken(String clientToken) {
		if(clientToken == null) {
			return null;
		}
		return (ClientTokenModel)getSaTokenDao().getObject(splicingClientTokenSaveKey(clientToken));
	}


	// ------------------- client_token 索引

	/**
	 * 持久化：Client-Token 索引
	 *
	 * @param ct /
	 * @param maxClientTokenCount 允许的最多 Client-Token 数量，超出的将被删除 (-1=不限制)
	 */
	public void saveClientTokenIndex(ClientTokenModel ct, int maxClientTokenCount) {
		if(ct == null) {
			return;
		}
		SaSession session = getRawSessionByClientToken(ct.clientId, true);
		addTokenIndex(session, CLIENT_TOKEN_MAP, ct.clientToken, ct.getExpiresIn(), maxClientTokenCount, this::deleteClientToken);
	}

	/**
	 * 删除：Client-Token 在 RawSession 上的单个索引数据
	 * @param clientId 应用 id
	 * @param clientToken 值
	 */
	public void deleteClientTokenIndexBySingleData(String clientId, String clientToken) {
		SaSession session = getRawSessionByClientToken(clientId, false);
		if(session == null) {
			return;
		}
		deleteTokenIndex(session, CLIENT_TOKEN_MAP, clientToken);
	}

	/**
	 * 删除：Client-Token 索引整体
	 *
	 * @param clientId 应用id
	 */
	public void deleteClientTokenIndex(String clientId) {
		oauth2RSD.deleteSessionById(splicingClientTokenRSDValue(clientId));
	}

	/**
	 * 获取 Client-Token 列表：此应用下 对 某个用户 签发的所有 Client-token
	 *
	 * @param clientId 应用id
	 * @return /
	 */
	public List<String> getClientTokenValueList(String clientId) {
		SaSession session = getRawSessionByClientToken(clientId, false);
		return getTokenValueList(session, CLIENT_TOKEN_MAP);
	}


	// ------------------- GrantScope

	/**
	 * 持久化：用户授权记录
	 * @param clientId 应用id
	 * @param loginId 账号id
	 * @param scopes 权限列表
	 */
	public void saveGrantScope(String clientId, Object loginId, List<String> scopes) {
		if( ! SaFoxUtil.isEmpty(scopes)) {
			long ttl = checkClientModel(clientId).getAccessTokenTimeout();
			String value = SaOAuth2Manager.getDataConverter().convertScopeListToString(scopes);
			getSaTokenDao().set(splicingGrantScopeKey(clientId, loginId), value, ttl);
		}
	}

	/**
	 * 删除：用户授权记录
	 * @param clientId 应用id
	 * @param loginId 账号id
	 */
	public void deleteGrantScope(String clientId, Object loginId) {
		getSaTokenDao().delete(splicingGrantScopeKey(clientId, loginId));
	}

	/**
	 * 获取：用户授权记录
	 * @param clientId 应用id
	 * @param loginId 账号id
	 * @return 权限
	 */
	public List<String> getGrantScope(String clientId, Object loginId) {
		String value = getSaTokenDao().get(splicingGrantScopeKey(clientId, loginId));
		return SaOAuth2Manager.getDataConverter().convertScopeStringToList(value);
	}


	// ------------------- State

	/**
	 * 持久化：state
	 * @param state /
	 */
	public void saveState(String state) {
		if( ! SaFoxUtil.isEmpty(state)) {
			long ttl = SaOAuth2Manager.getServerConfig().getCodeTimeout();
			getSaTokenDao().set(splicingStateSaveKey(state), state, ttl);
		}
	}

	/**
	 * 删除：state记录
	 * @param state /
	 */
	public void deleteState(String state) {
		getSaTokenDao().delete(splicingStateSaveKey(state));
	}

	/**
	 * 获取：state
	 * @param state /
	 * @return /
	 */
	public String getState(String state) {
		if(SaFoxUtil.isEmpty(state)) {
			return null;
		}
		return getSaTokenDao().get(splicingStateSaveKey(state));
	}


	// ------------------- 其它

	/**
	 * 持久化：nonce-索引
	 * @param c .
	 */
	public void saveCodeNonceIndex(CodeModel c) {
		if(c == null || SaFoxUtil.isEmpty(c.nonce)) {
			return;
		}
		getSaTokenDao().set(splicingCodeNonceIndexSaveKey(c.code), c.nonce, SaOAuth2Manager.getServerConfig().getCodeTimeout());
	}

	/**
	 * 获取：nonce
	 * @param code /
	 * @return /
	 */
	public String getNonce(String code) {
		if(SaFoxUtil.isEmpty(code)) {
			return null;
		}
		return getSaTokenDao().get(splicingCodeNonceIndexSaveKey(code));
	}


	// ------------------- 拼接key

	/**
	 * 拼接key：Code持久化
	 * @param code 授权码
	 * @return key
	 */
	public String splicingCodeSaveKey(String code) {
		return getSaTokenConfig().getTokenName() + ":oauth2:code:" + code;
	}

	/**
	 * 拼接key：Code 索引
	 * @param clientId 应用id
	 * @param loginId 账号id
	 * @return key
	 */
	public String splicingCodeIndexKey(String clientId, Object loginId) {
		return getSaTokenConfig().getTokenName() + ":oauth2:code-index:" + clientId + ":" + loginId;
	}

	/**
	 * 拼接key：Access-Token持久化
	 * @param accessToken accessToken
	 * @return key
	 */
	public String splicingAccessTokenSaveKey(String accessToken) {
		return getSaTokenConfig().getTokenName() + ":oauth2:access-token:" + accessToken;
	}

	/**
	 * 拼接key：Access-Token RSD Value
	 * @param clientId 应用id
	 * @param loginId 账号id
	 * @return key
	 */
	public String splicingAccessTokenRSDValue(String clientId, Object loginId) {
		return "access-token:" + clientId + ":" + loginId;
	}

	/**
	 * 拼接key：Refresh-Token RSD Value
	 * @param clientId 应用id
	 * @param loginId 账号id
	 * @return key
	 */
	public String splicingRefreshTokenRSDValue(String clientId, Object loginId) {
		return "refresh-token:" + clientId + ":" + loginId;
	}

	/**
	 * 拼接key：Client-Token RSD Value
	 * @param clientId 应用id
	 * @return key
	 */
	public String splicingClientTokenRSDValue(String clientId) {
		return "client-token:" + clientId;
	}

	/**
	 * 拼接key：Refresh-Token持久化
	 * @param refreshToken refreshToken
	 * @return key
	 */
	public String splicingRefreshTokenSaveKey(String refreshToken) {
		return getSaTokenConfig().getTokenName() + ":oauth2:refresh-token:" + refreshToken;
	}

	/**
	 * 拼接key：Client-Token持久化
	 * @param clientToken clientToken
	 * @return key
	 */
	public String splicingClientTokenSaveKey(String clientToken) {
		return getSaTokenConfig().getTokenName() + ":oauth2:client-token:" + clientToken;
	}

	/**
	 * 拼接key：用户授权记录
	 * @param clientId 应用id
	 * @param loginId 账号id
	 * @return key
	 */
	public String splicingGrantScopeKey(String clientId, Object loginId) {
		return getSaTokenConfig().getTokenName() + ":oauth2:grant-scope:" + clientId + ":" + loginId;
	}

	/**
	 * 拼接key：state 参数持久化
	 * @param state /
	 * @return key
	 */
	public String splicingStateSaveKey(String state) {
		return getSaTokenConfig().getTokenName() + ":oauth2:state:" + state;
	}

	/**
	 * 拼接key：code-nonce 索引 参数持久化
	 * @param code 授权码
	 * @return key
	 */
	public String splicingCodeNonceIndexSaveKey(String code) {
		return getSaTokenConfig().getTokenName() + ":oauth2:code-nonce-index:" + code;
	}


	// -------- 工具方法

	/**
	 * 获取一个新的 TokenMap 集合
	 * @return /
	 */
	protected Map<String, Long> newTokenIndexMap() {
		return new LinkedHashMap<>();
	}

	/**
	 * 过期时间转 ttl (秒) 获取最大 ttl 值
	 * @param expireTimeList /
	 * @return /
	 */
	protected long getMaxTtl(Collection<Long> expireTimeList) {
		long maxTtl = 0;
		for (long expireTime : expireTimeList) {
			long ttl = expireTimeToTtl(expireTime);
			if(ttl == SaTokenDao.NEVER_EXPIRE) {
				maxTtl = SaTokenDao.NEVER_EXPIRE;
				break;
			}
			if(ttl > maxTtl) {
				maxTtl = ttl;
			}
		}
		return maxTtl;
	}

	/**
	 * 过期时间转 ttl (秒)
	 * @param expireTime /
	 * @return /
	 */
	protected long expireTimeToTtl(long expireTime) {
		if(expireTime == SaTokenDao.NEVER_EXPIRE) {
			return SaTokenDao.NEVER_EXPIRE;
		}
		if(expireTime == SaTokenDao.NOT_VALUE_EXPIRE) {
			return SaTokenDao.NOT_VALUE_EXPIRE;
		}
		// TODO temp-token 模块与 apikey 模块是否也应该修改为这个逻辑 ？
		long currentTime = System.currentTimeMillis();
		if(expireTime < currentTime) {
			return SaTokenDao.NOT_VALUE_EXPIRE;
		}
		return (expireTime - currentTime) / 1000;
	}

	/**
	 * ttl (秒) 转 过期时间
	 * @param ttl /
	 * @return /
	 */
	protected long ttlToExpireTime(long ttl) {
		if(ttl == SaTokenDao.NEVER_EXPIRE) {
			return SaTokenDao.NEVER_EXPIRE;
		}
		if(ttl == SaTokenDao.NOT_VALUE_EXPIRE) {
			return SaTokenDao.NOT_VALUE_EXPIRE;
		}
		return ttl * 1000 + System.currentTimeMillis();
	}



	// -------- bean 对象代理

	/**
	 * 获取使用的 getSaTokenDao 实例
	 * 
	 * @return /
	 */
	public SaTokenDao getSaTokenDao() {
		return SaManager.getSaTokenDao();
	}

	/**
	 * 获取使用的 SaTokenConfig 实例
	 *
	 * @return /
	 */
	public SaTokenConfig getSaTokenConfig() {
		return SaManager.getConfig();
	}

}
