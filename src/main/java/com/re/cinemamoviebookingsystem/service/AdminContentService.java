package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.dto.response.ContentArticleDto;
import com.re.cinemamoviebookingsystem.enums.ContentCategory;
import com.re.cinemamoviebookingsystem.exception.BusinessException;
import com.re.cinemamoviebookingsystem.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminContentService {

    private final ContentArticleService contentArticleService;
    private final AuditLogService auditLogService;

    public List<ContentArticleDto> list(ContentCategory category) {
        return contentArticleService.list(category);
    }

    public Optional<ContentArticleDto> find(ContentCategory category, String id) {
        return contentArticleService.find(category, id);
    }

    @Transactional
    public void save(ContentCategory category, ContentArticleDto article, boolean isNew) {
        contentArticleService.save(category, article, isNew);
        auditLogService.log(isNew ? "CONTENT_CREATE" : "CONTENT_UPDATE", "CONTENT",
                article.getId(), category.name() + ": " + article.getTitle());
    }

    @Transactional
    public void delete(ContentCategory category, String id) {
        if (contentArticleService.find(category, id).isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Không tìm thấy bài viết");
        }
        contentArticleService.delete(category, id);
        auditLogService.log("CONTENT_DELETE", "CONTENT", id, category.name());
    }
}
