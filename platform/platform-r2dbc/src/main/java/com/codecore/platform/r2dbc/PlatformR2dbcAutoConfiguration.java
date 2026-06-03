package com.codecore.platform.r2dbc;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

/**
 * Shared R2DBC transaction utilities for CodeCore modules.
 */
@AutoConfiguration
@ConditionalOnClass({ReactiveTransactionManager.class, TransactionalOperator.class})
@ConditionalOnBean(ReactiveTransactionManager.class)
public class PlatformR2dbcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    TransactionalOperator transactionalOperator(ReactiveTransactionManager transactionManager) {
        return TransactionalOperator.create(transactionManager);
    }
}
