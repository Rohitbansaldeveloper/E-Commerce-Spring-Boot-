package com.scaler.productservicefeb25.services;

import com.scaler.productservicefeb25.exceptions.ProductNotFoundException;
import com.scaler.productservicefeb25.models.Category;
import com.scaler.productservicefeb25.dtos.CachedProduct;
import com.scaler.productservicefeb25.models.Product;
import com.scaler.productservicefeb25.repositories.CategoryRepository;
import com.scaler.productservicefeb25.repositories.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import com.scaler.productservicefeb25.specifications.ProductSpecifications;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service("selfProductService")
@org.springframework.context.annotation.Primary
public class SelfProductService implements ProductService {
    private static final String CACHE_PREFIX = "product:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public SelfProductService(ProductRepository productRepository,
                              CategoryRepository categoryRepository,
                              RedisTemplate<String, Object> redisTemplate) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Product getProductById(Long productId) throws ProductNotFoundException {
        String key = CACHE_PREFIX + productId;
        Object cachedValue = redisTemplate.opsForValue().get(key);
        if (cachedValue instanceof CachedProduct cached) return cached.toProduct();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product with id: " + productId + " doesn't exist."));
        redisTemplate.opsForValue().set(key, CachedProduct.from(product), CACHE_TTL);
        return product;
    }

    @Override
    public Page<Product> getAllProducts(int pageNumber, int pageSize) {
        if (pageNumber < 0 || pageSize <= 0 || pageSize > 100) {
            throw new IllegalArgumentException("pageNumber must be >= 0 and pageSize must be between 1 and 100");
        }
        Sort sort = Sort.by("title").ascending().and(Sort.by("price").ascending());
        return productRepository.findAll(PageRequest.of(pageNumber, pageSize, sort));
    }

    @Override
    public Page<Product> searchProducts(String title, String category, Double minPrice, Double maxPrice,
                                        int pageNumber, int pageSize, String sortBy, String direction) {
        if (pageNumber < 0 || pageSize <= 0 || pageSize > 100) {
            throw new IllegalArgumentException("pageNumber must be >= 0 and pageSize must be between 1 and 100");
        }
        if (minPrice != null && minPrice < 0) {
            throw new IllegalArgumentException("minPrice cannot be negative");
        }
        if (maxPrice != null && maxPrice < 0) {
            throw new IllegalArgumentException("maxPrice cannot be negative");
        }
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new IllegalArgumentException("minPrice cannot be greater than maxPrice");
        }

        Specification<Product> specification = Specification.where(null);
        if (title != null && !title.isBlank()) {
            specification = specification.and(ProductSpecifications.titleContains(title));
        }
        if (category != null && !category.isBlank()) {
            specification = specification.and(ProductSpecifications.categoryEquals(category));
        }
        if (minPrice != null) {
            specification = specification.and(ProductSpecifications.priceGreaterThanOrEqual(minPrice));
        }
        if (maxPrice != null) {
            specification = specification.and(ProductSpecifications.priceLessThanOrEqual(maxPrice));
        }

        String allowedSort = switch (sortBy == null ? "title" : sortBy) {
            case "title", "price", "id" -> sortBy == null ? "title" : sortBy;
            default -> throw new IllegalArgumentException("sortBy must be one of: title, price, id");
        };
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction)
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        return productRepository.findAll(specification,
                PageRequest.of(pageNumber, pageSize, Sort.by(sortDirection, allowedSort)));
    }

    @Override
    public Product createProduct(Product product) {
        if (product.getTitle() == null || product.getTitle().isBlank()) {
            throw new IllegalArgumentException("Product title is required");
        }
        if (product.getPrice() < 0) throw new IllegalArgumentException("Product price cannot be negative");
        Category category = product.getCategory();
        if (category == null || category.getName() == null || category.getName().isBlank()) {
            throw new IllegalArgumentException("Product category is required");
        }
        Category finalCategory = categoryRepository.findByName(category.getName()).orElseGet(() -> categoryRepository.save(category));
        product.setCategory(finalCategory);
        return productRepository.save(product);
    }

    @Override
    public Product replaceProduct(Long id, Product product) throws ProductNotFoundException {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product with id: " + id + " doesn't exist."));
        if (product.getTitle() == null || product.getTitle().isBlank()) throw new IllegalArgumentException("Product title is required");
        if (product.getPrice() < 0) throw new IllegalArgumentException("Product price cannot be negative");
        Category category = product.getCategory();
        if (category == null || category.getName() == null || category.getName().isBlank()) throw new IllegalArgumentException("Product category is required");
        Category finalCategory = categoryRepository.findByName(category.getName()).orElseGet(() -> categoryRepository.save(category));
        existing.setTitle(product.getTitle());
        existing.setImageUrl(product.getImageUrl());
        existing.setDescription(product.getDescription());
        existing.setPrice(product.getPrice());
        existing.setCategory(finalCategory);
        Product saved = productRepository.save(existing);
        evict(id);
        return saved;
    }

    @Override
    public Product patchProduct(Long id, Product product) throws ProductNotFoundException {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product with id: " + id + " doesn't exist."));
        if (product.getTitle() != null) existing.setTitle(product.getTitle());
        if (product.getImageUrl() != null) existing.setImageUrl(product.getImageUrl());
        if (product.getDescription() != null) existing.setDescription(product.getDescription());
        if (product.getPrice() > 0) existing.setPrice(product.getPrice());
        if (product.getCategory() != null && product.getCategory().getName() != null) {
            Category category = categoryRepository.findByName(product.getCategory().getName())
                    .orElseGet(() -> categoryRepository.save(product.getCategory()));
            existing.setCategory(category);
        }
        Product saved = productRepository.save(existing);
        evict(id);
        return saved;
    }

    @Override
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) return;
        productRepository.deleteById(id);
        evict(id);
    }

    private void evict(Long id) {
        redisTemplate.delete(CACHE_PREFIX + id);
    }
}
