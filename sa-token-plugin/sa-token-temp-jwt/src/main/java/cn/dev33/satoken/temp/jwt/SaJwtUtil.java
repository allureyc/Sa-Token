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
package cn.dev33.satoken.temp.jwt;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.exception.SaTokenException;
import cn.dev33.satoken.secure.SaSecureUtil;
import cn.dev33.satoken.temp.jwt.error.SaTempJwtErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

/**
 * jwt 相关操作工具类，封装一下
 *
 * @author click33
 * @since 1.20.0
 */
public class SaJwtUtil {
	
	/**
	 * key: value 前缀 
	 */
	public static final String KEY_VALUE = "value_"; 

	/**
	 * key: 有效期 (时间戳)
	 */
	public static final String KEY_EFF = "eff"; 

	/** 当有效期被设为此值时，代表永不过期 */ 
	public static final long NEVER_EXPIRE = SaTokenDao.NEVER_EXPIRE;
	
	/**
	 * 根据指定值创建 jwt-token
	 *
	 * @param value 要保存的值
	 * @param timeout token有效期 (单位 秒)
     * @param keyt 秘钥
	 * @return jwt-token 
	 */
    public static String createToken(Object value, long timeout, String keyt) {
    	// 计算eff有效期：
		// 		如果 timeout 指定为 -1，那么 eff 也为 -1，代表永不过期
		// 		如果 timeout 指定为一个具体的值，那么 eff 为 13 位时间戳，代表此数据到期的时间
    	long eff = timeout;
    	if(timeout != NEVER_EXPIRE) {
    		eff = timeout * 1000 + System.currentTimeMillis();
    	}

    	// 在这里你可以使用官方提供的claim方法构建载荷，也可以使用setPayload自定义载荷，但是两者不可一起使用
		SecretKey key = Keys.hmacShaKeyFor(SaSecureUtil.md5(keyt).getBytes());
		JwtBuilder builder = Jwts.builder()
				.header().add("typ", "JWT").and()
        		.claim(KEY_VALUE, value)
        		.claim(KEY_EFF, eff)
                .signWith(key);

        // 生成jwt-token 
        return builder.compact();
    }

    /**
     * 从一个 jwt-token 解析出载荷 
     * @param jwtToken JwtToken值 
     * @param keyt 秘钥
     * @return Claims对象 
     */
    public static Claims parseToken(String jwtToken, String keyt) {
    	// 解析出载荷
		SecretKey key = Keys.hmacShaKeyFor(SaSecureUtil.md5(keyt).getBytes());
        return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(jwtToken).getPayload();
    }

    /**
     * 从一个 jwt-token 解析出载荷, 并取出数据
     * @param jwtToken JwtToken值 
     * @param keyt 秘钥
     * @return 值 
     */
    public static Object getValue(String jwtToken, String keyt) {
    	// 取出数据 
    	Claims claims = parseToken(jwtToken, keyt);
    	
    	// 验证是否超时 
    	Long eff = claims.get(KEY_EFF, Long.class);
    	if(eff == null || (eff < System.currentTimeMillis() && eff != NEVER_EXPIRE)) {
    		throw new SaTokenException("token 已超时，无法解析：" + jwtToken).setCode(SaTempJwtErrorCode.CODE_30303);
    	}
    	
        // 获取数据 
        return claims.get(KEY_VALUE);
    }

    /**
     * 从一个 jwt-token 解析出载荷, 并取出其剩余有效期
     * @param jwtToken JwtToken值 
     * @param keyt 秘钥
     * @return 值 
     */
    public static long getTimeout(String jwtToken, String keyt) {
    	// 取出数据 
    	Claims claims = parseToken(jwtToken, keyt);

    	// 验证是否超时 
    	Long eff = claims.get(KEY_EFF, Long.class);
    	
    	// 永不过期 
    	if(eff == NEVER_EXPIRE) {
    		return NEVER_EXPIRE;
    	}
    	// 已经超时 
    	if(eff < System.currentTimeMillis()) {
    		return SaTokenDao.NOT_VALUE_EXPIRE;
    	}
    	
        // 计算timeout 
        return (eff - System.currentTimeMillis()) / 1000;
    }
    
}
