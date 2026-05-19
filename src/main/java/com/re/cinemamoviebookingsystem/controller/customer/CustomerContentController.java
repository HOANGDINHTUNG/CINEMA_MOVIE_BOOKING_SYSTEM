package com.re.cinemamoviebookingsystem.controller.customer;

import com.re.cinemamoviebookingsystem.service.StaticContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerContentController {

    private final StaticContentService staticContentService;

    @GetMapping("/promotions")
    public String promotions(Model model) {
        model.addAttribute("articles", staticContentService.listPromotions());
        return "customer/promotions";
    }

    @GetMapping("/promotions/{id}")
    public String promotionDetail(@PathVariable String id, Model model) {
        return staticContentService.findPromotion(id)
                .map(article -> {
                    model.addAttribute("article", article);
                    return "customer/promotion-detail";
                })
                .orElse("redirect:/customer/promotions");
    }

    @GetMapping("/news")
    public String news(Model model) {
        model.addAttribute("articles", staticContentService.listNews());
        return "customer/news";
    }

    @GetMapping("/news/{id}")
    public String newsDetail(@PathVariable String id, Model model) {
        return staticContentService.findNews(id)
                .map(article -> {
                    model.addAttribute("article", article);
                    return "customer/news-detail";
                })
                .orElse("redirect:/customer/news");
    }

    @GetMapping("/festival")
    public String festival(Model model) {
        model.addAttribute("articles", staticContentService.listFestivals());
        return "customer/festival";
    }

    @GetMapping("/festival/{id}")
    public String festivalDetail(@PathVariable String id, Model model) {
        return staticContentService.findFestival(id)
                .map(article -> {
                    model.addAttribute("article", article);
                    return "customer/festival-detail";
                })
                .orElse("redirect:/customer/festival");
    }

    @GetMapping("/ticket-price")
    public String ticketPrice() {
        return "customer/ticket-price";
    }
}
