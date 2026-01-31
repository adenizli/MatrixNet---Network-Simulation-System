/**
 * Max-heap priority queue with multi-criteria comparison and lexicographic
 * tie-breaking. Supports O(log n) insertion, extraction, and updates with index
 * tracking.
 * 
 * @param <Payload> the type of elements stored in the queue
 */
public class PriorityQueue<Payload> {
    /**
     * Internal node storing priority scores and payload.
     * 
     * @param <Payload> the type of payload
     * @param value array of priority scores
     * @param payload the stored element
     */
    private record PriorityQueueNode<Payload>(int[] value, Payload payload) {
    }

    /**
     * Configuration constants for the priority queue.
     */
    private static class Config {
        static final int INITIAL_HEAP_SIZE = 1024;
    }

    final private int numberOfCriteria;
    private final boolean isMaxHeap;
    private PriorityQueueNode<Payload>[] heap;
    private int nextAvailableIndex;
    private int size;

    /**
     * Creates a new priority queue with specified number of priority criteria.
     * 
     * @param numberOfCriteria the number of priority scores per element
     */
    public PriorityQueue(int numberOfCriteria) {
        this(numberOfCriteria, true);
    }

    /**
     * Creates a new priority queue with specified number of priority criteria and
     * heap direction.
     * 
     * @param numberOfCriteria the number of priority scores per element
     * @param isMaxHeap true for max-heap behavior, false for min-heap
     */
    public PriorityQueue(int numberOfCriteria, boolean isMaxHeap) {
        this.heap = new PriorityQueueNode[Config.INITIAL_HEAP_SIZE];
        this.size = Config.INITIAL_HEAP_SIZE;
        this.nextAvailableIndex = 1;
        this.numberOfCriteria = numberOfCriteria;
        this.isMaxHeap = isMaxHeap;
    }

    /**
     * Inserts an element with specified priority scores.
     * 
     * @param scores array of priority values
     * @param payload the element to insert
     */
    public void insert(int[] scores, Payload payload) {
        Indexable indexable = (Indexable) payload;

        heap[nextAvailableIndex] = new PriorityQueueNode<>(scores, payload);
        ((Indexable) payload).setCurrentIndex(nextAvailableIndex);

        perculateUp(nextAvailableIndex);
        nextAvailableIndex++;

        if (nextAvailableIndex >= size) {
            extendHeap();
        }
    }

    /**
     * Returns the root element without removing it (max for max-heap, min for
     * min-heap).
     * 
     * @return the root element, or null if empty
     */
    public Payload peek() {
        if (this.nextAvailableIndex == 1) return null;
        return heap[1].payload();
    }

    /**
     * Extracts and returns the root element (max for max-heap, min for min-heap).
     * 
     * @return the root element, or null if empty
     */
    public Payload extract() {
        if (this.nextAvailableIndex == 1) return null;

        Payload p = heap[1].payload();

        nextAvailableIndex--;
        heap[1] = heap[nextAvailableIndex];
        heap[nextAvailableIndex] = null;
        ((Indexable) p).setCurrentIndex(-1);

        // Update the index of the element that was moved to the root
        if (heap[1] != null) {
            ((Indexable) heap[1].payload()).setCurrentIndex(1);
        }

        if (nextAvailableIndex > 1) {
            perculateDown(1);
        }

        return p;
    }

    /**
     * Restores heap property by moving element up the tree.
     * 
     * @param index the starting index
     */
    private void perculateUp(int index) {
        if (index <= 1) return;
        int parentIndex = index / 2;

        int comparison = this.compare(index, parentIndex);
        if (comparison == 1) {
            swapNodes(index, parentIndex);
            perculateUp(parentIndex);
        }

    }

    private int compare(int index1, int index2) {
        PriorityQueueNode<Payload> positive = heap[index1];
        PriorityQueueNode<Payload> negative = heap[index2];

        return applyHeapDirection(this.compareBase(positive, negative));
    }

    /**
     * Compares two nodes based on priority criteria and lexicographic tie-breaking.
     * 
     * @param positive the first node
     * @param negative the second node
     * @return 1 if positive > negative, -1 if positive < negative, 0 if equal
     */
    private int compare(PriorityQueueNode<Payload> positive, PriorityQueueNode<Payload> negative) {
        return applyHeapDirection(this.compareBase(positive, negative));
    }

    /**
     * Base comparison independent of heap direction.
     */
    private int compareBase(PriorityQueueNode<Payload> positive, PriorityQueueNode<Payload> negative) {
        // Compare all criteria in order
        for (int i = 0; i < numberOfCriteria; i++) {
            if (positive.value()[i] > negative.value()[i]) {
                return 1;
            } else if (positive.value()[i] < negative.value()[i]) {
                return -1;
            }
        }

        if (positive.payload() instanceof Indexable && negative.payload() instanceof Indexable) {
            String positiveKey = ((Indexable) positive.payload()).getLexoCompareKey();
            String negativeKey = ((Indexable) negative.payload()).getLexoCompareKey();
            int lexoCompare = positiveKey.compareTo(negativeKey);
            // Base ordering: lexicographically larger key is considered "greater".
            // Heap direction (max vs min) is applied later in applyHeapDirection().
            if (lexoCompare > 0) return 1;
            if (lexoCompare < 0) return -1;
        }

        return 0;
    }

    private int applyHeapDirection(int comparison) {
        if (!isMaxHeap && comparison != 0) return -comparison;
        return comparison;
    }

    /**
     * Restores heap property by moving element down the tree.
     * 
     * @param index the starting index
     */
    private void perculateDown(int index) {
        boolean leftIsNull;
        if (2 * index >= this.nextAvailableIndex) leftIsNull = true;
        else leftIsNull = heap[2 * index] == null;

        boolean rightIsNull;
        if (2 * index + 1 >= this.nextAvailableIndex) rightIsNull = true;
        else rightIsNull = heap[2 * index + 1] == null;

        boolean noChild = leftIsNull && rightIsNull;

        if (noChild) return;
        if (!leftIsNull && !rightIsNull) {
            int childComparison = this.compare(2 * index, 2 * index + 1);
            if (childComparison == 1) {
                int comparison = this.compare(index, 2 * index);
                if (comparison == -1) {
                    swapNodes(index, 2 * index);
                    perculateDown(2 * index);
                }

            } else {
                int comparison = this.compare(index, 2 * index + 1);
                if (comparison == -1) {
                    swapNodes(index, 2 * index + 1);
                    perculateDown(2 * index + 1);
                }
            }
        } else if (!leftIsNull) {
            int comparison = this.compare(index, 2 * index);
            if (comparison == -1) {
                swapNodes(index, 2 * index);
                perculateDown(2 * index);
            }
        } else {
            int comparison = this.compare(index, 2 * index + 1);
            if (comparison == -1) {
                swapNodes(index, 2 * index + 1);
                perculateDown(2 * index + 1);
            }
        }

    }

    private void swapNodes(int index1, int index2) {
        PriorityQueueNode<Payload> temp = heap[index1];
        ((Indexable) temp.payload()).setCurrentIndex(index2);
        heap[index1] = heap[index2];
        ((Indexable) heap[index2].payload()).setCurrentIndex(index1);
        heap[index2] = temp;
    }

    /**
     * Doubles the heap capacity when needed.
     */
    public void extendHeap() {
        int initialSize = this.nextAvailableIndex - 1;
        this.size = this.size * 2;

        PriorityQueueNode<Payload>[] initialHeap = this.heap;
        this.heap = new PriorityQueueNode[this.size];

        for (int i = 1; i <= initialSize; i++) {
            this.heap[i] = initialHeap[i];
        }
    }

    /**
     * Updates the priority of an element at the specified index.
     * 
     * @param index the element's current index
     * @param newValue the new priority scores
     */
    public void handleIndexUpdate(int index, int[] newValue) {
        PriorityQueueNode<Payload> initialNode = heap[index];
        heap[index] = new PriorityQueueNode<>(newValue, initialNode.payload());

        int comparison = this.compare(heap[index], initialNode);

        switch (comparison) {
        case 1 -> perculateUp(index);
        case -1 -> perculateDown(index);
        default -> ((Indexable) initialNode.payload()).setCurrentIndex(index);
        }
    }

    /**
     * Removes the element at the specified index from the queue.
     * 
     * @param index the index of the element to remove
     */
    public void removeByIndex(int index) {
        if (index < 1 || index >= nextAvailableIndex) return;

        PriorityQueueNode<Payload> removedNode = heap[index];
        ((Indexable) removedNode.payload()).setCurrentIndex(-1);

        // If removing the last element, just decrease nextAvailableIndex
        if (index == nextAvailableIndex - 1) {
            heap[index] = null;
            nextAvailableIndex--;
            return;
        }

        // Replace with last element
        nextAvailableIndex--;
        PriorityQueueNode<Payload> lastNode = heap[nextAvailableIndex];
        heap[nextAvailableIndex] = null;

        if (lastNode != null) {
            heap[index] = lastNode;
            ((Indexable) lastNode.payload()).setCurrentIndex(index);

            // Compare replacement with removed node to decide direction
            // Similar to handleIndexUpdate logic
            int comparison = this.compare(lastNode, removedNode);
            switch (comparison) {
            case 1 -> perculateUp(index); // Replacement is better, move up
            case -1 -> perculateDown(index); // Replacement is worse, move down
            default -> {
            } // Equal priority, stay in place
            }
        }
    }

    public boolean isEmpty() {
        return this.nextAvailableIndex == 1;
    }
}
