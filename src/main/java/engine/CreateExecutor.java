package engine;

import command.CreateCommand;

public class CreateExecutor {
    CreateCommand command;
    public CreateExecutor(CreateCommand command) {
        this.command = command;
    }

    public void execute() {}
}
