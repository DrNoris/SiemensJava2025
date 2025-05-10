package com.siemens.internship;

import com.siemens.internship.Domain.Item;
import com.siemens.internship.Repository.ItemRepository;
import com.siemens.internship.Service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItemServiceIntegrationTest {

    @InjectMocks
    private ItemService itemService;

    @Mock
    private ItemRepository itemRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(ItemService.class, "executor", java.util.concurrent.Executors.newFixedThreadPool(10));
    }

    @Test
    void testFindAllItems() {
        List<Item> items = Arrays.asList(new Item(1L, "Item1", "Desc", "NEW", "a@a.com"));
        when(itemRepository.findAll()).thenReturn(items);

        List<Item> result = itemService.findAll();

        assertEquals(1, result.size());
        verify(itemRepository, times(1)).findAll();
    }

    @Test
    void testFindByIdSuccess() {
        Item item = new Item(1L, "Item1", "Desc", "NEW", "a@a.com");
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        Optional<Item> result = itemService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("Item1", result.get().getName());
    }

    @Test
    void testSaveItem() {
        Item item = new Item(null, "Item1", "Desc", "NEW", "a@a.com");
        Item savedItem = new Item(1L, "Item1", "Desc", "NEW", "a@a.com");

        when(itemRepository.save(item)).thenReturn(savedItem);

        Item result = itemService.save(item);

        assertNotNull(result.getId());
        verify(itemRepository, times(1)).save(item);
    }

    @Test
    void testDeleteItem() {
        itemService.deleteById(1L);
        verify(itemRepository, times(1)).deleteById(1L);
    }

    @Test
    void testProcessItemsAsync() throws Exception {
        // Setup fake items and repo behavior
        when(itemRepository.findAllIds()).thenReturn(Arrays.asList(1L, 2L));

        Item item1 = new Item(1L, "Item1", "Desc", "NEW", "a@a.com");
        Item item2 = new Item(2L, "Item2", "Desc", "NEW", "b@b.com");

        when(itemRepository.findById(1L)).thenReturn(Optional.of(item1));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(item2));

        when(itemRepository.save(any(Item.class))).thenAnswer(i -> i.getArgument(0)); // Echo save

        CompletableFuture<List<Item>> future = itemService.processItemsAsync();
        List<Item> processedItems = future.get();

        assertEquals(2, processedItems.size());
        assertEquals("PROCESSED", processedItems.get(0).getStatus());
        assertEquals("PROCESSED", processedItems.get(1).getStatus());

        verify(itemRepository, times(2)).save(any(Item.class));
    }

    @Test
    void testProcessItemsAsync_withOneFailingItem() throws Exception {
        when(itemRepository.findAllIds()).thenReturn(Arrays.asList(1L, 2L));

        Item item1 = new Item(1L, "Item1", "Desc", "NEW", "a@a.com");

        when(itemRepository.findById(1L)).thenReturn(Optional.of(item1));
        when(itemRepository.findById(2L)).thenThrow(new RuntimeException("DB error"));

        when(itemRepository.save(any(Item.class))).thenAnswer(i -> i.getArgument(0));

        CompletableFuture<List<Item>> future = itemService.processItemsAsync();
        List<Item> processedItems = future.get();

        assertEquals(1, processedItems.size());
        assertEquals(1L, processedItems.get(0).getId());
        assertEquals("PROCESSED", processedItems.get(0).getStatus());
    }
}
