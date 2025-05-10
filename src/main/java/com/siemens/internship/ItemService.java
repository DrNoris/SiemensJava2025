package com.siemens.internship;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;

    private static ExecutorService executor = Executors.newFixedThreadPool(10);


    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }


    /**
     * Your Tasks
     * Identify all concurrency and asynchronous programming issues in the code
     * Fix the implementation to ensure:
     * All items are properly processed before the CompletableFuture completes
     * Thread safety for all shared state
     * Proper error handling and propagation
     * Efficient use of system resources
     * Correct use of Spring's @Async annotation
     * Add appropriate comments explaining your changes and why they fix the issues
     * Write a brief explanation of what was wrong with the original implementation
     *
     * Hints
     * Consider how CompletableFuture composition can help coordinate multiple async operations
     * Think about appropriate thread-safe collections
     * Examine how errors are handled and propagated
     * Consider the interaction between Spring's @Async and CompletableFuture
     */


    /**
     * Asynchronously processes all items in the database by:
     * -Fetching each item by ID
     * -Updating its status to "PROCESSED"
     * -Saving it back to the repository
     *
     * Returns a CompletableFuture that completes only after ALL items are processed.
     * Successfully processed items are returned in the final result list.
     *
     * ******Before******
     * -Did not wait for all async tasks to complete (returned too early)
     * -Used shared mutable state (processedItems, processedCount) not thread-safe
     * -No proper error handling (exceptions were swallowed or ignored)
     * -Used @Async incorrectly because method returned List<Item>, not CompletableFuture
     *
     * ******After******
     * -Uses CompletableFuture.allOf(...) to await completion of all async tasks
     * -Avoids shared mutable state
     * -Exceptions are logged and filtered out (nulls are removed from final list)
     * -Method correctly returns a CompletableFuture<List<Item>>
     */
    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {
        List<Long> itemIds = itemRepository.findAllIds();

        List<CompletableFuture<Item>> futures = itemIds.stream()
                .map(id -> CompletableFuture.supplyAsync(() -> {
                    try {
                        Item item = itemRepository.findById(id).orElseThrow();
                        item.setStatus("PROCESSED");
                        return itemRepository.save(item);
                    } catch (Exception e) {
                        System.err.println("Failed to process item ID: " + id);
                        return null;
                    }
                }, executor))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
    }
}

