package com.re.cinemamoviebookingsystem.controller.admin;

import com.re.cinemamoviebookingsystem.dto.response.ContentArticleDto;
import com.re.cinemamoviebookingsystem.enums.ContentCategory;
import com.re.cinemamoviebookingsystem.service.AdminContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/content")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminContentController {

    private final AdminContentService adminContentService;

    @GetMapping
    public String index() {
        return "admin/content/index";
    }

    @GetMapping("/{category}")
    public String list(@PathVariable String category, Model model) {
        ContentCategory cat = parseCategory(category);
        model.addAttribute("category", cat);
        model.addAttribute("articles", adminContentService.list(cat));
        return "admin/content/list";
    }

    @GetMapping("/{category}/new")
    public String createForm(@PathVariable String category, Model model) {
        ContentCategory cat = parseCategory(category);
        model.addAttribute("category", cat);
        model.addAttribute("article", new ContentArticleDto());
        model.addAttribute("isNew", true);
        return "admin/content/form";
    }

    @GetMapping("/{category}/{id}/edit")
    public String editForm(@PathVariable String category, @PathVariable String id, Model model) {
        ContentCategory cat = parseCategory(category);
        var article = adminContentService.find(cat, id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài"));
        model.addAttribute("category", cat);
        model.addAttribute("article", article);
        model.addAttribute("isNew", false);
        return "admin/content/form";
    }

    @PostMapping("/{category}")
    public String save(@PathVariable String category,
                       @ModelAttribute ContentArticleDto article,
                       @RequestParam(defaultValue = "false") boolean isNew,
                       RedirectAttributes redirectAttributes) {
        ContentCategory cat = parseCategory(category);
        try {
            adminContentService.save(cat, article, isNew);
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu nội dung");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return isNew ? "redirect:/admin/content/" + category + "/new"
                    : "redirect:/admin/content/" + category + "/" + article.getId() + "/edit";
        }
        return "redirect:/admin/content/" + category;
    }

    @PostMapping("/{category}/{id}/delete")
    public String delete(@PathVariable String category, @PathVariable String id,
                         RedirectAttributes redirectAttributes) {
        ContentCategory cat = parseCategory(category);
        try {
            adminContentService.delete(cat, id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa bài viết");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/content/" + category;
    }

    private ContentCategory parseCategory(String category) {
        return ContentCategory.valueOf(category.toUpperCase());
    }
}
