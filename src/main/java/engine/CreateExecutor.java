package engine;

import command.CreateAccountCommand;
import command.CreateCommand;
import command.SymbolAccountCommand;
import command.SymbolCommand;
import entity.Account;
import entity.Position;
import org.w3c.dom.Document;
import utils.EntityManagement;
import utils.XMLResponseGenerator;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.util.List;

public class CreateExecutor {
    CreateCommand command;

    public CreateExecutor(CreateCommand command) {
        this.command = command;
    }

    public String execute() {
        try {
            Document responseDocument = XMLResponseGenerator.generateResponseDocument();
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
            return XMLResponseGenerator.convertToString(responseDocument);
        }
        catch (ParserConfigurationException | TransformerException e) {
            return "<error>Unexpected XML Parser Error</error>";
        }
    }
    public void handleSymbolCommand(SymbolCommand symbolCommand) {
        String symbol = symbolCommand.getSym();
        List<SymbolAccountCommand> accounts = symbolCommand.getAccounts();

        //Handle the case where there are no accounts
        if (accounts == null || accounts.isEmpty()) {
            System.out.println("No accounts specified for symbol: " + symbol);
            return;
        }
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
            }
            Position existingPosition = findPositionBySymbol(targetAccount, symbol);
            // Position exists, update shares
            if (existingPosition != null) {
                updateExistingPosition(existingPosition, account.getShares());
            }
            // No existing Position, proceed to persist a new Position
            else {
                createAndPersistNewPosition(entityManager, targetAccount, symbol, account.getShares());
            }
            // Commit if all operations are successful
            transaction.commit();
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

    private Position findPositionBySymbol(Account account, String symbol) {
        return account.getPositions().stream()
                .filter(pos -> symbol.equals(pos.getSym()))
                .findFirst()
                .orElse(null);
    }

    private void updateExistingPosition(Position position, int sharesToAdd) {
        position.setAmount(position.getAmount() + sharesToAdd);
    }

    private void createAndPersistNewPosition(EntityManager entityManager, Account account, String symbol, int shares) {
        Position newPosition = new Position();
        newPosition.setSym(symbol);
        newPosition.setAmount(shares);
        newPosition.addAccount(account);
        entityManager.persist(newPosition);
    }

    public void handleAccountCommand(CreateAccountCommand accountCommand) {
        // TODO add err handling or type check
        String accountID = accountCommand.getId();
        if (accountID == null || accountID.trim().isEmpty()) {
            throw new IllegalArgumentException("Account ID cannot be null or empty.");
        }
        double balance = accountCommand.getBalance();
        if (balance < 0) {
            throw new IllegalArgumentException("Balance cannot be negative.");
        }
        // cast to long for persistent
        long accountIDLong;
        try {
            accountIDLong = Long.parseLong(accountID);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Account ID must be a valid long number.", e);
        }
        insertNewAcc(accountIDLong, balance);
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
                // Account already exists
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
