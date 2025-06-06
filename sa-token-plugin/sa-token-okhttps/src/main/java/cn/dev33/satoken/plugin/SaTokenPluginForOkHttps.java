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
import cn.dev33.satoken.http.SaHttpTemplateForOkHttps;

/**
 * SaToken 插件安装：Http 请求处理器 - OkHttps 版
 *
 * @author click33
 * @since 1.43.0
 */
public class SaTokenPluginForOkHttps implements SaTokenPlugin {

    @Override
    public void install() {
        // 设置 OkHttps 作为 Http 请求处理器
        SaManager.setSaHttpTemplate(new SaHttpTemplateForOkHttps());
    }

}