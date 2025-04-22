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
package cn.dev33.satoken.session;

import cn.dev33.satoken.util.SaFoxUtil;
import org.noear.snack.ONode;

/**
 * Fastjson 定制版 SaSession，重写类型转换API
 * 
 * @author click33
 * @author noear
 * @since 1.34.0
 */
public class SaSessionForSnack3Customized extends SaSession {

	private static final long serialVersionUID = -7600983549653130681L;

	/**
	 * 构建一个 SaSession 对象
	 */
	public SaSessionForSnack3Customized() {
		super();
	}

	/**
	 * 构建一个 SaSession 对象
	 *
	 * @param id Session 的 id
	 */
	public SaSessionForSnack3Customized(String id) {
		super(id);
	}

	/**
	 * 取值 (指定转换类型)
	 *
	 * @param <T> 泛型
	 * @param key key
	 * @param cs  指定转换类型
	 * @return 值
	 */
	@Override
	public <T> T getModel(String key, Class<T> cs) {
		// 如果是想取出为基础类型
		Object value = get(key);
		if (SaFoxUtil.isBasicType(cs)) {
			return SaFoxUtil.getValueByType(value, cs);
		}
		// 为空提前返回
		if (valueIsNull(value)) {
			return null;
		}
		// 如果是 JSONObject 类型直接转，否则先转为 String 再转
		if (value instanceof ONode) {
			ONode jo = (ONode) value;
			return jo.toObject(cs);
		} else if (value instanceof String) {
			return ONode.deserialize((String) value, cs);
		} else {
			//有可能是 Map
			return ONode.load(value).toObject(cs);
		}
	}
}
