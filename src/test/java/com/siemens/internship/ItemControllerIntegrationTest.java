package com.siemens.internship;

import com.siemens.internship.Domain.Item;
import com.siemens.internship.Repository.ItemRepository;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ItemControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ItemRepository itemRepository;

    @BeforeEach
    public void setup() {
        itemRepository.deleteAll();
        itemRepository.save(new Item(null, "Item 1", "nieonvoertnv", "PENDING", "fdafsaf@gmail.com"));
        itemRepository.save(new Item(null, "Item 2", "vnrpevnep", "PENDING", "fdasfda@yahoo.com"));
    }

    //Controller tests
    //Test GET
    @Test
    public void testGetAllItems() {
        ResponseEntity<List> response = restTemplate.exchange("/api/items", HttpMethod.GET, null, List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
        assertThat(((List<Item>) response.getBody()).size()).isEqualTo(2);
    }

    //Test POST
    @Test
    public void testCreateItem() {
        Item newItem = new Item(null, "New Item", "New Description", "PENDING", "newemail@example.com");
        ResponseEntity<Item> response = restTemplate.postForEntity("/api/items", newItem, Item.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    //Test POST invalid
    @Test
    public void testCreateItemInvalid() {
        Item newItem = new Item(null, "New Item", "New Description", "PENDING", "invalid-email");
        ResponseEntity<String> response = restTemplate.postForEntity("/api/items", newItem, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    //Test GET item by ID
    @Test
    public void testGetItemById() {
        Item savedItem = itemRepository.findAll().get(0);
        ResponseEntity<Item> response = restTemplate.getForEntity("/api/items/" + savedItem.getId(), Item.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    //Test GET item by ID invalid
    @Test
    public void testGetItemByIdInvalid() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/items/999", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    //Test PUT
    @Test
    public void testUpdateItem() {
        Item savedItem = itemRepository.findAll().get(0);
        savedItem.setName("Updated Name");
        HttpEntity<Item> request = new HttpEntity<>(savedItem);
        ResponseEntity<Item> response = restTemplate.exchange("/api/items/" + savedItem.getId(), HttpMethod.PUT, request, Item.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // Test PUT invalid
    @Test
    public void testUpdateItemInvalid() {
        Item nonExistingItem = new Item(999L, "Non-Existing Item", "Description", "PENDING", "nonexisting@example.com");
        HttpEntity<Item> request = new HttpEntity<>(nonExistingItem);
        ResponseEntity<String> response = restTemplate.exchange("/api/items/999", HttpMethod.PUT, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // Test DELETE
    @Test
    public void testDeleteItem() {
        Item savedItem = itemRepository.findAll().get(0);
        ResponseEntity<Void> response = restTemplate.exchange("/api/items/" + savedItem.getId(), HttpMethod.DELETE, null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(itemRepository.existsById(savedItem.getId())).isFalse();
    }

    // Test DELETE invalid
    @Test
    public void testDeleteItemInvalid() {
        ResponseEntity<String> response = restTemplate.exchange("/api/items/999", HttpMethod.DELETE, null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }


    //Tests for Process Items
    @Test
    public void testProcessItems() {
        ResponseEntity<Item[]> response = restTemplate.getForEntity("/api/items/process", Item[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Item[] items = response.getBody();
        assertThat(items).isNotNull();
        assertThat(items.length).isEqualTo(2);

        for (Item item : items) {
            assertThat(item.getStatus()).isEqualTo("PROCESSED");
        }

        List<Item> dbItems = itemRepository.findAll();
        assertThat(dbItems).allMatch(i -> i.getStatus().equals("PROCESSED"));
    }


    //Tests for email validation
    @Test
    public void testCreateItem_withValidEmail() {
        Item validItem = new Item(null, "Item Valid", "Description", "PENDING", "validemail@example.com");
        ResponseEntity<Item> response = restTemplate.postForEntity("/api/items", validItem, Item.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getEmail()).isEqualTo("validemail@example.com");
    }

    @Test
    public void testCreateItem_withInvalidEmail() {
        Item invalidItem = new Item(null, "Item Invalid", "Description", "PENDING", "invalidemail.com");

        try {
            ResponseEntity<Item> response = restTemplate.postForEntity("/api/items", invalidItem, Item.class);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(ConstraintViolationException.class);
        }
    }


}
