package com.example.librarykafkaproducer.integration

import com.example.config.GeneralConfigurator
import com.example.data.Book
import com.example.data.LibraryEvent
import com.example.data.LibraryEventType
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.serialization.IntegerDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.http.*
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.context.TestPropertySource


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(topics = ["library-events"], partitions = 3)
@ComponentScan(basePackages = ["com.example"])
@Import(GeneralConfigurator::class)
@TestPropertySource(properties = ["spring.kafka.producer.bootstrap-servers=\${spring.embedded.kafka.brokers}", "spring.kafka.admin.properties.bootstrap.servers=\${spring.embedded.kafka.brokers}"])
class LibraryEventsControllerIntegrationTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker

    lateinit var consumer: Consumer<Int, String>



    @BeforeEach
    fun setUp() {
        val configs: Map<String, Any> = HashMap(KafkaTestUtils.consumerProps("group1", "true", embeddedKafkaBroker))
        consumer = DefaultKafkaConsumerFactory(configs, IntegerDeserializer(), StringDeserializer()).createConsumer()
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(consumer)
    }

    @AfterEach
    fun tearDown() {
        consumer.close()
    }

    @Test
    @Timeout(5)
    @Throws(InterruptedException::class)
    fun postLibraryEvent() {
        //given

        val book = Book(123, bookAuthor = "Kuku", bookName = "Kafka using Spring Boot")
        val libraryEvent = LibraryEvent(321, LibraryEventType.NEW, book)
        val headers = HttpHeaders()
        headers["content-type"] = MediaType.APPLICATION_JSON.toString()
        val request: HttpEntity<LibraryEvent> = HttpEntity<LibraryEvent>(libraryEvent, headers)

        //when
        val responseEntity: ResponseEntity<LibraryEvent> = restTemplate.exchange(
            "/v1/libraryevent", HttpMethod.POST, request,
            LibraryEvent::class.java
        )

        //then
        Assertions.assertEquals(HttpStatus.CREATED, responseEntity.statusCode)
        val consumerRecord = KafkaTestUtils.getSingleRecord(consumer, "library-events")
        //Thread.sleep(3000);
        val expectedRecord =
            "{\"libraryEventId\":321,\"libraryEventType\":\"NEW\",\"book\":{\"bookId\":123,\"bookName\":\"Kafka using Spring Boot\",\"bookAuthor\":\"Kuku\"}}"
        val value = consumerRecord.value()
        Assertions.assertEquals(expectedRecord, value)
    }

    @Test
    @Timeout(5)
    @Throws(InterruptedException::class)
    fun putLibraryEvent() {
        //given
        val book = Book(123, bookAuthor = "Kuku", bookName = "Kafka using Spring Boot")
        val libraryEvent = LibraryEvent(321, LibraryEventType.UPDATE, book)
        val headers = HttpHeaders()
        headers["content-type"] = MediaType.APPLICATION_JSON.toString()
        val request = HttpEntity(libraryEvent, headers)

        //when
        val responseEntity = restTemplate.exchange(
            "/v1/libraryevent", HttpMethod.PUT, request,
            LibraryEvent::class.java
        )

        //then
        Assertions.assertEquals(HttpStatus.OK, responseEntity.statusCode)
        val consumerRecord = KafkaTestUtils.getSingleRecord(consumer, "library-events")
        //Thread.sleep(3000);
        val expectedRecord =
            "{\"libraryEventId\":321,\"libraryEventType\":\"UPDATE\",\"book\":{\"bookId\":123,\"bookName\":\"Kafka using Spring Boot\",\"bookAuthor\":\"Kuku\"}}"
        val value = consumerRecord.value()
        Assertions.assertEquals(expectedRecord, value)
    }

}