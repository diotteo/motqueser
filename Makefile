JAVA ?= java
JAVA_ARGS ?= #-agentlib:jdwp=transport=dt_socket,server=y,suspend=n
JAVAC ?= javac
JAVAC_ARGS ?= -Xlint:unchecked

PRGM := motqueser
PKG := ca/dioo/java/motqueser
ROOT_DIR := $(dir $(lastword $(MAKEFILE_LIST)))
BUILD_DIR := $(ROOT_DIR)/build
BPATH := $(BUILD_DIR)/$(PKG)
empty :=
space := $(empty) $(empty)

src := $(wildcard src/*.java)
objects := $(patsubst src/%.java,$(BPATH)/%.class,$(src))
libs = $(wildcard libs/*.jar)

test_src := $(wildcard test/*.java)
test_objects := $(patsubst test/%.java,$(BUILD_DIR)/%.class,$(test_src))
test_libs := $(wildcard test/libs/*.jar)


all:


.PHONY: dist
dist: jar
	@[ -d dist/$(PRGM) ] || mkdir -p dist/$(PRGM)
	cp $(PRGM).jar libs/*.jar dist/$(PRGM)/
	cp $(PRGM).sh dist/$(PRGM)/
	cp $(PRGM).conf.sample dist/$(PRGM)/
	cd dist/ && tar -cf $(PRGM).tar $(PRGM)/


.PHONY: jar
jar: $(PRGM).jar


$(PRGM).jar: $(objects)
	jar -cf $@ -C $(BUILD_DIR) .


.PHONY: all
all: $(objects)


$(objects): $(BPATH)/%.class: src/%.java $(libs) $(BUILD_DIR)
	$(JAVAC) $(JAVAC_ARGS) -cp libs/*:$(BUILD_DIR) -d $(BUILD_DIR) $<


.PHONY: run
run: $(objects)
	$(JAVA) $(JAVA_ARGS) -cp libs/*:$(BUILD_DIR) $(subst /,.,$(PKG)/$(PRGM)) $(ARGS)


.PHONY: test
test: $(test_objects)
	$(JAVA) -ea $(JAVA_ARGS) -cp libs/*:test/libs/*:$(BUILD_DIR) Test


$(test_objects): $(BUILD_DIR)/%.class: test/%.java $(libs) $(BUILD_DIR)
	$(JAVAC) $(JAVAC_ARGS) -cp libs/*:$(BUILD_DIR) -d $(BUILD_DIR) $<



$(BUILD_DIR):
	@[ -d $(BUILD_DIR) ] || mkdir -p $(BUILD_DIR)


.PHONY: clean
clean:
	@[ ! -e $(BUILD_DIR) ] || rm -rv $(BUILD_DIR)
	@[ ! -e dist ] || rm -rv dist
	@[ ! -e $(PRGM).jar ] || rm -rv $(PRGM).jar


libs:
	@[ -d libs ] || mkdir libs


.PHONY: libjars
libjars: libs $(libs)


$(libs) $(test_libs):
	$(MAKE) -C $(dir $(shell readlink $@)) jar


$(patsubst %,$(BPATH)/%.class,ServerThread): $(patsubst %,$(BPATH)/%.class,ItemQueue Utils)
$(BPATH)/Item.class: $(patsubst %,$(BPATH)/%.class,Utils)
$(BPATH)/Utils.class: $(patsubst %,$(BPATH)/%.class,Config)
$(BPATH)/ScriptRunnerThread.class: $(patsubst %,$(BPATH)/%.class,Config Utils Item)
$(BPATH)/ItemQueue.class: $(patsubst %,$(BPATH)/%.class,Item ScriptRunnerThread)
$(BPATH)/DisplayThread.class: $(patsubst %,$(BPATH)/%.class,ItemQueue)
$(BPATH)/Motqueser.class: $(patsubst %,$(BPATH)/%.class,ServerThread Utils)
