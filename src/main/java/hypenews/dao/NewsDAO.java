package hypenews.dao;

import javax.ejb.Asynchronous;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import hypenews.entity.News;

@Singleton
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
@Asynchronous
public class NewsDAO {
	
	@PersistenceContext(unitName = "hypenews")
	EntityManager						entityManager;

	public void persist(News news) {
		entityManager.persist(news);
	}

}
