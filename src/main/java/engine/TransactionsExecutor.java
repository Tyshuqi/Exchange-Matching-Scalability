package engine;

import command.CreateCommand;
import command.TransactionsCommand;

public class TransactionsExecutor {
    TransactionsCommand command;
    public TransactionsExecutor(TransactionsCommand command) {
        this.command = command;
    }

    public void execute() {}


}
