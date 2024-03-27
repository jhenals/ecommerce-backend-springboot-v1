package com.progetto.ecommercebackend.services;

import com.progetto.ecommercebackend.entities.Book;
import com.progetto.ecommercebackend.entities.Order;
import com.progetto.ecommercebackend.entities.OrderBook;
import com.progetto.ecommercebackend.entities.User;
import com.progetto.ecommercebackend.repositories.BookRepository;
import com.progetto.ecommercebackend.repositories.OrderBookRepository;
import com.progetto.ecommercebackend.repositories.OrderRepository;
import com.progetto.ecommercebackend.repositories.UserRepository;
import com.progetto.ecommercebackend.support.common.OrderForm;
import com.progetto.ecommercebackend.support.enums.OrderStatus;
import com.progetto.ecommercebackend.support.enums.OrderStatusDTO;
import com.progetto.ecommercebackend.support.exceptions.CustomException;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
//@Slf4j
public class OrderService {

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OrderBookRepository orderBookRepository;

    @Autowired
    KeycloakService keycloakService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BookRepository bookRepository;


    public Order getPendingCart(String userId) {
        Optional<UserRepresentation> userRepresentationOptional = keycloakService.getUserById(userId);

        if (userRepresentationOptional.isEmpty()) {
            throw new CustomException("User not found.");
        }

        UserRepresentation userRepresentation = userRepresentationOptional.get();
        User user = userRepository.findById(userRepresentation.getId())
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setId(userRepresentation.getId());
                    newUser.setFirstName(userRepresentation.getFirstName());
                    newUser.setLastName(userRepresentation.getLastName());
                    return userRepository.save(newUser);
                });

        Order pendingCart = orderRepository.findByUserIdAndOrderStatus(userId, OrderStatus.PENDING);
        if (pendingCart == null) {
            pendingCart = new Order();
            pendingCart.setUser(user);
            pendingCart.setOrderStatus(OrderStatus.PENDING);
            pendingCart = orderRepository.save(pendingCart);
        }

        return pendingCart;
    }


    public Order resetCart(String userId, Long orderId) {
        Optional<Order> pendingCartOptional = orderRepository.findById(orderId);
        if (pendingCartOptional.isPresent()) {
            for (OrderBook orderBook : orderBookRepository.findAllByOrderId(orderId)) {
                orderBookRepository.delete(orderBook);
            }
            return pendingCartOptional.get();
        } else {
            throw new CustomException("Error in resetting cart");
        }
    }

    public Order addBookToCart(Book book, String userId) {

        Order pendingCart =  getPendingCart(userId);
        if(book == null ){
            throw new CustomException("Book is required");
        }
        Optional<OrderBook> orderBookOptional = Optional.ofNullable(orderBookRepository.findByBookIdAndOrderId(book.getId(), pendingCart.getId()));
        if( orderBookOptional.isPresent() ){
            throw new CustomException("Book is already present in cart");
        }else{
            // Decrement book quantity in inventory
            Book book1 = bookRepository.findBookById(book.getId());
            book1.setQuantity(book1.getQuantity()-1);
            bookRepository.save(book1);

            OrderBook orderBook = new OrderBook(pendingCart, book, 1);
            orderBookRepository.save(orderBook);
            return pendingCart;

        }
    }


    public Order removeBookFromCart(Book book, String userId) {
        Order pendingCart =  getPendingCart(userId);
        if(book == null ){
            throw new CustomException("Book is required");
        }
        Optional<OrderBook> orderBookOptional = Optional.ofNullable(orderBookRepository.findByBookIdAndOrderId(book.getId(), pendingCart.getId()));
        if( orderBookOptional.isPresent() ){
            OrderBook orderBook = orderBookOptional.get();
            if (orderBook.getQuantity() == 1) {
                orderBookRepository.delete(orderBook);
            }else{
                orderBook.setQuantity(orderBook.getQuantity()-1);
                orderBookRepository.save(orderBook);
            }
            orderRepository.save(pendingCart);
            return pendingCart;
        }else{
            throw new CustomException("Book is not present in cart");
        }

    }

    @Transactional(readOnly = false)
    public OrderBook increaseBookQtyInCart(String userId, Long bookId) {
        Order pendingCart = orderRepository.findByUserIdAndOrderStatus(userId, OrderStatus.PENDING);
        OrderBook orderBook = orderBookRepository.findByBookIdAndOrderId(bookId, pendingCart.getId());
        if (orderBook == null){
            throw new CustomException("Book is not present in cart");
        }else{
            pendingCart.getOrderBooks().remove(orderBook);
            orderBook.setQuantity(orderBook.getQuantity()+1);
            orderBook = orderBookRepository.save(orderBook);
            pendingCart.getOrderBooks().add(orderBook);
            orderRepository.save(pendingCart);
            return orderBook;
        }
    }

    @Transactional(readOnly = false )
    public Order checkout(String userId, OrderForm orderForm) {
        User user = new User();
        Optional<UserRepresentation> userRepresentationOptional = keycloakService.getUserById(userId);
        if (userRepresentationOptional.isPresent() ) {
            user.setId(userRepresentationOptional.get().getId());
        }else{
            throw new CustomException("User not is not found");
        }

        Order pendingCart = orderRepository.findByUserIdAndOrderStatus(userId, OrderStatus.PENDING);

        if( orderForm.getRecipientName()!=null && orderForm.getShippingAddress() != null && orderForm.getPhoneNumber()!=null){
            Order newOrder = pendingCart;
            newOrder.setDateCreated(LocalDateTime.now());
            newOrder.setOrderStatus(OrderStatus.PROCESSING);
            newOrder.setRecipientName(orderForm.getRecipientName());
            newOrder.setShippingAddress(orderForm.getShippingAddress());
            newOrder.setPhoneNumber(orderForm.getPhoneNumber());
            newOrder.setTotalAmount(pendingCart.getTotalPrice());
            orderRepository.save(newOrder);
            Order newPendingCart = new Order();
            newPendingCart.setUser(user);
            newPendingCart.setOrderStatus(OrderStatus.PENDING);
            orderRepository.save(newPendingCart);
            return newOrder;
        } else {
            throw new CustomException("Error in checking out.");
        }
    }


    public List<Order> getAllOrdersOfUser(String userId) {
        return orderRepository.findAllByUserId(userId);
    }


    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderStatus();
    }

    public Order updateOrderStatus(Long orderId, OrderStatusDTO orderStatus) {
        Optional<Order> orderOptional = orderRepository.findById(orderId);
        if(orderOptional.isPresent()) {
            Order updateOrder = orderOptional.get();
            updateOrder.setOrderStatus(orderStatus.getOrderStatus());
            return  orderRepository.save(updateOrder);
        }else{
            throw new CustomException("Order status can not be updated.");
        }
    }
}
