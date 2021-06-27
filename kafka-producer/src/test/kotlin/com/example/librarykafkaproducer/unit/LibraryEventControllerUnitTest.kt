package com.example.librarykafkaproducer.unit

import com.example.controller.LibraryEventsController
import com.example.data.Book
import com.example.data.LibraryEvent
import com.example.data.LibraryEventType
import com.example.producer.LibraryEventProducer
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.ComponentScan
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@WebMvcTest(value = [LibraryEventsController::class])
@AutoConfigureMockMvc
@ComponentScan(basePackages = ["com.example"])
class LibraryEventControllerUnitTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    private val objectMapper = ObjectMapper()

    @MockBean
    lateinit var libraryEventProducer: LibraryEventProducer

    @Test
    @Throws(Exception::class)
    fun postLibraryEvent() {
        //given
        val book = Book(123, "Kuku", "Kafka using Spring Boot")

        val libraryEvent = LibraryEvent(321,LibraryEventType.NEW, book)

        val json = objectMapper.writeValueAsString(libraryEvent)
        Mockito.`when`(libraryEventProducer.sendLibraryEventAsync(any())).thenReturn(null)


        //expect
        mockMvc.perform(
            MockMvcRequestBuilders.post("/v1/libraryevent")
                .content(json)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun postLibraryEventExpectError() {
        //given
        val book = Book(1, "Kuku", "Kafka using Spring Boot")

        val libraryEvent = LibraryEvent(321,LibraryEventType.NEW, book)

        val json = objectMapper.writeValueAsString(libraryEvent)
        Mockito.`when`(libraryEventProducer.sendLibraryEventAsync(any())).thenReturn(null)

        //expect
        val expectedErrorMessage = "book.bookAuthor - must not be blank, book.bookId - must not be null"
        mockMvc.perform(
            MockMvcRequestBuilders.post("/v1/libraryevent")
                .content(json)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andExpect(MockMvcResultMatchers.content().string(containsString("Bad arguments >>")))
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun updateLibraryEvent_withNullLibraryEventId() {



        //given
        val book = Book(1, "Kuku", "Kafka using Spring Boot")
        val libraryEvent = LibraryEvent(312,LibraryEventType.NEW, book)
        val json = objectMapper.writeValueAsString(libraryEvent)

        Mockito.`when`(libraryEventProducer.sendLibraryEventAsync(any())).thenReturn(null)

        //expect
        mockMvc.perform(
            MockMvcRequestBuilders.put("/v1/libraryevent")
                .content(json)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andExpect(MockMvcResultMatchers.content().string("Please pass the LibraryEventId"))
    }
}