package engine;

import command.CreateCommand;
import command.TransactionsCommand;
import utils.EntityManagement;

import javax.persistence.EntityManager;

public class TransactionsExecutor {
    TransactionsCommand command;
    public TransactionsExecutor(TransactionsCommand command) {
        this.command = command;
    }

    public void execute() {

    }

    public void executeOrder() {
        EntityManager entityManager = EntityManagement.getEntityManager();
    }


}
