package com.example.consumer

import com.example.service.LibraryEventsService
import com.fasterxml.jackson.core.JsonProcessingException
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class LibraryEventsConsumer {

    val log: Logger = LoggerFactory.getLogger(LibraryEventsConsumer::class.java)

    @Autowired
    lateinit var libraryEventsService: LibraryEventsService

    @KafkaListener(topics = ["library-events"])
    @Throws(JsonProcessingException::class)
    fun onMessage(consumerRecord: ConsumerRecord<Int, String>) {
        log.info("ConsumerRecord : $consumerRecord ", )
        libraryEventsService.processLibraryEvent(consumerRecord)
    }

}