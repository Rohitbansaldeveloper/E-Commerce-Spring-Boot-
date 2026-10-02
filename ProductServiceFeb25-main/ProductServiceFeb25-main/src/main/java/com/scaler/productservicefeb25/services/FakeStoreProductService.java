package com.scaler.productservicefeb25.services;

import com.scaler.productservicefeb25.dtos.FakeStoreProductDto;
import com.scaler.productservicefeb25.exceptions.ProductNotFoundException;
import com.scaler.productservicefeb25.models.Category;
import com.scaler.productservicefeb25.models.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service("fakeStoreProductService")
public class FakeStoreProductService implements ProductService {
    private static final String BASE_URL = "https://fakestoreapi.com/products";
    private static final String CACHE = "PRODUCTS";

    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    public FakeStoreProductService(RestTemplate restTemplate, RedisTemplate<String, Object> redisTemplate) {
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
    }

    private Product convert(FakeStoreProductDto dto) {
        Product product = new Product();
        product.setId(dto.getId());
        product.setTitle(dto.getTitle());
        product.setImageUrl(dto.getImage());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        Category category = new Category();
        category.setName(dto.getCategory());
        product.setCategory(category);
        return product;
    }

    private FakeStoreProductDto convertToDto(Product product) {
        FakeStoreProductDto dto = new FakeStoreProductDto();
        dto.setId(product.getId());
        dto.setTitle(product.getTitle());
        dto.setImage(product.getImageUrl());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setCategory(product.getCategory() == null ? null : product.getCategory().getName());
        return dto;
    }

    @Override
    public Product getProductById(Long productId) throws ProductNotFoundException {
        Product cached = (Product) redisTemplate.opsForHash().get(CACHE, "PRODUCT_" + productId);
        if (cached != null) return cached;

        ResponseEntity<FakeStoreProductDto> response = restTemplate.getForEntity(
                BASE_URL + "/" + productId, FakeStoreProductDto.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new ProductNotFoundException("Product with id: " + productId + " doesn't exist.");
        }

        Product product = convert(response.getBody());
        redisTemplate.opsForHash().put(CACHE, "PRODUCT_" + productId, product);
        return product;
    }

    @Override
    public Page<Product> getAllProducts(int pageNumber, int pageSize) {
        if (pageNumber < 0 || pageSize <= 0) {
            throw new IllegalArgumentException("pageNumber must be >= 0 and pageSize must be > 0");
        }
        FakeStoreProductDto[] dtos = restTemplate.getForObject(BASE_URL, FakeStoreProductDto[].class);
        List<Product> products = new ArrayList<>();
        if (dtos != null) {
            for (FakeStoreProductDto dto : dtos) products.add(convert(dto));
        }
        int from = Math.min(pageNumber * pageSize, products.size());
        int to = Math.min(from + pageSize, products.size());
        return new PageImpl<>(products.subList(from, to), PageRequest.of(pageNumber, pageSize), products.size());
    }

    @Override
    public Product createProduct(Product product) {
        FakeStoreProductDto response = restTemplate.postForObject(BASE_URL, convertToDto(product), FakeStoreProductDto.class);
        return response == null ? product : convert(response);
    }

    @Override
    public Product replaceProduct(Long id, Product product) throws ProductNotFoundException {
        ResponseEntity<FakeStoreProductDto> response = restTemplate.exchange(
                BASE_URL + "/" + id, HttpMethod.PUT, new HttpEntity<>(convertToDto(product)), FakeStoreProductDto.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new ProductNotFoundException("Product with id: " + id + " doesn't exist.");
        }
        redisTemplate.opsForHash().delete(CACHE, "PRODUCT_" + id);
        return convert(response.getBody());
    }

    @Override
    public void deleteProduct(Long id) {
        restTemplate.delete(BASE_URL + "/" + id);
        redisTemplate.opsForHash().delete(CACHE, "PRODUCT_" + id);
    }
}
