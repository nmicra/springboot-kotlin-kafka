package com.example.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary


@Configuration
class GeneralConfigurator {

    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        return ObjectMapper().also { it.registerModule(KotlinModule()) }
    }
}