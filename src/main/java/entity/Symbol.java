package entity;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Symbol {
    @Id
    private String sym;

    @ManyToMany(mappedBy = "symbols")
    private Set<Account> accounts = new HashSet<>();

}
