package com.example.librarykaconsumer.integration

import com.example.config.GeneralConfigurator
import com.example.data.Book
import com.example.data.LibraryEvent
import com.example.data.LibraryEventType
import com.example.repository.BookJdbcDAO
import com.example.repository.LibraryEventJdbcDAO
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.ContainerTestUtils
import org.springframework.test.context.TestPropertySource
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit


@SpringBootTest //(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(topics = ["library-events"], partitions = 3)
@Import(GeneralConfigurator::class)
@TestPropertySource(properties = ["spring.kafka.producer.bootstrap-servers=\${spring.embedded.kafka.brokers}", "spring.kafka.consumer.bootstrap-servers=\${spring.embedded.kafka.brokers}"])
class LibraryEventsConsumerIntegrationTest {

    @Autowired
    lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker

    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<Int, String>

    @Autowired
    lateinit var endpointRegistry: KafkaListenerEndpointRegistry


    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var bookJdbc : BookJdbcDAO

    @Qualifier("libraryEventJdbcDAO")
    @Autowired
    lateinit var libraryEventJdbc : LibraryEventJdbcDAO

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate


    @BeforeEach
    fun setUp() {
        endpointRegistry.listenerContainers.forEach {
            ContainerTestUtils.waitForAssignment(
                it, embeddedKafkaBroker.partitionsPerTopic
            ) }
    }

    @AfterEach
    fun tearDown() {
        jdbcTemplate.execute("TRUNCATE TABLE t_book")
        jdbcTemplate.execute("TRUNCATE TABLE t_library_event")
    }




    @Test
    @Throws(ExecutionException::class, InterruptedException::class, JsonProcessingException::class)
    fun publishNewLibraryEvent() {
        //given
        val bookForTest = Book("Kafka Using Spring Boot", "Kuku")
        val libraryEvent = LibraryEvent(LibraryEventType.NEW, book = bookForTest)



        //when
        kafkaTemplate.sendDefault(objectMapper.writeValueAsString(libraryEvent)).get()
        val latch = CountDownLatch(1)
        latch.await(3, TimeUnit.SECONDS)

        //then

        val libraryEventList = libraryEventJdbc.list()!!
        assert(libraryEventList.size == 1)
        assert(libraryEventList[0].id == libraryEvent.id)

        val bookList = bookJdbc.list()
        assert(bookList.size == 1)
        assert(bookList[0].id == bookForTest.id)
        assert(bookList[0].book2LibraryEvent == libraryEvent.id)
    }


    @Test
    @Throws(ExecutionException::class, InterruptedException::class, JsonProcessingException::class)
    fun publishUpdateLibraryEvent() {
        //given
        val bookForTest = Book("Kafka Using Spring Boot", "Kuku")
        val libraryEvent = LibraryEvent(LibraryEventType.NEW, book = bookForTest)
        libraryEventJdbc.create(libraryEvent)


        val bookForTest2 = bookForTest.copy(bookName = "Kafka Using Spring Boot1", bookAuthor = "Kuku1")
        val libraryEvent2 = libraryEvent.copy(libraryEventType = LibraryEventType.UPDATE, book = bookForTest2)

        //when
        kafkaTemplate.sendDefault(objectMapper.writeValueAsString(libraryEvent2)).get()
        val latch = CountDownLatch(1)
        latch.await(3, TimeUnit.SECONDS)

        //then

        val libraryEventList = libraryEventJdbc.list()!!
        assert(libraryEventList.size == 1)
        assert(libraryEventList[0].id == libraryEvent.id)

        val bookList = bookJdbc.list()
        assert(bookList.size == 1)
        assert(bookList[0].book2LibraryEvent == libraryEvent.id)
        assert(bookList[0].bookAuthor == "Kuku1")
        assert(bookList[0].bookName == "Kafka Using Spring Boot1")
    }

}

inline fun <reified T> typeOf() = T::class.java