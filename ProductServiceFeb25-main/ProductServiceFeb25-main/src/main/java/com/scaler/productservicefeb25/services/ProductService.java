package com.scaler.productservicefeb25.services;

import com.scaler.productservicefeb25.exceptions.ProductNotFoundException;
import com.scaler.productservicefeb25.models.Product;
import org.springframework.data.domain.Page;

public interface ProductService {
    Product getProductById(Long productId) throws ProductNotFoundException;

    Page<Product> getAllProducts(int pageNumber, int pageSize);

    Page<Product> searchProducts(String title, String category, Double minPrice, Double maxPrice,
                                 int pageNumber, int pageSize, String sortBy, String direction);

    Product createProduct(Product product);

    Product replaceProduct(Long id, Product product) throws ProductNotFoundException;

    Product patchProduct(Long id, Product product) throws ProductNotFoundException;

    void deleteProduct(Long id);
}
