package com.yangqifang.crm.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.oauth2.provider.token.store.InMemoryTokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yangqifang
 */
@Slf4j
@Configuration
@EnableResourceServer
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class ResourceServerConfigurer extends ResourceServerConfigurerAdapter {
    @Value("${security.oauth2.client.id}")
    private String ClientID;
    @Value("${security.oauth2.client.client-secret}")
    private String ClientSecret;
    @Value("${security.oauth2.authorization.check-token-access}")
    private String CheckTokenAccessURL;

//    @Autowired
//    private SpdataAccessDeniedHandler accessDeniedHandler;
//    @Autowired
//    private SpdataAuthenticationEntryPoint authenticationEntryPoint;

    /**
     * JSON WEB TOKEN 服务  设置密钥
     *
     * @return
     */
    @Bean
    public JwtAccessTokenConverter jwtAccessTokenConverter() {
        JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
        converter.setSigningKey("spdata-yang");
        return converter;
    }

    /**
     * 这个暂时没用 配置还要研究一下
     *
     * @return
     */
    @Bean
    @ConfigurationProperties(prefix = "security.oauth2.client")
    public ClientCredentialsResourceDetails clientCredentialsResourceDetails() {
        return new ClientCredentialsResourceDetails();
    }

    /**
     * 远程token服务
     *
     * @return
     */
    @Bean
    public ResourceServerTokenServices resourceTokenServices() {
        RemoteTokenServices tokenServices = new RemoteTokenServices();
        //检查TOKEN的URL
        tokenServices.setCheckTokenEndpointUrl(CheckTokenAccessURL);
        //客户端ID
        tokenServices.setClientId(ClientID);
        //客户端密钥
        tokenServices.setClientSecret(ClientSecret);
        return tokenServices;
    }

    /**
     * 默认的token服务 使用该TokenServices记得设置tokenstore 和OAUTH2认证服务设置的tokenstore一样.
     * tokenstore可以使用 redis,JDBC
     * 如果OAUTH2认证服务使用内存存储token 客户端最好使用远程token服务
     *
     * @return
     */
    public DefaultTokenServices defaultTokenServices() {
        DefaultTokenServices tokenServices = new DefaultTokenServices();
        tokenServices.setTokenStore(new InMemoryTokenStore());
        return tokenServices;
    }

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
        resources
                .resourceId("spdata").stateless(false)
                .tokenServices(resourceTokenServices());
//                .authenticationEntryPoint(authenticationEntryPoint)
//                .accessDeniedHandler(accessDeniedHandler);
    }

    /**
     * 不拦截的请求列表
     *
     * @return
     */
    public String[] permitAll() {
        List<String> stringList = new ArrayList<>();
        stringList.add("/admin/webjars/**");
        stringList.add("/admin/doc.html");
        stringList.add("/admin/swagger-resources/**");
        stringList.add("/admin/v2/api-docs");
        String[] permitAll = new String[stringList.size()];
        stringList.toArray(permitAll);
        return permitAll;
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers(permitAll()).permitAll().anyRequest().access("#oauth2.hasScope('all') and isAuthenticated()");
    }
}
