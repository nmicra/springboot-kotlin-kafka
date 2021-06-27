package com.example.config

import com.example.service.LibraryEventsService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.dao.RecoverableDataAccessException
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.retry.RetryContext
import org.springframework.retry.RetryPolicy
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import java.util.*
import java.util.function.Consumer

@Configuration
@EnableKafka
class KafkaConsumerConfig {

    val log: Logger = LoggerFactory.getLogger(KafkaConsumerConfig::class.java)

    @Autowired
    lateinit var libraryEventsService: LibraryEventsService

    @Autowired
    lateinit var kafkaProperties: KafkaProperties

    @Bean
    @ConditionalOnMissingBean(name = ["kafkaListenerContainerFactory"])
    fun kafkaListenerContainerFactory(configurer: ConcurrentKafkaListenerContainerFactoryConfigurer,
        kafkaConsumerFactory: ObjectProvider<ConsumerFactory<Any?, Any?>>): ConcurrentKafkaListenerContainerFactory<*, *>? {
        val factory = ConcurrentKafkaListenerContainerFactory<Any, Any>()
        configurer.configure(factory, kafkaConsumerFactory
            .getIfAvailable {
                DefaultKafkaConsumerFactory(
                    kafkaProperties.buildConsumerProperties()
                )
            })
        factory.setConcurrency(3)

        // factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        factory.setErrorHandler { thrownException: Exception, data: ConsumerRecord<*, *>? ->
            log.error("Exception in consumerConfig is ${thrownException.message} and the record is $data",thrownException)
        }
        factory.setRetryTemplate(retryTemplate()!!)
        factory.setRecoveryCallback { context: RetryContext ->
            if (context.lastThrowable.cause is RecoverableDataAccessException) {
                //invoke recovery logic
                log.info("Inside the recoverable logic")
                context.attributeNames().forEach { log.info("Attribute name=$it , Attribute value=${context.getAttribute(it)}") }

                val consumerRecord =
                    context.getAttribute("record") as ConsumerRecord<Int?, String?>
                libraryEventsService.handleRecovery(consumerRecord)
            } else {
                log.info("Inside the non recoverable logic")
                throw RuntimeException(context.lastThrowable.message)
            }
            null
        }
        return factory
    }


    /* @Bean
    ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> kafkaConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory);
        factory.setConcurrency(3);
       // factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setErrorHandler(((thrownException, data) -> {
            log.info("Exception in consumerConfig is {} and the record is {}", thrownException.getMessage(), data);
            //persist
        }));
        factory.setRetryTemplate(retryTemplate());
        factory.setRecoveryCallback((context -> {
            if(context.getLastThrowable().getCause() instanceof RecoverableDataAccessException){
                //invoke recovery logic
                log.info("Inside the recoverable logic");
               Arrays.asList(context.attributeNames())
                        .forEach(attributeName -> {
                            log.info("Attribute name is : {} ", attributeName);
                            log.info("Attribute Value is : {} ", context.getAttribute(attributeName));
                        });

               ConsumerRecord<Integer, String> consumerRecord = (ConsumerRecord<Integer, String>) context.getAttribute("record");
                libraryEventsService.handleRecovery(consumerRecord);
            }else{
                log.info("Inside the non recoverable logic");
                throw new RuntimeException(context.getLastThrowable().getMessage());
            }


            return null;
        }));
        return factory;
    }*/
    private fun retryTemplate(): RetryTemplate? {
        val fixedBackOffPolicy = FixedBackOffPolicy()
        fixedBackOffPolicy.backOffPeriod = 1000
        val retryTemplate = RetryTemplate()
        retryTemplate.setRetryPolicy(simpleRetryPolicy())
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy)
        return retryTemplate
    }

    private fun simpleRetryPolicy(): RetryPolicy? {

        /*SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy();
        simpleRetryPolicy.setMaxAttempts(3);*/

        val exceptionsMap = mapOf<Class<out Throwable?>, Boolean>(IllegalArgumentException::class.java to false,
                                                                    RecoverableDataAccessException::class.java to true)
        return SimpleRetryPolicy(3, exceptionsMap, true)
    }
}