package com.progetto.ecommercebackend.services;

import com.progetto.ecommercebackend.entities.Author;
import com.progetto.ecommercebackend.repositories.AuthorRepository;
import com.progetto.ecommercebackend.support.exceptions.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
public class AuthorService {

    @Autowired
    AuthorRepository authorRepository;

    public List<Author> getAllAuthors() {
        return authorRepository.findAll();
    }

    public Author addNewAuthor(String author) {
        Author newAuthor = new Author();
        newAuthor.setName(author);
        newAuthor.setBooks(new HashSet<>());
        return authorRepository.save(newAuthor);
    }

    public Author updateAuthorName(Long id, String author) {
        Optional<Author> authorOptional = authorRepository.findById(id);
        if( authorOptional.isPresent()){
            Author updatedAuthor = new Author();
            updatedAuthor.setId(authorOptional.get().getId());
            updatedAuthor.setName(author);
            updatedAuthor.setBooks(authorOptional.get().getBooks());
            authorRepository.deleteById(id);
            return authorRepository.save(updatedAuthor);
        }else{
            throw new CustomException("Author with id "+ id + " is not found");
        }
    }

    public void deleteAuthor(Long authorId) {
        try{
            authorRepository.deleteById(authorId);
        }catch (CustomException e){
            throw new CustomException("Author can not be deleted.");
        }

    }

    public Author getAuthorById(Long authorId) {
        Optional<Author> authorOptional = authorRepository.findById(authorId);
        if( authorOptional.isPresent() ){
            return authorOptional.get();
        }else{
            throw new CustomException("Author doesn't exist.");
        }
    }
}
