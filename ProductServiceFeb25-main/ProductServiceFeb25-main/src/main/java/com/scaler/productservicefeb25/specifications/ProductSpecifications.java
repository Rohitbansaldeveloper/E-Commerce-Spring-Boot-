package com.scaler.productservicefeb25.specifications;

import com.scaler.productservicefeb25.models.Product;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {
    private ProductSpecifications() {}

    public static Specification<Product> titleContains(String title) {
        return (root, query, cb) -> cb.like(
                cb.lower(root.get("title")),
                "%" + title.trim().toLowerCase() + "%"
        );
    }

    public static Specification<Product> categoryEquals(String category) {
        return (root, query, cb) -> cb.equal(
                cb.lower(root.join("category").get("name")),
                category.trim().toLowerCase()
        );
    }

    public static Specification<Product> priceGreaterThanOrEqual(Double minPrice) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Product> priceLessThanOrEqual(Double maxPrice) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }
}
