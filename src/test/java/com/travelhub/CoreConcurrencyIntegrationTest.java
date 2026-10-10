package com.travelhub;
import com.travelhub.address.dto.AddressCreateRequest;
import com.travelhub.address.entity.Address;
import com.travelhub.address.service.AddressService;
import com.travelhub.common.exception.DuplicateResourceException;
import com.travelhub.destination.entity.Destination;
import com.travelhub.favorite.entity.*;
import com.travelhub.favorite.service.FavoriteService;
import com.travelhub.place.entity.Place;
import com.travelhub.user.entity.User;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;
class CoreConcurrencyIntegrationTest extends PostgresIntegrationTest {
    @Autowired AddressService addresses;
    @Autowired FavoriteService favorites;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager manager;
    @Test void concurrentDefaultsRemainUnique() throws Exception {
        Long[] ids=fixture();
        try {
            runTogether(() -> addresses.create(ids[0],new AddressCreateRequest("First","12345678","Street",null,null,null,null,true)),
                    () -> addresses.create(ids[0],new AddressCreateRequest("Second","12345678","Street",null,null,null,null,true)));
            tx().executeWithoutResult(s -> assertThat(em.createQuery("select count(a) from Address a where a.user.id=:id and a.defaultAddress=true",Long.class)
                    .setParameter("id",ids[0]).getSingleResult()).isEqualTo(1));
        } finally {cleanup(ids);}
    }
    @Test void concurrentDuplicateFavoriteHasOneWinner() throws Exception {
        Long[] ids=fixture();AtomicInteger success=new AtomicInteger(),duplicates=new AtomicInteger();
        Runnable add=() -> {try{favorites.add(ids[0],ids[1]);success.incrementAndGet();}catch(DuplicateResourceException ex){duplicates.incrementAndGet();}};
        try {runTogether(add,add);assertThat(success.get()).isEqualTo(1);assertThat(duplicates.get()).isEqualTo(1);}
        finally {cleanup(ids);}
    }
    private void runTogether(Runnable... actions) throws Exception {
        CountDownLatch start=new CountDownLatch(1);
        try(var delegate=Executors.newFixedThreadPool(actions.length)) {
            var pool=new org.springframework.security.concurrent.DelegatingSecurityContextExecutorService(delegate);
            List<Future<?>> futures=new ArrayList<>();
            for(var action:actions) futures.add(pool.submit(() -> {try{start.await(10,TimeUnit.SECONDS);action.run();}catch(InterruptedException ex){Thread.currentThread().interrupt();throw new RuntimeException(ex);}}));
            start.countDown();for(var f:futures)f.get(20,TimeUnit.SECONDS);
        }
    }
    private TransactionTemplate tx(){return new TransactionTemplate(manager);}
    private Long[] fixture(){return tx().execute(s -> {
        User u=new User();u.setEmail(UUID.randomUUID()+"@example.test");u.setFullName("Concurrent");u.setPassword("test-only");em.persist(u);
        Destination d=new Destination();d.setName("Hanoi");d.setCountry("Vietnam");em.persist(d);
        Place p=new Place();p.setName("Lake");p.setDestination(d);em.persist(p);return new Long[]{u.getId(),p.getId(),d.getId()};
    });}
    private void cleanup(Long[] ids){tx().executeWithoutResult(s -> {
        em.createQuery("select a from Address a where a.user.id=:id",Address.class).setParameter("id",ids[0]).getResultList().forEach(em::remove);
        Favorite f=em.find(Favorite.class,new FavoriteId(ids[0],ids[1]));if(f!=null)em.remove(f);em.flush();
        em.remove(em.find(Place.class,ids[1]));em.flush();em.remove(em.find(Destination.class,ids[2]));em.remove(em.find(User.class,ids[0]));
    });}
}
