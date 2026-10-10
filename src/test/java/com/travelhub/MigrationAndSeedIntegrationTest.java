package com.travelhub;

import static org.assertj.core.api.Assertions.*;

import com.travelhub.config.DevDataInitializer;

import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Transactional
class MigrationAndSeedIntegrationTest extends PostgresIntegrationTest {
    @Autowired Flyway flyway;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired PasswordEncoder encoder;
    @Autowired Validator validator;
    @Autowired Clock clock;
    @Autowired ApplicationContext context;

    @Test
    void flywayOwnsSchemaAndNonDevProfilesDoNotSeedData() {
        flyway.validate();
        assertThat(flyway.info().applied()).hasSize(6);
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(context.getBeansOfType(DevDataInitializer.class)).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from users", Long.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from role_permissions", Long.class))
                .isGreaterThan(19);
    }

    @Test
    void devSeedIsIdempotentAndPasswordsAreHashed() throws Exception {
        var seed = new DevDataInitializer(em, encoder, validator, clock);
        ReflectionTestUtils.setField(seed, "adminEmail", "seed-admin@example.test");
        ReflectionTestUtils.setField(seed, "adminPassword", "seed-test-password");
        seed.run(new DefaultApplicationArguments());
        seed.run(new DefaultApplicationArguments());
        assertThat(jdbc.queryForObject("select count(*) from destinations", Long.class))
                .isEqualTo(6);
        assertThat(jdbc.queryForObject("select count(*) from places", Long.class)).isEqualTo(8);
        assertThat(jdbc.queryForObject("select count(*) from categories", Long.class))
                .isEqualTo(10);
        assertThat(jdbc.queryForObject("select count(*) from reviews", Long.class)).isEqualTo(16);
        assertThat(jdbc.queryForObject("select count(*) from bookings", Long.class)).isEqualTo(2);
        String hash =
                jdbc.queryForObject(
                        "select password from users where email='seed-admin@example.test'",
                        String.class);
        assertThat(encoder.matches("seed-test-password", hash)).isTrue();
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from places where average_rating=4.50 and"
                                    + " review_count=2",
                                Long.class))
                .isEqualTo(8);
    }

    @Test
    void databaseEnforcesDefaultAddressAndRatingConstraints() {
        Long user =
                jdbc.queryForObject(
                        "insert into users(email,password,full_name) values"
                            + " ('constraints@example.test','test-only-hash','Test') returning id",
                        Long.class);
        Long destination =
                jdbc.queryForObject(
                        "insert into destinations(name,country) values ('Test','Vietnam') returning"
                            + " id",
                        Long.class);
        Long place =
                jdbc.queryForObject(
                        "insert into places(destination_id,name) values (?, 'Test') returning id",
                        Long.class,
                        destination);
        jdbc.update(
                "insert into addresses(user_id,recipient_name,phone,address_line,is_default) values"
                    + " (?,'Test','12345','Street',true)",
                user);
        reject(
                "insert into addresses(user_id,recipient_name,phone,address_line,is_default) values"
                    + " ("
                        + user
                        + ",'Test','12345','Street',true)",
                "23505");
        reject(
                "insert into reviews(user_id,place_id,rating,content) values ("
                        + user
                        + ","
                        + place
                        + ",6,'Invalid')",
                "23514");
        reject(
                "insert into trips(user_id,name,start_date,end_date) values ("
                        + user
                        + ",'Invalid','2026-10-12','2026-10-11')",
                "23514");
    }

    private void reject(String sql, String state) {
        jdbc.execute(
                (ConnectionCallback<Void>)
                        connection -> {
                            var savepoint = connection.setSavepoint();
                            try (var statement = connection.createStatement()) {
                                assertThatThrownBy(() -> statement.execute(sql))
                                        .isInstanceOf(java.sql.SQLException.class)
                                        .satisfies(
                                                ex ->
                                                        assertThat(
                                                                        ((java.sql.SQLException) ex)
                                                                                .getSQLState())
                                                                .isEqualTo(state));
                            } finally {
                                connection.rollback(savepoint);
                                connection.releaseSavepoint(savepoint);
                            }
                            return null;
                        });
    }
}
