package com.example.SpringBatchTutorial.core.domain.accounts;

import com.example.SpringBatchTutorial.core.domain.orders.Orders;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;

@NoArgsConstructor
@Getter
@ToString
@Entity
public class Accounts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String orderItem;
    private Integer price;
    private Date orderDate;
    private Date accountDate;

    public Accounts(Orders orders) {
        //this.id = orders.getId(); 중복 키 에러가 나게 되어 DB가 자동으로 생성되게 수정
        this.orderItem = orders.getOrderItem();
        this.price = orders.getPrice();
        this.orderDate = orders.getOrderDate();
        this.accountDate = new Date();
    }

}
