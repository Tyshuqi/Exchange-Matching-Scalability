package entity;

import javax.persistence.*;

@Entity
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int shares;
    private double price;
    private long time;

    @ManyToOne(fetch = FetchType.LAZY)
    private Order order;

    // Constructors, getters, and setters
}
