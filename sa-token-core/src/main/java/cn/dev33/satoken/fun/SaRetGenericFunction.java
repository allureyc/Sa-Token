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
package cn.dev33.satoken.fun;

/**
 * 无形参、有返回值(泛型)的函数式接口，方便开发者进行 lambda 表达式风格调用
 *
 * @author click33
 * @since 1.42.0
 */
@FunctionalInterface
public interface SaRetGenericFunction<T> {
	
	/**
	 * 执行的方法 
	 * @return 返回值 
	 */
	T run();
	
}
