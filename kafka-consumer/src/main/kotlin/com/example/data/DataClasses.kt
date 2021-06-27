package com.example.data


data class Book(val bookName : String, val bookAuthor : String, val id : String = System.nanoTime().toString(), val book2LibraryEvent : String? = null){
    init {
        require(id.toLong() >= 0) { "bookId [$id] must be grater than 0" }
    }

}
enum class LibraryEventType { NEW, UPDATE, NONE }
data class LibraryEvent(val libraryEventType : LibraryEventType = LibraryEventType.NONE, val id : String = System.nanoTime().toString(), val book : Book? = null)

interface DAO<T> {
    fun list(): List<T>?
    fun create(entity: T)
    fun get(id: String): T?
    fun update(entity: T, id: String)
    fun delete(id: String)
}