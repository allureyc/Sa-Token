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
package cn.dev33.satoken.fun.strategy;

import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.context.model.SaResponse;

/**
 * 函数式接口：防火墙校验函数
 *
 * @author click33
 * @since 1.37.0
 */
@FunctionalInterface
public interface SaFirewallCheckFunction {

    /**
     * 执行函数
     *
     * @param req 请求对象
     * @param res 响应对象
     * @param extArg 预留扩展参数
     */
    void execute(SaRequest req, SaResponse res, Object extArg);

}