package com.scaler.productservicefeb25.dtos;

import com.scaler.productservicefeb25.models.Category;
import com.scaler.productservicefeb25.models.Product;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CachedProduct {
    private Long id; private String title; private String description; private String imageUrl; private double price;
    private Long categoryId; private String categoryName;

    public static CachedProduct from(Product p) {
        Category c=p.getCategory();
        return new CachedProduct(p.getId(),p.getTitle(),p.getDescription(),p.getImageUrl(),p.getPrice(),
                c==null?null:c.getId(),c==null?null:c.getName());
    }
    public Product toProduct() {
        Product p=new Product(); p.setId(id); p.setTitle(title); p.setDescription(description); p.setImageUrl(imageUrl); p.setPrice(price);
        if(categoryId!=null || categoryName!=null){ Category c=new Category(); c.setId(categoryId); c.setName(categoryName); p.setCategory(c); }
        return p;
    }
}
