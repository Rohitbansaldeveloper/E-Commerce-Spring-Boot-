package com.example.cartservicefeb25.services;

import com.example.cartservicefeb25.dtos.*;
import com.example.cartservicefeb25.models.Cart;
import com.example.cartservicefeb25.models.CartItem;
import com.example.cartservicefeb25.repositories.CartItemRepository;
import com.example.cartservicefeb25.repositories.CartRepository;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import java.util.*;

@Service
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository itemRepository;
    private final RestTemplate restTemplate;
    private final String orderServiceUrl;
    private final String productServiceUrl;

    public CartService(CartRepository cartRepository, CartItemRepository itemRepository, RestTemplate restTemplate,
                       @Value("${order.service.url}") String orderServiceUrl,
                       @Value("${product.service.url}") String productServiceUrl) {
        this.cartRepository=cartRepository; this.itemRepository=itemRepository; this.restTemplate=restTemplate;
        this.orderServiceUrl=orderServiceUrl; this.productServiceUrl=productServiceUrl;
    }

    @Transactional
    public void addItem(Long userId, Long productId, int quantity) {
        validateUser(userId); validateProduct(productId); if(quantity<=0) throw new IllegalArgumentException("quantity must be positive");
        Cart cart=getOrCreate(userId);
        CartItem item=itemRepository.findByCartIdAndProductId(cart.getId(),productId).orElse(null);
        if(item==null){ item=new CartItem(); item.setCart(cart); item.setProductId(productId); item.setQuantity(quantity); }
        else item.setQuantity(item.getQuantity()+quantity);
        itemRepository.save(item); touch(cart);
    }

    @Transactional(readOnly=true)
    public CartResponse getCart(Long userId) {
        validateUser(userId); Cart cart=cartRepository.findByUserId(userId).orElse(null);
        if(cart==null) return new CartResponse(userId,List.of());
        List<CartItemResponse> items=cart.getItems().stream().filter(i->i.getQuantity()>0)
                .map(i->new CartItemResponse(i.getProductId(),i.getQuantity())).sorted(Comparator.comparing(CartItemResponse::getProductId)).toList();
        return new CartResponse(userId,items);
    }

    @Transactional
    public void updateItem(Long userId, Long productId, UpdateCartItemRequest request) {
        validateUser(userId); validateProduct(productId); if(request==null||request.getQuantity()<=0) throw new IllegalArgumentException("quantity must be positive");
        Cart cart=cartRepository.findByUserId(userId).orElseThrow(()->new IllegalArgumentException("Cart not found"));
        CartItem item=itemRepository.findByCartIdAndProductId(cart.getId(),productId).orElseThrow(()->new IllegalArgumentException("Product is not in the cart"));
        item.setQuantity(request.getQuantity()); itemRepository.save(item); touch(cart);
    }

    @Transactional
    public void removeItem(Long userId, Long productId){ validateUser(userId); Cart cart=cartRepository.findByUserId(userId).orElse(null); if(cart==null)return; itemRepository.findByCartIdAndProductId(cart.getId(),productId).ifPresent(itemRepository::delete); touch(cart); }

    @Transactional
    public void clearCart(Long userId){ validateUser(userId); cartRepository.findByUserId(userId).ifPresent(c->{c.getItems().clear(); c.setStatus("ACTIVE"); touch(c); cartRepository.save(c);}); }

    public CheckoutResponse checkout(Long userId,String idempotencyKey){
        validateUser(userId); if(idempotencyKey==null||idempotencyKey.isBlank()) throw new IllegalArgumentException("Idempotency-Key header is required");
        CartResponse cart=getCart(userId); if(cart.getItems().isEmpty()) throw new IllegalStateException("Cart is empty");
        Map<String,Object> request=Map.of("items",cart.getItems().stream().map(i->Map.of("productId",i.getProductId(),"quantity",i.getQuantity())).toList());
        HttpHeaders h=new HttpHeaders(); h.setContentType(MediaType.APPLICATION_JSON); h.set("Idempotency-Key",idempotencyKey); h.set("X-User-Id",String.valueOf(userId));
        ResponseEntity<Object> response=restTemplate.exchange(orderServiceUrl + "/orders",HttpMethod.POST,new HttpEntity<>(request,h),Object.class);
        if(response.getStatusCode().is2xxSuccessful()){clearCart(userId);return new CheckoutResponse("ORDER_CREATED",response.getBody());}
        throw new IllegalStateException("Unable to create order from cart");
    }

    private Cart getOrCreate(Long userId){ return cartRepository.findByUserId(userId).orElseGet(()->{Cart c=new Cart();c.setUserId(userId);c.setStatus("ACTIVE");c.setCreatedAt(java.time.Instant.now());c.setUpdatedAt(java.time.Instant.now());return cartRepository.save(c);}); }
    private void touch(Cart c){c.setUpdatedAt(java.time.Instant.now());c.setStatus("ACTIVE");cartRepository.save(c);}
    private void validateProduct(Long id){ if(id==null||id<=0) throw new IllegalArgumentException("Valid productId is required"); try{ResponseEntity<Object> r=restTemplate.getForEntity(productServiceUrl + "/products/" + id,Object.class); if(!r.getStatusCode().is2xxSuccessful()) throw new IllegalArgumentException("Product not found: "+id);}catch(org.springframework.web.client.HttpClientErrorException.NotFound e){throw new IllegalArgumentException("Product not found: "+id);} }
    private void validateUser(Long id){if(id==null||id<=0)throw new IllegalArgumentException("Valid user id is required");}
}
