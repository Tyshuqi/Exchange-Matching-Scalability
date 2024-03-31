package engine;

import command.CancelCommand;
import command.OrderCommand;
import command.TransactionsCommand;
import entity.Account;
import entity.Order;
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
            for (Object subCommand : command.getCommands()) {
                if (subCommand instanceof OrderCommand) {
                    Element orderResponse = executeOrder((OrderCommand) subCommand, responseDocument);
                    responseDocument.getDocumentElement().appendChild(orderResponse);
                } else if (subCommand instanceof CancelCommand) {
                    executeCancel((CancelCommand) subCommand, responseDocument);
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
                newOrderAccount.setBalance(newOrderAccount.getBalance() - balanceChange);
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
        } catch (Exception e) {
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
            cq.orderBy(cb.asc(orderRoot.get("limitPrice")), cb.asc(orderRoot.get("id")));
        } else {
            predicates.add(cb.greaterThanOrEqualTo(orderRoot.get("limitPrice"), order.getLimitPrice()));
            predicates.add(cb.greaterThan(orderRoot.get("amount"), 0));
            cq.orderBy(cb.desc(orderRoot.get("limitPrice")), cb.asc(orderRoot.get("id")));
        }

        cq.select(orderRoot).where(cb.and(predicates.toArray(new Predicate[0])));

        TypedQuery<Order> query = entityManager.createQuery(cq).setLockMode(LockModeType.PESSIMISTIC_WRITE);

        return query.getResultList();
    }

    private void executeCancel(CancelCommand cancelCommand, Document responseDocument) {
        EntityManager entityManager = EntityManagement.getEntityManager();
        try {
            entityManager.getTransaction().begin();
            Order order = entityManager.find(Order.class, Long.parseLong(cancelCommand.getId()), LockModeType.PESSIMISTIC_WRITE);
            if (order.getStatus() == Order.Status.OPEN) {
                order.cancel();
            }

            entityManager.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
            entityManager.getTransaction().rollback();
        } finally {
            entityManager.close();
        }
    }
}
