package com.aerogrid.backend.repository;

import com.aerogrid.backend.domain.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;



@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {

}