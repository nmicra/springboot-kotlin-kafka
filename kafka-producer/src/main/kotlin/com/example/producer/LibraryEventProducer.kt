package com.example.producer

import com.example.data.LibraryEvent
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.Header
import org.apache.kafka.common.header.internals.RecordHeader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import org.springframework.util.concurrent.ListenableFuture
import org.springframework.util.concurrent.ListenableFutureCallback
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


@Service
class LibraryEventProducer {

    val log: Logger = LoggerFactory.getLogger(LibraryEventProducer::class.java)

    val topic = "library-events"


    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<Int, String>

    @Autowired
    lateinit var objectMapper: ObjectMapper

    /**
     * Sends even to default topic, which is also library-events
     * NOT in use
     */
    @Throws(JsonProcessingException::class)
    fun sendLibraryEvent(libraryEvent: LibraryEvent) {
        val key: Int = libraryEvent.libraryEventId //?: error("libraryEventId should not be null")
        val value = objectMapper.writeValueAsString(libraryEvent)
        val listenableFuture = kafkaTemplate.sendDefault(key, value)



        listenableFuture.addCallback(object : ListenableFutureCallback<SendResult<Int?, String?>?> {
            override fun onFailure(ex: Throwable) {
                log.error("failed to send event to default topic key=$key, val=$value", ex)
            }

            override fun onSuccess(result: SendResult<Int?, String?>?) {
                log.info("event successfully published to default topic key=$key, val=$value result=$result")
            }
        })
    }

    @Throws(
        JsonProcessingException::class,
        ExecutionException::class,
        InterruptedException::class,
        TimeoutException::class
    )

            /**
             * Sends even to default topic, which is also library-events
             * Timeout 1 sec
             * NOT in use
             */
    fun sendLibraryEventSynchronous(libraryEvent: LibraryEvent): SendResult<Int, String> {

        val key: Int = libraryEvent.libraryEventId ?: error("libraryEventId should not be null")
        val value = objectMapper.writeValueAsString(libraryEvent)

        return try {
            kafkaTemplate.sendDefault(key, value)[1, TimeUnit.SECONDS]
        } catch (e: ExecutionException) {
            log.error("ExecutionException/InterruptedException Sending the Message and the exception is $e.message")
            throw e
        } catch (e: InterruptedException) {
            log.error("ExecutionException/InterruptedException Sending the Message and the exception is $e.message")
            throw e
        } catch (e: Exception) {
            log.error("Exception Sending the Message and the exception is $e.message")
            throw e
        }
    }

    /**
     * Sends event in async mode to mentioned topic ( library-events)
     */
    @Throws(JsonProcessingException::class)
    fun sendLibraryEventAsync(libraryEvent: LibraryEvent): ListenableFuture<SendResult<Int, String>> {
        val key: Int = libraryEvent.libraryEventId ?: error("libraryEventId should not be null")
        val value = objectMapper.writeValueAsString(libraryEvent)
        val producerRecord = buildProducerRecord(key, value, topic)
        val listenableFuture = kafkaTemplate.send(producerRecord)
        listenableFuture.addCallback(object : ListenableFutureCallback<SendResult<Int?, String?>?> {
            override fun onFailure(ex: Throwable) {
                log.error("failed to send event to default topic key=$key, val=$value", ex)
            }

            override fun onSuccess(result: SendResult<Int?, String?>?) {
                log.info("event successfully published to default topic key=$key, val=$value result=$result")
            }
        })
        return listenableFuture
    }

    fun buildProducerRecord(key: Int, value: String, topic: String): ProducerRecord<Int, String> =
            ProducerRecord(topic, null, key, value, listOf(RecordHeader("event-source", "scanner".toByteArray())))


}