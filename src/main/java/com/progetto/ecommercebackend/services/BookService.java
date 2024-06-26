package com.progetto.ecommercebackend.services;

import com.progetto.ecommercebackend.entities.Author;
import com.progetto.ecommercebackend.entities.Book;
import com.progetto.ecommercebackend.repositories.AuthorRepository;
import com.progetto.ecommercebackend.repositories.BookRepository;
import com.progetto.ecommercebackend.repositories.CategoryRepository;
import com.progetto.ecommercebackend.support.exceptions.CustomException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.*;

@Service
@Slf4j
public class BookService {
    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    BookRepository bookRepository;

    @Autowired
    AuthorRepository authorRepository;

    @Transactional
    public Book addNewBook( @NotNull Book book, @NotNull List<Long> authorIds) {
        Set<Author> authors = new HashSet<>();
        for ( Long authorId : authorIds ){
            Optional<Author> authorOptional = authorRepository.findById(authorId);
            if (authorOptional.isPresent() ) {
                Author author = authorOptional.get();
                authors.add(author);
                author.getBooks().add(book);
                authorRepository.save(author);
            }
        }

        Book savedBook = bookRepository.save(book);
        return savedBook;
    }


    public List<Book> getAllBooksOfAuthor(Long authorId) {
        Optional<Author> authorOptional =  authorRepository.findById(authorId);
        if( authorOptional.isPresent() ){
            return authorOptional.get().getBooks().stream().toList();
        }else{
            throw new CustomException("Errore nel recupero dei libri scritti dall'autore con ID " + authorId);
        }
    }

    public List<Book> getAllDiscountedBooks() {
        return bookRepository.findAllWithDiscount();
    }


    public List<Book> getBestSellingBooks() {
        List<Book> bestSellingBooks = bookRepository.findAllByNumPurchasesIsNotNull();
        bestSellingBooks.sort(Comparator.comparingInt(Book::getNumPurchases).reversed());
        if(bestSellingBooks.size()>10){
            bestSellingBooks.subList(0,9); //Prendo solo i primi 10
        }

        return bestSellingBooks;
    }

    @Transactional
    public Book updateBook(long id, Book book) {
        Book existingBook = bookRepository.findBookById(id);
        book.setAuthors(existingBook.getAuthors());
        book.setCategory(existingBook.getCategory());
        return bookRepository.save(book );

    }

    @Transactional
    public String deleteBookById(long id) {
        Optional<Book> bookOptional = bookRepository.findById(id);
        if( bookOptional.isPresent() ){
            Book book = bookOptional.get();
            Set<Author>authors = bookOptional.get().getAuthors();
            for( Author a : authors ){
                a.getBooks().remove(book);
                authorRepository.save(a);
            }
             bookRepository.deleteById(id);
            return "Libro eliminato :"+ book;
        }else{
            throw new CustomException("Libro non trovato.");
        }
    };

    public Page<Book> getBooks( int page, int size, String sortBy, String sortDirection) {
        log.info("Fetching books for page {} of size {}", page, size);
        return bookRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy)));
    }


    public Book getBookById(Long bookId) {
        return bookRepository.findBookById(bookId);
    }

    public Page<Book> getAllBooksOfCategories(List<Long> categoryIds) {
        Pageable pageable =  PageRequest.of(0, 12);
        List<Book> bookList = new ArrayList<>();
        for( Long categoryId : categoryIds ){
            bookList.addAll( bookRepository.findByCategoryId(categoryId));
        }
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), bookList.size());
        List<Book> sublist = bookList.subList(start, end);

        return new PageImpl<>(sublist, pageable, bookList.size());
    }

}
