# 异步 & Mock 上下文

有一些方法（例如`StpUtil.isLogin()`）只可以在同步的 Web 上下文中才可以调用，如果在异步上下文中调用则会抛出异常：

``` text
cn.dev33.satoken.exception.SaTokenContextException: SaTokenContext 上下文尚未初始化
```

这是因为这些方法需要从前端的 `HttpServletRequest` 中读取 Token 参数，而异步上下文通常不是一次“请求”，不具有 `HttpServletRequest` 的概念，所以无法成功调用。

一般哪些场景属于异步上下文？
- 通过 `new Thread(() -> { ... }).start()` 启动子线程。
- 通过 `taskExecutor.execute(() -> { ... })` 线程池启动异步任务。
- 通过 `@Async` 注解标注的方法。
- 通过 `@Scheduled(cron = "")` 启动的定时任务。
- 消息队列中消费消息的函数。
- ...

凡是不通过 web 请求调用触发的线程，在 Sa-Token 中均属于异步上下文，也可以称作 “非 Web 上下文”。

此时调用 `StpUtil.isLogin()`、`StpUtil.getLoginId()` 等需要 Web 上下文的 API，就会抛出上述异常。

如果你需要在 非 Web 上下文 中调用上述 API，则需要手动 mock 一个上下文，才可以调用成功：

例如：
``` java
// 【异步】new Thread  
@RequestMapping("isLogin2")
public SaResult isLogin2() {
	System.out.println("是否登录：" + StpUtil.isLogin());
	String tokenValue = StpUtil.getTokenValue();
	new Thread(() -> {
		SaTokenContextMockUtil.setMockContext(()->{
			StpUtil.setTokenValueToStorage(tokenValue);
			System.out.println("是否登录：" + StpUtil.isLogin());
		});
	}).start();
	return SaResult.data(StpUtil.getTokenValue());
}
```

参考上述方法，你需要先调用 `SaTokenContextMockUtil.setMockContext(() -> { ... })` Mock 出一个 Web 上下文填充到上下文管理器中，
然后在 Mock 上下文范围内调用 `StpUtil.setTokenValueToStorage(tokenValue)` 指定当前上下文的 token 值，其效果等同于在 web 上下文中前端提交了此 token 值。

更多使用姿势请参考仓库示例：[Async-TestController.java](https://gitee.com/dromara/sa-token/blob/master/sa-token-demo/sa-token-demo-async/src/main/java/com/pj/test/TestController.java)

