package com.progetto.ecommercebackend.controllers;

import com.progetto.ecommercebackend.entities.Book;
import com.progetto.ecommercebackend.services.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InventoryController {

    @Autowired
    InventoryService inventoryService;

    //UPDATE
    @RequestMapping(value = ("inventory-quantity"), method = RequestMethod.PUT)
    @PreAuthorize("hasRole('ROLE_admin')")
    public ResponseEntity<Book> updateBookQuantityInInventory(@RequestParam Long bookId, @RequestParam Integer qty){
        Book book = inventoryService.updateBookQuantityInInventory(bookId, qty);
        return new ResponseEntity<>(book, HttpStatus.OK);
    }

    //UPDATE
    @RequestMapping(value = ("num-purchases"), method = RequestMethod.PUT)
    @PreAuthorize("hasRole('ROLE_user')")
    public ResponseEntity<String> incrementNumPurchases(@RequestParam Long bookId){
        inventoryService.incrementNumPurchases(bookId);
        return new ResponseEntity<>("Il numero di acquisti è stato aggiornato", HttpStatus.OK);
    }


}
