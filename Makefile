.PHONY: build test run clean

SOURCES := $(shell find src/main/java -name '*.java')
TEST_SOURCES := $(shell find src/test/java -name '*.java')

build:
	mkdir -p out/main
	javac --release 21 -d out/main $(SOURCES)

test: build
	mkdir -p out/test
	javac --release 21 -cp out/main -d out/test $(TEST_SOURCES)
	java -ea -cp out/main:out/test com.kaysonmirain.telemetry.EventProcessorTest

run: build
	java -cp out/main com.kaysonmirain.telemetry.Main --input data/sample_telemetry.csv

clean:
	rm -rf out
