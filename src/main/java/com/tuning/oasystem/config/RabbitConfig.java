package com.tuning.oasystem.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * RabbitMQ 配置：审批通知 Exchange / Queue / Binding + JSON 消息转换器。
 * <p>
 * 消息链路：审批服务 → producer → Exchange(direct) → Queue → Consumer → 写 sys_message。
 * 转换器用 Jackson 2（spring-amqp 内置支持）并显式注册 JavaTimeModule，
 * 保证 LocalDateTime 以 ISO 序列化，规避 Jackson 2/3 默认行为差异。
 */
@Configuration
@EnableRabbit
public class RabbitConfig {

    /** 审批通知交换机（direct） */
    public static final String EXCHANGE = "oa.approval.exchange";

    /** 审批通知队列 */
    public static final String QUEUE = "oa.approval.notice.queue";

    /** 路由键 */
    public static final String ROUTING_KEY = "approval.completed";

    @Bean
    public DirectExchange approvalExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue approvalQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding approvalBinding(Queue approvalQueue, DirectExchange approvalExchange) {
        return BindingBuilder.bind(approvalQueue).to(approvalExchange).with(ROUTING_KEY);
    }

    /**
     * JSON 消息转换器（Spring Boot 自动装配到 RabbitTemplate 与监听容器）。
     * <p>
     * 用 spring-amqp 4.x 推荐且未弃用的 {@link JacksonJsonMessageConverter}（替代已弃用的
     * Jackson2JsonMessageConverter）；其基于 Jackson 3（tools.jackson，Boot 4 内置
     * ObjectMapper，
     * databind 自带 Java time 支持，无需显式注册 JavaTimeModule）。
     */
    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new JacksonJsonMessageConverter(JsonMapper.builder().build());
    }
}
