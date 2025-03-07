package com.oauth.authentication.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.code.AuthorizationCodeServices;
import org.springframework.security.oauth2.provider.code.InMemoryAuthorizationCodeServices;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.InMemoryTokenStore;

/**
 * @author Aimony
 * @date 2024/11/28 23:10
 * @description
 */

@Configuration
@EnableAuthorizationServer
public class MyAuthorizationConfig extends AuthorizationServerConfigurerAdapter {

	/**
	 * 客户端存储策略，这里使用内存方式，后续可以存储在数据库
	 */
	@Autowired
	private ClientDetailsService clientDetailsService;

	/**
	 * Security的认证管理器，密码模式需要用到
	 */
	@Autowired
	private AuthenticationManager authenticationManager;

	/**
	 * 配置令牌访问的安全约束
	 */
	@Override
	public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
		security
				// 开启/oauth/token_key验证端口权限访问
				.tokenKeyAccess("permitAll()")
				// 开启/oauth/check_token验证端口认证权限访问
				.checkTokenAccess("permitAll()")
				// 表示支持 client_id 和 client_secret 做登录认证
				.allowFormAuthenticationForClients();
	}

	// 配置客户端
	@Override
	public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
		// 内存模式
		clients.inMemory()
				// 客户端id
				.withClient("test")
				// 客户端秘钥
				// 在最新Spring Cloud Security
				// OAuth2中，在AuthorizationServerConfigurerAdapters实现类中定义client设置secret时必须要使用密码加密，而不能直接使用明文密码。
				.secret(new BCryptPasswordEncoder().encode("123456"))
				// 资源id，唯一，比如订单服务作为一个资源,可以设置多个
				.resourceIds("order")
				// 授权模式，总共四种，1.
				// authorization_code（授权码模式）、password（密码模式）、client_credentials（客户端模式）、implicit（简化模式）
				// refresh_token并不是授权模式，
				.authorizedGrantTypes("authorization_code", "password", "client_credentials", "implicit", "refresh_token")
				// 允许的授权范围，客户端的权限，这里的all只是一种标识，可以自定义，为了后续的资源服务进行权限控制
				.scopes("all")
				// false 则跳转到授权页面
				// .accessTokenValiditySeconds(3600) // token有效期1小时
				// .refreshTokenValiditySeconds(86400) // 刷新token有效期24小时
				.autoApprove(false)
				// 授权码模式的回调地址
				.redirectUris("http://www.baidu.com"); // 可以and继续添加客户端
	}

	/**
	 * 令牌存储策略
	 */
	@Bean
	public TokenStore tokenStore() {
		return new InMemoryTokenStore();
	}

	public AuthorizationServerTokenServices tokenServices() {
		DefaultTokenServices services = new DefaultTokenServices();
		// 客户端端配置策略
		services.setClientDetailsService(clientDetailsService);
		// 支持令牌的刷新
		services.setSupportRefreshToken(true);
		// 令牌服务
		services.setTokenStore(tokenStore());
		// access_token的过期时间
		services.setAccessTokenValiditySeconds(60 * 60 * 2);
		// refresh_token的过期时间
		services.setRefreshTokenValiditySeconds(60 * 60 * 24 * 3);
		return services;
	}

	/**
	 * 授权码模式的service，使用授权码模式authorization_code必须注入
	 */
	@Bean
	public AuthorizationCodeServices authorizationCodeServices() {
		// 授权码存在内存中
		return new InMemoryAuthorizationCodeServices();
	}

	/**
	 * 配置令牌访问的端点
	 */
	@Override
	public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
		endpoints
				// 授权码模式所需要的authorizationCodeServices
				.authorizationCodeServices(authorizationCodeServices())
				// 密码模式所需要的authenticationManager
				.authenticationManager(authenticationManager)
				// 令牌管理服务，无论哪种模式都需要
				.tokenServices(tokenServices())
				// 只允许POST提交访问令牌，uri：/oauth/token
				.allowedTokenEndpointRequestMethods(HttpMethod.POST);
	}
}