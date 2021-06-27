package com.example.librarykafkaproducer.unit

import com.example.data.Book
import com.example.data.LibraryEvent
import com.example.data.LibraryEventType
import com.example.producer.LibraryEventProducer
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.util.concurrent.SettableListenableFuture
import java.util.concurrent.ExecutionException

@ExtendWith(MockKExtension::class)
class LibraryEventProducerUnitTest {

    @RelaxedMockK
    lateinit var kafkaTemplate: KafkaTemplate<Int, String>

    val objectMapper = ObjectMapper()

    @InjectMockKs
    lateinit var eventProducer: LibraryEventProducer

    @BeforeEach
    fun setUp() = MockKAnnotations.init(this, relaxUnitFun = true) // turn relaxUnitFun on for all mocks

    @Test
    @Throws(JsonProcessingException::class, ExecutionException::class, InterruptedException::class)
    fun `testcase when eventProducer_sendLibraryEventAsync fails`() {
        //given
        val book = Book(1, "Kuku", "Kafka using Spring Boot")
        val libraryEvent = LibraryEvent(321, LibraryEventType.NEW, book)

        // ListenableFuture<SendResult<Int, String>>
        // ProducerRecord<Int, String>
        val future: SettableListenableFuture<SendResult<Int, String>> = SettableListenableFuture<SendResult<Int, String>>()
            .also { it.setException(RuntimeException("Exception Calling Kafka")) }

        every { kafkaTemplate.send(any<ProducerRecord<Int, String>>()) } returns future

        //when->then
        Assertions.assertThrows(Exception::class.java) { eventProducer.sendLibraryEventAsync(libraryEvent).get() }
    }

    @Test
    @Throws(JsonProcessingException::class, ExecutionException::class, InterruptedException::class)
    fun `testcase when eventProducer_sendLibraryEventAsync succeeds`() {
        //given
        val book = Book(1, "Kuku", "Kafka using Spring Boot")
        val libraryEvent = LibraryEvent(1, LibraryEventType.NEW, book)
        val record = objectMapper.writeValueAsString(libraryEvent)

        val producerRecord = ProducerRecord<Int, String>("library-events", libraryEvent.libraryEventId, record)
        val recordMetadata = RecordMetadata(
            TopicPartition("library-events", 1), 1, 1, 342, System.currentTimeMillis(), 1, 2)
        val sendResult = SendResult(producerRecord, recordMetadata)


        // ListenableFuture<SendResult<Int, String>>
        // ProducerRecord<Int, String>
        val future: SettableListenableFuture<SendResult<Int, String>> = SettableListenableFuture<SendResult<Int, String>>()
            .also { it.set(sendResult) }

        every { kafkaTemplate.send(any<ProducerRecord<Int, String>>()) } returns future

        //when
        val sendLibraryEventResponse = eventProducer.sendLibraryEventAsync(libraryEvent).get()
        //then
        assert(sendLibraryEventResponse.recordMetadata.partition() == 1)
    }
}