package com.tuning.oasystem.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger 接口文档配置
 */
@Configuration
public class OpenApiConfig {

	private static final String SECURITY_SCHEME_NAME = "BearerAuth";

	@Bean
	public OpenAPI oaSystemOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("企业协同办公管理系统 API")
						.description("企业协同办公管理系统后端接口文档，供前端（Vue3）对接使用。")
						.version("1.0.0")
						.contact(new Contact().name("oa-system")))
				// 预置 JWT 认证方案（阶段 2 接入 Spring Security 后生效）
				.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
				.components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
						new SecurityScheme()
								.name(SECURITY_SCHEME_NAME)
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")));
	}
}
