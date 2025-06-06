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
package cn.dev33.satoken.dao.timedcache;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Map 包装类 (ConcurrentHashMap 版)
 *
 * @author click33
 * @since 1.41.0
 */
public class SaMapPackageForConcurrentHashMap<V> implements SaMapPackage<V> {

	private final ConcurrentHashMap<String, V> map = new ConcurrentHashMap<String, V>();

	@Override
	public Object getSource() {
		return map;
	}

	@Override
	public V get(String key) {
		return map.get(key);
	}

	@Override
	public void put(String key, V value) {
		map.put(key, value);
	}

	@Override
	public void remove(String key) {
		map.remove(key);
	}

	@Override
	public Set<String> keySet() {
		return map.keySet();
	}

}
