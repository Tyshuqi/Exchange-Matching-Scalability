package engine;

import command.CancelCommand;
import command.OrderCommand;
import command.QueryCommand;
import command.TransactionsCommand;
import entity.Account;
import entity.Order;
import entity.Position;
import entity.Transaction;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import utils.EntityManagement;
import utils.XMLResponseGenerator;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TransactionsExecutor {
    TransactionsCommand command;

    public TransactionsExecutor(TransactionsCommand command) {
        this.command = command;
    }

    public String execute() {
        try {
            Document responseDocument = XMLResponseGenerator.generateResponseDocument();
            EntityManager entityManager = EntityManagement.getEntityManager();

            Account account;
            try {
                entityManager.getTransaction().begin();
                account = entityManager.find(Account.class, Long.parseLong(command.getAccount()), LockModeType.PESSIMISTIC_READ);
                entityManager.getTransaction().commit();
            } finally {
                entityManager.close();
            }

            if (account != null) {
                for (Object subCommand : command.getCommands()) {
                    if (subCommand instanceof OrderCommand) {
                        Element orderResponse = executeOrder((OrderCommand) subCommand, responseDocument);
                        responseDocument.getDocumentElement().appendChild(orderResponse);
                    } else if (subCommand instanceof CancelCommand) {
                        Element cancelResponse = executeCancel((CancelCommand) subCommand, responseDocument);
                        responseDocument.getDocumentElement().appendChild(cancelResponse);
                    } else if (subCommand instanceof QueryCommand) {
                        Element queryResponse = executeQuery((QueryCommand) subCommand, responseDocument);
                        responseDocument.getDocumentElement().appendChild(queryResponse);
                    }
                }
            }
            else {
                Element error = XMLResponseGenerator.generateErrorResponseWithId(responseDocument, command.getAccount(), "Invalid Account ID");
                for (Object ignored : command.getCommands()) {
                    responseDocument.getDocumentElement().appendChild(error.cloneNode(true));
                }
            }
            return XMLResponseGenerator.convertToString(responseDocument);
        }
        catch (ParserConfigurationException | TransformerException e) {
            return "<error>Unexpected XML Parser Error</error>";
        }
    }

    private Element executeOrder(OrderCommand orderCommand, Document responseDocument) {
        EntityManager entityManager = EntityManagement.getEntityManager();
        try {
            entityManager.getTransaction().begin();
            Order newOrder = new Order(orderCommand);
            Account newOrderAccount = entityManager.find(Account.class, Long.parseLong(command.getAccount()), LockModeType.PESSIMISTIC_WRITE);

            entityManager.persist(newOrder);
            newOrder.setAccount(newOrderAccount);

            if (newOrder.getAmount() >= 0) {
                double cost = - newOrder.getAmount() * newOrder.getLimitPrice();
                if (newOrderAccount.getBalance() >= cost) {
                    newOrderAccount.setBalance(newOrderAccount.getBalance() - cost);
                }
                else {
                    entityManager.getTransaction().rollback();
                    return XMLResponseGenerator.generateOrderErrorResponse(responseDocument, orderCommand, "Rejected for insufficient funds.");
                }
            }
            else {
                Position position = newOrderAccount.getPositions().stream()
                        .filter(pos -> orderCommand.getSym().equals(pos.getSym()))
                        .findFirst()
                        .orElse(null);

                String errorMessage = null;
                if (position != null) {
                    entityManager.lock(position, LockModeType.PESSIMISTIC_WRITE);
                    if (position.getAmount() >= orderCommand.getAmount()){
                        position.setAmount(position.getAmount() - orderCommand.getAmount());
                        entityManager.merge(position);
                    }
                    else {
                        errorMessage = "Rejected for insufficient shares.";
                    }
                }
                else {
                    errorMessage = "Rejected for nonexistent position.";
                }

                if (errorMessage != null) {
                    entityManager.getTransaction().rollback();
                    return XMLResponseGenerator.generateOrderErrorResponse(responseDocument, orderCommand, errorMessage);
                }
            }

            List<Order> matchingOrders = this.getMatchingOrders(entityManager, newOrder);
            for (Order order : matchingOrders) {
                int tradeAmount = Math.min(Math.abs(newOrder.getAmount()), Math.abs(order.getAmount()));
                newOrder.setAmount(newOrder.getAmount() + (newOrder.getAmount() >= 0 ? -tradeAmount : tradeAmount));
                order.setAmount(order.getAmount() + (order.getAmount() >= 0 ? -tradeAmount : tradeAmount));

                long createdAt = Instant.now().getEpochSecond();

                Transaction transactionForMatch = new Transaction(tradeAmount, order.getLimitPrice(), createdAt, order);
                order.addTransaction(transactionForMatch);
                entityManager.persist(transactionForMatch);

                Transaction transactionForNewOrder = new Transaction(tradeAmount, order.getLimitPrice(), createdAt, newOrder);
                order.addTransaction(transactionForNewOrder);
                entityManager.persist(transactionForNewOrder);

                Account orderAccount = order.getAccount();
                entityManager.lock(orderAccount, LockModeType.PESSIMISTIC_WRITE);
                double balanceChange = newOrder.getAmount() >= 0 ? order.getLimitPrice() * tradeAmount : -(order.getLimitPrice() * tradeAmount);
                orderAccount.setBalance(orderAccount.getBalance() + balanceChange);
                if (newOrder.getAmount() >= 0 && order.getLimitPrice() < newOrder.getLimitPrice()){
                    newOrderAccount.setBalance(newOrderAccount.getBalance() + (newOrder.getLimitPrice() - order.getLimitPrice()) * tradeAmount);
                }
                entityManager.merge(orderAccount);

                if (order.getAmount() == 0) {
                    order.setStatus(Order.Status.EXECUTED);
                }
                entityManager.merge(order);

                if (newOrder.getAmount() == 0) {
                    newOrder.setStatus(Order.Status.EXECUTED);
                    break;
                }
            }

            entityManager.getTransaction().commit();

            return XMLResponseGenerator.generateOpenedResponse(responseDocument, newOrder.getId(), orderCommand.getSym(), orderCommand.getAmount(), orderCommand.getLimit());
        }
        catch (Exception e) {
            e.printStackTrace();
            entityManager.getTransaction().rollback();
            return XMLResponseGenerator.generateErrorResponse(responseDocument, "Transaction Error");
        } finally {
            entityManager.close();
        }
    }

    private List<Order> getMatchingOrders(EntityManager entityManager, Order order) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Order> cq = cb.createQuery(Order.class);
        Root<Order> orderRoot = cq.from(Order.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(orderRoot.get("symbol"), order.getSymbol()));
        predicates.add(cb.equal(orderRoot.get("status"), Order.Status.OPEN));

        if (order.getAmount() >= 0) {
            predicates.add(cb.lessThanOrEqualTo(orderRoot.get("limitPrice"), order.getLimitPrice()));
            predicates.add(cb.lessThan(orderRoot.get("amount"), 0));
            cq.orderBy(cb.asc(orderRoot.get("limitPrice")), cb.desc(orderRoot.get("id")));
        } else {
            predicates.add(cb.greaterThanOrEqualTo(orderRoot.get("limitPrice"), order.getLimitPrice()));
            predicates.add(cb.greaterThan(orderRoot.get("amount"), 0));
            cq.orderBy(cb.desc(orderRoot.get("limitPrice")), cb.desc(orderRoot.get("id")));
        }

        cq.select(orderRoot).where(cb.and(predicates.toArray(new Predicate[0])));

        TypedQuery<Order> query = entityManager.createQuery(cq).setLockMode(LockModeType.PESSIMISTIC_WRITE);

        return query.getResultList();
    }

    private Element executeCancel(CancelCommand cancelCommand, Document responseDocument) {
        EntityManager entityManager = EntityManagement.getEntityManager();
        try {
            Element response;
            entityManager.getTransaction().begin();
            Order order = entityManager.find(Order.class, Long.parseLong(cancelCommand.getId()), LockModeType.PESSIMISTIC_WRITE);

            if (order != null) {
                if (order.getStatus() == Order.Status.OPEN) {
                    order.cancel();
                    response = XMLResponseGenerator.generateStatusByOrder(responseDocument, order, true);
                } else {
                    response = XMLResponseGenerator.generateErrorResponseWithId(responseDocument, cancelCommand.getId(), "Order closed.");
                }
            }
            else {
                response = XMLResponseGenerator.generateErrorResponseWithId(responseDocument, cancelCommand.getId(), "Order not exist.");
            }

            entityManager.getTransaction().commit();
            return response;
        }
        catch (Exception e) {
            e.printStackTrace();
            entityManager.getTransaction().rollback();
            return XMLResponseGenerator.generateErrorResponse(responseDocument, "Transaction Error");
        } finally {
            entityManager.close();
        }
    }

    private Element executeQuery(QueryCommand queryCommand, Document responseDocument) {
        EntityManager entityManager = EntityManagement.getEntityManager();
        try {
            Element response;
            entityManager.getTransaction().begin();
            Order order = entityManager.find(Order.class, Long.parseLong(queryCommand.getId()), LockModeType.PESSIMISTIC_READ);
            if (order != null) {
                response = XMLResponseGenerator.generateStatusByOrder(responseDocument, order, false);
            }
            else {
                response = XMLResponseGenerator.generateErrorResponseWithId(responseDocument, queryCommand.getId(), "Order not exist.");
            }

            entityManager.getTransaction().commit();
            return response;
        }
        catch (Exception e) {
            e.printStackTrace();
            entityManager.getTransaction().rollback();
            return XMLResponseGenerator.generateErrorResponse(responseDocument, "Transaction Error");
        } finally {
            entityManager.close();
        }
    }
}
