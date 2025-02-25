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
package cn.dev33.satoken.plugin;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.json.SaJsonTemplateForFastjson2;
import cn.dev33.satoken.session.SaSessionForFastjson2Customized;
import cn.dev33.satoken.strategy.SaStrategy;

/**
 * SaToken 插件安装：JSON 转换器 - Fastjson2 版
 *
 * @author click33
 * @since 1.41.0
 */
public class SaTokenPluginForFastjson2 implements SaTokenPlugin {

    @Override
    public void install() {

        // 设置 JSON 转换器：Fastjson2 版
        SaManager.setSaJsonTemplate(new SaJsonTemplateForFastjson2());

        // 重写 SaSession 生成策略
        SaStrategy.instance.createSession = SaSessionForFastjson2Customized::new;

        // 指定 SaSession 类型
        SaStrategy.instance.sessionClassType = SaSessionForFastjson2Customized.class;

    }

}