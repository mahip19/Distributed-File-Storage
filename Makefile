# Java Makefile

# Variables
SRC_DIR = src/main/java
OUT_DIR = out
JAVAC = javac
JAVA = java
JFLAGS = -d $(OUT_DIR) -sourcepath $(SRC_DIR)

# Source files
SOURCES = $(wildcard $(SRC_DIR)/com/distributed/storage/common/*.java) \
          $(wildcard $(SRC_DIR)/com/distributed/storage/dht/*.java) \
          $(wildcard $(SRC_DIR)/com/distributed/storage/network/*.java) \
          $(wildcard $(SRC_DIR)/com/distributed/storage/storage/*.java) \
          $(wildcard $(SRC_DIR)/com/distributed/storage/metadata/*.java) \
          $(wildcard $(SRC_DIR)/com/distributed/storage/client/*.java)

# Default target: Compile everything
all: clean compile

# Compile
compile:
	mkdir -p $(OUT_DIR)
	$(JAVAC) $(JFLAGS) $(SOURCES)

# Run System Tests (The "Experiment")
test: compile
	$(JAVA) -cp $(OUT_DIR) com.distributed.storage.client.SystemTests

# Run Client (Interactive/Manual)
client: compile
	$(JAVA) -cp $(OUT_DIR) com.distributed.storage.client.Client

# Clean
clean:
	rm -rf $(OUT_DIR)
	rm -f *.txt
