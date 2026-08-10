package com.oj.ojbackendjudgeservice.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 用于创建测试程序用到的交换机和队列（只用在程序启动前执行一次）
 */
@Slf4j
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "code_exchange";
    public static final String QUEUE_NAME = "code_queue";
    public static final String ROUTING_KEY = "my_routingKey";

    // 声明交换机
    @Bean
    public DirectExchange codeExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    // 声明队列
    @Bean
    public Queue codeQueue() {
        return new Queue(QUEUE_NAME, true, false, false);
    }

    // 将队列绑定到交换机，并指定路由键
    @Bean
    public Binding bindingCodeQueueToExchange(Queue codeQueue, DirectExchange codeExchange) {
        return BindingBuilder.bind(codeQueue)
                             .to(codeExchange)
                             .with(ROUTING_KEY);
    }
}

