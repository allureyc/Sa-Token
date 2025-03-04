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
package cn.dev33.satoken.dao;

import cn.dev33.satoken.dao.auto.SaTokenDaoByObjectFollowString;
import cn.dev33.satoken.util.SaFoxUtil;
import org.noear.redisx.RedisClient;
import org.noear.redisx.plus.RedisBucket;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * SaTokenDao 的 redis 适配（基于json序列化，不能完全精准还原所有类型）
 *
 * @author noear
 * @since 1.34.0
 * @since 1.41.0
 */
public class SaTokenDaoForRedisx implements SaTokenDaoByObjectFollowString {
    private final RedisBucket redisBucket;

    public SaTokenDaoForRedisx(Properties props) {
        this(new RedisClient(props));
    }

    public SaTokenDaoForRedisx(RedisClient redisClient) {
        redisBucket = redisClient.getBucket();
    }

    /**
     * 获取Value，如无返空
     */
    @Override
    public String get(String key) {
        return redisBucket.get(key);
    }

    /**
     * 写入Value，并设定存活时间 (单位: 秒)
     */
    @Override
    public void set(String key, String value, long timeout) {
        if (timeout == 0 || timeout <= SaTokenDao.NOT_VALUE_EXPIRE) {
            return;
        }

        // 判断是否为永不过期
        if (timeout == SaTokenDao.NEVER_EXPIRE) {
            redisBucket.store(key, value);
        } else {
            redisBucket.store(key, value, (int) timeout);
        }
    }

    /**
     * 修改指定key-value键值对 (过期时间不变)
     */
    @Override
    public void update(String key, String value) {
        long expire = getTimeout(key);
        // -2 = 无此键
        if (expire == SaTokenDao.NOT_VALUE_EXPIRE) {
            return;
        }

        this.set(key, value, expire);
    }

    /**
     * 删除Value
     */
    @Override
    public void delete(String key) {
        redisBucket.remove(key);
    }

    /**
     * 获取Value的剩余存活时间 (单位: 秒)
     */
    @Override
    public long getTimeout(String key) {
        return redisBucket.ttl(key);
    }

    /**
     * 修改Value的剩余存活时间 (单位: 秒)
     */
    @Override
    public void updateTimeout(String key, long timeout) {
        // 判断是否想要设置为永久
        if (timeout == SaTokenDao.NEVER_EXPIRE) {
            long expire = getTimeout(key);
            if (expire == SaTokenDao.NEVER_EXPIRE) {
                // 如果其已经被设置为永久，则不作任何处理
            } else {
                // 如果尚未被设置为永久，那么再次set一次
                this.set(key, this.get(key), timeout);
            }
            return;
        }


        redisBucket.delay(key, (int) timeout);
    }

    /**
     * 搜索数据
     */
    @Override
    public List<String> searchData(String prefix, String keyword, int start, int size, boolean sortType) {
        Set<String> keys = redisBucket.keys(prefix + "*" + keyword + "*");
        List<String> list = new ArrayList<>(keys);
        return SaFoxUtil.searchList(list, start, size, sortType);
    }
}