package com.pj;

import cn.dev33.satoken.SaManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * Sa-Token 测试  
 * @author click33
 *
 */
@SpringBootApplication
public class SaTokenSseApplication {

	// SSE 连接测试在线工具：https://toolshu.com/sse

	public static void main(String[] args) {
		SpringApplication.run(SaTokenSseApplication.class, args);
		System.out.println("\n启动成功：Sa-Token配置如下：" + SaManager.getConfig());
	}

}
