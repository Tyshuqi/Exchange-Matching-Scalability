package entity;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Position {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sym;

    private int amount;

    @ManyToMany(mappedBy = "positions")
    private Set<Account> accounts = new HashSet<>();

}
