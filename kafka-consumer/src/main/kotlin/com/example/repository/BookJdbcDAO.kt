package com.example.repository

import com.example.data.Book
import com.example.data.DAO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Service
import java.sql.ResultSet
import javax.annotation.PostConstruct

@Service
class BookJdbcDAO : DAO<Book> {

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @PostConstruct
    fun createTable(){
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_book(id VARCHAR(50), bookName VARCHAR(50), bookAuthor VARCHAR(50),book2LibraryEvent VARCHAR(50), PRIMARY KEY(id))")
    }

    val rowMapper : RowMapper<Book> = RowMapper<Book> { resultSet : ResultSet, _:Int -> Book(resultSet.getString("bookName"),
        resultSet.getString("bookAuthor"),
        resultSet.getString("id"),
        resultSet.getString("book2LibraryEvent")) }

    override fun list(): List<Book> {
        return jdbcTemplate.query("SELECT id, bookName, bookAuthor, book2LibraryEvent from t_book", rowMapper)
    }

    override fun create(book: Book) {
        when(book.book2LibraryEvent){
            null -> jdbcTemplate.execute("INSERT INTO t_book(id, bookName, bookAuthor, book2LibraryEvent) values('${book.id}','${book.bookName}','${book.bookAuthor}',null)")
            else-> jdbcTemplate.execute("INSERT INTO t_book(id, bookName, bookAuthor, book2LibraryEvent) values('${book.id}','${book.bookName}','${book.bookAuthor}','${book.book2LibraryEvent}')")
        }

    }

    override fun get(id: String): Book? {
        val queryResult = jdbcTemplate.query("SELECT id, bookName, bookAuthor, book2LibraryEvent FROM t_book WHERE id = '$id'", rowMapper)
        return when (queryResult.size){
            0 -> null
            1 -> queryResult[0]
            else -> error("found too many books with the same id='$id'")
        }
    }

    override fun update(book: Book, id: String) {
        when(book.book2LibraryEvent){
            null -> jdbcTemplate.execute("UPDATE t_book SET bookName='${book.bookName}', bookAuthor='${book.bookAuthor}', book2LibraryEvent=null where id=$id")
            else-> jdbcTemplate.execute("UPDATE t_book SET bookName='${book.bookName}', bookAuthor='${book.bookAuthor}', book2LibraryEvent='${book.book2LibraryEvent}' where id=$id")
        }

    }

    override fun delete(id: String) {
        jdbcTemplate.execute("DELETE FROM t_book where id='$id'")
    }

    fun findBookByAuthor(author : String): List<Book> {
        return jdbcTemplate.query("SELECT id, bookName, bookAuthor, book2LibraryEvent FROM t_book WHERE bookAuthor='$author'", rowMapper)
    }

    fun findBookByLibraryTypeId(libraryTypeId : String): List<Book> {
        return jdbcTemplate.query("SELECT id, bookName, bookAuthor, book2LibraryEvent FROM t_book WHERE book2LibraryEvent='$libraryTypeId'", rowMapper)
    }
}