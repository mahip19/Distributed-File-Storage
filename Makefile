# Compiler and flags
CXX = g++
CXXFLAGS = -std=c++17 -Wall -Wextra -g -I./include
LDFLAGS = -lssl -lcrypto -lpthread

# Directories
SRC_DIR = src
COMMON_DIR = $(SRC_DIR)/common
TEST_DIR = tests
BUILD_DIR = build

# Common source files (shared utilities)
COMMON_SRCS = $(COMMON_DIR)/FileUtils.cpp \
              $(COMMON_DIR)/HashUtils.cpp \
              $(COMMON_DIR)/TCPSocket.cpp \
              $(COMMON_DIR)/ConsistentHash.cpp

COMMON_OBJS = $(BUILD_DIR)/FileUtils.o \
              $(BUILD_DIR)/HashUtils.o \
              $(BUILD_DIR)/TCPSocket.o \
              $(BUILD_DIR)/ConsistentHash.o \
			  $(BUILD_DIR)/NodeConfig.o

# Test executables
TEST_BINS = $(BUILD_DIR)/test_chunking \
            $(BUILD_DIR)/test_hashing \
            $(BUILD_DIR)/verify_files \
            $(BUILD_DIR)/test_tcp \
            $(BUILD_DIR)/verify_pipeline \
            $(BUILD_DIR)/test_consistent_hash

# Default target: build all tests
all: $(BUILD_DIR) $(TEST_BINS)

# Create build directory
$(BUILD_DIR):
	mkdir -p $(BUILD_DIR)

# Compile common object files
$(BUILD_DIR)/FileUtils.o: $(COMMON_DIR)/FileUtils.cpp
	$(CXX) $(CXXFLAGS) -c $< -o $@

$(BUILD_DIR)/HashUtils.o: $(COMMON_DIR)/HashUtils.cpp
	$(CXX) $(CXXFLAGS) -c $< -o $@

$(BUILD_DIR)/TCPSocket.o: $(COMMON_DIR)/TCPSocket.cpp
	$(CXX) $(CXXFLAGS) -c $< -o $@

$(BUILD_DIR)/ConsistentHash.o: $(COMMON_DIR)/ConsistentHash.cpp
	$(CXX) $(CXXFLAGS) -c $< -o $@

# Build test executables
$(BUILD_DIR)/test_chunking: $(TEST_DIR)/test_chunking.cpp $(COMMON_OBJS)
	$(CXX) $(CXXFLAGS) $^ -o $@ $(LDFLAGS)

$(BUILD_DIR)/test_hashing: $(TEST_DIR)/test_hashing.cpp $(COMMON_OBJS)
	$(CXX) $(CXXFLAGS) $^ -o $@ $(LDFLAGS)

$(BUILD_DIR)/verify_files: $(TEST_DIR)/verify_files.cpp $(COMMON_OBJS)
	$(CXX) $(CXXFLAGS) $^ -o $@ $(LDFLAGS)

$(BUILD_DIR)/test_tcp: $(TEST_DIR)/test_tcp.cpp $(COMMON_OBJS)
	$(CXX) $(CXXFLAGS) $^ -o $@ $(LDFLAGS)

$(BUILD_DIR)/verify_pipeline: $(TEST_DIR)/verify_pipeline.cpp $(COMMON_OBJS)
	$(CXX) $(CXXFLAGS) $^ -o $@ $(LDFLAGS)

$(BUILD_DIR)/test_consistent_hash: $(TEST_DIR)/test_consistent_hash.cpp $(COMMON_OBJS)
	$(CXX) $(CXXFLAGS) $^ -o $@ $(LDFLAGS)

# Convenience targets for individual tests
chunking: $(BUILD_DIR) $(BUILD_DIR)/test_chunking
hashing: $(BUILD_DIR) $(BUILD_DIR)/test_hashing
verify: $(BUILD_DIR) $(BUILD_DIR)/verify_files
tcp: $(BUILD_DIR) $(BUILD_DIR)/test_tcp
dht: $(BUILD_DIR) $(BUILD_DIR)/test_consistent_hash

# Clean build artifacts
clean:
	rm -rf $(BUILD_DIR)

# Run all tests
test: all
	@echo "=== Running test_chunking ==="
	./$(BUILD_DIR)/test_chunking
	@echo "\n=== Running test_hashing ==="
	./$(BUILD_DIR)/test_hashing
	@echo "\n=== Running test_consistent_hash ==="
	./$(BUILD_DIR)/test_consistent_hash

# Help
help:
	@echo "Available targets:"
	@echo "  all       - Build all test executables (default)"
	@echo "  chunking  - Build only test_chunking"
	@echo "  hashing   - Build only test_hashing"
	@echo "  verify    - Build only verify_files"
	@echo "  tcp       - Build only test_tcp"
	@echo "  dht       - Build only test_consistent_hash"
	@echo "  test      - Build and run all tests"
	@echo "  clean     - Remove all build artifacts"
	@echo "  help      - Show this message"

.PHONY: all clean test help chunking hashing verify tcp dht