package org.surino.untraceable.controller;

import org.springframework.data.jpa.repository.JpaRepository;
import org.surino.untraceable.model.Person;

public interface PersonRepository extends JpaRepository<Person, Long> {}
