package com.btg.commission.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    private final ApiProperties apiProperties;

    @Bean
    public OpenAPI btgCommissionOpenAPI() {
        String prefix = apiProperties.getBasePath();
        return new OpenAPI()
                .info(new Info()
                        .title("BTG 推荐分佣 API")
                        .description("统一返回：`{ \"code\": 200, \"message\": \"success\", \"data\": ... }`；业务接口前缀 `"
                                + prefix
                                + "`（由 `btg.api.base-path` 配置）；邀请码可选（空则挂根用户）；除注册/登录与文档外需携带 `Authorization: Bearer <token>`。")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("登录接口返回的 token，形如：Bearer eyJhbG...")));
    }
}
