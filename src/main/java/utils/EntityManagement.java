package utils;

import javax.persistence.*;

public class EntityManagement {
    private static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("myPersistenceUnit");
    private static final ThreadLocal<EntityManager> threadLocalEntityManager = new ThreadLocal<>();

    public static EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public static void shutdown() {
        if (emf != null) {
            emf.close();
        }
    }
}
