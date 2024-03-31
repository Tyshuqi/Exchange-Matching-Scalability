package engine;

import command.CreateAccountCommand;
import command.CreateCommand;
import command.SymbolAccountCommand;
import command.SymbolCommand;
import entity.Account;
import entity.Position;
import utils.EntityManagement;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceException;
import javax.swing.text.html.parser.Entity;
import java.util.List;

public class CreateExecutor {
    CreateCommand command;

    public CreateExecutor(CreateCommand command) {
        this.command = command;
    }

    public void execute() {
        List<Object> inputCommands = command.getCommands();
        for (Object command : inputCommands) {
            if (command instanceof CreateAccountCommand) {
                CreateAccountCommand accountCommand = (CreateAccountCommand) command;
                // handle accountCommand
                handleAccountCommand(accountCommand);
            } else if (command instanceof SymbolCommand) {
                // Handle SymbolCommand
                SymbolCommand symbolCommand = (SymbolCommand) command;
                // handle symbolCommand
                handleSymbolCommand(symbolCommand);
            }
        }
    }
    public void handleSymbolCommand(SymbolCommand symbolCommand) {
        String symbol = symbolCommand.getSym();
        List<SymbolAccountCommand> accounts = symbolCommand.getAccounts();
        for (SymbolAccountCommand account : accounts) {
            insertNewShares(account, symbol);
        }
    }

    public void insertNewShares(SymbolAccountCommand account, String symbol) {
        // todo tidy up the code
        EntityManager entityManager = EntityManagement.getEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            // Convert account ID from String to long
            long accountId = Long.parseLong(account.getId());

            // Attempt to find the target Account in the database
            Account targetAccount = entityManager.find(Account.class, accountId, LockModeType.PESSIMISTIC_WRITE);

            if (targetAccount == null) {
                // Target account does not exist, handle this case appropriately
                throw new IllegalStateException("Account with ID " + accountId + " does not exist.");
            } else {
                // Check if a Position with the same symbol already exists for the Account
                Position existingPosition = targetAccount.getPositions().stream()
                        .filter(pos -> symbol.equals(pos.getSym()))
                        .findFirst()
                        .orElse(null);

                if (existingPosition != null) {
                    // Position exists, update shares
                    int totalShares = existingPosition.getAmount() + account.getShares();
                    existingPosition.setAmount(totalShares);
                    // Assuming Position entity is managed, no need to explicitly persist
                } else {
                    // No existing Position, proceed to persist a new Position
                    int shares = account.getShares();
                    Position newPosition = new Position();
                    newPosition.setSym(symbol);
                    newPosition.setAmount(shares);
                    newPosition.addAccount(targetAccount);

                    entityManager.persist(newPosition);
                }
            }

            transaction.commit(); // Commit if all operations are successful
        } catch (NumberFormatException e) {
            // Handle case where account ID is not in a valid long format
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new IllegalArgumentException("Invalid account ID format: " + account.getId(), e);
        } catch (Exception e) {
            // Handle any other exceptions
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Failed to insert new shares due to an unexpected error.", e);
        } finally {
            // Ensure the EntityManager is always closed
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }


    public void handleAccountCommand(CreateAccountCommand accountCommand) {
        // TODO add err handling or type check
        String accountID = accountCommand.getId();
        double balance = accountCommand.getBalance();
        // cast to long for persistent
        long accountIdLong = Long.parseLong(accountID);
        insertNewAcc(accountIdLong, balance);
    }

    public void insertNewAcc(long accountIdLong, double balance) {
        EntityManager entityManager = EntityManagement.getEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();

            Account existedAccount = entityManager.find(Account.class, accountIdLong, LockModeType.PESSIMISTIC_WRITE);
            // Account does not exist
            if (existedAccount == null) {
                Account newAccount = new Account();
                newAccount.setId(accountIdLong);
                newAccount.setBalance(balance);

                entityManager.persist(newAccount);
            } else {
                // Account al ready exists
                System.out.println("Account with ID " + accountIdLong + " already exists.");
            }

            entityManager.getTransaction().commit();
        } catch (PersistenceException e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Failed to insert new account due to persistence error", e);
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Failed to insert new account due to an unexpected error", e);
        }
        finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }
}
