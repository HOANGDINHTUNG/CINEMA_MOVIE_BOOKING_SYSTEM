package com.re.cinemamoviebookingsystem.repository;

import com.re.cinemamoviebookingsystem.entity.ContentArticle;
import com.re.cinemamoviebookingsystem.enums.ContentCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContentArticleRepository extends JpaRepository<ContentArticle, String> {

    List<ContentArticle> findByCategoryOrderByPublishedDateDescTitleAsc(ContentCategory category);

    Optional<ContentArticle> findByCategoryAndId(ContentCategory category, String id);

    boolean existsByCategoryAndId(ContentCategory category, String id);
}
