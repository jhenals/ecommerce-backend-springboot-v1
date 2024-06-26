package com.progetto.ecommercebackend.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.progetto.ecommercebackend.support.enums.OrderStatus;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "Order")
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "date_order")
    private LocalDateTime dateCreated;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status")
    private OrderStatus orderStatus;

    @Column(name = "total_amount")
    private Double totalAmount;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(name = "shipping_address")
    private String shippingAddress;

    @Column(name = "phone_number")
    private String phoneNumber;

    @JsonManagedReference
    @Valid
    @OneToMany(mappedBy = "pk.order", orphanRemoval = true)
    private List<OrderBook> orderBooks = new ArrayList<>();

    @Transient
    public Double getTotalPrice(){
        double sum= 0D;
        List<OrderBook> orderBooks = getOrderBooks();
        for(OrderBook ob : orderBooks ){
            sum += ob.getBookFinalPrice();
        }
        return sum;
    }

    @Transient
    public int getNumberOfBooksInCart(){
        return this.orderBooks.size();
    }

}