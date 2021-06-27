package com.example.librarykaconsumer.integration.db

import com.example.data.Book
import com.example.data.LibraryEvent
import com.example.data.LibraryEventType
import com.example.librarykaconsumer.integration.repository.LibraryEventJdbcExtendedForTestDAO
import com.example.repository.BookJdbcDAO
import com.example.repository.LibraryEventJdbcDAO
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate

@SpringBootTest
class DbCrudIntegrationTests {

    @Autowired
    lateinit var bookJdbc : BookJdbcDAO

    @Qualifier("libraryEventJdbcDAO")
    @Autowired
    lateinit var libraryEventJdbc : LibraryEventJdbcDAO

    @Autowired
    lateinit var libraryEventJdbcExtendedForTestDAO : LibraryEventJdbcExtendedForTestDAO

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @AfterEach
    fun deleteAllRecords() {
        jdbcTemplate.execute("TRUNCATE TABLE t_book")
        jdbcTemplate.execute("TRUNCATE TABLE t_library_event")
    }

    @Test
    fun `test book crud operations`(){
        val book1 = Book("Better use JDBCTemplate", "nmicra")
        val book2 = Book("Better not to use ORM", "nmicra")
        bookJdbc.create(book1)
        bookJdbc.create(book2)

        val book11 = bookJdbc.get(book1.id)

        assert(book1 == book11)
        assert(bookJdbc.list().size == 2)
        assert(bookJdbc.findBookByAuthor("nmicra").size == 2)

        bookJdbc.update(book2.copy(bookAuthor = "kuku"), book2.id)
        val book22 = bookJdbc.get(book2.id)

        assert(book22!!.bookAuthor == "kuku")
    }

    @Test
    fun `test libraryEvent crud operations`(){
        val book1 = Book("Better use JDBCTemplate", "nmicra")
        val libraryEvent1 = LibraryEvent(LibraryEventType.NEW, book = book1)
        libraryEventJdbc.create(libraryEvent1)


        val libraryEvent11 = libraryEventJdbc.get(libraryEvent1.id)!!

        assert(libraryEvent11.book!!.bookName == book1.bookName)
        assert(libraryEvent11.libraryEventType == libraryEvent1.libraryEventType)
    }

    @Test
    fun `test libraryEvent transaction works`(){


        val book1 = Book("Better use JDBCTemplate", "nmicra")
        val libraryEvent1 = LibraryEvent(LibraryEventType.NEW, book = book1)
        try {
            libraryEventJdbcExtendedForTestDAO.create(libraryEvent1)
        } catch (ex : IllegalStateException){ /* DO NOTHING EXPECTING THIS EXCEPTION */ }

        assert(bookJdbc.findBookByLibraryTypeId(libraryEvent1.id).isEmpty())
    }
}