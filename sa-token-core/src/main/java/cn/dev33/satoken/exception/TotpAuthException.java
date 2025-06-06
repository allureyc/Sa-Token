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
package cn.dev33.satoken.exception;

/**
 * 一个异常：代表 TOTP 校验未通过
 *
 * @author click33
 * @since 1.41.0
 */
public class TotpAuthException extends SaTokenException {

	/**
	 * 序列化版本号
	 */
	private static final long serialVersionUID = 6806129545290130144L;

	/** 异常提示语 */
	public static final String BE_MESSAGE = "totp check fail";

	/**
	 * 一个异常：代表会话未通过 Totp 校验
	 */
	public TotpAuthException() {
		super(BE_MESSAGE);
	}

}
