package entity;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Account {
    @Id
    private Long id;

    @Column(nullable = false)
    private Double balance;

    @ManyToMany
    @JoinTable(
            name = "account_symbol",
            joinColumns = @JoinColumn(name = "account_id"),
            inverseJoinColumns = @JoinColumn(name = "symbol_id"))
    private Set<Symbol> symbols = new HashSet<>();
}
